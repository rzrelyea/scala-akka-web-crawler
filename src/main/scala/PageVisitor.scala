import akka.actor.{ActorRefFactory, ActorRef, Actor}

import scala.collection.mutable.HashMap



class PageVisitor( tracker:ActorRef, childMaker: ActorRefFactory => ActorRef ) extends Actor{

  var crawlResult: CrawlResult = CrawlResult(succeeded = List[Page](),
    failed = List[Page](),
    skipped = List[Page](),
    requestedPage = Page(null))

  var testableChildren: scala.collection.mutable.Map[Page,ActorRef] = HashMap()
  var currentPageRequest: PageRequest = PageRequest(Page(null), null)

  def receive = {
    case request: PageRequest => {
      tracker ! PageCheck(request.page, self)
      crawlResult = CrawlResult(succeeded = List[Page](),
        failed = List[Page](),
        skipped = List[Page](),
        requestedPage = request.page)
      currentPageRequest = request
    }
    case DoCrawl(page) => {
      val site = Internet.find(page.url)
      if (site.isDefined) {
        site.get.links.foreach({ link =>
          val newChild = childMaker(context)
          //mapped page link to child actor
          testableChildren.put(Page(link), newChild)
          newChild ! PageRequest(Page(link), self)
        })
        // A parent is successful as long as he can discover all the children. parents success does not depend on childs crawl result
        crawlResult = CrawlResult(succeeded = List(page), failed = List[Page](),
          skipped = List[Page](),page)
        if(site.get.links.isEmpty){
          currentPageRequest.replyTo!crawlResult
        }
      } else {
        // A parent is not successful if he can't discover all the children. parents success does not depend on childs crawl result
        crawlResult = CrawlResult(succeeded = List[Page](), failed = List(page),
          skipped = List[Page](),page)
        currentPageRequest.replyTo!crawlResult
      }

    }
    case DoNotCrawl(page) => {
      crawlResult = CrawlResult(
        crawlResult.succeeded,
        crawlResult.failed,
        crawlResult.skipped :+ page,
        crawlResult.requestedPage)
      replyIfDone(page, false)
    }
    case CrawlResult(succeeded, failed, skipped, requestedPage) => {
      crawlResult = CrawlResult(
        crawlResult.succeeded ++ succeeded,
        crawlResult.failed ++ failed,
        crawlResult.skipped ++ skipped,
        crawlResult.requestedPage)
      replyIfDone(requestedPage, true)

    }
  }

  def replyIfDone(page: Page, removeChild: Boolean): Unit ={
    if(removeChild) {
      val removedActorOpt = testableChildren.remove(page)
      if (removedActorOpt.isEmpty){
        println("ERROR: tried to remove a child that didn't exist: " + page)
      } else {
        val completedChild = removedActorOpt.get
        context.system.stop(completedChild)
      }
    }

    if (testableChildren.isEmpty){
      currentPageRequest.replyTo ! crawlResult
    }

  }
}
