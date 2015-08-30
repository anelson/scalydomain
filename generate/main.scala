package scalydomain

import java.io.File
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.JavaConversions._
import scala.collection.mutable.SortedSet
import scala.util.matching.Regex
import ExecutionContext.Implicits.global

import scalydomain.core.DomainDb
import scalydomain.core.ModelDbReader
import scalydomain.core.MarkovChainGenerator

case class CliOptions(domainDbFile: File = new File("."),
	modelDbFile: File = new File("."),
	ngramSize: Int = -1,
	prefix: String = "",
	maxLength: Int = -1,
	domainsToGenerate: Int = 20,
	pattern: Option[Regex] = None,
	includeWords: String = "")

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
		  opt[Int]('n', "ngram") required() action { (x, c) =>
		    c.copy(ngramSize = x) } text("Size of n-grams used to train the model")
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
		}

  	val config = optParser.parse(args, CliOptions()).get

		val modelDb = new ModelDbReader(config.modelDbFile.getPath())
		val markov = new MarkovChainGenerator(modelDb, config.ngramSize)
		val domainDb = new DomainDb(config.domainDbFile.getPath())
		val generatedNames = SortedSet[String]()

		try {
			println("Generating domain names")

			val markovGeneratedDomains = Stream.continually { markov.generate(config.maxLength, config.prefix) }
			val userSpecifiedDomains = config.includeWords.split(",")
			val domainHose = userSpecifiedDomains.toStream #::: markovGeneratedDomains
			val acceptableDomains = domainHose.filter(acceptableDomain(config, domainDb, generatedNames, _))

			acceptableDomains.take(config.domainsToGenerate).foreach { domain =>
				generatedNames += domain
				val p = markov.computeProbabilities(domain).toArray
				val score = p.min
				val charProbabilities = (domain+"$").zip(p).map { case (c, prob) => f"P($c)=$prob%4f" }.mkString(",")

				println(f"\t$domain\t$score%4f\t$charProbabilities")
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

