package avokka.arangodb.api

import avokka.arangodb.ArangoRequest.HeaderTrait
import avokka.velocypack.VPackEncoder

/**
  * arangodb api call
  *
  * @tparam Ctx context (session, database, collection)
  * @tparam C   command
  * @tparam B   body
  */
trait Api[Ctx, C, B] {

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
  def header(context: Ctx, command: C): HeaderTrait

  /**
    * build the request body
    *
    * @param context context
    * @param command command value
    * @return body value
    */
  def body(context: Ctx, command: C): B

  /**
    * @return body vpack encoder
    */
  def encoder: VPackEncoder[B]
}

object Api {

  // no body to send
  trait EmptyBody[Ctx, C] extends Api[Ctx, C, Unit] {
    override def body(context: Ctx, command: C): Unit = ()
    override val encoder: VPackEncoder[Unit] = implicitly
  }
  object EmptyBody {
    type Aux[Ctx, C, R] = Api.EmptyBody[Ctx, C] { type Response = R }
  }

  // body is command
  trait Command[Ctx, C] extends Api[Ctx, C, C] {
    override def body(context: Ctx, command: C): C = command
  }
  object Command {
    type Aux[Ctx, C, R] = Api.Command[Ctx, C] { type Response = R }
  }

  // aux pattern for implicits
  type Aux[Ctx, C, B, R] = Api[Ctx, C, B] { type Response = R }

}
