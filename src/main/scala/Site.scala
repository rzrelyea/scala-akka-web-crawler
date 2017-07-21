
case class Site(title: String, address: String, links: List[String])

//case class NoSite() extends Site("None", "None", List[String]())

object Internet {
  val sites: List[Site] = List(
    Site("The Go Programming Language",
      "http://golang.org/", //su, sk, sk, sk, sk
      List("http://golang.org/pkg/", "http://golang.org/cmd/")),
    Site("Packages",
      "http://golang.org/pkg/", //su, sk, sk
      List("http://golang.org/",
        "http://golang.org/cmd/",
        "http://golang.org/pkg/fmt/",
        "http://golang.org/pkg/os/")),
    Site("Package fmt",
      "http://golang.org/pkg/fmt/", //su
      List("http://golang.org/",
        "http://golang.org/pkg/", "http://golang.org/err")),
    Site("Package os", //su
      "http://golang.org/pkg/os/",
      List("http://golang.org/",
        "http://golang.org/pkg/")),
    Site("Packages",
      "http://golang.org/cmd/", List()) //su, sk
    )

  //Sucess list :
  /*
  http://golang.org/
  http://golang.org/pkg/
  http://golang.org/pkg/fmt/
  http://golang.org/pkg/os/
  http://golang.org/cmd/
   */

  //Skipped List
  /*
  http://golang.org/pkg/ , http://golang.org/pkg/ http://golang.org/pkg/
  http://golang.org/cmd/ , http://golang.org/cmd/
  http://golang.org/ , http://golang.org/ , http://golang.org/
  http://golang.org/pkg/fmt/
  http://golang.org/pkg/os/

   */

  //Failed
  //http://golang.org/err
  def find(link: String): Option[Site] = {
    this.sites.find(site => site.address.equalsIgnoreCase(link))
  }

}
