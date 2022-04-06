package demo

import akka.actor.typed._
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.ExecutionContext

@main def runSupervisedGreeter(): Unit = {
  val unsupervisedBehavior = FailingGreeter.create()

  withTypedActorSystem(unsupervisedBehavior) { s =>
    s ! "boom"
    s ! "Daniel"
  }

  val supervisedBehavior =
    Behaviors.supervise(FailingGreeter.create()).onFailure[Exception](SupervisorStrategy.resume)

  withTypedActorSystem(supervisedBehavior) { s =>
    s ! "boom"
    s ! "Daniel"
  }
}

object FailingGreeter {
  def create(): Behavior[String] = Behaviors.receiveMessage {
    case "boom" =>
      println("stopping")
      throw new Exception("boom")
    case msg =>
      println(s"Hello $msg from Greeter 2")
      Behaviors.same
  }
}
