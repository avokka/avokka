package avokka.velocystream

import java.net.InetSocketAddress

import akka.actor._
import akka.io.{IO, Tcp}
import akka.pattern.BackoffSupervisor
import akka.util.ByteString

import scala.collection.mutable

/** Velocystream TCP connection
  *
  * @param conf configuration
  * @param begin first messages to send (authorization for example)
  */
class VStreamConnection(conf: VStreamConfiguration, begin: Iterable[VStreamMessage])
    extends Actor
    with ActorLogging {
  import VStreamConnection._
  import context.system

  /** TCP manager actor */
  val manager: ActorRef = IO(Tcp)

  /** server remote address */
  private val address: InetSocketAddress = InetSocketAddress.createUnresolved(conf.host, conf.port)

  /** read side of TCP connection */
  var reader: ActorRef = context.actorOf(VStreamReader.props(), name = "reader")

  /** chunks waiting for write ack */
  private val queue: mutable.PriorityQueue[VStreamChunk] =
    mutable.PriorityQueue.empty(chunkOrdering)

  /** flag to stop sending chunks */
  private var waitingForAck: Boolean = false

  /*
  private val buffer: mutable.Queue[VStreamMessage] = mutable.Queue.empty
  private var messagesWaitingReply: Long = 0
  private val maxMessagesInTransit: Long = 2
*/

  override def preStart(): Unit = {
    manager ! Tcp.Connect(
      address,
      timeout = Some(conf.connectTimeout),
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
    log.debug("send chunk #{}-{} {} bytes", chunk.header.id, chunk.header.x.position, chunk.header.length)
    connection ! Tcp.Write(chunkToByteString(chunk), ack)
    waitingForAck = true
  }

  private def flushChunkQueue(connection: ActorRef, ack: Tcp.Event): Unit = {
    val chunks: Seq[VStreamChunk] = queue.dequeueAll
    log.debug("flush chunk queue #{} {} bytes", chunks.map(c => s"${c.header.id}-${c.header.x.position}"), chunks.map(_.header.length).sum)
    val bs = ByteString.newBuilder
    chunks.foreach { chunk =>
      bs.append(chunkToByteString(chunk))
    }
    connection ! Tcp.Write(bs.result(), ack)
    waitingForAck = true
    bs.clear()
    queue.clear()
  }

  def sendMessage(connection: ActorRef, m: VStreamMessage): Unit = {
    log.debug("send message #{} {} bytes, waiting for ack = {}", m.id, m.data.length, waitingForAck)
    reader forward VStreamReader.MessageInit(m.id)
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

  }

  def sending(connection: ActorRef): Receive = {

    case VStreamClient.MessageSend(m) =>
      sendMessage(connection, m)

      /*
      if (messagesWaitingReply > maxMessagesInTransit) {
        log.debug("buffer message waiting={} max={}", messagesWaitingReply, maxMessagesInTransit)
        buffer.enqueue(m)
      } else {
        messagesWaitingReply += 1
        sendMessage(connection, m)
        log.debug("sent message waiting={}", messagesWaitingReply)
      }

    case VStreamReader.MessageReplied =>
      messagesWaitingReply -= 1
      log.debug("replied message waiting={}", messagesWaitingReply)
      if (buffer.nonEmpty && (messagesWaitingReply < maxMessagesInTransit)) {
        log.debug("dequeue buffer")
        messagesWaitingReply += 1
        sendMessage(connection, buffer.dequeue())
      }
       */

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

  /** velocystream handshake */
  val VST_HANDSHAKE = ByteString("VST/1.1\r\n\r\n")

  /** order chunks by message ID + position in order to interleave a little bit
    * MessageId-Position : 1-0 -> [2-0|1-1] -> 3-0 -> [4-0|3-1] -> [5-0|3-2] -> ...
    */
  private val chunkOrdering: Ordering[VStreamChunk] =
    Ordering.by[VStreamChunk, Long](chunk => chunk.header.id + chunk.header.x.position).reverse


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

  final case class Ready(link: ActorRef)

  case object HandshakeAck extends Tcp.Event
  case object WriteAck extends Tcp.Event
}
