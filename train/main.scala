package scalydomain

import java.io.File
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.JavaConversions._
import scala.io.Source
import ExecutionContext.Implicits.global

import scalydomain.core.DomainDb
import scalydomain.core.ModelDb
import scalydomain.core.ModelDbWriter
import scalydomain.core.MarkovChainBuilder

case class CliOptions(domainDbFile: Option[File] = None,
	textFile: Option[File] = None,
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
		  opt[File]('d', "domaindb") optional() action { (x, c) =>
		    c.copy(domainDbFile = Some(x)) } text("Path to domain database file which contains list of taken domain names")
		  opt[File]('t', "textfile") optional() action { (x, c) =>
		    c.copy(textFile = Some(x)) } text("Path to text file with training corpus, one word per line")
		  opt[File]('m', "modeldb") required() action { (x, c) =>
		    c.copy(modelDbFile = x) } text("Path to model database file which contains the trained model parameters")
		  opt[Int]('n', "ngram") required() action { (x, c) =>
		    c.copy(ngramSize = x) } text("Size of n-grams to train (defaults to 2)")
		  opt[Int]('n', "maxlength") optional() action { (x, c) =>
		    c.copy(maxLength = x) } text("Limit training inputs to domains up to a certain length")
		  opt[Boolean]('p', "punycode") optional() action { (x, c) =>
		    c.copy(includePunycode = x) } text("Include Punycode domain names in the training corpus; by default they are excluded")

	    checkConfig { c =>
	    	if (c.domainDbFile.isEmpty && c.textFile.isEmpty) {
	    		failure("must specify either a domain database file or a text file to train from")
	    	} else {
	    		success
	    	}
	    }
		}

  	val config = optParser.parse(args, CliOptions()).get

		println(s"Writing model to ${config.modelDbFile.getPath}")

		ModelDb.delete(config.modelDbFile.getPath())
		val modelDb = new ModelDbWriter(config.modelDbFile.getPath())
		val markov = new MarkovChainBuilder(modelDb, config.ngramSize)

		println(s"Commencing training")

		config.domainDbFile foreach { domainDbFile =>
			var inputCount = 0l
			println(s"Starting to read ${domainDbFile.getPath}")
			val domainDb = new DomainDb(domainDbFile.getPath())

			domainDb.domains.grouped(BatchSize).foreach { batch =>
				inputCount += batch.par.map { pair =>
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

				println(s"Trained on $inputCount domains")
			}

			println(s"Training complete; processed $inputCount domains")
		}

	  config.textFile foreach { textFile =>
  		var inputCount = 0l
  		var skippedCount = 0l

  		//Only include lines that are valid domain names, meaning no whitespace or punctuation
  		var re = """^\w[\w\-]*$""".r

			for (line <- Source.fromFile(textFile).getLines) {
				if (config.maxLength == -1 || config.maxLength > line.length) {
					if (line.length >= config.ngramSize) {
						line match {
							case re(_*) => {
								markov.learn(line.toLowerCase)
								inputCount += 1
							}

							case _ => skippedCount += 1
						}
					}
				}
			}

			println(s"Training complete; processed $inputCount words ($skippedCount invalid lines skipped)")
  	}

		println("Persisting model to database")
		modelDb.saveToDisk(config.ngramSize)
	}
}

