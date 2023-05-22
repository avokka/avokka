package avokka.arangodb

import avokka.arangodb.fs2.*
import avokka.test.ArangodbContainer
import cats.effect.*
import cats.effect.testing.scalatest.*
import com.dimafeng.testcontainers.{ForAllTestContainer, GenericContainer}
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import org.scalatest.flatspec.FixtureAsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.*
import org.typelevel.log4cats.slf4j.Slf4jLogger
import just.semver.SemVer
import org.scalatest.{Assertion, Succeeded}

abstract class ArangoIOBase extends FixtureAsyncFlatSpec
  with AsyncIOSpec
  with CatsResourceIO[Arango[IO]]
  with Matchers
  with TestContainerForAll {

  implicit val unsafeLogger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override val containerDef: GenericContainer.Def[ArangodbContainer] = ArangodbContainer.Def()

  override val resource = for {
    container <- Resource.eval(IO(withContainers(identity(_))))
    arango <- Arango[IO](container.configuration)
  } yield arango

  // protected val containerVersion = SemVer.parseUnsafe(container.version)

  /*
  protected def assumeArangoVersion(version: String) = {
    assume(containerVersion >= SemVer.parseUnsafe(version))
  }
   */

  protected def withArangoVersion(version: String)(assertion: => IO[Assertion]) = withContainers { container =>
    val containerVersion = SemVer.parseUnsafe(container.version)
    if (containerVersion >= SemVer.parseUnsafe(version)) assertion else IO.pure(Succeeded)
  }
}
