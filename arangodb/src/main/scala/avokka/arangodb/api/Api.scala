package avokka.arangodb.api

import avokka.arangodb.ArangoRequest.HeaderTrait
import avokka.velocypack.VPackEncoder

/**
  * arangodb api call
  *
  * @tparam Ctx context (session, database)
  * @tparam Cmd command
  * @tparam Req body
  */
trait Api[Ctx, Cmd, Req] {

  /**
    * response type
    */
  type Response

  /**
    * builds the request header
    *
    * @param command command value
    * @param context context
    * @return header value
    */
  def header(context: Ctx, command: Cmd): HeaderTrait

  /**
    * build the request body
    *
    * @param context context
    * @param command command value
    * @return body value
    */
  def body(context: Ctx, command: Cmd): Req

  /**
    * @return body vpack encoder
    */
  def encoder: VPackEncoder[Req]
}

object Api {

  // no body to send
  trait EmptyBody[Ctx, Cmd] extends Api[Ctx, Cmd, Unit] {
    override def body(context: Ctx, command: Cmd): Unit = ()
    override val encoder: VPackEncoder[Unit] = implicitly
  }
  object EmptyBody {
    type Aux[Ctx, Cmd, Res] = Api.EmptyBody[Ctx, Cmd] { type Response = Res }
  }

  // body is command
  trait Command[Ctx, Cmd] extends Api[Ctx, Cmd, Cmd] {
    override def body(context: Ctx, command: Cmd): Cmd = command
  }
  object Command {
    type Aux[Ctx, Cmd, Res] = Api.Command[Ctx, Cmd] { type Response = Res }
  }

  // aux pattern for implicits
  type Aux[Ctx, Cmd, Req, Res] = Api[Ctx, Cmd, Req] { type Response = Res }

}
