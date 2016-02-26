package com.tritondigital.dojo


import org.scalatest.concurrent.ScalaFutures
import org.scalautils.TypeCheckedTripleEquals

import scala.concurrent.{Future, blocking}
import scala.concurrent.duration._

import org.scalatest.{Matchers, FunSuite}

import scala.language.postfixOps

class FuturesAnswers extends FunSuite with ScalaFutures with Matchers with TypeCheckedTripleEquals {

  // A Future[A] represents a computation that will eventually return a value of type A. Or a Throwable.
  //  futures (time + failure) are simpler than actors (time + failure + state), but actors are strictly more expressive
  //  "Don't use actors for concurrency. Instead, use actors for state and use futures for concurrency."

  // Future != Callback
  //   - A future represents a value (generally obtained via a side effect). Futures are composable and easy to test
  //   - A callback is a procedure that produces *another* side effect when the value is received (or when the future fails).
  //       Several callbacks can be registered on a Future's result, and their order of evaluation is undefined
  //       Callbacks are not composable and harder to test

  // Some Future operations: .map, .filter, .flatMap, for blocks, .foreach
  // Some Callbacks declarations: .onComplete, .onFailure, .onSuccess, .andThen

  // Promises
  //  A Promise is an object that can write to a Future. It's mostly useful for async library authors

  // A good read about concurrency:
  //   https://github.com/alexandru/scala-best-practices/blob/master/sections/4-concurrency-parallelism.md

  // Actors need an execution context. The global one is usually ok
  // It can be tuned to throttle execution
  import scala.concurrent.ExecutionContext.Implicits.global

  // A dummy future
  // Warning: The blocking construct spawn a real thread. It must be used to wrap blocking APIs, such as JDBC
  def fut[A](value: A, wait: Duration = 500 millis): Future[A] = Future {
    blocking {
      Thread.sleep(wait.toMillis)
    }
    value
  }

  // A dummy future to be used as a fallback for a failed future
  def fallback[A](default: A, timeout: Duration): Future[A] = fut(default, timeout)

  // A dummy failed future
  def failed(): Future[Int] = Future.failed(new RuntimeException("reason"))

  def futOfFileContent(): Future[String] = Future {
    blocking {
      scala.io.Source.fromFile("myText.txt").mkString
    }
  }

  implicit val patience = PatienceConfig(1 second, 10 millis) // configuration of the futures (max & retries)

  test("Check the future's value") {
    val f = fut(1)
    assert(f.futureValue === 1)
  }

  // Hint: What is the type of f.failed?
  test("Check that the future failed") {
    val f = failed()
    intercept[RuntimeException] {
      assert(f.futureValue === ???)
    }
    assert(f.failed.futureValue.getMessage === "reason")
  }


  test("Check that the future is ready within 500 milliseconds (or not)") {
    def check(f: Future[Int]): Boolean = f.isReadyWithin(500 millis)
    val fast = fut(1, wait = 100 millis)
    assert(check(fast))
    val slow = fut(1, wait = 1 second)
    assert(!check(slow))
  }

  test("Race between two futures (i.e keep the fastest one)") {
    val fast = fut(1, wait = 100 millis)
    val slow = fut(2, wait = 1 second)
    val faster = Future.firstCompletedOf(Seq(slow, fast))
    assert(faster.isReadyWithin(500 millis))
    assert(faster.futureValue === 1)
  }


  def withTimeoutDefault[A](f: Future[A], d: Duration, default: A): Future[A] = {
    val futs = Seq(f, fut(default, d))
    Future.firstCompletedOf(futs)
  }

  test("Use a race to replace a timeout with a default value (withTimeoutDefault)") {
    val slow = fut(2, wait = 100 second)
    val f = withTimeoutDefault(slow, 100 millis, 0)
    assert(f.isReadyWithin(1 second))
    assert(f.futureValue === 0)
  }


  def withFailureDefault[A](f: Future[A], default: A): Future[A] = {
    f.fallbackTo(Future(default))
  }

  test("Fallback if a future fails (withFailureDefault)") {
    val bad = failed()
    val f = withFailureDefault(bad, 1)
    assert(f.futureValue === 1)
  }

  def withFailureDefaultNone[A](f: Future[A]): Future[Option[A]] = {
    withFailureDefault(f.map(Some(_)), None) // Or directly:  f.map(Some(_)).fallbackTo(Future(None))
  }

  test("Fallback if a future fails (withFailureDefaultNone)") {
    val bad = failed()
    val f = withFailureDefaultNone(bad)
    assert(f.futureValue === None)
  }



  test("Recover from a specific exception (with another future)") {
    val bad1 = failed()
    val chain = bad1.recoverWith {
      case e: RuntimeException => fut(0) // More specific than fallbackTo, but also more verbose
    }
    assert(chain.futureValue === 0)
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

  test("Print the content of a future if it succeeded") {
    val f = fut(1)
    f.foreach(println)
  }


  // We're transforming a Seq[Future[A]] into a Future[Seq[A]]
  // Someday you may read that type signature and think "ok, so Future is also an Applicative!"
  test("Run a list of futures") {
    val futs: Seq[Future[Int]] = (1 to 50).map(fut(_))
    val futs2: Seq[Future[String]] = futs.map(f => f.map(_.toString))
    assert(Future.sequence(futs2).futureValue === (1 to 50).map(_.toString)) // Wait for them in parallel
  }

  // Works over any Traversable type
  test("Run the same future constructor over a list of values (we need to 'traverse' a sequence of Int))") {
    def f(n: Int) = fut(n + 1)
    assert(Future.traverse(1 to 50)(f).futureValue === (2 to 51)) // Create them and wait for them in parallel
  }


  // Futures implement flatMap, so they can be used in 'for' blocks to make one receive the result of another future
  // We got Future[Future[Future[A]] -> Future[A]
  test("Chain several futures in a for block. Each of them adds 1 to the previous future content") {
    def plus1(i: Int): Int = i + 1
    val d = 100 millis
    val f = for {
      a <- fut(1, d)
      b <- fut(plus1(a), d) // the for block allows reusing some previous result
      c <- fut(plus1(b), d)
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
      assert(f.futureValue === 100)
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