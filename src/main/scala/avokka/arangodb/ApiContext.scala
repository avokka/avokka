package avokka.arangodb

import avokka.velocypack._

import scala.concurrent.Future

trait ApiContext[Ctx] { self: Ctx =>

  def session: Session

  /**
   * executes an api command in this context
   * @param c command value
   * @param command implicit api
   * @param encoder request body encoder
   * @param decoder response body decoder
   * @tparam C command type
   * @tparam T request body type
   * @tparam O response body type
   * @return
   */
  def apply[C, T, O](c: C)(
    implicit command: api.Api.Aux[Ctx, C, T, O],
    encoder: VPackEncoder[T],
    decoder: VPackDecoder[O]
  ): Future[Either[ArangoError, Response[O]]] = {
    val header = command.header(self, c)
    val body = command.body(self, c)
    session.execute(Request(header, body))(command.encoder, decoder).value
  }
}