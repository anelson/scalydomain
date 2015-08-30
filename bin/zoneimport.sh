#/bin/sh

# Imports the .net and .com zone files into a LevelDB database
sbt "zoneimport/run data/com.zone data/net.zone data/domains"
