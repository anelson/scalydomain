package scalydomain.core

import scala.collection._
import scala.util.Random
import scala.util.control.Breaks._

import java.io.File
import java.security.MessageDigest
import java.util.concurrent.atomic._

import org.msgpack.annotation.{Message, Ignore}
import org.msgpack.ScalaMessagePack

import org.iq80.leveldb.{Options}
import org.fusesource.leveldbjni.JniDBFactory.{factory}

@Message
class NgramEntry {
	@Ignore
	var occurrenceCount: AtomicLong = new AtomicLong(0)
	var p: Double = 0.0
	var allNextSymbolsSum: Long = 0
	var nextSymbols: concurrent.Map[String, Long] = new concurrent.TrieMap()
}

@Message
class ModelInfo {
	var n: Int = 0
	var totalOccurrenceCount: Long = 0
}

object ModelDb {
	val WriteBatchSize = 10 * 1024
	val CacheSize = 256 * 1024 * 1024
	val BlockSize = 256 * 1024
	val ModelInfoKey = "$$modelInfo".getBytes("UTF-8")

	def delete(path: String) {
		factory.destroy(new File(path), new Options())
	}
}
