import avokka.velocypack.VPack.VArray
import avokka.{Configuration, Transport}
import cats.effect.{Blocker, ExitCode, IO, IOApp, Sync}
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax._
import scodec.bits.ByteVector
import avokka.velocypack._

object connect extends IOApp {
  implicit def unsafeLogger[F[_]: Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  override def run(args: List[String]): IO[ExitCode] = for {
    config <- Blocker[IO].use(ConfigSource.default.at("avokka").loadF[IO, Configuration])
    transport <- Transport(config)
    _ <- transport.use { client =>
      for {
        e <- IO.fromEither(VArray.empty.toVPackBits)
        _ <- client.execute(e.bytes)
      } yield ()
    }
    _ <- transport.use { client =>
      for {
        r <- client.execute(ByteVector(0x19))
        _ <- IO(println(r.bits.asVPack[VPack]))
        _ <- client.execute(ByteVector(0x20))
      } yield ()
    }
  } yield ExitCode.Success
}
