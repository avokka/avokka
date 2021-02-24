import avokka.Transport
import avokka.arangodb.ArangoConfiguration
import cats.effect.{Blocker, ExitCode, IO, IOApp, Sync}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax._

object connect extends IOApp {
  implicit def unsafeLogger[F[_]: Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  override def run(args: List[String]): IO[ExitCode] = for {
    config <- Blocker[IO].use(ConfigSource.default.at("avokka").loadF[IO, ArangoConfiguration])
    transport <- Transport(config)
    _ <- transport.use { client =>
      for {
        v <- client.server.version()
        _ <- IO { println(v.body) }
      } yield ()
    }
    _ <- transport.use { client =>
      for {
        e <- client.server.engine()
        dbs <- client.server.databases()
        _ <- IO {
          println(e.body)
          println(dbs.body)
        }
      } yield ()
    }
  } yield ExitCode.Success
}
