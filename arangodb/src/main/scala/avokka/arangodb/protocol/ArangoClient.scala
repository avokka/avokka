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

trait ArangoClient[F[_]] {

  /**
    * send a velocystream request message and receive a velocystream reply message
    * @param message VST message
    * @return VST message
    */
  protected def send(message: ByteVector): F[ByteVector]

  /**
    * send a arangodb request without body and receive arango response
    * @param header arango header
    * @tparam O output body type
    * @return arango response
    */
  def execute[O: VPackDecoder](header: ArangoRequest.Header): F[ArangoResponse[O]]

  /**
    * send a arangodb request with a body payload and receive arango response
    * @param request arango request
    * @tparam P payload body type
    * @tparam O output body type
    * @return arango response
    */
  def execute[P: VPackEncoder, O: VPackDecoder](request: ArangoRequest[P]): F[ArangoResponse[O]]

  def login(username: String, password: String): F[ArangoResponse[ResponseError]]

  /**
    * arangodb client api to server
    * @return client
    */
  def server: ArangoServer[F]

  /**
    * database api
    * @param name database name
    * @return database api
    */
  def database(name: DatabaseName): ArangoDatabase[F]

  /**
    * _system database api
    * @return database
    */
  def system: ArangoDatabase[F]

  /**
    * configured database api
    * @return database
    */
  def db: ArangoDatabase[F]

}

object ArangoClient {
  @inline def apply[F[_]](implicit ev: ArangoClient[F]): ArangoClient[F] = ev

  abstract class Impl[F[_]](configuration: ArangoConfiguration)(implicit F: MonadThrow[F], L: MessageLogger[F])
    extends ArangoClient[F] {

    override def server: ArangoServer[F] = ArangoServer(this)
    override def database(name: DatabaseName): ArangoDatabase[F] = ArangoDatabase(name)(this, F)
    override def system: ArangoDatabase[F] = database(DatabaseName.system)
    override def db: ArangoDatabase[F] = database(configuration.database)

    override def login(username: String, password: String): F[ArangoResponse[ResponseError]] = execute(
      ArangoRequest.Authentication(user = username, password = password)
    )

    private val traceREQ = Console.CYAN ++ "\u25b2REQ" ++ Console.RESET
    private val traceRES = Console.CYAN_B ++ Console.BLACK ++ "\u25bcRES" ++ Console.RESET

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
          F.fromEither(header.remainder.asVPack[ResponseError]) >>=
            (err => F.raiseError[DecodeResult[O]](ArangoError.Response(header.value, err.value)))
        } else {
          L.trace(show"$traceRES body ${header.remainder.asVPackValue}") >>
          F.fromEither(header.remainder.asVPack[O])
        }
      } yield ArangoResponse(header.value, body.value)

  }

}