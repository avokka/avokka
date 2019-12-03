package avokka.velocystream

import akka.stream._
import akka.stream.stage._

import scala.collection.mutable

class VChunkMessageStage extends GraphStage[FlowShape[VChunk, VMessage]] {
  val in = Inlet[VChunk]("VChunkMessageStage.in")
  val out = Outlet[VMessage]("VChunkMessageStage.out")

  override val shape: FlowShape[VChunk, VMessage] = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with InHandler with OutHandler {

      // message id -> stack of chunks
      private val buffer = mutable.LongMap.empty[VChunkStack]

      private def pushMessage(message: VMessage): Unit = {
        push(out, message)
        if (buffer.isEmpty && isClosed(in)) {
          completeStage()
        }
      }

      override def onUpstreamFinish(): Unit = {
        if (buffer.isEmpty) completeStage()
        else failStage(new RuntimeException("Stream finished but there was incomplete chunks in the buffer"))
      }

      override def onPush(): Unit = {
        val chunk = grab(in)
        // solo chunk
        if (chunk.x.first && chunk.x.number == 1) {
          val message = VMessage(chunk.messageId, chunk.data)
          pushMessage(message)
        }
        else {
          // retrieve the stack of chunks
          val stack = buffer.getOrElseUpdate(chunk.messageId, VChunkStack(chunk.messageId))
          // push chunk in stack
          val pushed = stack.push(chunk)
          // check completeness
          pushed.complete match {
            case Some(message) => {
              // a complete message, remove stack from map
              buffer.remove(message.id)
              pushMessage(message)
            }
            case None => {
              // stack is pending more chunks
              buffer.update(chunk.messageId, pushed)
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
