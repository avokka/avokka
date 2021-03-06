import avokka.arangodb.ArangoConfiguration
import avokka.arangodb.api.admin.AdminLog
import avokka.arangodb.aql._
import avokka.arangodb.fs2._
import avokka.arangodb.protocol.ArangoError
import avokka.arangodb.types._
import avokka.velocypack._
import cats.effect._
import cats.syntax.flatMap._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.module.catseffect.syntax._

object scratch extends IOApp {

  case class Country(
                      _key: DocumentKey = DocumentKey.empty,
                      _rev: DocumentRevision = DocumentRevision.empty,
                      name: String
                    )
  object Country {
    implicit val countryEncoder: VPackEncoder[Country] = VPackEncoder.gen
    implicit val countryDecoder: VPackDecoder[Country] = VPackDecoder.gen
  }

  override def run(args: List[String]): IO[ExitCode] = for {
    implicit0(logger: Logger[IO]) <- Slf4jLogger.create[IO]
    config <- Blocker[IO].use(ArangoConfiguration.at().loadF[IO, ArangoConfiguration])
    arango = Arango(config)
    _ <- arango.use { client =>
      val v10 = client.database(DatabaseName("v10"))
      val countries = v10.collection(CollectionName("countries"))
      for {
        t <- v10.transactions.begin()
        l <- v10.query(
            aql"FOR c IN countries FILTER c.name LIKE @name RETURN c".bind("name", "France%")
        ).batchSize(1).transaction(t.id).stream[Country].compile.toVector
        _ <- v10.transactions.list() >>= (ls => IO(println(ls)))
        _ <- t.status() >>= (ls => IO(println(ls)))
        c <- t.commit()
        _ <- v10.transactions.list() >>= (ls => IO(println(ls)))
      _ <- IO { println(l) }
      } yield ()
    }
  } yield ExitCode.Success
}
