package scalydomain.core

import scala.util.Random

class MarkovChainBuilder(val db: ModelDbWriter, val n: Int) {
	val Prefix = "^" * n
	val Suffix = "$" * n

	//Update the model to reflect the contents of the given word
	def learn(text: String) {
		(Prefix + text + Suffix).sliding(n+1).foreach { seg =>
			val ngram = seg take n
			val nextSym = seg drop n

			db.addNgramNextSymbol(ngram, nextSym)
		}
	}
}