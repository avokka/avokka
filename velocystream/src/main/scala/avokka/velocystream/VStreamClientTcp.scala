package avokka.velocystream

import java.net.InetSocketAddress

import akka.actor._
import akka.io.{IO, Tcp}
import akka.routing._
import akka.util.ByteString
import scodec.{Attempt, Codec, Err}
import scodec.bits.BitVector

import scala.concurrent.duration._

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
    manager ! Tcp.Connect(address, timeout = Some(15.seconds))
  }

  var buffer: BitVector = BitVector.empty

  def connecting: Receive = {
    case Tcp.CommandFailed(_: Tcp.Connect) =>
      log.debug("connect failed")
      context.stop(self)

    case c @ Tcp.Connected(remote, local) =>
      log.debug("connected remote={} local={}", remote, local)
      val connection = sender()
      connection ! Tcp.Register(self, keepOpenOnPeerClosed = true, useResumeWriting = false)
      context.become(handshaking(connection))
      begin.foreach { m =>
        self ! MessageSend(m)
      }
      unstashAll()

    case _ => stash()
  }

  def handshaking(connection: ActorRef): Receive = {
    connection ! Tcp.Write(VStreamFlow.VST_HANDSHAKE, HandshakeAck)

    {
      case HandshakeAck => {
        context.become(connected(connection))
        unstashAll()
      }

      case _ => stash()
    }
  }

  def connected(connection: ActorRef): Receive = {
    case Tcp.CommandFailed(_: Tcp.Write) =>
      // O/S buffer was full
      log.debug("write failed")
      context.stop(self)

    case _: Tcp.ConnectionClosed =>
      log.debug("connection closed")
      context.stop(self)

    case MessageSend(m) =>
      log.debug("MESSAGE SEND {}", m)
      val child = context.actorOf(VStreamMessageActor.props(m.id, sender()), s"message-${m.id}")
      m.chunks() foreach { chunk => self ! ChunkSend(chunk) }

    case ChunkSend(chunk) =>
      val bs = ByteString(VStreamChunk.codec.encode(chunk).require.toByteBuffer)
      connection ! Tcp.Write(bs, WriteAck)
      context.become(waitingForAck(connection, WriteAck))

    case Tcp.Received(data) =>
      log.debug("received data")
      buffer = buffer ++ BitVector(data.asByteBuffer)
      log.debug("buffer is {}", buffer.bytes)

      Codec.decodeCollect(VStreamChunk.codec, None)(buffer) match {
        case Attempt.Successful(result) => {
          log.debug("successful decode {}", result)
          result.value.foreach { chunk =>
            context.child(s"message-${chunk.messageId}").foreach { child =>
              log.debug("send chunk to child {}", child)
              child ! VStreamMessageActor.ChunkReceived(chunk)
            }
          }
          buffer = result.remainder
        }
        case Attempt.Failure(cause: Err.InsufficientBits) =>
          log.debug("insufficent bits needed={} have={}", cause.needed, cause.have)
        case Attempt.Failure(cause) =>
          log.error(cause.toString())
      }
  //    connection ! Tcp.ResumeReading

  }

  def waitingForAck(connection: ActorRef, ack: Tcp.Event): Receive = {
    case `ack` =>
      context.become(connected(connection))
      unstashAll()

    case _: Tcp.ConnectionClosed =>
      log.debug("connection closed")
      context.stop(self)

    case _ => stash()
  }

  override def receive: Receive = connecting

}

object VStreamClientTcp {

  final def decider: SupervisorStrategy.Decider = { case t: Throwable ⇒ SupervisorStrategy.Restart }

  val supervisionStrategy: SupervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = -1, withinTimeRange = Duration.Inf, loggingEnabled = true)(
      decider = decider)

  val poolResizer: Resizer = DefaultResizer(
    lowerBound = 2,
    upperBound = 10,
    pressureThreshold = 1,
    messagesPerResize = 100
  )

  val routerConfig: RouterConfig = SmallestMailboxPool(
    nrOfInstances = 2,
    supervisorStrategy = supervisionStrategy,
    resizer = Some(poolResizer)
  )
  val routerBalanceConfig: RouterConfig = BalancingPool(
    nrOfInstances = 2,
    supervisorStrategy = supervisionStrategy,
  )

  def apply(conf: VStreamConfiguration, begin: Iterable[VStreamMessage]): Props =
    Props(new VStreamClientTcp(conf, begin)) //.withRouter(routerConfig)

  case class MessageSend(message: VStreamMessage)
  case class ChunkSend(chunk: VStreamChunk)

  case object HandshakeAck extends Tcp.Event
  case object WriteAck extends Tcp.Event
}
