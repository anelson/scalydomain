package scalydomain.core.test

import java.io.File

import scala.collection.concurrent.TrieMap
import scala.util.Random

import org.scalatest._

import org.scalactic.TimesOnInt._
import org.scalactic.Tolerance._

import scalydomain.core.{MarkovChainGenerator, MarkovChainBuilder, ModelDbReader, ModelDbWriter, DomainDb, NgramEntry}

class MarkovChainSpec extends UnitSpec with BeforeAndAfterEach {
	var dbPath: File = _
	var domainDb: DomainDb = _

	override def beforeEach {
		val temp = System.getProperty("java.io.tmpdir")

		do {
			dbPath = new File(temp, "markov-" + System.nanoTime)
		} while (!dbPath.mkdir())

		domainDb = new DomainDb(new File(dbPath, "domain").getPath)
	}

	override def afterEach {
		domainDb.close()
		dbPath.delete()
	}

	def openReader() = { new ModelDbReader(new File(dbPath, "model").getPath) }
	def openWriter() = { new ModelDbWriter(new File(dbPath, "model").getPath) }

	"A MarkovChain" should "generate a string of 'E's when trained on a 'E' corpus" in {
		val modelDbWriter = openWriter()
		val builder =  new MarkovChainBuilder(modelDbWriter, 2)
		builder.learn("weeeeeeeek")
		modelDbWriter.saveToDisk()

		val modelDbReader = openReader()
		val generator = new MarkovChainGenerator(modelDbReader, 2)
		val output = generator.generate()
		modelDbReader.close()

		withClue(s"output: $output") {
			assert(output.head === 'w')
			assert(output.takeRight(1) === "k")

			assert(output.drop(1).reverse.drop(1).reverse.count(_ == 'e') == output.length - 2)
		}
	}

	it should "generate a string of repeated 'ea' tuples when trained with a 'ea' corpus" in {
		val modelDbWriter = openWriter()
		val builder =  new MarkovChainBuilder(modelDbWriter, 2)
		builder.learn("eaeaeaeaea")
		modelDbWriter.saveToDisk()

		val modelDbReader = openReader()
		val generator = new MarkovChainGenerator(modelDbReader, 2)
		val output = generator.generate()

		withClue(s"output: $output") {
			assert(output.grouped(2).count(_ != "ea") == 0)
		}
	}

	it should "generate one of two possible strings when trained on a very limited corpus" in {
		val modelDbWriter = openWriter()
		val builder =  new MarkovChainBuilder(modelDbWriter, 2)
		builder.learn("fear")
		builder.learn("febr")
		modelDbWriter.saveToDisk()

		val modelDbReader = openReader()
		val generator = new MarkovChainGenerator(modelDbReader, 2)

		100 times {
			val output = generator.generate()

			withClue(s"output: $output") {
				assert(output === "fear" || output === "febr")
			}
		}
	}

	it should "select the most probable next symbol most of the time, in proportion to the actual probability" in {
		val modelDbReader = openReader()
		val generator = new MarkovChainGenerator(modelDbReader, 2)

		val entry = new NgramEntry()

		entry.allNextSymbolsSum = 11
		entry.nextSymbols = TrieMap(
			"aa" -> 10,
			"zz" -> 1
		)

		var aaCount: Int = 0
		var zzCount: Int = 0

		val data = for (i <- 1 to 1000) yield generator.chooseNextSymbol(entry, new Random())
		assert(data.forall (s => s == "aa" || s == "zz"))
		aaCount = data.count (s => s == "aa")
		zzCount = data.count (s => s == "zz")

		val percentAA: Double = aaCount.toDouble / (aaCount + zzCount).toDouble

		assert(aaCount + zzCount === 1000)
		assert(percentAA === 0.9 +- 0.1)
	}
}