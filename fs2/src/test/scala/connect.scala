import avokka.arangodb.ArangoConfiguration
import avokka.Transport
import cats.effect.{Blocker, ExitCode, IO, IOApp, Sync}
import cats.syntax.all._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax._
import scodec.bits.ByteVector
import avokka.velocypack._

object connect extends IOApp {
  implicit def unsafeLogger[F[_]: Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  override def run(args: List[String]): IO[ExitCode] = for {
    config <- Blocker[IO].use(ConfigSource.default.at("avokka").loadF[IO, ArangoConfiguration])
    transport <- Transport(config)
    _ <- transport.use { client =>
      for {
        _ <- VArray.empty.toVPackBits.liftTo[IO]
        f1 <- client.execute(ByteVector(1,2,3,4)).start
        f2 <- client.execute(ByteVector(10,20,30,40)).start
        _ <- f1.join
        _ <- f2.join
      } yield ()
    }
    _ <- transport.use { client =>
      for {
        r <- client.execute(ByteVector(0x19))
        d <- r.bits.asVPack[VPack].liftTo[IO]
        _ <- IO(println(d))
        _ <- client.execute(ByteVector(0x20))
      } yield ()
    }
  } yield ExitCode.Success
}
