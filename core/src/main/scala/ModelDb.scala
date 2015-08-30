package scalydomain.core

import java.io.File
import java.security.MessageDigest

import scala.collection._
import scala.util.Random
import scala.util.control.Breaks._

import org.msgpack.annotation.Message
import org.msgpack.ScalaMessagePack

import org.iq80.leveldb.{Options}
import org.fusesource.leveldbjni.JniDBFactory.{factory}

@Message
class NgramEntry {
	var allNextSymbolsSum: Long = 0
	var nextSymbols: concurrent.Map[String, Long] = new concurrent.TrieMap()
}

object ModelDb {
	val WriteBatchSize = 10 * 1024
	val CacheSize = 256 * 1024 * 1024
	val BlockSize = 256 * 1024

	def delete(path: String) {
		factory.destroy(new File(path), new Options())
	}
}
