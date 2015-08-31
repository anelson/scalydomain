package scalydomain

import java.io.File
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.JavaConversions._
import scala.collection.mutable.SortedSet
import scala.math.pow
import scala.util.matching.Regex
import ExecutionContext.Implicits.global

import scalydomain.core.DomainDb
import scalydomain.core.ModelDbReader
import scalydomain.core.MarkovChainGenerator

case class CliOptions(domainDbFile: File = new File("."),
	modelDbFile: File = new File("."),
	prefix: String = "",
	maxLength: Int = -1,
	domainsToGenerate: Int = 20,
	pattern: Option[Regex] = None,
	includeWords: String = "",
	sort: Boolean = false)

object Generate {
	def compilePattern(pattern: String) = {
		val re = pattern.map { char =>
			char match {
				case 'L' => "[a-z]"
				case 'N' => "\\d"
				case 'V' => "[aeiouy]" //TODO: Is there a way to use Unicode char class to make this work for other writing systems?
				case 'C' => "[^aeiou]"
				case x => x
			}
		}.mkString

		println(s"Limiting domains to those matching regex $re")
		Some(new Regex("^" + re + "$"))
	}

  def main(args: Array[String]): Unit = {
		val optParser = new scopt.OptionParser[CliOptions]("generate") {
		  head("generate", "SNAPSHOT")
		  opt[File]('d', "domaindb") required() action { (x, c) =>
		    c.copy(domainDbFile = x) } text("Path to domain database file which contains list of taken domain names")
		  opt[File]('m', "modeldb") required() action { (x, c) =>
		    c.copy(modelDbFile = x) } text("Path to model database file which contains the trained model parameters")
		  opt[String]('p', "prefix") optional() action { (x, c) =>
		    c.copy(prefix = x) } text("Generate only domain names that start with this prefix")
		  opt[Int]('l', "maxlength") optional() action { (x, c) =>
		    c.copy(maxLength = x) } text("Generate only domain names that are no longer than this")
		  opt[Int]('c', "count") optional() action { (x, c) =>
		    c.copy(domainsToGenerate = x) } text("Generate this many domains")
		  opt[String]('f', "pattern") optional() action { (x, c) =>
		    c.copy(pattern = compilePattern(x)) } text("Generate domains that match this pattern (LNCV plus regex)")
		  opt[String]('i', "include") optional() action { (x, c) =>
		    c.copy(includeWords = x) } text("Include these words in the generated output")
		  opt[Unit]('s', "sort") optional() action { (x, c) =>
		    c.copy(sort = true) } text("Sort output with highest score first")
		}

  	val config = optParser.parse(args, CliOptions()).get

		val modelDb = new ModelDbReader(config.modelDbFile.getPath())
		val markov = new MarkovChainGenerator(modelDb)
		val domainDb = new DomainDb(config.domainDbFile.getPath())
		val generatedNames = SortedSet[String]()
		val ngramSize = modelDb.modelInfo.n

		try {
			println("Generating domain names")

			val markovGeneratedDomains = Stream.continually { markov.generate(config.maxLength, config.prefix) }
			val userSpecifiedDomains = config.includeWords.split(",").filter(_.length > 0)
			val domainHose = userSpecifiedDomains.toStream #::: markovGeneratedDomains
			val acceptableDomains = domainHose.filter(acceptableDomain(config, domainDb, generatedNames, _))
			val domainsWithScores = acceptableDomains.map { domain =>
				val p = markov.computeCharacterProbabilities(domain).toArray
				//val score = p.foldLeft(1.0)(_*_)
				//val score = p.map(pow(_, 2)).foldLeft(1.0)(_*_)
				val scores = p.sorted

				val charProbabilities = (domain+"$").zip(p).map { case (c, prob) => f"P($c)=$prob%4f" }.mkString(",")

				(domain, scores, charProbabilities)
			}

			val ordering = new Ordering[(String, Array[Double], String)] {
				def compare(x: (String, Array[Double], String), y: (String, Array[Double], String)) : Int = {
					x._2.zip(y._2).dropWhile(tuple => tuple._1 == tuple._2).headOption match {
						case Some((lhs, rhs)) => Ordering[Double].compare(rhs, lhs)
						case None => Ordering[Int].compare(y._2.length, x._2.length)
					}
				}
			}

			val domainOutput = config.sort match {
				case true => domainsWithScores.take(config.domainsToGenerate).toArray.sorted(ordering).toStream
				case false => domainsWithScores.take(config.domainsToGenerate).toStream
			}

			domainOutput.foreach { case (domain, _, charProbabilities) =>
				generatedNames += domain
				println(f"\t$domain\t$charProbabilities")
			}
		} finally {
			domainDb.close
			modelDb.close
		}
  }

  def acceptableDomain(config: CliOptions, domainDb: DomainDb, generatedNames: SortedSet[String], domain: String) = {
  	(
	  	!domainDb.domainExists(domain) &&
	  	!generatedNames.contains(domain) &&
	  	(config.pattern match {
	  		case Some(re) => !re.findPrefixOf(domain).isEmpty
	  		case None => true
	  	})
  	)
  }
}

