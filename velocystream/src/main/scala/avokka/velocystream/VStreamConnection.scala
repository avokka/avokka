package avokka.velocystream

import java.net.InetSocketAddress

import akka.actor._
import akka.io.{IO, Tcp}
import akka.pattern.BackoffSupervisor
import akka.routing._
import akka.util.ByteString
import scodec.{Attempt, Codec, Err}
import scodec.bits.BitVector
import cats.syntax.foldable._
import cats.instances.vector._
import cats.instances.list._
import scodec.interop.cats.BitVectorMonoidInstance

import scala.concurrent.duration._
import scala.collection.mutable

/** Velocystream client actor
  *
  */
class VStreamConnection(conf: VStreamConfiguration, begin: Iterable[VStreamMessage])
    extends Actor
//    with Stash
    with ActorLogging {
  import VStreamConnection._
  import context.system

  val manager: ActorRef = IO(Tcp)

  private val address: InetSocketAddress = InetSocketAddress.createUnresolved(conf.host, conf.port)

  override def preStart(): Unit = {
    manager ! Tcp.Connect(address,
      timeout = Some(10.seconds),
     // options = List(Tcp.SO.KeepAlive(true)),
      pullMode = true
    )
  }

  def connecting: Receive = {
    case Tcp.CommandFailed(_: Tcp.Connect) =>
      log.debug("connect failed")
      context.stop(self)

    case Tcp.Connected(remote, local) =>
      log.debug("connected remote={} local={}", remote, local)
      context.parent ! BackoffSupervisor.Reset
      val connection = sender()
      connection ! Tcp.Register(self,
        keepOpenOnPeerClosed = false,
        useResumeWriting = false
      )
      context.become(handshaking(connection))
  //    unstashAll()

  //  case _ => stash()
  }

  val beginQueue: mutable.Queue[VStreamChunk] = mutable.Queue.empty

  def handshaking(connection: ActorRef): Receive = {
    connection ! Tcp.Write(VST_HANDSHAKE, HandshakeAck)

    beginQueue.clear()
    beginQueue ++= begin.flatMap(_.chunks())

    receiving(connection) orElse handleFailure(connection) orElse {

      case HandshakeAck =>
        if (beginQueue.isEmpty) {
          waitingForAck = false
          context.become(connected(connection))
   //       unstashAll()
        } else {
          doSendChunk(connection, beginQueue.dequeue(), HandshakeAck)
        }

   //   case _ => stash()
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

  private def flushChunkQueue(connection: ActorRef, ack: Tcp.Event): Unit = {
    val chunks: Vector[VStreamChunk] = sendQueue.toVector
    log.debug("flush chunk queue #{}-{} {} bytes", chunks.map(c => s"${c.messageId}-${c.x.position}"), chunks.map(_.length))
    val bits: BitVector = chunks.map { chunk =>
      VStreamChunk.codec.encode(chunk).require
    }.reduce(_ ++ _)
    val bs = ByteString(bits.toByteBuffer)
    connection ! Tcp.Write(bs, ack)
    waitingForAck = true
    sendQueue.clear()
  }

  def sending(connection: ActorRef): Receive = {
    case VStreamClient.MessageSend(m) =>
      log.debug("send message #{} {} bytes", m.id, m.data.length)
      context.actorOf(VStreamMessageActor.props(m.id, sender()), s"message-${m.id}")
      m.chunks() foreach { chunk => self ! ChunkSend(chunk) }

    case ChunkSend(chunk) if waitingForAck => {
      log.debug("enqueue chunk to buffer while waiting for write ack")
      sendQueue.enqueue(chunk)
    }
    case ChunkSend(chunk) => doSendChunk(connection, chunk, WriteAck)

    case WriteAck =>
      log.debug("receive write ack")
//      connection ! Tcp.ResumeReading
      if (sendQueue.isEmpty) {
        waitingForAck = false
      } else {
        flushChunkQueue(connection, WriteAck)
        //doSendChunk(connection, sendQueue.dequeue(), WriteAck)
      }
  }

  val recvBuffer: mutable.ListBuffer[BitVector] = mutable.ListBuffer.empty

  def receiving(connection: ActorRef): Receive = {
    connection ! Tcp.ResumeReading

    {
      case Tcp.Received(data) =>
        log.debug("received data")
        recvBuffer += BitVector(data.asByteBuffer)

        val theBuffer = recvBuffer.result().reduce(_ ++ _)
        Codec.decodeCollect(VStreamChunk.codec, None)(theBuffer) match {
          case Attempt.Successful(result) => {
            val chunks = result.value
            log.debug("successful decode {}", chunks.map(_.messageId))
            chunks.foreach { chunk =>
              context.child(s"message-${chunk.messageId}").foreach { child =>
                log.debug("send chunk to child {}", child)
                child ! ChunkReceived(chunk)
              }
            }
            recvBuffer.clear()
            if (result.remainder.nonEmpty) {
              recvBuffer += result.remainder
            }
          }
          case Attempt.Failure(cause: Err.InsufficientBits) =>
            log.debug("insufficent bits needed={} have={}", cause.needed, cause.have)
          case Attempt.Failure(cause) =>
            log.error(cause.toString())
        }
        connection ! Tcp.ResumeReading
    }
  }

  def handleFailure(connection: ActorRef): Receive = {
    case Tcp.CommandFailed(_: Tcp.Write) =>
      // O/S buffer was full
      log.debug("write failed")
      connection ! Tcp.Abort
      context.stop(self)

    case VStreamClient.Stop =>
      log.debug("close connection")
      connection ! Tcp.Close

    case _: Tcp.ConnectionClosed =>
      log.debug("connection closed")
      context.stop(self)
  }

  def connected(connection: ActorRef): Receive = {
    log.debug("connected, send ready to parent")
    context.parent ! Ready(self)

    sending(connection) orElse receiving(connection) orElse handleFailure(connection)
  }

  override def receive: Receive = connecting

}

object VStreamConnection {
  val VST_HANDSHAKE = ByteString("VST/1.1\r\n\r\n")

  /*
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
  */

  def apply(conf: VStreamConfiguration, begin: Iterable[VStreamMessage]): Props =
    Props(new VStreamConnection(conf, begin))//.withRouter(routerConfig)

  case class Ready(link: ActorRef)
  case class ChunkSend(chunk: VStreamChunk)
  case class ChunkReceived(chunk: VStreamChunk)

  case object HandshakeAck extends Tcp.Event
  case object WriteAck extends Tcp.Event
}
