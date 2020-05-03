package avokka.arangodb

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.dimafeng.testcontainers.ForAllTestContainer
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AsyncFlatSpecLike
import org.scalatest.matchers.should.Matchers

class ArangoSessionTest
    extends TestKit(ActorSystem("arangodb-test"))
    with AsyncFlatSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ForAllTestContainer {
  override val container = ArangodbContainer.Def().start()

  val session: ArangoSession = new ArangoSession(container.configuration)

  it should "connect" in {
    session(api.Version()).map { res =>
      res should be ('right)
      res.right.get.body.version should be(container.version)
    }
  }

  override def afterAll(): Unit = {
    session.closeClient()
    TestKit.shutdownActorSystem(system)
  }
}
