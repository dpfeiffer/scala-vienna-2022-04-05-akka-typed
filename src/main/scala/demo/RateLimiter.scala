package demo

import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.duration.FiniteDuration
import akka.actor.typed.Behavior
import akka.actor.typed.ActorRef
import scala.concurrent.duration._
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import scala.concurrent.ExecutionContext
import java.time.Instant
import java.time.temporal.ChronoUnit

@main def runRateLimiter() = {

  import RateLimiter._

  given ExecutionContext = ExecutionContext.global
  given Timeout          = Timeout(1.second)

  val behavior = RateLimiter.create(2, 1.seconds)

  withTypedActorSystem(behavior) { implicit s =>
    s.ask(Tick(_)).foreach(println(_))
    s.ask(Tick(_)).foreach(println(_))
    s.ask(Tick(_)).foreach(println(_))
    Thread.sleep(2000)
    s.ask(Tick(_)).foreach(println(_))

    Thread.sleep(1000)
    s.terminate()
  }

}

object RateLimiter {

  sealed trait Command
  case class Tick(replyTo: ActorRef[Result]) extends Command
  private case object Reset                  extends Command

  sealed trait Result
  case class Success(remaining: Int)           extends Result
  case class Exceeded(resetIn: FiniteDuration) extends Result

  def create(max: Int, duration: FiniteDuration): Behavior[RateLimiter.Command] = Behaviors.setup { ctx =>
    Behaviors.withTimers { timers =>

      /** Schedules a message to be sent repeatedly to the `self` actor with a fixed `delay` between messages.
        */

      timers.startTimerWithFixedDelay(Reset, duration)
      RateLimiter(max, duration).initialize(0, Instant.now)
    }
  }

}

class RateLimiter(max: Int, duration: FiniteDuration) {
  import RateLimiter._

  def initialize(n: Int, lastReset: Instant): Behavior[Command] = Behaviors.receiveMessage {
    case Reset =>
      initialize(0, Instant.now)
    case Tick(replyTo) if n < max =>
      replyTo.tell(Success(max - n - 1))
      initialize(n + 1, lastReset)
    case Tick(replyTo) =>
      val millisToNextReset = Instant.now.until(lastReset.plusMillis(duration.toMillis), ChronoUnit.MILLIS)
      replyTo.tell(Exceeded(millisToNextReset.millis))
      Behaviors.same
  }

}
