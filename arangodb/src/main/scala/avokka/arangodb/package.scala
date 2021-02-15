package avokka

import cats.data.EitherT

import scala.concurrent.Future

package object arangodb {

  private[arangodb] type FEE[T] = EitherT[Future, ArangoError, T]

  /*
  type DatabaseName = types.DatabaseName
  val DatabaseName = types.DatabaseName

  type CollectionName = types.CollectionName
  val CollectionName = types.CollectionName

  type DocumentKey = types.DocumentKey
  val DocumentKey = types.DocumentKey

  type DocumentHandle = types.DocumentHandle
  val DocumentHandle = types.DocumentHandle

  type DocumentRevision = types.DocumentRevision
  val DocumentRevision = types.DocumentRevision
*/
}
