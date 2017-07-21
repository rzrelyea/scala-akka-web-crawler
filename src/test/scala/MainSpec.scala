import akka.actor.{ActorRef, ActorRefFactory, ActorSystem, Inbox, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.collection.mutable
import scala.concurrent.duration._

class MainSpec (_system: ActorSystem)
  extends TestKit(_system)
  with ImplicitSender
  with Matchers
  with FlatSpecLike
  with BeforeAndAfterAll {
  def this() = this(ActorSystem("PageVisitorSpec"))

  val actorSystem = this.system


  val page = Page(Internet.sites.head.address)
//create the system
  val crawlersystem = ActorSystem("crawler")
  // Create the 'tracker' actor
  val tracker = crawlersystem.actorOf(Props[PageTracker], "tracker")

  override def afterAll: Unit = {
    crawlersystem.shutdown()
    crawlersystem.awaitTermination(10.seconds)
  }

  "A web crawler" should "crawl all the pages in the Internet" in {



    // Create an "actor-in-a-box"
    val inbox = Inbox.create(crawlersystem)
    // Create the page visitor
    val visitor = crawlersystem.actorOf(Props(new PageVisitor(tracker, childMaker)),"visitor")
    // Tell the 'greeter' to change its 'greeting' message
    visitor!PageRequest(page,inbox.getRef())
    val resul= inbox.receive(5.seconds).asInstanceOf[CrawlResult]
    println(resul)
//    val greeter = system.actorOf(Props(new PageVisitor(testActor, childMaker)), "pageVisitor")
//
//    val pageVisitor :TestActorRef[PageVisitor]= TestActorRef(Props(new PageVisitor(testActor, childMaker)))
//    pageVisitor ! PageRequest(page, testActor)
//    expectMsg()
  }
  def childMaker(actorRefFactory: ActorRefFactory): ActorRef = {
   return actorRefFactory.actorOf(Props(new PageVisitor(tracker, childMaker)))
  }


}
