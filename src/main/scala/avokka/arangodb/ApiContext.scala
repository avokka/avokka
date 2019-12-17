package avokka.arangodb

import avokka.velocypack.{VPackDecoder, VPackEncoder, VPackError}

import scala.concurrent.Future

trait ApiContext[Ctx] { self: Ctx =>
  def session: Session

  def get[C, O](c: C)(implicit command: api.Api.Aux[Ctx, C, O], decoder: VPackDecoder[O]): Future[Either[VPackError, Response[O]]] = {
    session.exec(command.requestHeader(self, c))(decoder).value
  }

  def apply[C, T, O](c: C)(implicit command: api.ApiPayload.Aux[Ctx, C, T, O], encoder: VPackEncoder[T], decoder: VPackDecoder[O]): Future[Either[VPackError, Response[O]]] = {
    session.exec(Request(command.requestHeader(self, c), command.body(self, c)))(command.bodyEncoder, decoder).value
  }
}
