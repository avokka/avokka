package avokka.arangodb
package protocol

import avokka.velocypack._
import avokka.velocystream.VStreamMessage
import cats.MonadThrow
import cats.syntax.flatMap._
import cats.syntax.functor._
import scodec.DecodeResult
import scodec.bits.ByteVector

trait ArangoProtocol[F[_]] {
//  type S[_[_], _]

  protected def send(message: VStreamMessage): F[VStreamMessage]

  def execute[O: VPackDecoder](header: ArangoRequest.Header): F[ArangoResponse[O]]

  def execute[P: VPackEncoder, O: VPackDecoder](request: ArangoRequest[P]): F[ArangoResponse[O]]

//  def raiseError[A](e: ArangoError): F[A]

  def client: ArangoClient[F]

//  def fromQuery[V, T : VPackDecoder](query: ArangoQuery[F, V]): S[F, T]
}

object ArangoProtocol {
  //  type Aux[F, S] = ArangoProtocol[F] { type S = S }

  @inline def apply[F[_]](implicit ev: ArangoProtocol[F]): ArangoProtocol[F] = ev

  abstract class Impl[F[_]](implicit F: MonadThrow[F])
    extends ArangoProtocol[F] {

    override def execute[O: VPackDecoder](header: ArangoRequest.Header): F[ArangoResponse[O]] =
      for {
        in <- F.fromEither(header.toVPackBits)
        out <- sendBytes(in.bytes)
        res <- handleResponse(out)
      } yield res

    override def execute[P: VPackEncoder, O: VPackDecoder](request: ArangoRequest[P]): F[ArangoResponse[O]] =
      for {
        inh <- F.fromEither(request.header.toVPackBits)
        inb <- F.fromEither(request.body.toVPackBits)
        out <- sendBytes((inh ++ inb).bytes)
        res <- handleResponse(out)
      } yield res

    protected def sendBytes(data: ByteVector): F[VStreamMessage] = send(VStreamMessage.create(data))

    protected def handleResponse[O: VPackDecoder](response: VStreamMessage): F[ArangoResponse[O]] =
      for {
        header <- F.fromEither(response.data.bits.asVPack[ArangoResponse.Header])
        body <- if (header.remainder.isEmpty) {
          F.raiseError[DecodeResult[O]](ArangoError.Head(header.value))
        } else if (header.value.responseCode >= 400) {
          F.fromEither(header.remainder.asVPack[ResponseError])
            .flatMap(err => F.raiseError[DecodeResult[O]](ArangoError.Resp(header.value, err.value)))
        } else {
          F.fromEither(header.remainder.asVPack[O])
        }
      } yield ArangoResponse(header.value, body.value)

    //  override def raiseError[A](e: ArangoError): F[A] = F.raiseError(e)

    override def client: ArangoClient[F] = ArangoClient(this, F)
  }

}