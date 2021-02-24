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

    override def execute[O: VPackDecoder](header: ArangoRequest.Header): F[ArangoResponse[O]] =
      for {
        in <- F.fromEither(header.toVPackBits)
        _  <- L.trace(s"REQ head ${in.asVPackValue.map(_.show)}")
        out <- send(in.bytes)
        res <- handleResponse(out)
      } yield res

    override def execute[P: VPackEncoder, O: VPackDecoder](request: ArangoRequest[P]): F[ArangoResponse[O]] =
      for {
        inh <- F.fromEither(request.header.toVPackBits)
        _   <- L.trace(s"REQ head ${inh.asVPackValue.map(_.show)}")
        inb <- F.fromEither(request.body.toVPackBits)
        _   <- L.trace(s"REQ body ${inb.asVPackValue.map(_.show)}")
        out <- send((inh ++ inb).bytes)
        res <- handleResponse(out)
      } yield res

    // protected def sendBytes(data: ByteVector): F[VStreamMessage] = send(VStreamMessage.create(data))

    protected def handleResponse[O: VPackDecoder](response: ByteVector): F[ArangoResponse[O]] =
      for {
        _      <- L.trace(s"RES head ${response.bits.asVPackValue.map(_.show)}")
        header <- F.fromEither(response.bits.asVPack[ArangoResponse.Header])
        body <- if (header.remainder.isEmpty) {
          F.raiseError[DecodeResult[O]](ArangoError.Head(header.value))
        } else if (header.value.responseCode >= 400) {
          L.trace(s"RES body ${header.remainder.asVPackValue.map(_.show)}") >>
          F.fromEither(header.remainder.asVPack[ResponseError])
            .flatMap(err => F.raiseError[DecodeResult[O]](ArangoError.Resp(header.value, err.value)))
        } else {
          L.trace(s"RES body ${header.remainder.asVPackValue.map(_.show)}") >>
          F.fromEither(header.remainder.asVPack[O])
        }
      } yield ArangoResponse(header.value, body.value)

  }

}