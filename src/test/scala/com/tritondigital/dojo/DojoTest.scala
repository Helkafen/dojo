package com.tritondigital.dojo

import java.io.FileWriter
import java.nio.file.{Files, Path}

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{FunSuite, Matchers, fixture}
import org.scalatest.prop.PropertyChecks
import org.scalacheck.Arbitrary._
import org.scalacheck.Arbitrary

import scala.io.Source


/*
  This dojo covers:
  - Scalatest styles
  - Scalatest matchers
  - Basics of scalacheck
  - Use of Fixtures
 */


case class Name(value: String) extends AnyVal

case class NamePair(name1: Name, name2: Name)

object Dummy {
  def sayHello1(name: String): String = s"Hello $name!"

  def sayHello2(name: Name): String = s"Hello ${name.value}!"

  def sayHello3(names: NamePair): String = s"Hello ${names.name1.value} and ${names.name2.value}!"

  def sayHello4(name: String): String = throw new RuntimeException("")

  def sayHello5(directory: Path): Option[String] = {
    val firstLine = Source.fromFile(directory.toString + "/names.txt").getLines().toList.headOption
    firstLine.map(sayHello1)
  }

}


// Tell sbt to run tests in parallel: "testForkedParallel in Test := true"
// Tell sbt to run tests sequentially, but in another jvm: "fork in Test := true"
// Tell sbt to only run the failing tests: "test-quick"

// For the different test styles, see http://www.scalatest.org/user_guide/selecting_a_style
// More advice at the end of this file

// FunSuite is nice to test pure functions
class DojoTest1 extends FunSuite with Matchers with TypeCheckedTripleEquals with PropertyChecks {

  /*
  Testing the sayHello1 function
  Using: FunSuite + Matchers + PropertyChecks
  Check that for all names:
  - The greeting starts with Hello
  - The greeting ends with !
  - The greeting contains the caller’s name
   */
  test("Greetings start with Hello") {
    // TODO
  }

  test("Greetings end with !") {
    // TODO
  }

  test("Greetings contain the caller's name") {
    // TODO
  }
}


class DojoTest2 extends FunSuite with Matchers with TypeCheckedTripleEquals with PropertyChecks {
  /*
  Testing the sayHello2 function
  Same checks, for any random Name, using custom Gen and Arbitrary instances from scalacheck
  */

  val nameGen = ???
  implicit val nameArbitrary = ??? // Why implicit?

  test("Greetings start with Hello") {
    // TODO
  }

  test("Greetings end with !") {
    // TODO
  }

  test("Greetings contain the caller's name") {
    // TODO
  }
}


class DojoTest3 extends FunSuite with Matchers with TypeCheckedTripleEquals with PropertyChecks {
  /*
  Testing the sayHello3 function
  Same checks, for any pair of Names, using a new Arbitrary instance
  Create a new Arbitrary instance of NamePair, reusing the Arbitrary instance of Name
  */

  val nameGen = ???
  implicit val nameArbitrary = ???

  val namePairGen = ???
  implicit val namePairArbitrary = ???

  test("Greetings start with Hello") {
    // TODO
  }

  test("Greetings end with !") {
    // TODO
  }

  test("Greetings contain the caller's names") {
    // TODO
  }
}


class DojoTest4 extends FunSuite with Matchers with TypeCheckedTripleEquals with PropertyChecks {
  /*
  Testing the sayHello4 function
  Check that sayHello4 throws a RuntimeException for any argument
  */
  test("sayHello4 throws a runtime exception") {
    // TODO
  }
}

/*
Testing the sayHello5 procedure with a stateful context
Before each test, create a temporary directory containing the file names.txt, and write Bob\nAlice in it.
After each test, delete the directory: scalax.file.Path.fromString(outputDir.toAbsolutePath.toString).deleteRecursively()
Use a FixtureParam case class to pass the state around.

This example is inspired by WCM2Feature in wcm2-datamart
*/
// FeatureSpec is nice when a test needs to have some state ready
/*class DojoTest5 extends fixture.FeatureSpec with Matchers {

  case class FixtureParam(directory: Path)

  def withFixture(test: OneArgTest) = {
    // TODO
  }

  feature("Some feature") {
    scenario("sayHello5 should return Hello Bob!") {
      // TODO
    }
  }

}*/

/*
  So we just used FunSuite for pure tests, and FeatureSpec for stateful tests.

  On other testing styles:
  - FlatSpec is almost the same as FunSuite
  - WordSpec/SetSpec/FreeSpec work too, but I've seen them misused quite a lot (with names of
    classes or methods in the descriptions). They are hierarchical.

    Example of misuse:
    "A CountyEnrichmentHelper" when {
      "applied" should {
         "create a data frame with one county information" {
    This could have been a test("Create a dataframe with county information").

    However they can be nice if there are many test cases and when they are easy to categorize.

    Example of better use:
    "A Station" when {
      "merged on mounts" should {
        "yield a new station object containing its mounts and the mounts of the other station" in {
    The descriptions have business meaning!
 */


/*
A note on Mocking:
Mocking is mostly useful when coding in a imperative style, which generally means misusing scala.
It happened in dwh-log-processing-jobs:
  We used mockito to mock the datadog client, as the crunch needs to send some statistics.
  Instead, we should have returned the statistics as a return value.
*/

/*
To test code that runs several side effects and intertwines them, we can use the interpreter pattern (keyword: “free monad”)
and make the tests deterministic. It could be another dojo.
 */


