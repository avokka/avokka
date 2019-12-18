package avokka.arangodb.api

import avokka.arangodb.Request.HeaderTrait
import avokka.velocypack.VPackEncoder

/**
 *
 * @tparam Ctx
 * @tparam C
 * @tparam B
 */
trait ApiPayload[Ctx, C, B] {
  /**
   * builds the request header
   * @param context context
   * @param command command value
   * @return header value
   */
  def requestHeader(context: Ctx, command: C): HeaderTrait

  /**
   * response type
   */
  type Response

  /**
   * build the request body
   * @param context context
   * @param command command value
   * @return body value
   */
  def body(context: Ctx, command: C): B

  /**
   * @return body vpack encoder
   */
  def bodyEncoder: VPackEncoder[B]
}

object ApiPayload {
  trait Command[Ctx, C] extends ApiPayload[Ctx, C, C] {
    override def body(context: Ctx, command: C): C = command
  }

  /**
   * aux pattern for implicits
   *
   * @tparam Ctx context
   * @tparam C command
   * @tparam B body
   * @tparam R response
   */
  type Aux[Ctx, C, B, R] = ApiPayload[Ctx, C, B] { type Response = R }
}
