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

class ModelDbWriter(val path: String) {
	val ngrams: concurrent.Map[String, NgramEntry] = new concurrent.TrieMap()
	val file = new File(path)

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

	def saveToDisk() {
		val options = new Options()

		options.createIfMissing(true)
		options.cacheSize(ModelDb.CacheSize)
		options.blockSize(ModelDb.BlockSize)

		val db = factory.open(file, options)

		ngrams.foreach { pair =>
			val (key, entry) = pair

			//Compute the total count of all next symbols prior to writing to disk
			entry.allNextSymbolsSum = entry.nextSymbols.map(_._2).sum

			//Now write to the database
			db.put(key.getBytes("UTF-8"), writeEntry(entry))
		}

		db.compactRange(null, null)
		println(db.getProperty("leveldb.stats"))
	}

	def writeEntry(entry: NgramEntry) = {
		ScalaMessagePack.write(entry)
	}
}