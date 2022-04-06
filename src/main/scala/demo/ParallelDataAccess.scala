package demo

import akka.actor.typed._
import akka.actor.typed.scaladsl._

import scala.concurrent._

@main def runParallelDataAccess(): Unit = {
  given ExecutionContext = ExecutionContext.global

  val behavior = ParallelDataAccess.create(DAO())

  withTypedActorSystem(behavior) { s =>
    s ! "foo"
    s ! "foo"
    s ! "foo"
  }
}

object ParallelDataAccess {
  def create(dao: DAO)(using ExecutionContext): Behavior[String] = Behaviors.receiveMessage { case "foo" =>
    dao.foo()
    Behaviors.same
  }
}
