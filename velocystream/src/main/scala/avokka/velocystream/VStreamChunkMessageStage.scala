package avokka.velocystream

import akka.stream._
import akka.stream.stage._

import scala.collection.mutable

/**
  * accumulates chunks in a map of message id and emits when message is complete
  */
class VStreamChunkMessageStage extends GraphStage[FlowShape[VStreamChunk, VStreamMessage]] {
  val in = Inlet[VStreamChunk]("VChunkMessageStage.in")
  val out = Outlet[VStreamMessage]("VChunkMessageStage.out")

  override val shape: FlowShape[VStreamChunk, VStreamMessage] = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with InHandler with OutHandler {

      // message id -> stack of chunks
      private val messages = mutable.LongMap.empty[VStreamChunkStack]

      private def pushMessage(message: VStreamMessage): Unit = {
        push(out, message)
        if (messages.isEmpty && isClosed(in)) {
          completeStage()
        }
      }

      override def onUpstreamFinish(): Unit = {
        if (messages.isEmpty) completeStage()
        else
          failStage(
            new RuntimeException(
              "Stream finished but there was incomplete messages in the chunks buffer"))
      }

      override def onPush(): Unit = {
        val chunk = grab(in)
        if (chunk.x.isWhole) {
          // solo chunk, bypass stack
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
              pull(in)
            }
          }
        }
      }

      override def onPull(): Unit = {
        pull(in)
      }

      setHandlers(in, out, this)
    }
}
