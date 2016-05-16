package com.tritondigital.dojo

import java.io.FileWriter
import java.nio.file.{Files, Path}

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{FunSuite, Matchers, fixture}
import org.scalatest.prop.PropertyChecks
import org.scalacheck.Arbitrary._
import org.scalacheck.Arbitrary

import scala.io.Source


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
    forAll { name: String =>
      Dummy.sayHello1(name) should startWith("Hello")
    }
  }

  test("Greetings end with !") {
    forAll { name: String =>
      Dummy.sayHello1(name) should endWith("!")
    }
  }

  test("Greetings contain the caller's name") {
    forAll { name: String =>
      Dummy.sayHello1(name) should include(name)
    }
  }
}


class DojoTest2 extends FunSuite with Matchers with TypeCheckedTripleEquals with PropertyChecks {
  /*
  Testing the sayHello2 function
  Same checks, for any random Name, using custom Gen and Arbitrary instances from scalacheck
  */

  val nameGen = arbitrary[String].map(Name)
  implicit val nameArbitrary = Arbitrary(nameGen)

  test("Greetings start with Hello") {
    forAll { name: Name =>
      Dummy.sayHello2(name) should startWith("Hello")
    }
  }

  test("Greetings end with !") {
    forAll { name: Name =>
      Dummy.sayHello2(name) should endWith("!")
    }
  }

  test("Greetings contain the caller's name") {
    forAll { name: Name =>
      Dummy.sayHello2(name) should include(name.value)
    }
  }
}


class DojoTest3 extends FunSuite with Matchers with TypeCheckedTripleEquals with PropertyChecks {
  /*
  Testing the sayHello3 function
  Same checks, for any pair of Names, using a new Arbitrary instance
  Create a new Arbitrary instance of NamePair, reusing the Arbitrary instance of Name
  */

  val nameGen = arbitrary[String].map(Name)
  implicit val nameArbitrary = Arbitrary(nameGen)

  val namePairGen = for {
    n1 <- arbitrary[Name]
    n2 <- arbitrary[Name]
  } yield NamePair(n1, n2)
  implicit val namePairArbitrary = Arbitrary(namePairGen)

  test("Greetings start with Hello") {
    forAll { names: NamePair =>
      Dummy.sayHello3(names) should startWith("Hello")
    }
  }

  test("Greetings end with !") {
    forAll { names: NamePair =>
      Dummy.sayHello3(names) should endWith("!")
    }
  }

  test("Greetings contain the caller's names") {
    forAll { names: NamePair =>
      Dummy.sayHello3(names) should include(names.name1.value)
      Dummy.sayHello3(names) should include(names.name2.value)
    }
  }
}


class DojoTest4 extends FunSuite with Matchers with TypeCheckedTripleEquals with PropertyChecks {
  /*
  Testing the sayHello4 function
  Check that sayHello4 throws a RuntimeException for any argument
  */
  test("sayHello4 throws a runtime exception") {
    a[RuntimeException] should be thrownBy Dummy.sayHello4("name")
  }
}


class DojoTest5 extends fixture.FeatureSpec with Matchers {
  /*
  Testing the sayHello5 procedure with a stateful context
  Before each test, create a temporary directory containing the file names.txt, and write Bob\nAlice in it.
  After each test, delete the directory: scalax.file.Path.fromString(outputDir.toAbsolutePath.toString).deleteRecursively()
  Use a FixtureParam case class to pass the state around.

  This example is inspired by WCM2Feature in wcm2-datamart
  */

  case class FixtureParam(directory: Path)

  def withFixture(test: OneArgTest) = {
    val outputDir: Path = Files.createTempDirectory(s"dojoTest5")
    val fw = new FileWriter(outputDir.toString + "/names.txt", true)
    fw.write("Bob\nAlice")
    fw.close()

    val theFixture = FixtureParam(outputDir)
    try {
      withFixture(test.toNoArgTest(theFixture))
    } finally {
      scalax.file.Path.fromString(outputDir.toAbsolutePath.toString).deleteRecursively()
    }
  }

  feature("Some feature") {
    scenario("sayHello5 should return Hello Bob!") {
      f: FixtureParam =>
        Dummy.sayHello5(f.directory) shouldEqual Some("Hello Bob!")
    }
  }

}
