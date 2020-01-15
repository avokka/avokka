package avokka.velocystream

import java.net.InetSocketAddress

import akka.actor._
import akka.io.{IO, Tcp}
import akka.routing._
import akka.util.ByteString
import scodec.{Attempt, Codec, Err}
import scodec.bits.BitVector

import scala.concurrent.duration._
import scala.collection.mutable

/** Velocystream client actor
  *
  */
class VStreamClientTcp(conf: VStreamConfiguration, begin: Iterable[VStreamMessage])
    extends Actor
    with Stash
    with ActorLogging {
  import VStreamClientTcp._
  import context.system

  val manager: ActorRef = IO(Tcp)

  private val address: InetSocketAddress = InetSocketAddress.createUnresolved(conf.host, conf.port)

  override def preStart(): Unit = {
    manager ! Tcp.Connect(address,
      timeout = Some(15.seconds),
      pullMode = true
    )
  }

  def connecting: Receive = {
    case Tcp.CommandFailed(_: Tcp.Connect) =>
      log.debug("connect failed")
      context.stop(self)

    case Tcp.Connected(remote, local) =>
      log.debug("connected remote={} local={}", remote, local)
      val connection = sender()
      connection ! Tcp.Register(self, keepOpenOnPeerClosed = true, useResumeWriting = false)
      context.become(handshaking(connection))
      unstashAll()

    case _ => stash()
  }

  val beginQueue: mutable.Queue[VStreamChunk] = mutable.Queue.empty

  def handshaking(connection: ActorRef): Receive = {
    connection ! Tcp.Write(VStreamFlow.VST_HANDSHAKE, HandshakeAck)
    connection ! Tcp.ResumeReading

    beginQueue.clear()
    beginQueue ++= begin.flatMap(_.chunks())

    receiving(connection) orElse handleFailure(connection) orElse {

      case HandshakeAck =>
        if (beginQueue.isEmpty) {
          waitingForAck = false
          context.become(connected(connection))
          unstashAll()
        } else {
          doSendChunk(connection, beginQueue.dequeue(), HandshakeAck)
        }

      case _ => stash()
    }
  }

  /*
  implicit val chunkOrdering: Ordering[VStreamChunk] = new Ordering[VStreamChunk] {
    override def compare(x: VStreamChunk, y: VStreamChunk): Int = x.messageId compare y.messageId
  }
  val sendQueue: mutable.PriorityQueue[VStreamChunk] = mutable.PriorityQueue.empty
  */
  val sendQueue: mutable.Queue[VStreamChunk] = mutable.Queue.empty
  var waitingForAck: Boolean = false

  private def doSendChunk(connection: ActorRef, chunk: VStreamChunk, ack: Tcp.Event): Unit = {
    log.debug("send chunk #{}-{} {} bytes", chunk.messageId, chunk.x.position, chunk.length)
    val bs = ByteString(VStreamChunk.codec.encode(chunk).require.toByteBuffer)
    connection ! Tcp.Write(bs, ack)
    waitingForAck = true
  }

  def sending(connection: ActorRef): Receive = {
    case MessageSend(m) =>
      log.debug("send message #{} {} bytes", m.id, m.data.length)
      context.actorOf(VStreamMessageActor.props(m.id, sender()), s"message-${m.id}")
      m.chunks() foreach { chunk => self ! ChunkSend(chunk) }

    case ChunkSend(chunk) if waitingForAck => sendQueue.enqueue(chunk)
    case ChunkSend(chunk) => doSendChunk(connection, chunk, WriteAck)

    case WriteAck =>
      if (sendQueue.isEmpty) {
        waitingForAck = false
      } else {
        doSendChunk(connection, sendQueue.dequeue(), WriteAck)
      }
  }

  var recvBuffer: BitVector = BitVector.empty

  def receiving(connection: ActorRef): Receive = {
    case Tcp.Received(data) =>
      log.debug("received data")
      recvBuffer = recvBuffer ++ BitVector(data.asByteBuffer)

      Codec.decodeCollect(VStreamChunk.codec, None)(recvBuffer) match {
        case Attempt.Successful(result) => {
          log.debug("successful decode {}", result.map(_.map(_.messageId)))
          result.value.foreach { chunk =>
            context.child(s"message-${chunk.messageId}").foreach { child =>
              log.debug("send chunk to child {}", child)
              child ! VStreamMessageActor.ChunkReceived(chunk)
            }
          }
          recvBuffer = result.remainder
        }
        case Attempt.Failure(cause: Err.InsufficientBits) =>
          log.debug("insufficent bits needed={} have={}", cause.needed, cause.have)
        case Attempt.Failure(cause) =>
          log.error(cause.toString())
      }
      connection ! Tcp.ResumeReading
  }

  def handleFailure(connection: ActorRef): Receive = {
    case Tcp.CommandFailed(_: Tcp.Write) =>
      // O/S buffer was full
      log.debug("write failed")
      context.stop(self)

    case _: Tcp.ConnectionClosed =>
      log.debug("connection closed")
      context.stop(self)
  }

  def connected(connection: ActorRef): Receive = sending(connection) orElse receiving(connection) orElse handleFailure(connection)

  override def receive: Receive = connecting

}

object VStreamClientTcp {

  final def decider: SupervisorStrategy.Decider = { case t: Throwable ⇒ SupervisorStrategy.Restart }

  val supervisionStrategy: SupervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = -1, withinTimeRange = Duration.Inf, loggingEnabled = true)(
      decider = decider)

  val poolResizer: Resizer = DefaultResizer(
    lowerBound = 1,
    upperBound = 4,
    pressureThreshold = 1,
    messagesPerResize = 1000
  )

  val routerConfig: RouterConfig = SmallestMailboxPool(
    nrOfInstances = 1,
    supervisorStrategy = supervisionStrategy,
    resizer = Some(poolResizer)
  )
  val routerBalanceConfig: RouterConfig = BalancingPool(
    nrOfInstances = 2,
    supervisorStrategy = supervisionStrategy,
  )

  def apply(conf: VStreamConfiguration, begin: Iterable[VStreamMessage]): Props =
    Props(new VStreamClientTcp(conf, begin))//.withRouter(routerConfig)

  case class MessageSend(message: VStreamMessage)
  case class ChunkSend(chunk: VStreamChunk)

  case object HandshakeAck extends Tcp.Event
  case object WriteAck extends Tcp.Event
}
