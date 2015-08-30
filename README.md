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
* When a domain is generated that is almost right, like 'mindsymx', zoom in on that output and see what other strings can be generated from the graph nodes one, two, maybe three n-grams before the end of the sequence
* Given a domain that has potential, explore it by trying 'more like this' domains, where you traverse the same chain that generates that domain, but giving much stronger weights to the path taken to generate that domain.  This way you'll generate similar output, but different in interesting ways
* portmanteu generation ala werdmerge is also useful

# Scoring

I'm playing with ways to score.  I did a run against the dictionary-trained 4-gram markov model with maxlength 6.  For score I computed the mean of each character's probability.  The results are not promising.

For `sbt "generate/run --domaindb data/domain --modeldb data/dict-markov-4 --ngram 4 --count 200 --maxlength 6"`:

* `abmhos` score is 0.445430549009762
* `whoppy` score is 0.24894521910470443
* `spist` score is 0.1289497172539744
* `outbeg` score is 0.26271347376782245

I would say `whoppy` and `spist` and `outbeg` are English-seeming easily-pronounced words, and `abmhos` is random garbage, but the scores suggest otherwise.  I will look more closely at the per-char probabilities to see why this is

Here's the result:

		info] Generating domain names
		[info] 	abmhos	0.445431	P(a)=0.066741,P(b)=0.047666,P(m)=0.003607,P(h)=0.500000,P(o)=1.000000,P(s)=0.500000,P($)=1.000000
		[info] 	whoppy	0.248945	P(w)=0.017920,P(h)=0.171282,P(o)=0.118692,P(p)=0.047244,P(p)=0.625000,P(y)=0.048193,P($)=0.714286
		[info] 	spist	0.128950	P(s)=0.105846,P(p)=0.088650,P(i)=0.193825,P(s)=0.011041,P(t)=0.040000,P($)=0.334337
		[info] 	outbeg	0.262713	P(o)=0.034627,P(u)=0.162124,P(t)=0.957588,P(b)=0.102455,P(e)=0.088083,P(g)=0.294118,P($)=0.200000
		[info] 	unnate	0.354218	P(u)=0.064716,P(n)=0.891755,P(n)=0.012675,P(a)=0.262745,P(t)=0.271739,P(e)=0.400000,P($)=0.575893
		[info] 	bygang	0.267179	P(b)=0.049555,P(y)=0.007294,P(g)=0.047619,P(a)=0.333333,P(n)=1.000000,P(g)=0.300000,P($)=0.132450
		[info] 	caulix	0.265814	P(c)=0.086222,P(a)=0.197259,P(u)=0.037443,P(l)=0.220721,P(i)=0.307692,P(x)=0.011364,P($)=1.000000
		[info] 	pygous	0.364942	P(p)=0.094374,P(y)=0.019697,P(g)=0.049383,P(o)=0.281250,P(u)=0.230769,P(s)=1.000000,P($)=0.879121
		[info] 	haemia	0.385307	P(h)=0.036626,P(a)=0.207315,P(e)=0.055157,P(m)=0.917808,P(i)=0.175141,P(a)=0.693548,P($)=0.611554
		[info] 	cissio	0.153185	P(c)=0.086222,P(i)=0.035266,P(s)=0.051887,P(s)=0.109091,P(i)=0.465909,P(o)=0.317697,P($)=0.006221

This shows the problem perfectly.  That final sequence `hos` in `abmhos` is very high probability, because this is obviously an obscure branch of the tree and thus there are not many options.  Let's try computing a score that is the _lowest_ probability of the entire chain:

		[info] Generating domain names
		[info] 	abmhos	0.003607	P(a)=0.066741,P(b)=0.047666,P(m)=0.003607,P(h)=0.500000,P(o)=1.000000,P(s)=0.500000,P($)=1.000000
		[info] 	whoppy	0.017920	P(w)=0.017920,P(h)=0.171282,P(o)=0.118692,P(p)=0.047244,P(p)=0.625000,P(y)=0.048193,P($)=0.714286
		[info] 	spist	0.011041	P(s)=0.105846,P(p)=0.088650,P(i)=0.193825,P(s)=0.011041,P(t)=0.040000,P($)=0.334337
		[info] 	outbeg	0.034627	P(o)=0.034627,P(u)=0.162124,P(t)=0.957588,P(b)=0.102455,P(e)=0.088083,P(g)=0.294118,P($)=0.200000
		[info] 	nosity	0.013957	P(n)=0.036936,P(o)=0.667754,P(s)=0.013957,P(i)=0.091667,P(t)=0.164794,P(y)=0.324638,P($)=0.974820
		[info] 	unpite	0.049264	P(u)=0.064716,P(n)=0.891755,P(p)=0.077692,P(i)=0.049264,P(t)=0.211765,P(e)=0.210526,P($)=0.311111
		[info] 	foetic	0.013169	P(f)=0.032825,P(o)=0.212357,P(e)=0.013169,P(t)=0.468750,P(i)=0.444444,P(c)=0.647059,P($)=0.557315
		[info] 	scopid	0.009119	P(s)=0.105846,P(c)=0.078785,P(o)=0.140007,P(p)=0.132678,P(i)=0.300915,P(d)=0.009119,P($)=0.857143
		[info] 	unsed	0.064716	P(u)=0.064716,P(n)=0.891755,P(s)=0.120738,P(e)=0.122273,P(d)=0.067055,P($)=0.571429
		[info] 	ungaio	0.027190	P(u)=0.064716,P(n)=0.891755,P(g)=0.027190,P(a)=0.142596,P(i)=0.168000,P(o)=0.040000,P($)=0.333333

`abmhos` is now scoring very low, which is correct.  But now `scopid` is scoring very low, but that's a pretty good name, certainly better than `ungaio` which scored much higher.

Maybe looking at per-character probabilities is the wrong answer.  Maybe I should be looking at per n-gram probabilities.  The model doesn't currently capture that data but I easily could start to include it.  Let's try.

		> sbt "generate/run --domaindb data/domain --modeldb data/dict-markov-4 --count 10 --maxlength 6 --include abmhos,whoppy,spist,outbeg,scopid,unsed,ungaio"
