package avokka.velocystream

import akka.actor._
import akka.pattern.pipe
import akka.routing._
import akka.stream.scaladsl._
import akka.stream.{Materializer, OverflowStrategy}

import scala.collection.mutable
import scala.concurrent.Promise
import scala.concurrent.duration.Duration

/** Velocystream client actor
  *
  */
class VStreamClient(conf: VStreamConfiguration, begin: Source[VStreamMessage, _])(
    implicit materializer: Materializer)
    extends Actor
    with ActorLogging {
  import VStreamClient._
  import context.{dispatcher, system}

  private val promises = mutable.LongMap.empty[Promise[VStreamMessage]]
//  private val senders = mutable.LongMap.empty[ActorRef]

  private val flow = new VStreamFlow(conf, begin)

  // val q = Source.queue[VStreamMessage](conf.queueSize, OverflowStrategy.fail)
  private val source = Source.actorRef(conf.queueSize, OverflowStrategy.fail)

  private val connection = source
    .via(flow.protocol)
    .map(MessageReceived)
    .to(Sink.actorRef(self, ConnectionTerminated))
    .run()

  override def receive: Receive = {
    case ConnectionTerminated => context.stop(self)

    case m: MessageSend => {
      val message = m.message
      val promise = Promise[VStreamMessage]()
      promises.update(message.id, promise)
//      senders.update(message.id, sender())
      promise.future pipeTo sender()
      connection ! message
      /*
        .offer(message)
        .map({
          case QueueOfferResult.Enqueued       => promise
          case QueueOfferResult.Dropped        => promise.failure(new RuntimeException("queue drop"))
          case QueueOfferResult.Failure(cause) => promise.failure(cause)
          case QueueOfferResult.QueueClosed    => promise.failure(new RuntimeException("queue closed"))
        })
        .flatMap(_.future) pipeTo sender()
     */
    }

    case m: MessageReceived => {
      promises.remove(m.message.id).foreach(_.success(m.message))
//      senders.remove(m.message.id).foreach(_ ! m)
    }

    case _ =>
  }
}

object VStreamClient {

  final def decider: SupervisorStrategy.Decider = { case t: Throwable ⇒ SupervisorStrategy.Restart }

  val supervisionStrategy: SupervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = -1, withinTimeRange = Duration.Inf, loggingEnabled = true)(
      decider = decider)

  val poolResizer: Resizer = DefaultResizer(
    lowerBound = 1,
    upperBound = 10,
    pressureThreshold = 1,
  )

  val routerConfig: RouterConfig = SmallestMailboxPool(
    nrOfInstances = 1,
    supervisorStrategy = supervisionStrategy,
    resizer = Some(poolResizer)
  )

  def apply(conf: VStreamConfiguration, begin: Source[VStreamMessage, _])(
      implicit materializer: Materializer): Props =
    Props(new VStreamClient(conf, begin)(materializer)).withRouter(routerConfig)

  case class MessageSend(message: VStreamMessage)
  case class MessageReceived(message: VStreamMessage)
  case object ConnectionTerminated
}
