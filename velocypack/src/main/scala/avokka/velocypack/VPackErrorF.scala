package avokka.velocypack

/*
import cats.ApplicativeError

trait VPackErrorF[F[_]] {
  def raiseError[A](e: VPackError): F[A]
}

object VPackErrorF {
  def apply[F[_]](implicit ev: VPackErrorF[F]): VPackErrorF[F] = ev

  implicit def instance[F[_]](implicit F: ApplicativeError[F, Throwable]): VPackErrorF[F] =
    new VPackErrorF[F] {
      override def raiseError[A](e: VPackError): F[A] = F.raiseError(e)
    }

  implicit val result: VPackErrorF[Result] = new VPackErrorF[Result] {
    override def raiseError[A](e: VPackError): Result[A] = Left(e)
  }

  object syntax {
    implicit class ErrorChannelOps[F[_]](e: VPackError)(implicit FE: VPackErrorF[F]) {
      def raise[A]: F[A] = FE.raiseError[A](e)
    }
  }
}

 */