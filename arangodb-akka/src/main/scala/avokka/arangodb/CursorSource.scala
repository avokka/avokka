package avokka.arangodb

import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import akka.stream.{Attributes, Outlet, SourceShape}
import avokka.arangodb.api._
import avokka.arangodb.protocol.{ArangoError, ArangoResponse}
import avokka.velocypack._
import cats.data.EitherT
import cats.instances.future._

import scala.concurrent.Future

final class CursorSource[C, T](
    c: C,
    db: ArangoDatabase[Future]
)(implicit decoder: VPackDecoder[T]
)
    extends GraphStage[SourceShape[T]] {

 // import db.session.system.dispatcher

  val out: Outlet[T] = Outlet("CursorSource.out")
  override val shape: SourceShape[T] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with OutHandler {

      private var cursorId: Option[String] = None
      private val responseHandler = getAsyncCallback[ArangoResponse[Cursor[T]]](handleResponse)
      private val failureHandler = getAsyncCallback[ArangoError](handleFailure)

      def sendScrollScanRequest(): Unit = {
        /*
        val req = cursorId.fold(db(c)) { id =>
          db(CursorNext[T](id))
        }
        EitherT(req).fold(failureHandler.invoke, responseHandler.invoke)

         */
      }

      def handleFailure(ex: ArangoError): Unit = failStage(new IllegalStateException(ex))

      def handleResponse(response: ArangoResponse[Cursor[T]]): Unit = {
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
