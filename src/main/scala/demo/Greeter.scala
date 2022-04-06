package demo

import akka.actor.typed._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout

import scala.concurrent._
import scala.concurrent.duration._

@main def runGreeter(): Unit = {
  given ExecutionContext = ExecutionContext.global
  given Timeout          = Timeout(5.seconds)

  /** The behavior of an actor defines how it reacts to the messages that it receives. The message may either be of the
    * type that the Actor declares and which is part of the [[ActorRef]] signature, or it may be a system [[Signal]]
    * that expresses a lifecycle event of either this actor or one of its child actors.
    */

  val behavior: Behavior[Greeter.Greet] = Greeter.create()

  /** An ActorSystem is home to a hierarchy of Actors. It is created using [[ActorSystem#apply]] from a [[Behavior]]
    * object that describes the root Actor of this hierarchy and which will create all other Actors beneath it. A system
    * also implements the [[ActorRef]] type, and sending a message to the system directs that message to the root Actor.
    *
    * Not for user extension.
    */

  given system: ActorSystem[Greeter.Greet] = ActorSystem(behavior, "greeter")

  val result: Future[Greeter.GreetResult] = system.ask(respondto => Greeter.Greet("Daniel", respondto))

  val r = Await.result(result, 5.seconds)

  println(r.message)

  system.terminate()
}

object Greeter {

  case class Greet(name: String, respondTo: ActorRef[GreetResult])

  case class GreetResult(message: String)

  def create(): Behavior[Greet] = Behaviors.receiveMessage { msg =>
    val result = GreetResult(s"Hello ${msg.name} from Greeter 2")
    msg.respondTo.tell(result)
    Behaviors.same
  }
}
