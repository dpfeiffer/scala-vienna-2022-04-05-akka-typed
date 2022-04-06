package demo

import akka.actor.typed._
import akka.actor.typed.scaladsl._

import scala.concurrent._

@main def runSequentialDataAccess(): Unit = {

  given ExecutionContext = ExecutionContext.global

  val behavior = SequentialDataAccess.create(DAO())
  withTypedActorSystem(behavior, stopAfter = 4000) { s =>
    s ! "foo"
    s ! "foo"
    s ! "foo"
  }

}

object SequentialDataAccess {

  /** `setup` is a factory for a behavior. Creation of the behavior instance is deferred until the actor is started, as
    * opposed to [[Behaviors.receive]] that creates the behavior instance immediately before the actor is running. The
    * `factory` function pass the `ActorContext` as parameter and that can for example be used for spawning child
    * actors.
    *
    * `setup` is typically used as the outer most behavior when spawning an actor, but it can also be returned as the
    * next behavior when processing a message or signal. In that case it will be started immediately after it is
    * returned, i.e. next message will be processed by the started behavior.
    */

  def create(dao: DAO)(using ExecutionContext): Behavior[String] = Behaviors.setup { ctx =>
    Behaviors.withStash(1) { stash =>
      SequentialDataAccess(dao, stash, ctx).idle()
    }
  }
}

class SequentialDataAccess(dao: DAO, stash: StashBuffer[String], ctx: ActorContext[String])(using ExecutionContext) {
  private def accessingData(): Behavior[String] = Behaviors.receiveMessage {
    case "done" =>
      stash.unstashAll(idle())
    case x =>
      stash.stash(x)
      Behaviors.same

  }
  private def idle(): Behavior[String] = Behaviors.receiveMessage { case "foo" =>
    ctx.pipeToSelf(dao.foo()) { _ => "done" }
    accessingData()
  }
}
