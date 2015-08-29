package scalydomain

import java.io.File
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.JavaConversions._
import scala.collection.mutable.SortedSet
import ExecutionContext.Implicits.global

import scalydomain.core.DomainDb
import scalydomain.core.ModelDb
import scalydomain.core.MarkovChain

case class CliOptions(domainDbFile: File = new File("."), modelDbFile: File = new File("."), ngramSize: Int = 2)

object Generate {
  def main(args: Array[String]): Unit = {
		val optParser = new scopt.OptionParser[CliOptions]("train") {
		  head("train", "SNAPSHOT")
		  arg[File]("<domain db file>") required() action { (x, c) =>
		    c.copy(domainDbFile = x) } text("Path to domain database file which contains list of taken domain names")
		  arg[File]("<model db file>") required() action { (x, c) =>
		    c.copy(modelDbFile = x) } text("Path to model database file which contains the trained model parameters")
		  arg[Int]("<n-gram size>") optional() action { (x, c) =>
		    c.copy(ngramSize = x) } text("Size of n-grams used to train the model (defaults to 2)")
		}

  	val config = optParser.parse(args, CliOptions()).get

		val modelDb = new ModelDb(config.modelDbFile.getPath())
		val markov = new MarkovChain(modelDb, config.ngramSize)
		val domainDb = new DomainDb(config.domainDbFile.getPath())
		val generatedNames = SortedSet[String]()

		try {
			println("Generating domain names")

			(0 to 50).foreach { _ =>
				var generated: String = null

				do {
					generated = markov.generate(10, "deep")
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

