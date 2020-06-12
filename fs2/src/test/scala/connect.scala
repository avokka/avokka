import avokka.{Configuration, Transport}
import cats.effect.{ExitCode, IO, IOApp, Sync}
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax._

import scodec.bits.ByteVector

object connect extends IOApp {
  implicit def unsafeLogger[F[_]: Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  override def run(args: List[String]): IO[ExitCode] = for {
    config <- ConfigSource.default.at("avokka").loadF[IO, Configuration]
    transport <- Transport(config)
    _ <- transport.use { client =>
      client.execute(ByteVector(0x19))
    }
    _ <- transport.use { client =>
      client.execute(ByteVector(0x19))
    }
  } yield ExitCode.Success
}
