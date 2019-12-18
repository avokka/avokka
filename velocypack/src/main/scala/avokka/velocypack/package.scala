package avokka

package object velocypack extends ShowInstances with SyntaxImplicits {

  type Result[T] = Either[VPackError, T]

}
