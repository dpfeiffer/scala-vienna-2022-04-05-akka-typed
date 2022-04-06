package demo

import akka.actor.typed._
import scala.concurrent._

def withTypedActorSystem[A](behavior: Behavior[A], stopAfter: Long = 500)(f: ActorSystem[A] => Unit): Unit = {
  val s = ActorSystem(behavior, "test")

  f(s)

  Thread.sleep(stopAfter)
  s.terminate()
}

class DAO(using ExecutionContext) {
  def foo(): Future[Unit] = {
    Future {
      blocking {
        println("loading")
        Thread.sleep(1000)
        println("finished loading")
      }
    }
  }
}
