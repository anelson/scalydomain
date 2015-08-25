package scalydomain

import scalydomain.core.ZoneFile

object Hi {
  def main(args: Array[String]) = {
  	println("Hi!")
  	val whereami = System.getProperty("user.dir")
  	println(s"cwd: $whereami")
  	val zonefile = new ZoneFile("data/com.zone")
  	var count: Long = 0

  	for (domain <- zonefile) count = count + 1

  	println(s"There are $count domains")
  }
}
