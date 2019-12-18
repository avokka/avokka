package avokka.arangodb

import avokka.velocypack._

import scala.concurrent.Future

trait ApiContext[Ctx] { self: Ctx =>

  def session: Session

  def apply[C, T, O](c: C)(
    implicit command: api.Api.Aux[Ctx, C, T, O],
    encoder: VPackEncoder[T],
    decoder: VPackDecoder[O]
  ): Future[Either[VPackError, Response[O]]] = {
    val header = command.requestHeader(self, c)
    val body = command.body(self, c)
    session.execute(Request(header, body))(command.bodyEncoder, decoder).value
  }
}
