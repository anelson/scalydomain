#!/bin/sh

# Trains 2-, 3-, and 4-gram markov models from the domains database
sbt "train/run --domaindb data/domain --modeldb data/markov-2 --ngram 2 --maxlength 12"
sbt "train/run --domaindb data/domain --modeldb data/markov-3 --ngram 3 --maxlength 12"
sbt "train/run --domaindb data/domain --modeldb data/markov-4 --ngram 4 --maxlength 12"
