#!/bin/sh

# Using the Norvig list of the top 300K English words sorted by frequency, build some word lists with various top-n subsets
cat data/wordlist-norvig-counts | awk '{print $1}' | head -n 1000 > data/wordlist-top1000
cat data/wordlist-norvig-counts | awk '{print $1}' | head -n 5000 > data/wordlist-top5000
cat data/wordlist-norvig-counts | awk '{print $1}' | head -n 10000 > data/wordlist-top10000
cat data/wordlist-norvig-counts | awk '{print $1}' | head -n 20000 > data/wordlist-top20000
cat data/wordlist-norvig-counts | awk '{print $1}' | head -n 30000 > data/wordlist-top30000
cat data/wordlist-norvig-counts | awk '{print $1}' > data/wordlist-norvig-all
