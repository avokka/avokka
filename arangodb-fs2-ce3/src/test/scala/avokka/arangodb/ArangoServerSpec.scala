package avokka.arangodb

import avokka.arangodb.models.Engine
import avokka.arangodb.models.admin.AdminLog
import avokka.arangodb.types._

class ArangoServerSpec extends ArangoIOBase {

  it should "get version" in { arango =>
    arango.server.version().map { res =>
      res.header.responseCode should be (200)
      res.body.version should startWith (container.version)
    }
  }

  it should "engine" in { arango =>
    arango.server.engine().map { res =>
      res.header.responseCode should be (200)
      res.body.name should be (Engine.Name.rocksdb)
    }
  }

  it should "role" in { arango =>
    arango.server.role().map { res =>
      res.header.responseCode should be (200)
      res.body.role should be ("SINGLE")
    }
  }

  it should "get version with details" in { arango =>
    arango.server.version(details = true).map { res =>
      res.header.responseCode should be (200)
      res.body.details should not be (empty)
    }
  }

  it should "have a _system and test database" in { arango =>
    arango.server.databases().map { res =>
      res.header.responseCode should be (200)
      res.body should contain (DatabaseName.system)
      res.body should contain (DatabaseName("test"))
    }
  }

  it should "get log levels" in { arango =>
    arango.server.logLevel().map { res =>
      res.header.responseCode should be (200)
      res.body.get(AdminLog.Topic.general) should not be (empty)
    }
  }
}
