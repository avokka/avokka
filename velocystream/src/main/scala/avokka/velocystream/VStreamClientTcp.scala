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
import scala.concurrent.duration.Duration

/** Velocystream client actor
  *
  */
class VStreamClientTcp(conf: VStreamConfiguration, begin: Iterable[VStreamMessage])
    extends Actor
    with ActorLogging {
  import VStreamClientTcp._
  import Tcp._
  //import context.{dispatcher, system}

  val manager = IO(Tcp)(context.system)

  private val address: InetSocketAddress = InetSocketAddress.createUnresolved(conf.host, conf.port)
  manager ! Connect(address)

  var buffer = BitVector.empty

  override def receive: Receive = {
    case CommandFailed(_: Connect) =>
      log.debug("connect failed")
      // listener ! "connect failed"
      context.stop(self)

    case c @ Connected(remote, local) =>
      log.debug("connected")
      // listener ! c
      val connection = sender()
      connection ! Register(self)
      connection ! Write(VStreamFlow.VST_HANDSHAKE)
      begin.foreach { m =>
//        self ! MessageSend(m)
        m.chunks() foreach { chk =>
          val bs = ByteString(VStreamChunk.codec.encode(chk).require.toByteBuffer)
          log.debug("SEND {}", bs)
          connection ! Write(bs)
        }
      }

      context.become {
        case MessageSend(m) =>
          m.chunks() foreach { chk =>
            val bs = ByteString(VStreamChunk.codec.encode(chk).require.toByteBuffer)
            log.debug("SEND {}", bs)
            connection ! Write(bs)
          }

        case CommandFailed(w: Write) =>
          // O/S buffer was full
          log.debug("write failed")
          context.stop(self)

        case Received(data) =>
          log.debug("received data")
          buffer = buffer ++ BitVector(data.asByteBuffer)
          log.debug("buffer is {}", buffer)

          Codec.decodeCollect(VStreamChunk.codec, None)(buffer) match {
            case Attempt.Successful(value) => {
              value.value.foreach { chunk =>
                log.debug(chunk.toString)
              }
              buffer = value.remainder
            }
            case Attempt.Failure(cause) => cause match {
              case Err.InsufficientBits(needed, have, context) =>
              case e => log.error(e.toString())
            }
          }
          //listener ! data
        case "close" =>
          connection ! Close

        case _: ConnectionClosed =>
          log.debug("connection closed")
          context.stop(self)
      }
  }

  private val promises = mutable.LongMap.empty[Promise[VStreamMessage]]
//  private val senders = mutable.LongMap.empty[ActorRef]

//  private val flow = new VStreamFlow(conf, begin)

//  private val source = Source.queue[VStreamMessage](conf.queueSize, OverflowStrategy.backpressure)
//  private val source = Source.actorRef(conf.queueSize, OverflowStrategy.fail)

  /*
  private val connection = source
    .via(flow.protocol)
    .map(MessageReceived)
    .to(Sink.actorRefWithAck(self, Init, Ack, ConnectionTerminated))
    .run()

  context.watch(connection)
*/

/*
  override def receive: Receive = {
    case Terminated => context.stop(self)
    case ConnectionTerminated => context.stop(self)

    case m: MessageSend => {
      val message = m.message
      val promise = Promise[VStreamMessage]()
      promises.update(message.id, promise)
//      senders.update(message.id, sender())
      promise.future pipeTo sender()
      connection ! message
    }

    case m: MessageReceived => {
      promises.remove(m.message.id).foreach(_.success(m.message))
      sender() ! Ack
//      senders.remove(m.message.id).foreach(_ ! m)
    }

    case Init => sender() ! Ack
  }

 */
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
