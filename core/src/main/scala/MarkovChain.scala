package scalydomain.core

import scala.util.Random

object MarkovChain {

}

class MarkovChain(val db: ModelDb, val n: Int) {
	val Prefix = "^" * n
	val Suffix = "$" * n
	val rand = new Random()

	//Update the model to reflect the contents of the given word
	def learn(text: String) {
		(Prefix + text + Suffix).sliding(n+1).foreach { seg =>
			val ngram = seg take n
			val nextSym = seg drop n

			db.addNgramNextSymbol(ngram, nextSym)
		}
	}

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

		//If the ngram at the end of the string so far is the suffix, that's always the end of the string
		ngram match {
			case Suffix => soFar
			case ngram => {
				val entry = db.lookupNgram(ngram)
				assert(entry != None)

				entry.get.chooseNextSymbol(rand) match {
					case Some(sym) => {
						generateNext(soFar + sym)
					}

					case None => soFar
				}
			}
		}

	}
}