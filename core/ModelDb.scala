package scalydomain.core

import java.io.File
import java.lang.ThreadLocal
import java.security.MessageDigest

import org.iq80.leveldb.{Options}
import org.fusesource.leveldbjni.JniDBFactory.{factory}

object ModelDb {
	val WriteBatchSize = 10 * 1024
	val CacheSize = 256 * 1024 * 1024
	val BlockSize = 256 * 1024
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