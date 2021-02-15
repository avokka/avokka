package avokka.velocypack
package bench
import org.openjdk.jmh.annotations._

import scala.util.Random

@State(Scope.Benchmark)
class ToString {
  @Benchmark
  def vpack(): Unit = {
    randomString().toVPackBits
  }


  @Benchmark
  def arango(): Unit = {
    val builder = new com.arangodb.velocypack.VPack.Builder().build()
    builder.serialize(randomString())
  }

  def randomString(): String = Random.nextString(50)
}
