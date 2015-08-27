package scalydomain

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import java.security.MessageDigest
import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.JavaConversions._
import ExecutionContext.Implicits.global

import org.fusesource.lmdbjni.{Env, Database, LMDBException, Constants}
import org.fusesource.lmdbjni.Constants._

import scalydomain.core.ZoneFile

case class DomainName(val name: String, val hash: Array[Byte])

object ZoneImport {
  def main(args: Array[String]): Unit = {
  	val zonefiles: Seq[ZoneFile] = args.flatMap { a =>
  		try {
  			Some(new ZoneFile(a))
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

  	val queue = new LinkedBlockingQueue[Option[DomainName]](10*1024)

  	println("Starting writer")
  	val writer = Future {
  		val env = new Env("data/domains.lmdb")
  		val db = env.openDatabase()

  		var count: Long = 0
  		var eof = false

			while(!eof) {
				queue.take match {
					case Some(domain) => {
  					val name = domain.name
  					val hash = domain.hash

  					try {
  						db.put(hash, name.getBytes("UTF-8"), Constants.NOOVERWRITE)
  						count = count + 1
						} catch {
							case e: LMDBException if e.getErrorCode() == LMDBException.KEYEXIST => {}
						}
					}

					case None => {
						println("Writer shutting down")
						eof = true
					}
				}
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
