package scalydomain.core

import scala.io.Source

import scala.collection.Iterable

class ZoneFile(path: String) extends Iterable[String] {
	val file = Source.fromFile(path)
	val lineExpression = """^([A-Z0-9][A-Z0-9-]*)\s+NS\s+.+$""".r

	def iterator: Iterator[String] = {
		file.getLines.collect { line =>
			line match {
				case lineExpression(domainName) => domainName.toLowerCase()
			}
		}
	}
}