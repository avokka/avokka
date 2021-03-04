import avokka.arangodb.ArangoConfiguration
import avokka.arangodb.fs2._
import avokka.arangodb.types.{CollectionName, DatabaseName, DocumentKey, DocumentRevision}
import avokka.velocypack.{VPackDecoder, VPackEncoder}
import cats.effect.{Blocker, ExitCode, IO, IOApp, Sync}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax._

object connect extends IOApp {
  implicit def unsafeLogger[F[_]: Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  case class Country(
                      _key: DocumentKey = DocumentKey.empty,
                      _rev: DocumentRevision = DocumentRevision.empty,
                      name: String
                    )

  implicit val countryEncoder: VPackEncoder[Country] = VPackEncoder.gen
  implicit val countryDecoder: VPackDecoder[Country] = VPackDecoder.gen

  override def run(args: List[String]): IO[ExitCode] = for {
    config <- Blocker[IO].use(ConfigSource.default.at("avokka").loadF[IO, ArangoConfiguration])
    arango = Arango(config)
    _ <- arango.use { client =>
      for {
        v <- client.server.version()
        _ <- IO { println(v.body) }
      } yield ()
    }
    _ <- arango.use { client =>
      for {
        e <- client.server.engine()
        dbs <- client.server.databases()
        _ <- IO {
          println(e.body)
          println(dbs.body)
        }
        cns <- client
          .database(DatabaseName("v10"))
          .collection(CollectionName("countries"))
          .all.batchSize(10)
          .stream[Country]
          .evalTap { c =>
            IO { println(c.name) }
          }
          .compile
          .toVector
        _ <- IO ( println(cns.length) )
      } yield ()
    }
  } yield ExitCode.Success
}
