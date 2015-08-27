package scalydomain.core

import java.io.File
import java.security.MessageDigest

import scala.collection.mutable.Map

import org.msgpack.annotation.Message
import org.msgpack.ScalaMessagePack

import org.iq80.leveldb.{Options}
import org.fusesource.leveldbjni.JniDBFactory.{factory}

@Message
class NgramEntry {
	var allNextSymbolsSum: Long = 0
	var nextSymbols: Map[String, Long] = Map.empty
}

object ModelDb {
	val WriteBatchSize = 10 * 1024
	val CacheSize = 256 * 1024 * 1024
	val BlockSize = 256 * 1024

	def delete(path: String) {
		factory.destroy(new File(path), new Options())
	}
}

class ModelDb(val path: String) {
	val file = new File(path)
	val options = new Options()

	options.createIfMissing(true)
	options.cacheSize(ModelDb.CacheSize)
	options.blockSize(ModelDb.BlockSize)

	val db = factory.open(file, options)
	var batch = db.createWriteBatch()
	var batchSize = 0

	def addNgramNextSymbol(ngram: String, nextSymbol: String) {
		val key = ngram.getBytes("UTF-8")
		val entry = db.get(key) match {
			case existingEntryBytes if existingEntryBytes != null => readEntry(existingEntryBytes)
			case null => {
				//println(s"Adding new ngram $ngram")
				new NgramEntry()
			}
		}

		entry.nextSymbols(nextSymbol) = entry.nextSymbols.getOrElse(nextSymbol, 0l) + 1l
		entry.allNextSymbolsSum += 1

		db.put(key, writeEntry(entry))
	}

	def close() {
		db.write(batch)
		batch.close()
		db.close()
	}

	def compact() {
		db.compactRange(null, null)
	}

	def stats() = {
		println(db.getProperty("leveldb.stats"))
	}

	def readEntry(ser: Array[Byte]) = {
		ScalaMessagePack.read[NgramEntry](ser)
	}

	def writeEntry(entry: NgramEntry) = {
		ScalaMessagePack.write(entry)
	}

	def dump() {
		val iter = db.iterator()
		iter.seekToFirst()

		while (iter.hasNext()) {
			val key = new String(iter.peekNext().getKey(), "UTF-8")
			val value = readEntry(iter.peekNext().getValue())

			println(s"$key:")
			println(s"  Count: ${value.allNextSymbolsSum}")
			for ((ngram, count) <- value.nextSymbols) {
				println(s"    $ngram - $count")
			}

			iter.next()
		}
	}
}