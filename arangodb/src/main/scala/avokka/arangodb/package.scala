package avokka

import cats.data.EitherT

import scala.concurrent.Future

package object arangodb {

  private[arangodb] type FEE[T] = EitherT[Future, ArangoError, T]

}
