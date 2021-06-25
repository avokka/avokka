package avokka.arangodb
package protocol

import avokka.arangodb.types.DatabaseName
import avokka.velocypack._
import cats.MonadThrow
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.show._
import org.typelevel.log4cats.MessageLogger
import scodec.DecodeResult
import scodec.bits.ByteVector

/**
  * ArangoDB client
  *
  * @tparam F effect
  */
trait ArangoClient[F[_]] {

  /**
    * send a velocystream request message and receive a velocystream reply message
    *
    * @param message VST message
    * @return VST message
    */
  protected def send(message: ByteVector): F[ByteVector]

  /**
    * send a arangodb request without body and receive a arangodb response without body (HEAD)
    *
    * @param header arango request header
    * @return arango header
    */
  def execute(header: ArangoRequest.Header): F[ArangoResponse.Header]

  /**
    * send a arangodb request without body and receive arangodb response
    *
    * @param header arango header
    * @tparam O output body type
    * @return arango response
    */
  def execute[O: VPackDecoder](header: ArangoRequest.Header): F[ArangoResponse[O]]

  /**
    * send a arangodb request without body and receive arangodb response as velocypack sequence
    *
    * @param header arango header
    * @tparam O output body inner type
    * @return arango response
    */
  def executeSequence[O: VPackDecoder](header: ArangoRequest.Header): F[ArangoResponse[Vector[O]]]

  /**
    * send a arangodb request with a body payload and receive arangodb response
    *
    * @param request arango request
    * @tparam P payload body type
    * @tparam O output body type
    * @return arango response
    */
  def execute[P: VPackEncoder, O: VPackDecoder](request: ArangoRequest[P]): F[ArangoResponse[O]]

  /**
    * authenticate with arango server
    *
    * @param username
    * @param password
    * @return
    */
  def login(username: String, password: String): F[ArangoResponse[ArangoResponse.Error]]

  /** arangodb server api */
  def server: ArangoServer[F]

  /** database api */
  def database(name: DatabaseName): ArangoDatabase[F]

  /** _system database api */
  def system: ArangoDatabase[F]

  /** configured database api */
  def db: ArangoDatabase[F]

}

object ArangoClient {
  @inline def apply[F[_]](implicit ev: ArangoClient[F]): ArangoClient[F] = ev

  /**
    * Abstract implementation of client execute methods, send method is left to akka or fs2
    *
    * @param configuration client configuration
    * @param F monad
    * @param L logger
    * @tparam F effect
    */
  abstract class Impl[F[_]](configuration: ArangoConfiguration)(implicit F: MonadThrow[F], L: MessageLogger[F])
    extends ArangoClient[F] {

    override val server: ArangoServer[F] = ArangoServer(this, F)

    override def database(name: DatabaseName): ArangoDatabase[F] = ArangoDatabase(name)(this, F)
    override val system: ArangoDatabase[F] = database(DatabaseName.system)
    override val db: ArangoDatabase[F] = database(configuration.database)

    override def login(username: String, password: String): F[ArangoResponse[ArangoResponse.Error]] = execute(
      ArangoRequest.Authentication(user = username, password = password)
    )

    private val traceREQ = Console.CYAN + "\u25b2REQ" + Console.RESET
    private val traceRES = Console.CYAN_B + Console.BLACK + "\u25bcRES" + Console.RESET

    override def execute(header: ArangoRequest.Header): F[ArangoResponse.Header] = for {
      _      <- L.trace(show"$traceREQ header $header")
      in     <- F.fromEither(header.toVPackBits)
      _      <- L.trace(show"$traceREQ header ${in.asVPackValue}")
      out    <- send(in.bytes)
      _      <- L.trace(show"$traceRES header ${out.bits.asVPackValue}")
      header <- F.fromEither(out.bits.asVPack[ArangoResponse.Header])
      _      <- L.trace(show"$traceRES header ${header.value}")
      res    <- if (header.value.responseCode >= 300) {
        F.raiseError(ArangoError.Header(header.value))
      } else {
        F.pure(header.value)
      }
    } yield res

    override def execute[O: VPackDecoder](header: ArangoRequest.Header): F[ArangoResponse[O]] =
      for {
        _  <- L.trace(show"$traceREQ header $header")
        in <- F.fromEither(header.toVPackBits)
        _  <- L.trace(show"$traceREQ header ${in.asVPackValue}")
        out <- send(in.bytes)
        res <- handleResponse(out)
      } yield res

    override def execute[P: VPackEncoder, O: VPackDecoder](request: ArangoRequest[P]): F[ArangoResponse[O]] =
      for {
        _   <- L.trace(show"$traceREQ header ${request.header}")
        inh <- F.fromEither(request.header.toVPackBits)
        _   <- L.trace(show"$traceREQ header ${inh.asVPackValue}")
        inb <- F.fromEither(request.body.toVPackBits)
        _   <- L.trace(show"$traceREQ body ${inb.asVPackValue}")
        out <- send((inh ++ inb).bytes)
        res <- handleResponse(out)
      } yield res

    protected def handleResponse[O: VPackDecoder](response: ByteVector): F[ArangoResponse[O]] =
      for {
        _      <- L.trace(show"$traceRES header ${response.bits.asVPackValue}")
        header <- F.fromEither(response.bits.asVPack[ArangoResponse.Header])
        _      <- L.trace(show"$traceRES header ${header.value}")
        body <- if (header.remainder.isEmpty) {
          F.raiseError[DecodeResult[O]](ArangoError.Header(header.value))
        } else if (header.value.responseCode >= 300) {
          L.trace(show"$traceRES body ${header.remainder.asVPackValue}") >>
            F.fromEither(header.remainder.asVPack[ArangoResponse.Error]) >>=
            (err => F.raiseError[DecodeResult[O]](ArangoError.Response(header.value, err.value)))
        } else {
          L.trace(show"$traceRES body ${header.remainder.asVPackValue}") >>
            F.fromEither(header.remainder.asVPack[O])
        }
      } yield ArangoResponse(header.value, body.value)

    override def executeSequence[O: VPackDecoder](header: ArangoRequest.Header): F[ArangoResponse[Vector[O]]] =
      for {
        _  <- L.trace(show"$traceREQ header $header")
        in <- F.fromEither(header.toVPackBits)
        _  <- L.trace(show"$traceREQ header ${in.asVPackValue}")
        out <- send(in.bytes)
        res <- handleResponseSequence(out)
      } yield res

    protected def handleResponseSequence[O: VPackDecoder](response: ByteVector): F[ArangoResponse[Vector[O]]] =
      for {
        _      <- L.trace(show"$traceRES header ${response.bits.asVPackValue}")
        header <- F.fromEither(response.bits.asVPack[ArangoResponse.Header])
        _      <- L.trace(show"$traceRES header ${header.value}")
        body <- if (header.value.responseCode >= 300) {
          L.trace(show"$traceRES body ${header.remainder.asVPackValue}") >>
            F.fromEither(header.remainder.asVPack[ArangoResponse.Error]) >>=
            (err => F.raiseError[DecodeResult[Vector[O]]](ArangoError.Response(header.value, err.value)))
        } else {
          L.trace(show"$traceRES body ${header.remainder.asVPackValue}") >>
            F.fromEither(header.remainder.asVPackSequence[O])
        }
      } yield ArangoResponse(header.value, body.value)

  }

}