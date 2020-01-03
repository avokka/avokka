package avokka.arangodb

import akka.stream.{Attributes, Outlet, SourceShape}
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import avokka.velocypack.{VPackDecoder, VPackEncoder}
import cats.data.EitherT
import cats.instances.future._
import api._

final class CursorSource[C, T](
    c: C,
    db: ArangoDatabase
)(implicit api: Api.Command.Aux[ArangoDatabase, C, Cursor.Response[T]],
  ce: VPackEncoder[C],
  td: VPackDecoder[T],
)
    extends GraphStage[SourceShape[T]] {

  import db.session.system.dispatcher

  val out: Outlet[T] = Outlet("CursorSource.out")
  override val shape: SourceShape[T] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with OutHandler {

      private var cursorId: Option[String] = None
      private val responseHandler = getAsyncCallback[ArangoResponse[Cursor.Response[T]]](handleResponse)
      private val failureHandler = getAsyncCallback[ArangoError](handleFailure)

      def sendScrollScanRequest(): Unit = {
        val req = cursorId.fold(db(c)) { id =>
          db(CursorNext[T](id))
        }
        EitherT(req).fold(failureHandler.invoke, responseHandler.invoke)
      }

      def handleFailure(ex: ArangoError): Unit = failStage(new IllegalStateException(ex.message))

      def handleResponse(response: ArangoResponse[Cursor.Response[T]]): Unit = {
        cursorId = response.body.id

        emitMultiple(out, response.body.result.iterator)
        if (!response.body.hasMore) {
          completeStage()
        }
      }

      setHandler(out, this)

      override def onPull(): Unit = sendScrollScanRequest()

    }

}
