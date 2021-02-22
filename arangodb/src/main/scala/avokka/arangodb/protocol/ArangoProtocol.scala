package avokka.arangodb
package protocol

import avokka.velocypack._
import avokka.velocystream.VStreamMessage
import cats.{Functor, MonadThrow}
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.show._
import org.typelevel.log4cats.{Logger, MessageLogger}
import scodec.DecodeResult
import scodec.bits.ByteVector

trait ArangoProtocol[F[_]] {
  protected def send(message: VStreamMessage): F[VStreamMessage]

  def execute[O: VPackDecoder](header: ArangoRequest.Header): F[ArangoResponse[O]]

  def execute[P: VPackEncoder, O: VPackDecoder](request: ArangoRequest[P]): F[ArangoResponse[O]]

  def client: ArangoClient[F]
}

object ArangoProtocol {
  @inline def apply[F[_]](implicit ev: ArangoProtocol[F]): ArangoProtocol[F] = ev

  abstract class Impl[F[_]](implicit F: MonadThrow[F], L: MessageLogger[F])
    extends ArangoProtocol[F] {

    override def execute[O: VPackDecoder](header: ArangoRequest.Header): F[ArangoResponse[O]] =
      for {
        in <- F.fromEither(header.toVPackBits)
        _  <- L.trace(s"REQ head ${in.asVPackValue.map(_.show)}")
        out <- sendBytes(in.bytes)
        res <- handleResponse(out)
      } yield res

    override def execute[P: VPackEncoder, O: VPackDecoder](request: ArangoRequest[P]): F[ArangoResponse[O]] =
      for {
        inh <- F.fromEither(request.header.toVPackBits)
        _   <- L.trace(s"REQ head ${inh.asVPackValue.map(_.show)}")
        inb <- F.fromEither(request.body.toVPackBits)
        _   <- L.trace(s"REQ body ${inb.asVPackValue.map(_.show)}")
        out <- sendBytes((inh ++ inb).bytes)
        res <- handleResponse(out)
      } yield res

    protected def sendBytes(data: ByteVector): F[VStreamMessage] = send(VStreamMessage.create(data))

    protected def handleResponse[O: VPackDecoder](response: VStreamMessage): F[ArangoResponse[O]] =
      for {
        _      <- L.trace(s"RES head ${response.data.bits.asVPackValue.map(_.show)}")
        header <- F.fromEither(response.data.bits.asVPack[ArangoResponse.Header])
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

    override def client: ArangoClient[F] = ArangoClient(this, F)
  }

}