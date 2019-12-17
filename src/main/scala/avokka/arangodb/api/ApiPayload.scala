package avokka.arangodb.api

import avokka.arangodb.Request.HeaderTrait
import avokka.velocypack.VPackEncoder

trait ApiPayload[Ctx, C, B] {
  /**
   * builds the request header
   * @param command command value
   * @param context context
   * @return header value
   */
  def requestHeader(context: Ctx, command: C): HeaderTrait

  /**
   * response type
   */
  type Response

  def body(context: Ctx, command: C): B
  def bodyEncoder: VPackEncoder[B]
}

object ApiPayload {

  /**
   * aux pattern for implicits
   *
   * @tparam C command type
   * @tparam R response type
   */
  type Aux[Ctx, C, B, R] = ApiPayload[Ctx, C, B] { type Response = R }
}
