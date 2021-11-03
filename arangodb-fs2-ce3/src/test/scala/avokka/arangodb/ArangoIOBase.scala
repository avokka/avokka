package avokka.arangodb

import avokka.arangodb.fs2._
import avokka.test.ArangodbContainer
import cats.effect._
import cats.effect.testing.scalatest._
import com.dimafeng.testcontainers.ForAllTestContainer
import org.scalatest.flatspec.FixtureAsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats._
import org.typelevel.log4cats.slf4j.Slf4jLogger
import just.semver.SemVer
import org.scalatest.{Assertion, Succeeded}

abstract class ArangoIOBase extends FixtureAsyncFlatSpec
  with AsyncIOSpec
  with CatsResourceIO[Arango[IO]]
  with Matchers
  with ForAllTestContainer {

  implicit val unsafeLogger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override val container = ArangodbContainer.Def().start()
  override val resource = Arango[IO](container.configuration)

  protected val containerVersion = SemVer.parseUnsafe(container.version)

  /*
  protected def assumeArangoVersion(version: String) = {
    assume(containerVersion >= SemVer.parseUnsafe(version))
  }
   */

  protected def withArangoVersion(version: String)(assertion: => IO[Assertion]) = {
    if (containerVersion >= SemVer.parseUnsafe(version)) assertion else IO.pure(Succeeded)
  }
}
