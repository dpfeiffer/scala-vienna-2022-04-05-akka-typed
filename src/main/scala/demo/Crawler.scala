package demo

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout

import scala.concurrent._
import scala.concurrent.duration._

@main def runCrawler(): Unit = {

  given Timeout          = Timeout(10.minutes)
  given ExecutionContext = ExecutionContext.global

  withTypedActorSystem(CrawlerManager.create(), stopAfter = 2000) { implicit s =>
    s.ask(CrawlerManager.Crawl("https://google.com" :: "https://firstbird.com" :: Nil, _)).foreach(println(_))
  }
}

object CrawlerManager {

  sealed trait Command
  case class Crawl(urls: List[String], replyTo: ActorRef[Result])        extends Command
  case class RecordIndividualResult(url: String, children: List[String]) extends Command

  sealed trait Result
  case class CrawlResult(urls: List[String]) extends Result

  def create(): Behavior[Command] = Behaviors.setup { ctx =>
    CrawlerManager(ctx).idle()
  }
}

class CrawlerManager(ctx: ActorContext[CrawlerManager.Command]) {

  import CrawlerManager._

  def idle(): Behavior[Command] = Behaviors.receiveMessage {
    case CrawlerManager.Crawl(urls, replyTo) =>
      urls.foreach { url =>

        /** Create a child Actor from the given [[akka.actor.typed.Behavior]] under a randomly chosen name. It is good
          * practice to name Actors wherever practical.
          */

        val worker = ctx.spawnAnonymous(CrawlerWorker.create())

        /** Create a message adapter that will convert or wrap messages such that other Actor’s protocols can be
          * ingested by this Actor.
          */

        val workerResultAdapter = ctx.messageAdapter[CrawlerWorker.Result] { case r: CrawlerWorker.CrawlResult =>
          RecordIndividualResult(r.url, r.children)
        }

        worker.tell(CrawlerWorker.Crawl(url, workerResultAdapter))
      }

      crawling(urls, Map.empty, replyTo)
    case _ =>
      Behaviors.unhandled
  }

  def crawling(urls: List[String],
               results: Map[String, List[String]],
               replyTo: ActorRef[CrawlResult]): Behavior[Command] = {

    if (urls.size == results.size) {
      replyTo.tell(CrawlResult(results.values.flatten.toList))
      idle()
    } else {
      Behaviors.receiveMessage {
        case RecordIndividualResult(url, children) =>
          println(s"Received result for $url")
          crawling(urls, results + (url -> children), replyTo)
        case x => Behaviors.unhandled
      }
    }
  }

}

object CrawlerWorker {
  sealed trait Command
  case class Crawl(url: String, replyTo: ActorRef[Result])                                  extends Command
  private case class Finish(url: String, children: List[String], replyTo: ActorRef[Result]) extends Command

  sealed trait Result
  case class CrawlResult(url: String, children: List[String]) extends Result

  def create(): Behavior[Command] = Behaviors.setup { _ =>
    Behaviors.withTimers { timers =>
      Behaviors.receiveMessage {
        case Crawl(url, reply) =>
          val urls = 1.to(5).map(i => s"$url$i").toList
          timers.startSingleTimer(Finish(url, urls, reply), 1.second)
          Behaviors.same

        case Finish(url, children, reply) =>
          reply.tell(CrawlResult(url, children))
          Behaviors.stopped(() => println("stopped worker"))
      }
    }
  }
}
