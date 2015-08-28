package scalydomain.core.test

import java.io.File

import org.scalatest._
import org.scalactic.TimesOnInt._

import scalydomain.core.{MarkovChain, ModelDb, DomainDb}

class MarkovChainSpec extends UnitSpec with BeforeAndAfterEach {
	var dbPath: File = _
	var modelDb: ModelDb = _
	var domainDb: DomainDb = _

	override def beforeEach {
		val temp = System.getProperty("java.io.tmpdir")

		do {
			dbPath = new File(temp, "markov-" + System.nanoTime)
		} while (!dbPath.mkdir())

		modelDb = new ModelDb(new File(dbPath, "model").getPath)
		domainDb = new DomainDb(new File(dbPath, "domain").getPath)
	}

	override def afterEach {
		modelDb.close()
		domainDb.close()
		dbPath.delete()
	}

	"A MarkovChain" should "generate a string of 'E's when trained on a 'E' corpus" in {
		val chain =  new MarkovChain(modelDb, 2)
		chain.learn("weeeeeeeek")

		val output = chain.generate()

		withClue(s"output: $output") {
			assert(output.head === 'w')
			assert(output.takeRight(1) === "k")

			assert(output.drop(1).reverse.drop(1).reverse.count(_ == 'e') == output.length - 2)
		}
	}

	it should "generate a string of repeated 'ea' tuples when trained with a 'ea' corpus" in {
		val chain =  new MarkovChain(modelDb, 2)
		chain.learn("eaeaeaeaea")

		val output = chain.generate()

		withClue(s"output: $output") {
			assert(output.grouped(2).count(_ != "ea") == 0)
		}
	}

	it should "generate one of two possible strings when trained on a very limited corpus" in {
		val chain =  new MarkovChain(modelDb, 2)
		chain.learn("fear")
		chain.learn("febr")

		100 times {
			val output = chain.generate()

			withClue(s"output: $output") {
				assert(output === "fear" || output === "febr")
			}
		}
	}
}