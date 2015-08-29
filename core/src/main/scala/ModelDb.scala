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

class ModelDb(val path: String) {
	val ngrams: concurrent.Map[String, NgramEntry] = new concurrent.TrieMap()
	val file = new File(path)
	val options = new Options()

	options.createIfMissing(true)
	options.cacheSize(ModelDb.CacheSize)
	options.blockSize(ModelDb.BlockSize)

	val db = factory.open(file, options)
	loadFromDisk()

	def addNgramNextSymbol(ngram: String, nextSymbol: String) {
		val entry = getOrAddNgram(ngram)

		//Perform a thread-safe increment of the symbol count
		//Note how we retry if the underlying value changed
		var nextSymbolCount = 0l
		do {
			//Get the current count, adding a new entry with a zero count if this symbol hasn't
			//been seen before.  Remember kids, concurrency is hard.
			nextSymbolCount = entry.nextSymbols.get(nextSymbol) match {
				case Some(x) => x
				case None => {
					entry.nextSymbols.putIfAbsent(nextSymbol, 0l) match {
						case Some(y) => y
						case None => 0l
					}
				}
			}
		} while (!entry.nextSymbols.replace(nextSymbol, nextSymbolCount, nextSymbolCount + 1))
	}

	def lookupNgram(ngram: String) =  {
		ngrams.get(ngram)
	}

	def getOrAddNgram(ngram: String) = {
		ngrams.get(ngram) match {
			case Some(entry) => entry
			case None => {
				//There is no entry for this ngram.  Create a new one and add it to the map, but
				//bear in mind concurrency is hard, and another thread might do the same thing, so
				//handle that case
				val empty = new NgramEntry()
				ngrams.putIfAbsent(ngram, empty) match {
					case Some(existingEntry) => existingEntry //Some other thread must have added this when we weren't looking; use what's already there
					case None => empty //There was nothing in the hash table, so 'empty' was added successfully
				}
			}
		}
	}

	def close() {
		db.close()
	}

	def compact() {
		db.compactRange(null, null)
	}

	def stats() = {
		println(db.getProperty("leveldb.stats"))
	}

	def loadFromDisk() {
		val iter = db.iterator()
		iter.seekToFirst()

		while (iter.hasNext()) {
			val key = new String(iter.peekNext().getKey(), "UTF-8")
			val value = readEntry(iter.peekNext().getValue())

			ngrams(key) = value

			iter.next()
		}
	}

	def saveToDisk() {
		ngrams.foreach { pair =>
			val (key, entry) = pair

			//Compute the total count of all next symbols prior to writing to disk
			entry.allNextSymbolsSum = entry.nextSymbols.map(_._2).sum

			//Now write to the database
			db.put(key.getBytes("UTF-8"), writeEntry(entry))
		}
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