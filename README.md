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


		> bin/train-dictionary.sh
		> sbt "generate/run --domaindb data/domain --modeldb data/dict-markov-4 --count 10 --maxlength 6 --include abmhos,whoppy,spist,outbeg,scopid,unsed,ungaio"
		info] Generating domain names
		[info] 	abmhos	0.000001	P(abmh)=0.000001,P(bmho)=0.000001,P(mhos)=0.000002
		[info] 	whoppy	0.000004	P(whop)=0.000004,P(hopp)=0.000037,P(oppy)=0.000012
		[info] 	spist	0.000022	P(spis)=0.000022,P(pist)=0.000146
		[info] 	outbeg	0.000002	P(outb)=0.000085,P(utbe)=0.000007,P(tbeg)=0.000002
		[info] 	scopid	0.000006	P(scop)=0.000385,P(copi)=0.000145,P(opid)=0.000006
		[info] 	unsed	0.000034	P(unse)=0.000151,P(nsed)=0.000034
		[info] 	ungaio	0.000001	P(unga)=0.000055,P(ngai)=0.000011,P(gaio)=0.000001
		[info] 	inappe	0.000030	P(inap)=0.000030,P(napp)=0.000122,P(appe)=0.000231
		[info] 	cozeys	0.000001	P(coze)=0.000009,P(ozey)=0.000001,P(zeys)=0.000001
		[info] 	tomcab	0.000000	P(tomc)=0.000003,P(omca)=0.000003,P(mcab)=0.000000

This is dreadful!  `whoppy` is a great name, but it scores below `scopid` and `inappe`.  `tomcab` is a brillian name and it's score is so close to zero it doesn't even register!

The independent probabilities of each n-gram clearly say very little about the quality of the resulting domain name.  Maybe the conditional probabilities would be more informative, but I'd have to modify my model substantially to be able to compute that.

It seems stupid, but what about the product of the probabilities, or if we want to really penalize low probability transitions, the product of the squares of the character probabilities?  Let's try that:

		> sbt "generate/run --domaindb data/domain --modeldb data/dict-markov-4 --count 10 --maxlength 6 --include abmhos,whoppy,spist,outbeg,scopid,unsed,ungaio --sort"
		[info] Generating domain names
		[info] 	unsed	3.264541863138844E-5	P(u)=0.064716,P(n)=0.891755,P(s)=0.120738,P(e)=0.122273,P(d)=0.067055,P($)=0.571429
		[info] 	pogony	5.3536449708373905E-6	P(p)=0.094374,P(o)=0.119639,P(g)=0.007876,P(o)=0.548387,P(n)=0.965517,P(y)=0.142857,P($)=0.795918
		[info] 	abmhos	2.8686008686123427E-6	P(a)=0.066741,P(b)=0.047666,P(m)=0.003607,P(h)=0.500000,P(o)=1.000000,P(s)=0.500000,P($)=1.000000
		[info] 	outbeg	2.8537376516765284E-6	P(o)=0.034627,P(u)=0.162124,P(t)=0.957588,P(b)=0.102455,P(e)=0.088083,P(g)=0.294118,P($)=0.200000
		[info] 	lapids	2.0758137144938927E-6	P(l)=0.025998,P(a)=0.295266,P(p)=0.055306,P(i)=0.256757,P(d)=0.609375,P(s)=0.031250,P($)=1.000000
		[info] 	ungaio	5.012019437639485E-7	P(u)=0.064716,P(n)=0.891755,P(g)=0.027190,P(a)=0.142596,P(i)=0.168000,P(o)=0.040000,P($)=0.333333
		[info] 	upfuls	4.033832844306495E-7	P(u)=0.064716,P(p)=0.034752,P(f)=0.035714,P(u)=0.035714,P(l)=0.888889,P(s)=0.170213,P($)=0.929412
		[info] 	whoppy	3.703013169465159E-7	P(w)=0.017920,P(h)=0.171282,P(o)=0.118692,P(p)=0.047244,P(p)=0.625000,P(y)=0.048193,P($)=0.714286
		[info] 	scopid	3.6432315010894386E-7	P(s)=0.105846,P(c)=0.078785,P(o)=0.140007,P(p)=0.132678,P(i)=0.300915,P(d)=0.009119,P($)=0.857143
		[info] 	spist	2.685425150496133E-7	P(s)=0.105846,P(p)=0.088650,P(i)=0.193825,P(s)=0.011041,P(t)=0.040000,P($)=0.334337

