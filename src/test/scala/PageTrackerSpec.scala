import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import scala.concurrent.duration._
import akka.actor.Props
import akka.testkit.TestActors

class PageTrackerSpec(_system: ActorSystem)
  extends TestKit(_system)
  with ImplicitSender
  with Matchers
  with FlatSpecLike
  with BeforeAndAfterAll {
  def this() = this(ActorSystem("PageTrackerSpec"))

  val page = Page(Internet.sites.head.address)

  override def afterAll: Unit = {
    system.shutdown()
    system.awaitTermination(10.seconds)
  }


  "A page tracker" should "send a DoCrawl message when a URL wasn't crawled" in {
    val pageTracker = TestActorRef(Props[PageTracker])
    pageTracker ! PageCheck(page, testActor)
    expectMsg(DoCrawl(page))
  }

  "A page tracker" should "send a DoNotCrawl message when a URL was already crawled" in {
    val pageTracker = TestActorRef(Props[PageTracker])
    pageTracker ! PageCheck(page, testActor)
    expectMsg(DoCrawl(page))
    pageTracker ! PageCheck(page, testActor)
    expectMsg(DoNotCrawl(page))
  }

  "A page tracker" should "remove a URL when it receieves a RemovePage message" in {
    val pageTracker = TestActorRef[PageTracker]
    val actor = pageTracker.underlyingActor
    pageTracker ! PageCheck(page, testActor)
    expectMsg(DoCrawl(page))
    pageTracker ! RemovePage(page)
    actor.pages.contains(page) should be (false)
  }

}
