package avokka.arangodb
package api

import avokka.velocypack._

final case class DatabaseCreate(
    result: Boolean
)

object DatabaseCreate {

  /**
    * @param active A flag indicating whether the user account should be activated or not. The default value is *true*. If set to *false*, the user won't be able to log into the database.
    * @param extra A JSON object with extra user information. The data contained in *extra* will be stored for the user but not be interpreted further by ArangoDB.
    * @param passwd The user password as a string. If not specified, it will default to an empty string.
    * @param username Login name of the user to be created
    */
  final case class User(
      username: String,
      passwd: Option[String] = None,
      active: Boolean = true,
      //  extra: Option[Any],
  )

  object User {
    implicit val encoder: VPackEncoder[User] = VPackEncoder.gen
  }

  implicit val decoder: VPackDecoder[DatabaseCreate] = VPackDecoder.gen

}
