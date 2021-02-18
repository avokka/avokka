package avokka.arangodb

import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import akka.stream.{Attributes, Outlet, SourceShape}
import avokka.velocypack._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

final class CursorSource[V, T](
    query: ArangoQuery[Future, V],
)(implicit decoder: VPackDecoder[T], executionContext: ExecutionContext)
    extends GraphStage[SourceShape[T]] {

  val out: Outlet[T] = Outlet("CursorSource.out")
  override val shape: SourceShape[T] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with OutHandler {

      private var cursor: Option[ArangoCursor[Future, T]] = None
      private val responseHandler = getAsyncCallback[ArangoCursor[Future, T]](handleResponse)
      private val failureHandler = getAsyncCallback[Throwable](handleFailure)

      // def cursorId: Option[String] = cursor.flatMap(_.body.id)

      def sendScrollScanRequest(): Unit = {

        val req = cursor.fold(query.cursor)(_.next())

        req.onComplete({
          case Failure(exception) => failureHandler.invoke(exception)
          case Success(value) => responseHandler.invoke(value)
        })
        //req.fold(failureHandler.invoke, responseHandler.invoke)

      }

      def handleFailure(ex: Throwable): Unit = failStage(ex)

      def handleResponse(response: ArangoCursor[Future, T]): Unit = {
        cursor = Some(response)

        emitMultiple(out, response.body.result)
        if (!response.body.hasMore) {
          completeStage()
        }
      }

      setHandler(out, this)

      override def onPull(): Unit = sendScrollScanRequest()

    }

}
