package scalydomain.core

import scala.io.Source

import scala.collection.Iterable

class ZoneFile(val path: String) extends Iterable[String] {
	val file = Source.fromFile(path)
	val lineExpression = """^([A-Z0-9][A-Z0-9-]*)\s+NS\s+.+$""".r

	def iterator: Iterator[String] = {
		var lastDomain: String = null

		file.getLines.collect { line =>
			line match {
				case lineExpression(domainName) if domainName != lastDomain => {
					lastDomain = domainName
					domainName.toLowerCase()
				}
			}
		}
	}
}