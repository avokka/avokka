package avokka

package object velocypack extends ShowInstances with SyntaxImplicits {

  private[velocypack] type Result[T] = Either[VPackError, T]

}
