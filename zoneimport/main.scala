package scalydomain

import java.io.File
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.JavaConversions._
import ExecutionContext.Implicits.global

import scalydomain.core.{DomainDb, ZoneFile}

case class DomainName(val name: String, val hash: Array[Byte])

case class CliOptions(domainDbFile: File = new File("."), zoneFiles: Seq[File] = Seq())

object ZoneImport {
  def main(args: Array[String]): Unit = {
		val optParser = new scopt.OptionParser[CliOptions]("zoneimport") {
		  head("zoneimport", "SNAPSHOT")
		  arg[File]("<domain db file>") required() action { (x, c) =>
		    c.copy(domainDbFile = x) } text("Path to domain database file which will be populated by this command")
		  arg[File]("<zonefilefile>...") unbounded() required() action { (x, c) =>
		    c.copy(zoneFiles = c.zoneFiles :+ x) } text("DNS zone file(s) to import")
		}

  	val config = optParser.parse(args, CliOptions()).get

  	val zonefiles: Seq[ZoneFile] = config.zoneFiles.flatMap { a =>
  		try {
  			Some(new ZoneFile(a.getPath))
  		} catch {
  			case e: Exception => {
  				println(s"Error reading zonefile $a: $e")
  				None
  			}
  		}
  	}

  	if (zonefiles.isEmpty) {
  		println("No zonefiles to read; nothing to do")
  		return
  	}

  	val queue = new LinkedBlockingQueue[Option[DomainName]](DomainDb.WriteBatchSize)

  	println("Starting writer")
  	val writer = Future {
  		println(s"Writing domains to ${config.domainDbFile.getPath}")

  		var count: Long = 0
  		val domainDb = new DomainDb(config.domainDbFile.getPath)

  		try {
	  		var eof = false

				while(!eof) {
					queue.take match {
						case Some(domain) => {
							domainDb.write(domain.name, domain.hash)
  						count = count + 1
						}

						case None => {
							println("Writer shutting down")
							eof = true
						}
					}
				}

				println("Compacting domain database")
				domainDb.compact()

				println(domainDb.stats)
  		} finally {
  			domainDb.close()
  		}

  		count
  	}

  	println("Starting zone file readers")
  	val readers = Future.sequence(zonefiles.map { zonefile =>
  		Future {
  			println(s"Starting to read ${zonefile.path}")
  			var count: Long = 0

  			for (domain <- zonefile) {
  				val hash = DomainDb.computeDomainHash(domain)

  				queue.put(Some(DomainName(domain, hash)))
  				count += 1

  				if (count % 1000000 == 0) {
  					println(s"Processed ${zonefile.path}:$count")
  				}
  			}

  			(zonefile, count)
  		}
  	})

  	println("Waiting for readers to complete")
  	for (result <- Await.result(readers, Duration.Inf)) {
  		val (zonefile, count) = result

  		println(s"Zone file ${zonefile.path} contained $count domains")
  	}

  	println("Waiting for writer to complete")
  	queue.put(None)
  	val writeCount = Await.result(writer, Duration.Inf)
  	println(s"Wrote $writeCount")
  }
}
