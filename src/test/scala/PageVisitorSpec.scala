import akka.actor.{ActorRef, ActorRefFactory, Props, ActorSystem}
import akka.testkit.{TestProbe, TestActorRef, ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import scala.collection.mutable
import scala.concurrent.duration._

class PageVisitorSpec (_system: ActorSystem)
  extends TestKit(_system)
    with ImplicitSender
    with Matchers
    with FlatSpecLike
    with BeforeAndAfterAll {
  def this() = this(ActorSystem("PageVisitorSpec"))

  val page = Page(Internet.sites.head.address)
  val children: mutable.MutableList[TestProbe] = mutable.MutableList()

  override def afterAll: Unit = {
    system.shutdown()
    system.awaitTermination(10.seconds)
  }

  def childMaker(actorRefFactory: ActorRefFactory): ActorRef = {
    val newChild = TestProbe()
    children += newChild
    return children.last.ref
  }

  def statelessChildMaker(actorRefFactory: ActorRefFactory): ActorRef = {
    TestProbe().ref
  }

  def noChildMaker(actorRefFactory: ActorRefFactory): ActorRef = {
    throw new RuntimeException("Child actor was made when it shouldn't have been made")
  }

  "A page visitor" should "send a PageCheck for self page" in {
    val localTestProbe = TestProbe()
    val localTestActor = localTestProbe.ref
    val pageVisitor :TestActorRef[PageVisitor]= TestActorRef(Props(new PageVisitor(localTestActor, childMaker)))
    pageVisitor ! PageRequest(page, localTestActor)
    localTestProbe.expectMsg(PageCheck(page,pageVisitor))
  }

  it should "react to DoCrawl by creating a new Actor for each page and send a PageRequest to each " +
    "actor and create CrawlResult, adding itself to success list" in {
    val localTestProbe = TestProbe()
    val localTestActor = localTestProbe.ref
    val pageVisitor :TestActorRef[PageVisitor]= TestActorRef(Props(new PageVisitor(localTestActor, childMaker)))
    val pages: mutable.Queue[Page] = mutable.Queue()
    pages += Page("http://golang.org/pkg/")
    pages += Page("http://golang.org/cmd/")

    val actor = pageVisitor.underlyingActor
    pageVisitor ! DoCrawl(page)
    actor.crawlResult should not be (null)
    actor.crawlResult.succeeded.size should be (1)
    actor.crawlResult.succeeded.head should be (page)
    actor.crawlResult.failed.isEmpty should be (true)
    actor.crawlResult.skipped.isEmpty should be (true)
    actor.crawlResult.requestedPage should be (page)

    actor.testableChildren.size should be  (pages.size)
    children.foreach(
      child => {
        child.expectMsg(100 millis, PageRequest(pages.dequeue(), pageVisitor))
      }
    )
    pages.isEmpty should be (true)
  }

  it should "react to DoNotCrawl by adding itelsf to the skipped list in CrawlResult and " +
    "sending CrawlResult to it's replyTo actor ref" in {

    val localTestProbe = TestProbe()
    val localTestActor = localTestProbe.ref
    val pageVisitor :TestActorRef[PageVisitor]= TestActorRef(Props(new PageVisitor(localTestActor, noChildMaker)))
    val actor = pageVisitor.underlyingActor
    actor.currentPageRequest = PageRequest(page, localTestActor)
    actor.crawlResult = CrawlResult(succeeded = List[Page](),
      failed = List[Page](),
      skipped = List[Page](),
      requestedPage = page)
    pageVisitor ! DoNotCrawl(page)
    val expectedMessage = CrawlResult(List(), List(), List(page), page)
    localTestProbe.expectMsg(100 millis, expectedMessage)

  }

  it should "react to received Successful CrawlResult by merging into own CrawlResult" in {
    val localTestProbe = TestProbe()
    val localTestActor = localTestProbe.ref
    val pageVisitor :TestActorRef[PageVisitor]= TestActorRef(Props(new PageVisitor(localTestActor, noChildMaker)))
    val successChildPage1 = Page("success1")
    val  successResult1 =  CrawlResult(succeeded =List(successChildPage1),failed=List(), skipped=List(), successChildPage1)
    val successChildPage2 = Page("success2")
    val successChildPage3 = Page("success3")
    val failedChildPage2 = Page("failed2")
    val skippedChildPage2 = Page("skipped2")
    val  successResult2 =  CrawlResult(
      succeeded =List(successChildPage2,successChildPage3),
      failed=List(failedChildPage2),
      skipped=List(skippedChildPage2),
      successChildPage2)
    pageVisitor ! PageRequest(page,testActor)

    val actor = pageVisitor.underlyingActor
    pageVisitor ! successResult1

    println("Crawl result is " + actor.crawlResult.succeeded)

    actor.crawlResult.succeeded.contains(successChildPage1) should be (true)
    actor.crawlResult.succeeded.size should be (1)


    pageVisitor ! successResult2

    actor.crawlResult.succeeded.contains(successChildPage2) should be (true)
    actor.crawlResult.succeeded.contains(successChildPage3) should be (true)
    actor.crawlResult.succeeded.size should be (3)
    actor.crawlResult.failed.contains(failedChildPage2) should be (true)
    actor.crawlResult.failed.size should be (1)
    actor.crawlResult.skipped.contains(skippedChildPage2) should be (true)
    actor.crawlResult.skipped.size should be (1)


    val failedChildPage1 = Page("failed1")
    val failedResult1 =  CrawlResult(succeeded = List(),failed=List(failedChildPage1), skipped=List(), failedChildPage1)
    pageVisitor ! failedResult1

    actor.crawlResult.failed.contains(failedChildPage1) should be (true)
    actor.crawlResult.failed.size should be (2)

    val skippedChildPage1 = Page("skipped1")
    val skippedResult1 =  CrawlResult(succeeded = List(),failed=List(), skipped=List(skippedChildPage1), skippedChildPage1)
    pageVisitor ! skippedResult1

    actor.crawlResult.skipped.contains(skippedChildPage1) should be (true)
    actor.crawlResult.skipped.size should be (2)

    actor.crawlResult.requestedPage should be (page)

  }


  it should "react to received any CrawlResult by Killing child Actor and send back CrawlResults " +
    "to reply to Actor if child Actors are gone" in {

    val pageVisitor :TestActorRef[PageVisitor]= TestActorRef(Props(new PageVisitor(testActor, statelessChildMaker)))
    val localTestProbe = TestProbe()
    val localTestActor = localTestProbe.ref
    pageVisitor ! PageRequest(page, localTestActor)

    pageVisitor ! DoCrawl(page)
    val actor = pageVisitor.underlyingActor


    val successChildPage1 = Page("http://golang.org/pkg/")
    //actor.testableChildren.put(successChildPage1, statelessChildMaker(this.system))
    val successChildPage2 = Page("http://golang.org/cmd/")
    //actor.testableChildren.put(successChildPage2, statelessChildMaker(this.system))

    actor.testableChildren.size should be (2)

    val successResult1 =  CrawlResult(succeeded =List(successChildPage1),failed=List(), skipped=List(), successChildPage1)
    pageVisitor ! successResult1
    val successResult2 =  CrawlResult(succeeded =List(successChildPage2),failed=List(), skipped=List(), successChildPage2)
    pageVisitor ! successResult2

    val expectedResult = CrawlResult(succeeded =List(page,successChildPage1, successChildPage2),failed=List(), skipped=List(), page)

    localTestProbe.expectMsg(100 millis, expectedResult)

    actor.testableChildren.size should be (0)

  }

  it should "handle a page with no children" in {
    val pageNoChild = Page("http://golang.org/cmd/")
    val pageTracker :TestActorRef[PageTracker]= TestActorRef(Props(new PageTracker()))
    val pageVisitor :TestActorRef[PageVisitor]= TestActorRef(Props(new PageVisitor(pageTracker, noChildMaker)))
    val localTestProbe = TestProbe()
    val localTestActor = localTestProbe.ref
    pageVisitor ! PageRequest(pageNoChild,localTestActor)
    pageVisitor ! DoCrawl(pageNoChild)
    val expectedMessage = CrawlResult(List(pageNoChild), List(), List(), pageNoChild)
    localTestProbe.expectMsg(100 millis, expectedMessage)
  }

  /*

   success means i could gather the list of links from my own content

    Send page check for self
    - get back dnc: cr(self-page,su=() ,f=() ,sk=(self-page))
    - get back dc:
      - create actors for each child page
      - add child cr results to my cr
          - cr(child-page, su(), f(), sk(child-page))
          - cr(child-page, su(), f(child-page), sk())
          - cr(child-page, su(child-page), f(), sk())
      - check for outstanding children, if none,
        - add self to own cr success: cr(self-page, su+=self-page, f, sk)
        - send my cr up to parent

   */



}
