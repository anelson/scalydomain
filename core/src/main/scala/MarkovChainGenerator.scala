package scalydomain.core

import scala.util.Random

class MarkovChainGenerator(val db: ModelDbReader, val n: Int) {
	val Prefix = "^" * n
	val Suffix = "$" * n

	val rand = new Random()

	def generate(maxLength:Long = -1, withPrefix:String = ""): String = {
		var next: String = null

		do {
			//Drop the prefix ngram and the suffix ngram from the generated text
			next = generateNext(Prefix + withPrefix).drop(n).reverse.drop(n).reverse
		} while (maxLength != -1 && next.length() > maxLength)

		next
	}

	def generateNext(soFar: String): String = {
		//Start with the pure prefix ngram, and keep iterating until we get to the
		//end.  If there is a prefix, use that as the initial ngram
		val ngram = soFar takeRight n

		db.lookupNgram(ngram) match {
			case Some(entry) => {
				generateNext(soFar + chooseNextSymbol(entry, rand))
			}

			//The only ngram that won't ever have an entry is the suffix ngram, so
			//if we can't choose a next symbol it means we've come to the end
			case None => {
				soFar
			}
		}
	}

	def chooseNextSymbol(entry: NgramEntry, rand: Random) = {
		var sum = 0l
		var randomIndex: Long = 0

		do {
			randomIndex = rand.nextLong % entry.allNextSymbolsSum
		} while (randomIndex < 0)

		//Create a collection of the next symbols with the occurrence count replaced by the
		//running total of all counts of all next symbols up to this point
		val nextSymbols = entry.nextSymbols.toIterable
			.scanLeft(("", 0l)) { (runningTotal, nextSymbolPair) =>
				val (_, sum) = runningTotal
				val (symbol, count) = nextSymbolPair

				(symbol, sum + count)
			}
			.tail //The first element from scanLeft is the initial state, but that's just to start the sum at 0, we don't want to use it as a next symbol

		//Skip all next symbols whose running total is less than the randomly selected index.
		//The first symbol after those dropped ones is the one we randomly pick
		nextSymbols.dropWhile { pair => pair._2 <= randomIndex }.map(_._1).headOption match {
			case Some(sym) => sym
			case None => throw new Exception(s"failed to nominate a next symbol for ngram $entry")
		}
	}
}