That's awful.  `unsed` is a good one, but `abmhos` is probably the worst in that list and it scores second.  That's the product of the probabilities.  Let's try the square:

		> sbt "generate/run --domaindb data/domain --modeldb data/dict-markov-4 --count 10 --maxlength 6 --include abmhos,whoppy,spist,outbeg,scopid,unsed,ungaio --sort"
		info] Generating domain names
		[info] 	unsed	1.0657233576186035E-9	P(u)=0.064716,P(n)=0.891755,P(s)=0.120738,P(e)=0.122273,P(d)=0.067055,P($)=0.571429
		[info] 	pruria	2.5086756970315492E-11	P(p)=0.094374,P(r)=0.275328,P(u)=0.010157,P(r)=0.119565,P(i)=1.000000,P(a)=0.333333,P($)=0.476190
		[info] 	abmhos	8.228870943403486E-12	P(a)=0.066741,P(b)=0.047666,P(m)=0.003607,P(h)=0.500000,P(o)=1.000000,P(s)=0.500000,P($)=1.000000
		[info] 	outbeg	8.143818584596265E-12	P(o)=0.034627,P(u)=0.162124,P(t)=0.957588,P(b)=0.102455,P(e)=0.088083,P(g)=0.294118,P($)=0.200000
		[info] 	oozoic	1.5715123054092783E-12	P(o)=0.034627,P(o)=0.014912,P(z)=0.072222,P(o)=0.230769,P(i)=0.200000,P(c)=0.738462,P($)=0.986301
		[info] 	ungaio	2.5120338843276016E-13	P(u)=0.064716,P(n)=0.891755,P(g)=0.027190,P(a)=0.142596,P(i)=0.168000,P(o)=0.040000,P($)=0.333333
		[info] 	whoppy	1.37123065332324E-13	P(w)=0.017920,P(h)=0.171282,P(o)=0.118692,P(p)=0.047244,P(p)=0.625000,P(y)=0.048193,P($)=0.714286
		[info] 	scopid	1.3273135770530406E-13	P(s)=0.105846,P(c)=0.078785,P(o)=0.140007,P(p)=0.132678,P(i)=0.300915,P(d)=0.009119,P($)=0.857143
		[info] 	trowet	1.2069840363297618E-13	P(t)=0.050034,P(r)=0.260005,P(o)=0.117751,P(w)=0.044944,P(e)=0.390244,P(t)=0.017241,P($)=0.750000
		[info] 	spist	7.21150823891718E-14	P(s)=0.105846,P(p)=0.088650,P(i)=0.193825,P(s)=0.011041,P(t)=0.040000,P($)=0.334337

That's just unspeakably bad.

What if we sort the character probabilities, lowest to highest, and use that vector of probabilities as the sort key?

		> sbt "generate/run --domaindb data/domain --modeldb data/dict-markov-4 --count 10 --maxlength 6 --include abmhos,whoppy,spist,outbeg,scopid,unsed,ungaio --sort"
			info] Generating domain names
		[info] 	unsed	P(u)=0.064716,P(n)=0.891755,P(s)=0.120738,P(e)=0.122273,P(d)=0.067055,P($)=0.571429
		[info] 	outbeg	P(o)=0.034627,P(u)=0.162124,P(t)=0.957588,P(b)=0.102455,P(e)=0.088083,P(g)=0.294118,P($)=0.200000
		[info] 	furcae	P(f)=0.032825,P(u)=0.087040,P(r)=0.221888,P(c)=0.095023,P(a)=0.723404,P(e)=0.052632,P($)=0.800000
		[info] 	aureic	P(a)=0.066741,P(u)=0.054887,P(r)=0.114330,P(e)=0.171233,P(i)=0.029412,P(c)=0.066667,P($)=0.250000
		[info] 	ungaio	P(u)=0.064716,P(n)=0.891755,P(g)=0.027190,P(a)=0.142596,P(i)=0.168000,P(o)=0.040000,P($)=0.333333
		[info] 	whoppy	P(w)=0.017920,P(h)=0.171282,P(o)=0.118692,P(p)=0.047244,P(p)=0.625000,P(y)=0.048193,P($)=0.714286
		[info] 	spist	P(s)=0.105846,P(p)=0.088650,P(i)=0.193825,P(s)=0.011041,P(t)=0.040000,P($)=0.334337
		[info] 	scopid	P(s)=0.105846,P(c)=0.078785,P(o)=0.140007,P(p)=0.132678,P(i)=0.300915,P(d)=0.009119,P($)=0.857143
		[info] 	jamnum	P(j)=0.007126,P(a)=0.314010,P(m)=0.076923,P(n)=0.016667,P(u)=1.000000,P(m)=0.200000,P($)=1.000000
		[info] 	abmhos	P(a)=0.066741,P(b)=0.047666,P(m)=0.003607,P(h)=0.500000,P(o)=1.000000,P(s)=0.500000,P($)=1.000000

That's a bit better, but the problem is the ways in which the scoring fails is spectacular.  `unsed` is reasonable as the top scoring word.  But `furcae` and `aureic` are crap, and `whoppy` which is one of the best words is in the bottom 50%

Given that I can't seem to score words reliably, I am looking into better word lists.  I started searching for available domains matching some of the Moby word lists, and this has lead me to discover there is a shocking amount of garbage in these files, which makes it no wonder it generates rubbish.  I downloaded the Norvig data set from Google; tomorrow I need to re-generate the Markov models from this list and see if word generation improves.  This might also be the key to a more accurate scoring mechanism, particularly if I score against a model of, say, the 2000 most common English words, no matter what model was used to generate the words
