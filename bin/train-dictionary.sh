#!/bin/sh

# Trains 2-, 3-, and 4-gram markov models from the wordlist
sbt "train/run --textfile data/wordlist --modeldb data/dict-markov-2 --ngram 2"
sbt "train/run --textfile data/wordlist --modeldb data/dict-markov-3 --ngram 3"
sbt "train/run --textfile data/wordlist --modeldb data/dict-markov-4 --ngram 4"
