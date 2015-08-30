package scalydomain.core

import java.io.File
import java.security.MessageDigest

import scala.collection.mutable.Map
import scala.util.Random
import scala.util.control.Breaks._

import org.msgpack.annotation.Message
import org.msgpack.ScalaMessagePack

import org.iq80.leveldb.{Options}
import org.fusesource.leveldbjni.JniDBFactory.{factory}

class ModelDbReader(val path: String) {
	val file = new File(path)
	val options = new Options()

	options.createIfMissing(true)
	options.cacheSize(ModelDb.CacheSize)
	options.blockSize(ModelDb.BlockSize)

	val db = factory.open(file, options)

	def lookupNgram(ngram: String) =  {
		db.get(ngram.getBytes()) match {
			case entry if entry != null => Some(readEntry(entry))
			case null => None
		}
	}

	def close() {
		db.close()
	}

	def readEntry(ser: Array[Byte]) = {
		ScalaMessagePack.read[NgramEntry](ser)
	}
}