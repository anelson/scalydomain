package scalydomain

import java.io.File
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.JavaConversions._
import ExecutionContext.Implicits.global

import scalydomain.core.DomainDb
import scalydomain.core.ModelDb

case class CliOptions(domainDbFile: File = new File("."), modelDbFile: File = new File("."), ngramSize: Int = 2)

case class NGram(ngram: String)

object Train {
  def main(args: Array[String]): Unit = {
		val optParser = new scopt.OptionParser[CliOptions]("train") {
		  head("train", "SNAPSHOT")
		  arg[File]("<domain db file>") required() action { (x, c) =>
		    c.copy(domainDbFile = x) } text("Path to domain database file which contains training corpus")
		  arg[File]("<model db file>") required() action { (x, c) =>
		    c.copy(modelDbFile = x) } text("Path to model database file which contains the trained model parameters")
		  arg[Int]("<n-gram size>") optional() action { (x, c) =>
		    c.copy(ngramSize = x) } text("Size of n-grams to train (defaults to 2)")
		}

  	val config = optParser.parse(args, CliOptions()).get

  	val queue = new LinkedBlockingQueue[Option[NGram]](ModelDb.WriteBatchSize)

  	println("Starting writer")
  	val writer = Future {
  		println(s"Writing model to ${config.modelDbFile.getPath}")

  		var count: Long = 0
  		val modelDb = new ModelDb(config.modelDbFile.getPath())

  		try {
	  		var eof = false

				while(!eof) {
					queue.take match {
						case Some(ngram) => {
							//modelDb.write(domain.name, domain.hash)
  						count = count + 1
						}

						case None => {
							println("Writer shutting down")
							eof = true
						}
					}
				}

				println("Compacting domain database")
				modelDb.compact()

				println(modelDb.stats)
  		} finally {
  			modelDb.close()
  		}

  		count
  	}

  	println("Starting reader")
  	val reader = Future {
  			println(s"Starting to read ${config.domainDbFile.getPath}")

  			val domainDb = new DomainDb(config.domainDbFile.getPath())
  			var count: Long = 0

  			for ((hash, name) <- domainDb.domains) {
  				count = count + 1
  			}

  			println(s"Read $count domains")
  			count
  	}

  	println("Waiting for reader to complete")
  	Await.result(reader, Duration.Inf)

  	println("Waiting for writer to complete")
  	queue.put(None)
  	val writeCount = Await.result(writer, Duration.Inf)
  	println(s"Wrote $writeCount")
  }
}
