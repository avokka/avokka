package avokka.velocystream

import java.net.InetSocketAddress

import akka.actor._
import akka.io.{IO, Tcp}
import akka.pattern.pipe
import akka.routing._
import akka.util.ByteString
import scodec.{Attempt, Codec, Err}
import scodec.bits.BitVector
//import akka.stream.scaladsl._
//import akka.stream.{Materializer, OverflowStrategy, QueueOfferResult}

import scala.collection.mutable
import scala.concurrent.Promise
import scala.concurrent.duration._

/** Velocystream client actor
  *
  */
class VStreamClientTcp(conf: VStreamConfiguration, begin: Iterable[VStreamMessage])
    extends Actor
    with ActorLogging {
  import VStreamClientTcp._
  import context.{dispatcher, system}

  // message id -> stack of chunks
  private val messages = mutable.LongMap.empty[VStreamChunkStack]
  private val promises = mutable.LongMap.empty[Promise[VStreamMessage]]

  val manager = IO(Tcp)

  private val address: InetSocketAddress = InetSocketAddress.createUnresolved(conf.host, conf.port)
  manager ! Tcp.Connect(address, timeout = Some(10.seconds))

  var buffer = BitVector.empty

  def pushMessage(message: VStreamMessage) = {
    promises.get(message.id).foreach { promise =>
      promise.success(message)
    }
  }

  override def receive: Receive = {
    case Tcp.CommandFailed(_: Tcp.Connect) =>
      log.debug("connect failed")
      context.stop(self)

    case c @ Tcp.Connected(remote, local) =>
      log.debug("connected")
      val connection = sender()
      connection ! Tcp.Register(self)
      connection ! Tcp.Write(VStreamFlow.VST_HANDSHAKE)
      begin.foreach { m =>
//        self ! MessageSend(m)
        m.chunks() foreach { chk =>
          val bs = ByteString(VStreamChunk.codec.encode(chk).require.toByteBuffer)
          log.debug("CHUNK SEND {}", bs)
          connection ! Tcp.Write(bs)
        }
      }

      context.become {
        case MessageSend(m) =>
          log.debug("MESSAGE SEND {}", m)
          val promise = Promise[VStreamMessage]()
          promises.update(m.id, promise)
          promise.future pipeTo sender()
          m.chunks() foreach { chk =>
            val bs = ByteString(VStreamChunk.codec.encode(chk).require.toByteBuffer)
            connection ! Tcp.Write(bs)
          }

        case Tcp.CommandFailed(w: Tcp.Write) =>
          // O/S buffer was full
          log.debug("write failed")
          context.stop(self)

        case Tcp.Received(data) =>
          log.debug("received data")
          buffer = buffer ++ BitVector(data.asByteBuffer)
          log.debug("buffer is {}", buffer.bytes)

          Codec.decodeCollect(VStreamChunk.codec, None)(buffer) match {
            case Attempt.Successful(value) => {
              value.value.foreach { chunk =>
                log.debug(chunk.toString)
                if (chunk.x.isWhole) {
                  // solo chunk, bypass stack merge computation
                  val message = VStreamMessage(chunk.messageId, chunk.data)
                  pushMessage(message)
                } else {
                  // retrieve the stack of chunks
                  val stack = messages.getOrElseUpdate(chunk.messageId, VStreamChunkStack(chunk.messageId))
                  // push chunk in stack
                  val pushed = stack.push(chunk)
                  // check completeness
                  pushed.complete match {
                    case Some(message) => {
                      // a full message, remove stack from map
                      messages.remove(message.id)
                      pushMessage(message)
                    }
                    case None => {
                      // stack is pending more chunks
                      messages.update(chunk.messageId, pushed)
                    }
                  }
                }
              }
              buffer = value.remainder
            }
            case Attempt.Failure(cause) => cause match {
              case Err.InsufficientBits(needed, have, _) => log.debug("insufficent bits needed={} have={}", needed, have)
              case e => log.error(e.toString())
            }
          }
          //listener ! data
        case "close" =>
          connection ! Tcp.Close

        case _: Tcp.ConnectionClosed =>
          log.debug("connection closed")
          context.stop(self)
      }
  }

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
  /*
  case class MessageReceived(message: VStreamMessage)
  case object ConnectionTerminated
  case object Init
  case object Ack
   */
}
