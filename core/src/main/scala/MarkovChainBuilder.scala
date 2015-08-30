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

			//For the purposes of our stats, the synthetic ngrams which include the prefix or suffix characters
			//should not be counted, as that will skew the actual frequency numbers for ngrams in the training corpus
			val isSyntheticNgram = ngram.contains("$") || ngram.contains("^")
			if (!isSyntheticNgram) {
				db.incrementNgramOccurrenceCount(ngram)
			}
		}
	}
}