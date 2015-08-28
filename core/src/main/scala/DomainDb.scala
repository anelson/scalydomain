package scalydomain.core

import java.io.File
import java.lang.ThreadLocal
import java.security.MessageDigest

import org.iq80.leveldb.{Options}
import org.fusesource.leveldbjni.JniDBFactory.{factory}

object DomainDb {
	val WriteBatchSize = 10 * 1024
	val CacheSize = 256 * 1024 * 1024
	val BlockSize = 256 * 1024

	val digest = new ThreadLocal[MessageDigest]() {
		override def initialValue() = { MessageDigest.getInstance("MD5") }
	}

	def computeDomainHash(domain: String) = {
		digest.get().digest(domain.getBytes("UTF-8"))
	}
}

class DomainDb(val path: String) {
	val file = new File(path)
	val options = new Options()

	options.createIfMissing(true)
	options.cacheSize(DomainDb.CacheSize)
	options.blockSize(DomainDb.BlockSize)

	val db = factory.open(file, options)
	var batch = db.createWriteBatch()
	var batchSize = 0

	def write(domain: String, hash: Array[Byte]) {
		batch.put(hash, domain.getBytes("UTF-8"))
		batchSize = batchSize + 1

		if (batchSize >= DomainDb.WriteBatchSize) {
			db.write(batch)
			batch.close()
			batch = db.createWriteBatch()
		}
	}

	def domains = {
		new Iterator[(Array[Byte], String)] {
			val iter = db.iterator()
			iter.seekToFirst()

			def hasNext = iter.hasNext

			def next = {
				val key = iter.peekNext().getKey()
				val value = new String(iter.peekNext.getValue(), "UTF-8")
				iter.next()

				(key, value)
			}
		}
	}

	def domainExists(domain: String): Boolean = {
		domainExists(DomainDb.computeDomainHash(domain))
	}

	def domainExists(hash: Array[Byte]): Boolean = {
		val entry = db.get(hash)

		entry != null
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
}