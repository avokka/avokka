package avokka.arangodb

import akka.actor.ActorSystem
import cats.Applicative
import org.typelevel.log4cats.Logger
import cats.syntax.applicative._

object implicits {

  implicit def akkaLogger[F[_]: Applicative](system: ActorSystem): Logger[F] = new Logger[F] {
    override def error(t: Throwable)(message: => String): F[Unit] = system.log.error(t, message).pure
    override def warn(t: Throwable)(message: => String): F[Unit] = system.log.warning(message).pure
    override def info(t: Throwable)(message: => String): F[Unit] = system.log.info(message).pure
    override def debug(t: Throwable)(message: => String): F[Unit] = system.log.debug(message).pure
    override def trace(t: Throwable)(message: => String): F[Unit] = system.log.debug(message).pure

    override def error(message: => String): F[Unit] = ???
    override def warn(message: => String): F[Unit] = ???
    override def info(message: => String): F[Unit] = ???
    override def debug(message: => String): F[Unit] = ???
    override def trace(message: => String): F[Unit] = ???
  }

}
