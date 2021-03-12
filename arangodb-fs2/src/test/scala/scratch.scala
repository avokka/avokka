import avokka.arangodb.ArangoConfiguration
import avokka.arangodb.aql._
import avokka.arangodb.fs2._
import avokka.arangodb.types._
import avokka.velocypack._
import cats.effect._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.module.catseffect.syntax._

object scratch extends IOApp {

  case class Country(
                      _key: DocumentKey = DocumentKey.empty,
                      _rev: DocumentRevision = DocumentRevision.empty,
                      name: String
                    )

  implicit val countryEncoder: VPackEncoder[Country] = VPackEncoder.gen
  implicit val countryDecoder: VPackDecoder[Country] = VPackDecoder.gen

  override def run(args: List[String]): IO[ExitCode] = for {
    implicit0(logger: Logger[IO]) <- Slf4jLogger.create[IO]
    config <- Blocker[IO].use(ArangoConfiguration.at().loadF[IO, ArangoConfiguration])
    arango = Arango(config)
    _ <- arango.use { client =>
      for {
        v <- client.server.logLevel()
        _ <- IO { println(v.body) }
      } yield ()
    }
  } yield ExitCode.Success
}
