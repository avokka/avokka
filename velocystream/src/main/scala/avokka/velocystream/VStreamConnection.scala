package avokka.velocystream

import java.net.InetSocketAddress

import akka.actor._
import akka.io.{IO, Tcp}
import akka.pattern.BackoffSupervisor
import akka.routing._
import akka.util.{ByteString, ByteStringBuilder}
import scodec.{Attempt, Codec, Err}
import scodec.bits.BitVector
import cats.syntax.foldable._
import cats.instances.vector._
import cats.instances.list._
import scodec.interop.cats.BitVectorMonoidInstance

import scala.concurrent.duration._
import scala.collection.mutable

/** Velocystream tcp connection
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

  var reader: ActorRef = context.actorOf(VStreamConnectionReader.props(), name = "reader")

  //  val queue: mutable.Queue[VStreamChunk] = mutable.Queue.empty
  private val chunkOrdering: Ordering[VStreamChunk] =
    Ordering.by[VStreamChunk, Long](chunk => chunk.messageId + chunk.x.position).reverse
  private val queue: mutable.PriorityQueue[VStreamChunk] =
    mutable.PriorityQueue.empty(chunkOrdering)

  private var waitingForAck: Boolean = false

  override def preStart(): Unit = {
    manager ! Tcp.Connect(
      address,
      timeout = Some(10.seconds),
      /*
      options = List(
        Tcp.SO.KeepAlive(true),
        Tcp.SO.TcpNoDelay(false)
      ),
       */
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
      context.watch(connection)
      connection ! Tcp.Register(reader, keepOpenOnPeerClosed = false, useResumeWriting = false)
      context.become(handshaking(connection))

  }

  def handshaking(connection: ActorRef): Receive = {
    connection ! Tcp.Write(VST_HANDSHAKE, HandshakeAck)
    connection ! Tcp.ResumeReading

    // initialize send queue with begin messages
    queue.clear()
    queue ++= begin.flatMap(_.chunks(conf.chunkLength))

    handleFailure(connection) orElse {

      case HandshakeAck =>
        log.debug("received handshake ack")
        if (queue.isEmpty) {
          // handshake and begin sequence is finished
          waitingForAck = false
          context.parent ! BackoffSupervisor.Reset
          context.become(connected(connection))
        } else {
          doSendChunk(connection, queue.dequeue(), HandshakeAck)
        }
    }
  }

  private def chunkToByteString(chunk: VStreamChunk): ByteString = {
    ByteString(VStreamChunk.codec.encode(chunk).require.toByteBuffer)
  }

  private def doSendChunk(connection: ActorRef,
                          chunk: VStreamChunk,
                          ack: Tcp.Event = WriteAck): Unit = {
    log.debug("send chunk #{}-{} {} bytes", chunk.messageId, chunk.x.position, chunk.length)
    connection ! Tcp.Write(chunkToByteString(chunk), ack)
    waitingForAck = true
  }

  private def flushChunkQueue(connection: ActorRef, ack: Tcp.Event): Unit = {
    val chunks: Vector[VStreamChunk] = queue.dequeueAll
    log.debug("flush chunk queue #{} {} bytes", chunks.map(c => s"${c.messageId}-${c.x.position}"), chunks.map(_.length).sum)
    val bs = new ByteStringBuilder()
    chunks.foreach { chunk =>
      bs.append(chunkToByteString(chunk))
    }
    connection ! Tcp.Write(bs.result(), ack)
    waitingForAck = true
    bs.clear()
    queue.clear()
  }

  def sending(connection: ActorRef): Receive = {
    case VStreamClient.MessageSend(m) =>
      log.debug("send message #{} {} bytes, waiting for ack = {}", m.id, m.data.length, waitingForAck)
      reader forward VStreamConnectionReader.MessageInit(m.id)
      val chunks = m.chunks(conf.chunkLength)
      if (waitingForAck) {
        log.debug("append chunks to queue")
        queue ++= chunks
      } else {
        chunks.headOption.foreach { chunk =>
          doSendChunk(connection, chunk)
          log.debug("append remaining chunks to queue")
          queue ++= chunks.tail
        }
      }

    case WriteAck =>
      log.debug("receive write ack")
      if (queue.isEmpty) {
        waitingForAck = false
      } else {
        flushChunkQueue(connection, WriteAck)
       //  doSendChunk(connection, queue.dequeue(), WriteAck)
      }
  }

  def handleFailure(connection: ActorRef): Receive = {
    case Tcp.CommandFailed(w: Tcp.Write) =>
      log.debug("write failed {}", w)
      // O/S buffer was full
      log.debug("write failed")
      connection ! w

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

    sending(connection) orElse handleFailure(connection)
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
    Props(new VStreamConnection(conf, begin)) //.withRouter(routerConfig)

  case class Ready(link: ActorRef)

  case object HandshakeAck extends Tcp.Event
  case object WriteAck extends Tcp.Event
}
