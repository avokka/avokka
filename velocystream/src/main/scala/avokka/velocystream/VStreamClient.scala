package avokka.velocystream

import akka.actor._
import akka.pattern.{BackoffOpts, BackoffSupervisor}

import scala.concurrent.duration._

/** Velocystream client actor
  *
  */
class VStreamClient(conf: VStreamConfiguration, begin: Iterable[VStreamMessage])
    extends Actor
    with Stash
    with ActorLogging {
  import VStreamClient._

  val connection: ActorRef = context.actorOf(BackoffSupervisor.props(
    BackoffOpts.onStop(
      VStreamConnection(conf, begin),
      childName = "connection",
      minBackoff = 1.second,
      maxBackoff = 10.seconds,
      randomFactor = 0.1
    )
    .withFinalStopMessage(_ == Stop)
    .withManualReset
    .withDefaultStoppingStrategy
  ))

  /*
  val connection: ActorRef = context.actorOf(
      VStreamConnection(conf, begin),
      name = "connection",
    )
*/

  def idle(): Receive = {
    log.debug("became idle")

    {
      case VStreamConnection.Ready(link) =>
        log.debug("client received connection ready")
        context.become(active(link))
        unstashAll()

      case m: MessageSend => {
        log.debug("stash message #{} in client", m.message.id)
        stash()
      }

      case Stop => connection ! Stop
    }
  }

  def active(link: ActorRef): Receive = {
    log.debug("client become active with {}", link)
    context.watch(link)

    {
      case Terminated(ref) if ref == link =>
        log.debug("client received terminated connection")
        context.become(idle())

      case m: MessageSend =>
        log.debug("client forward message #{} to connection", m.message.id)
        connection forward m

      case Stop => connection ! Stop
    }
  }

  override def receive: Receive = idle()
}

object VStreamClient {

  def apply(conf: VStreamConfiguration, begin: Iterable[VStreamMessage]): Props =
    Props(new VStreamClient(conf, begin))

  case class MessageSend(message: VStreamMessage)
  case object Stop
}
