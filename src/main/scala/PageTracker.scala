import akka.actor.Actor
import scala.collection.mutable.{HashSet, Set}

class PageTracker extends Actor {
  var pages: Set[Page] = new HashSet[Page]()

  def receive = {

    case PageCheck(page, replyTo) => {
      if (pages.contains(page)) {
        replyTo ! DoNotCrawl(page)
      } else {
        pages.add(page)
        replyTo ! DoCrawl(page)
      }
    }
    case RemovePage(page) => {
       pages.remove(page)
    }
  }
}
