package scalydomain.core

object MarkovChain {

}

class MarkovChain(val db: ModelDb, val n: Int) {
	val prefix = "^" * n
	val suffix = "$" * n

	//Update the model to reflect the contents of the given word
	def learn(text: String) {
		(prefix + text + suffix).sliding(n+1).foreach { seg =>
			val ngram = seg take n
			val nextSym = seg drop n

			db.addNgramNextSymbol(ngram, nextSym)
		}
	}
}