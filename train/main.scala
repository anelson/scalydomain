package scalydomain

import java.io.File
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.JavaConversions._
import ExecutionContext.Implicits.global

import scalydomain.core.DomainDb
import scalydomain.core.ModelDb
import scalydomain.core.MarkovChain

case class CliOptions(domainDbFile: File = new File("."),
	modelDbFile: File = new File("."),
	ngramSize: Int = 2,
	maxLength: Int = -1,
	includePunycode: Boolean = false
)

object Train {
	val BatchSize = 128 * 1000

  def main(args: Array[String]): Unit = {
		val optParser = new scopt.OptionParser[CliOptions]("train") {
		  head("train", "SNAPSHOT")
		  arg[File]("<domain db file>") required() action { (x, c) =>
		    c.copy(domainDbFile = x) } text("Path to domain database file which contains training corpus")
		  arg[File]("<model db file>") required() action { (x, c) =>
		    c.copy(modelDbFile = x) } text("Path to model database file which contains the trained model parameters")
		  arg[Int]("<n-gram size>") optional() action { (x, c) =>
		    c.copy(ngramSize = x) } text("Size of n-grams to train (defaults to 2)")
		  opt[Int]('n', "maxlength") optional() action { (x, c) =>
		    c.copy(maxLength = x) } text("Limit training inputs to domains up to a certain length")
		  opt[Boolean]('p', "punycode") optional() action { (x, c) =>
		    c.copy(includePunycode = x) } text("Include Punycode domain names in the training corpus; by default they are excluded")
		}

  	val config = optParser.parse(args, CliOptions()).get

		println(s"Starting to read ${config.domainDbFile.getPath}")

		val domainDb = new DomainDb(config.domainDbFile.getPath())
		println(s"Writing model to ${config.modelDbFile.getPath}")

		ModelDb.delete(config.modelDbFile.getPath())
		val modelDb = new ModelDb(config.modelDbFile.getPath())
		val markov = new MarkovChain(modelDb, config.ngramSize)

		println(s"Commencing training")

		var domainCount = 0l
		try {
			domainDb.domains.grouped(BatchSize).foreach { batch =>
				domainCount += batch.par.map { pair =>
					val (hash, name) = pair

					if (
							(config.maxLength == -1 || config.maxLength > name.length) &&
							(config.includePunycode || !name.startsWith("xn--"))
						) {
						markov.learn(name)
						1
					} else {
						0
					}
				}.sum

				println(s"Trained on $domainCount domains")
			}

			println(s"Training complete; processed $domainCount domains")

			println("Persisting model to database")
			modelDb.saveToDisk()

			println("Compacting model database")
			modelDb.compact()

			println(modelDb.stats)
		} finally {
			modelDb.close()
		}
  }
}

