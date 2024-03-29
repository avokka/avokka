package avokka.examples

import avokka.arangodb.ArangoConfiguration
import avokka.arangodb.aql._
import avokka.arangodb.fs2._
import avokka.arangodb.types._
import avokka.velocypack._
import cats.effect._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.module.catseffect.syntax._

object connect extends IOApp {

  case class Country(
                      _key: DocumentKey = DocumentKey.empty,
                      _rev: DocumentRevision = DocumentRevision.empty,
                      name: String
                    )

  implicit val countryEncoder: VPackEncoder[Country] = VPackEncoder.derived
  implicit val countryDecoder: VPackDecoder[Country] = VPackDecoder.derived

  override def run(args: List[String]): IO[ExitCode] = Slf4jLogger.create[IO].flatMap { implicit logger => for {
    config <- ArangoConfiguration.at().loadF[IO, ArangoConfiguration]()
    arango = Arango[IO](config)
    _ <- arango.use { client =>
      val cName = CollectionName("countries")
      for {
        e <- client.server.engine()
        dbs <- client.server.databases()
        _ <- IO {
          println(e.body)
          println(dbs.body)
        }
        cns <- client.db
          .collection(cName)
          .all.batchSize(10)
          .stream[Country]
          .evalTap { c =>
            IO { println(c.name) }
          }
          .compile
          .toVector
        _ <- IO ( println(cns.length) )

        // aql query with bound variable
        code = "FR"
        f <- client.db.query(aql"FOR c IN countries FILTER c._key == $code RETURN c").execute[Country]
        _ <- IO { println(f.body.result) }

        // aql query with bound variable and aql param
        code = "FR"
        f <- client.db.query(aql"FOR c IN countries FILTER c._key == $code OR CONTAINS(c.name, @name) RETURN c".bind("name", "Ital")).execute[Country]
        _ <- IO { println(f.body.result) }

        // aql query with bound variable and aql param
        code = "FR"
        f <- client.db.query(aql"FOR c IN @@coll FILTER c._key == $code RETURN c".bind("@coll", cName)).execute[Country]
        _ <- IO { println(f.body.result) }

      } yield ()
    }
  } yield ExitCode.Success }
}
