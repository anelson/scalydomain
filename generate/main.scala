package scalydomain

import java.io.File
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.JavaConversions._
import scala.collection.mutable.SortedSet
import ExecutionContext.Implicits.global

import scalydomain.core.DomainDb
import scalydomain.core.ModelDbReader
import scalydomain.core.MarkovChainGenerator

case class CliOptions(domainDbFile: File = new File("."),
	modelDbFile: File = new File("."),
	ngramSize: Int = -1,
	prefix: String = "",
	maxLength: Int = -1,
	domainsToGenerate: Int = 20)

object Generate {
  def main(args: Array[String]): Unit = {
		val optParser = new scopt.OptionParser[CliOptions]("generate") {
		  head("generate", "SNAPSHOT")
		  opt[File]('d', "domaindb") required() action { (x, c) =>
		    c.copy(domainDbFile = x) } text("Path to domain database file which contains list of taken domain names")
		  opt[File]('m', "modeldb") required() action { (x, c) =>
		    c.copy(modelDbFile = x) } text("Path to model database file which contains the trained model parameters")
		  opt[Int]('n', "ngram") required() action { (x, c) =>
		    c.copy(ngramSize = x) } text("Size of n-grams used to train the model")
		  opt[String]('p', "prefix") optional() action { (x, c) =>
		    c.copy(prefix = x) } text("Generate only domain names that start with this prefix")
		  opt[Int]('l', "maxlength") optional() action { (x, c) =>
		    c.copy(maxLength = x) } text("Generate only domain names that are no longer than this")
		  opt[Int]('c', "count") optional() action { (x, c) =>
		    c.copy(domainsToGenerate = x) } text("Generate this many domains")
		}

  	val config = optParser.parse(args, CliOptions()).get

		val modelDb = new ModelDbReader(config.modelDbFile.getPath())
		val markov = new MarkovChainGenerator(modelDb, config.ngramSize)
		val domainDb = new DomainDb(config.domainDbFile.getPath())
		val generatedNames = SortedSet[String]()

		try {
			println("Generating domain names")

			(0 to config.domainsToGenerate).foreach { _ =>
				var generated: String = null

				do {
					generated = markov.generate(config.maxLength, config.prefix)
				} while (domainDb.domainExists(generated) || generatedNames.contains(generated))

				generatedNames += generated
				println(s"Generated available domain $generated")
			}
		} finally {
			domainDb.close
			modelDb.close
		}
  }
}

