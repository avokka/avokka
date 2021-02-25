import avokka.Transport
import avokka.arangodb.{ArangoConfiguration, ArangoCursor, ArangoQuery, ArangoStream}
import avokka.arangodb.types.{CollectionName, DatabaseName, DocumentKey, DocumentRevision}
import avokka.velocypack.{VPackDecoder, VPackEncoder}
import cats.{Applicative, Functor, Monad}
import cats.effect.{Blocker, ExitCode, IO, IOApp, Sync}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax._
import fs2.{Chunk, Pull, Stream}
import cats.syntax.all._

object connect extends IOApp {
  implicit def unsafeLogger[F[_]: Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  case class Country(
                      _key: DocumentKey = DocumentKey.empty,
                      _rev: DocumentRevision = DocumentRevision.empty,
                      name: String
                    )

  implicit val countryEncoder: VPackEncoder[Country] = VPackEncoder.gen
  implicit val countryDecoder: VPackDecoder[Country] = VPackDecoder.gen

  implicit def arangoFs2Stream[F[_]](implicit F: Applicative[F]): ArangoStream.Aux[F, Stream] = new ArangoStream[F] {
    type S[A[_], B] = Stream[A, B]

    override def fromQuery[V, T: VPackDecoder](query: ArangoQuery[F, V]): S[F, T] =
      Stream.eval(query.cursor[T])
        .flatMap { c =>
          Stream.unfoldLoopEval(c) { c =>
            if (c.body.hasMore) c.next().map { n => (c.body.result, Option(n)) }
            else F.pure((c.body.result, none[ArangoCursor[F, T]]))
          }
        }
        .flatMap(r => Stream.chunk(Chunk.vector(r)))
      /*
        .repeatPull(_.uncons1.flatMap {
          case Some((hd, tl)) if hd.body.hasMore => Pull.output(Chunk.vector(hd.body.result)).as(Some(Stream.eval(hd.next()) ++ tl))
          case Some((hd, tl)) => Pull.output(Chunk.vector(hd.body.result)).as(None)
          case None => Pull.pure(None)
        })
*/
  }

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
        cns <- client.database(DatabaseName("v10")).collection(CollectionName("countries")).all.batchSize(10).stream[Country].compile.toVector
        _ <- IO ( println(cns.length) )
      } yield ()
    }
  } yield ExitCode.Success
}
