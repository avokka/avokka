package avokka.arangodb.api

import avokka.arangodb.Request.HeaderTrait

/**
 * arangodb api call
 * @tparam Ctx context type (session, database, collection)
 * @tparam C command type
 */
trait Api[Ctx, C] {
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
}

object Api {

  /**
   * aux pattern for implicits
   * @tparam C command type
   * @tparam R response type
   */
  type Aux[Ctx, C, R] = Api[Ctx, C] { type Response = R }

  /*
  def instance[C, R : Decoder](m: HttpMethod, p: C => String): Aux[C, R] = new Command[C] {
    override type Response = R
    override val method: HttpMethod = m
    override def path(c: C): String = p(c)
    override def responseDecoder: Decoder[R] = implicitly
  }
   */
}