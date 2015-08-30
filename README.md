# scalydomain
Implementation of my toy domain name generator idea but in Scala

This is a collection of experiments into the viability of generating domain names using various novel algorithms.

It assumes the .com zonefile is available.

Note that some domains, for example 'velk.com', are registered but do not appear in the zonefile.  This seems rare, but it must be considered a possibility.  A more comprehensive solution would need to submit WHOIS queries for candidate domains that do not appear in the zonefile, in order to be sure they are truly available.

I'm trying various ngram lengths.  Not surprisingly, 2-grams are the most random-looking.  Even at five chars, the output is too random to be worthwhile.

I find 4-grams to be pretty useful.  They found me domains like null5.com and 2jack.com, among others.

Training on domain names, and not words, means that even 4-grams have seemingly random chars.  For example, I just generated LLLLL domains with 4-grams, and got results like xvsyg, hpgjb, jdkei, etc.  This means there are domains that start with 'xvsy', 'jdke', etc.  Training on a dictionary word corpus would not produce these results.

I'm discovering that it's not hard to generate lots of possibilities with various rules.  The challenge is sorting the results and filtering out the junk, like above.  I have a few ideas about this:

* Score domains based on what proportion of the string contains dictionary words.  This is computationally non-trivial but it would be worthwhile
* Train another 2-gram or 3-gram model on the dictionary, then measure the domain by the probabilities of that dictionary-trained model generating the domain.