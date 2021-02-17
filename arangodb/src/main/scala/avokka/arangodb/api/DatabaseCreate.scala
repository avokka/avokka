package avokka.arangodb
package api

import avokka.velocypack._
import types._

/**
  * @param name Has to contain a valid database name.
  * @param users Has to be an array of user objects to initially create for the new database. User information will not be changed for users that already exist. If *users* is not specified or does not contain any users, a default user *root* will be created with an empty string password. This ensures that the new database will be accessible after it is created. Each user object can contain the following attributes:
  */
final case class DatabaseCreate(
    name: DatabaseName,
    users: List[DatabaseCreate.User] = List.empty,
)

object DatabaseCreate { self =>

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

  implicit val encoder: VPackEncoder[DatabaseCreate] = VPackEncoder.gen

  final case class Response(
      result: Boolean
  )
  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackDecoder.gen
  }

}
