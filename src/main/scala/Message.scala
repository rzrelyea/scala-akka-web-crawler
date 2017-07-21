
import akka.actor.ActorRef
case class Page(url: String)
//case class NoPage() extends Page(url = null)
case class PageRequest (page:Page, replyTo: ActorRef)
case class RemovePage (page:Page)
case class PageCheck (page:Page, replyTo: ActorRef)
case class DoCrawl (page:Page)
case class DoNotCrawl (page:Page)
case class CrawlResult (succeeded:List[Page], failed: List[Page], skipped: List[Page], requestedPage: Page)
