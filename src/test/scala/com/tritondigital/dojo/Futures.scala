package com.tritondigital.dojo


import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.concurrent.ScalaFutures
import org.scalautils.TypeCheckedTripleEquals

import scala.concurrent.{TimeoutException, Future, blocking}
import scala.concurrent.duration._


import org.scalatest.{Matchers, FunSuite}

import scala.language.postfixOps
import scala.util.{Failure, Success}

class Futures extends FunSuite with ScalaFutures with Matchers with TypeCheckedTripleEquals {

  // A Future[A] represents a computation that will eventually return a value of type A
  //  futures (time + failure) are simpler than actors (time + failure + state), but actors are strictly more expressive
  //  "Don't use actors for concurrency. Instead, use actors for state and use futures for concurrency."

  //https://github.com/alexandru/scala-best-practices/blob/master/README.md

  // Actors need an execution context
  import scala.concurrent.ExecutionContext.Implicits.global

  // A dummy future
  def fut[A](value: A, wait: Duration = 500 millis): Future[A] = Future {
    blocking {
      Thread.sleep(wait.toMillis)
    }
    value
  }

  def fallback[A](default: A, timeout: Duration): Future[A] = fut(default, timeout)

  def failed(): Future[Int] = Future.failed(new RuntimeException(""))

  def futOfFileContent(): Future[String] = Future {
    scala.io.Source.fromFile("myText.txt").mkString
  }

  implicit val patience = PatienceConfig(1 second, 10 millis) // configuration of the futures (max & retries)

  test("Check the future value") {
    val f = fut(1)
    assert(f.futureValue === 1)
  }


  test("Check that the future is ready within 500 milliseconds (or not)") {
    val fast = fut(1, wait = 100 millis)
    assert(fast.isReadyWithin(500 millis))
    val slow = fut(1, wait = 1 second)
    assert(!slow.isReadyWithin(500 millis))
  }

  test("Race between two futures (i.e keep the first one to complete)") {
    val fast = fut(1, wait = 100 millis)
    val slow = fut(2, wait = 1 second)
    val faster = Future.firstCompletedOf(Seq(slow, fast))
    assert(faster.isReadyWithin(500 millis))
    assert(faster.futureValue === 1)
  }

  test("Use a race to replace a timeout with a default value") {
    val slow = fut(2, wait = 100 second)
    val default = fallback(0, 100 millis)
    val faster = Future.firstCompletedOf(Seq(slow, default))
    assert(faster.isReadyWithin(1 second))
    assert(faster.futureValue === 0)
  }

  test("Fallback if a future fails") {
    val bad1 = failed()
    val bad2 = failed()
    val good = fut(1, wait = 100 millis)
    val chain = bad1.fallbackTo(bad2).fallbackTo(good)
    assert(chain.futureValue === 1)
  }

  test("Recover if a future fails (with a value)") {
    val bad1 = failed()
    val chain = bad1.recover {
      case e: RuntimeException => 0
    }
    assert(chain.futureValue === 0)
  }

  test("Recover if a future fails (with another future)") {
    val bad1 = failed()
    val chain = bad1.recoverWith {
      case e: RuntimeException => fut(0) // More precise than fallbackTo, but also more verbose
    }
    assert(chain.futureValue === 0)
  }

  test("Recover if a future fails (with an Either value)") {
    sealed trait AppError
    case class UserNotExistingError() extends AppError
    case class UnknownError(e: Throwable) extends AppError

    val bad1 = failed()
    val chain: Future[Either[AppError, Int]] = bad1.map(Right(_)).recover {
      case e: RuntimeException => Left(UserNotExistingError())
      case e: Throwable => Left(UnknownError(e))
    }
    assert(chain.futureValue === Left(UserNotExistingError()))
  }

  // We can also register callbacks, but I prefer the fallback/recover methods
  // Callbacks perform side effects and they are not tracked by the type system
  /*f onComplete {
    case Success(value) => println(value)
    case Failure(t) => println("An error has occured: " + t.getMessage)
  }*/


  // Hint: Future is a Functor, just as List/Seq/Option
  test("Map a function on a future") {
    val f: Future[Int] = fut(1)
    val f2: Future[String] = f.map(_.toString)
    assert(f2.futureValue === "1")
  }

  test("Map twice on a future") {
    val f = fut(1)
    val f2 = f.map(_.toString).map(_ ++ "!")
    assert(f2.futureValue === "1!")
  }

  test("Print the content of a future") {
    val f = fut(1)
    f.foreach(println)
  }


  // Seq[Future[A]] -> Future[Seq[A]]
  test("Run a list of futures") {
    val futs: Seq[Future[Int]] = (1 to 50).map(fut(_))
    val futs2: Seq[Future[String]] = futs.map(f => f.map(_.toString))
    assert(Future.sequence(futs2).futureValue === (1 to 50).map(_.toString)) // Wait for them in parallel
  }


  test("Run the same future constructor over a list of values (we need to 'traverse' a sequence of Int))") {
    def f(n: Int) = fut(n + 1)
    assert(Future.traverse(1 to 50)(f).futureValue === (2 to 51)) // Create them and wait for them in parallel
  }


  // Future implements flatMap, so it can be used in 'for' blocks
  // We got Future[Future[Future[A]] -> Future[A]
  test("Chain several futures in a for block. Each of them adds 1 to the previous future content") {
    val d = 100 millis
    val f = for {
      a <- fut(1, d)
      b <- fut(a + 1, d) // the for block allows reusing some previous result
      c <- fut(b + 1, d)
    } yield c
    assert(f.futureValue === 3)
  }

  test("Chain several futures in a for block, with one of them failing") {
    val f = for {
      a <- fut(1)
      b <- failed()
      c <- fut(b + 1) // This line will not be executed
    } yield c
    intercept[RuntimeException] {
      f.futureValue === ???
    }
  }


  /* Notice the difference:
  // Version 1: it runs in sequence
  for {
    result1 <- myFooRequester.fooResult(request1)
    result2 <- myFooRequester.fooResult(request2)
  } yield (combination(result1, result2))

  // Version 2: it runs in parallel
  val r1 = myFooRequester.fooResult(request1)
  val r2 = myFooRequester.fooResult(request2)
  for {
    result1 <- r1
    result2 <- r2
  } yield (combination(result1, result2))
  */


}