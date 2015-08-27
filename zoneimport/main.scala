package scalydomain

import java.io.File
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import java.security.MessageDigest
import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.JavaConversions._
import ExecutionContext.Implicits.global

import org.iq80.leveldb.{Options}
//import org.iq80.leveldb.impl.Iq80DBFactory.{factory}
import org.fusesource.leveldbjni.JniDBFactory.{factory}

import scalydomain.core.ZoneFile

case class DomainName(val name: String, val hash: Array[Byte])

case class CliOptions(domainDbFile: File = new File("."), zoneFiles: Seq[File] = Seq())

object ZoneImport {
	val WriteBatchSize = 10 * 1024
	val CacheSize = 256 * 1024 * 1024

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

  	val queue = new LinkedBlockingQueue[Option[DomainName]](WriteBatchSize)

  	println("Starting writer")
  	val writer = Future {
  		println(s"Writing domains to ${config.domainDbFile.getPath}")

  		var count: Long = 0
  		val options = new Options()

  		options.createIfMissing(true)
  		options.cacheSize(CacheSize)

  		val db = factory.open(config.domainDbFile, options)
  		var batch = db.createWriteBatch()
  		var batchSize = 0

  		try {
	  		var eof = false

				while(!eof) {
					queue.take match {
						case Some(domain) => {
	  					val name = domain.name
	  					val hash = domain.hash

  						batch.put(hash, name.getBytes("UTF-8"))
  						count = count + 1
  						batchSize = batchSize + 1

  						if (batchSize >= WriteBatchSize) {
  							db.write(batch)
  							batch.close()
  							batch = db.createWriteBatch()
  						}
						}

						case None => {
							println("Writer shutting down")
							eof = true
						}
					}
				}

				db.write(batch)
				batch.close()

				println("Compacting domain database")
				db.compactRange(null, null)

				println(db.getProperty("leveldb.stats"))
  		} finally {
  			batch.close()
  			db.close()
  		}

  		count
  	}

  	println("Starting zone file readers")
  	val readers = Future.sequence(zonefiles.map { zonefile =>
  		Future {
  			println(s"Starting to read ${zonefile.path}")
  			var count: Long = 0
  			val digest = MessageDigest.getInstance("MD5")

  			for (domain <- zonefile) {
  				val hash = digest.digest(domain.getBytes("UTF-8"))

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
