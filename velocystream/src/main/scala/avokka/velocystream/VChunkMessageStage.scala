package avokka.velocystream

import akka.stream._
import akka.stream.stage._

class VChunkMessageStage extends GraphStage[FlowShape[VChunk, VMessage]] {
  val in = Inlet[VChunk]("VChunkMessageStage.in")
  val out = Outlet[VMessage]("VChunkMessageStage.out")

  override val shape: FlowShape[VChunk, VMessage] = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with InHandler with OutHandler {
      // message id -> accumulated chunks
      private var buffer = Map.empty[Long, Vector[VChunk]]

      override def onPush(): Unit = {
        val chunk = grab(in)
        if (chunk.isFirst && chunk.chunk == 1) {
          val m = VMessage(chunk.messageId, chunk.data)
          push(out, m)
        }
      }

      override def onPull(): Unit = {
        pull(in)
      }

      setHandlers(in, out, this)
    }
}
