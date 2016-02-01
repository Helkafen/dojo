package com.tritondigital.dojo

import org.scalatest.FunSuite
import org.scalautils._
import scala.collection.immutable.BitSet
import org.scalactic.TypeCheckedTripleEquals

import scala.collection.parallel.ForkJoinTaskSupport

/*
class Collections extends FunSuite with TypeCheckedTripleEquals {

  // See https://stackoverflow.com/questions/3634023/should-i-use-lista-or-seqa-or-something-else
  // Beaucoup des methodes utilisees ici sont communes a Traversable

  // Bonne habitude: La signature la plus generale est generalement la meilleure (Seq vs List)

  // Attention a Seq vs List
  // Seq est une interface, mais Seq(1,2,3) est comme une List java
  // List(1,2,3) est une liste chainee


  // All of those are one-liners
  test("Basic methods over all traversable types") {
    val list = List(1,2,3,4)

    assert(??? === false, "not empty")
    assert(??? === 4, "size of the collection")
    assert(??? === List(1,2,3,4,1,2,3,4), "concatenate")
    assert(??? === List(11,12,13,14), "map a function") // map renvoit la meme "structure" avec le meme nombre d'elements, seul le type peut changer
    assert(??? === List(1,2), "filter")
    assert(??? === List(3,4), "opposite of filter")

    // Dividing the container
    assert(??? === (List(2,4), List(1,3)), "partition with a predicate")
    assert(??? === Map(0 -> List(3), 1 -> List(1,4), 2 -> List(2)), "group by 'modulo 3'")
    assert(??? === (List(1,2), List(3,4)), "split after the second element")

    assert(???, "all elements are positive")
    assert(???, "the list contains at least one element superior to 1")
    assert(???, "the list contains 4 positive numbers")

    // Contains something?
    assert(???, "list contains the number 4")
    assert(??? === Some(2), "find the first element that respects a criteria")

    // Produce a side effect for each element (e.g print the value)
    list.???
  }


  // A function is total if it is guaranteed to return something useful
  // It has to be defined over all inputs, and perform no side effect
  test("First and last element of a list") {
    val list = List(1,2,3,4)
    val empty = List()

    assert(list.??? === 1, "head of a list")                    // The type system is lying here
    assert(list.??? === Some(1), "head of a list, total") // The type system is telling the truth this time
    assert(empty.??? === None, "head of a list, total")

    assert(list.??? === 4, "last element of a list")
    assert(list.??? === Some(4), "last element of a list, total")
    assert(empty.??? === None, "last element of a list, total")
  }


  test("Convert the list to various Traversable types") {
    val list = List(1,2,3,4)
    val l: List[Int] = list.???
    val seq: Seq[Int] = list.???
    val array: Array[Int] = list.???
    val iterable: Iterable[Int] = list.???
    val stream: Stream[Int] = list.???
  }

  test("streams") {
    val stream = (1 to 10).toStream // Stream = Infinite List with lazy evaluation

    assert(stream.??? === List(1,2), "take the two first elements")
    assert(stream.??? === List(1,2), "take while i < 3")
  }

  test("Turn a list into a dictionnary") {
    case class Person(id: Int, name: String)
    val people = List(Person(1,"a"), Person(2, "b"))

    assert(people.??? === Map(1 -> "a", 2 -> "b"), "id -> name")
  }


  test("zipping lists") {
    val list1 = List(1,2,3,4)
    val list2 = List(10,20,30,40,50)

    assert(??? === List((1,10),(2,20),(3,30),(4,40)))

    assert(??? === List(11,22,33,44), "zip then map. start with a tuple of lists")
  }

  test("sorting a list (sorted, sortBy, sortWith)") {
    val list = List(1, 5, 3, 4, 6, 2)

    // Sort using the implicit ordering
    assert(list.??? === List(1,2,3,4,5,6))

    // Sort over a field, using the implicit ordering for that field
    case class Person(name: String, age: Int)
    val ps = Seq(Person("John", 32), Person("Bruce", 24), Person("Cindy", 33), Person("Sandra", 18))
    assert(ps.??? === List(Person("Sandra",18), Person("Bruce",24), Person("John",32), Person("Cindy",33)))

    // Sort With a custom lambda
    assert(ps.??? === List(Person("Cindy",33), Person("John",32), Person("Bruce",24), Person("Sandra",18)))
  }


  test("Distribute a map over several CPUs") {
    val list = List(1,2,3,4)
    def plus1(x: Int): Int = x + 1

    // Warning: Use parallel collections only for independent stuff
    assert(list.??? == list.map(plus1), "executing in parallel does not change the result")

    // Parallelism can be configured
    val p = list.par
    p.tasksupport = new ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(2)) // A pool of 2 executors
    assert(p.map(plus1) == list.map(plus1), "executing in parallel does not change the result")
  }


  // Folding
  // NB: The state in event sourcing is a foldLeft of the stream of events
  test("folding lists") {
    case class Station(id: Int)
    case class AddStation(stationId: Int)
    val events = List(1,2,3,4,3).map(AddStation)
    val initialState: Set[Station] = Set.empty

    val finalState = events.???

    assert(finalState === List(1,2,3,4).map(Station).toSet)

    // Challenge: How to make the fold run in constant space? Hint: lazy evaluation.
    //            Then what would happen with foldRight instead of foldLeft?

    // Warning: events.reduce* are partial functions! They all fail on empty collections
  }

  test("Use a fold to get the union of a list of Sets") {
    val set1: Set[Int] = (1 to 10).toSet
    val set2: Set[Int] = (2 to 11).toSet
    val set3: Set[Int] = (3 to 12).toSet
    val listOfSets: List[Set[Int]] = List(set1, set2, set3)
    assert(listOfSets.??? === (1 to 12).toSet)
  }


  // Functors
  // List is a functor because:
  //  - it implements the map method: F[A]  A->B  F[B]
  //  - whatever the function you map, the shape and size of the container doesn't change
  // The two formal laws:
  //  - c.map (f . g) = c.map(f).map(g)
  //  - c.map (identity) = c
  test("Check that List follows the functor properties") {
    val list = Seq(1, 2, 3, 4)
    def plus10(x: Int): Int = x + 10
    def times2(x: Int): Int = x * 2
    assert(list.map(plus10).map(times2) === list.map(x => times2(plus10(x))))
    assert(list.map(x => x) === list)
  }


  // Other functors
  test("Verify functor behaviour for the option/either type") {
    val opt: Option[Int] = Some(1)
    val none: Option[Int] = None
    def f(x: Int) = x + 1
    assert(opt.??? === Some(2)) // shortcut for error handling
    assert(none.??? === None)

    val r: Either[String, Int] = Right(1)
    val l: Either[String, Int] = Left("error")
    val expected = (Right(2), Left("error"))
    assert((r.right.map(_ + 1), l.right.map(_ + 1)) === expected)


    // Some examples of nonsense that the functor laws rule out:
    //   - removing or adding elements from a list
    //   - reversing a list
    //   - changing a Some-value into a None
    //   - depending on a external value (unless the mapped function is impure)

    /*
    An example from billing:

    case class BillingEntry(dummy: Int)
    def replaceNulls(b : BillingEntry): BillingEntry = ???

    val billingEntries = sql"""SELECT [...];""".as[BillingEntry](getResult)

    val cleanedNulls = billingEntries.map(vector => vector.map(replaceNulls))

    cleanedNulls.map(_.filter(_.quantity > 0))
    */
  }

  // No class in the Set hierarchy is a functor, but they do have a map method.
  test("Prove that Set is not a functor") {
    val set = Set(1, 2, 3, 4)
    case class AlwaysEqual(x: Int) {
      override def equals(other: Any) = true
    }
    assert(set.map(x => AlwaysEqual(x)).map(_.x) != set) // c.map (f . g) != c.map(f).map(g)
  }


  test("Tricks") {
    val list: List[Option[String]] = List(Some("abc"), Some("def"), None, Some("yahoo"))

    // Works the same with other functors (e.g Futures)
    assert(list.??? === List(Some("abc!"), Some("def!"), None, Some("yahoo!")), "functor of a functor. map twice!")

    assert(list.??? === List("abc","def","yahoo"), "collect the elements of all subcollections")

    assert(list.flatten === list.???{ case Some(s) => s }, "collect Some values using a partial function")
  }




  test("flatMap over lists is a cartesian product") {
    val nums = Seq(1, 2, 3)
    val letters = Seq("a","b","c")

    // Use a for comprehension to get the cartesian product of nums and letters
    val product = for {
      ???
    } yield ???
    assert(product === List((1,"a"), (1,"b"), (1,"c"), (2,"a"), (2,"b"), (2,"c"), (3,"a"), (3,"b"), (3,"c")))

    // Difficult: rewrite the same logic with flatMap directly
    val product2 = nums.???
    assert(product === product2)
  }

  // Some inconsistencies in the Scala collections (from Paul Phillips, ex scala maintainer)
  test("For the lulz") {
    val b = BitSet(1,2,3)
    println(b.map(_.toString.toInt))        // Still a BitSet
    println(b.map(_.toString).map(_.toInt)) // Became a TreeSet!
    assert(b.map(_.toString).map(_.toInt) === b.map(_.toString.toInt))

    // Became a List[AnyVal] instead of failing to compile
    println(List(1,2) ++ List(3, 4.0))

    // Wat
    assert(List(1,2,3).toSet() == false)
    assert(List(1,2,3).toSet == Set(1,2,3))

    // Should not even compile
    assert(List(1,2,3).contains("your mom") == false)

    // Streams shouldn't have a "size" method
    //print(Stream.continually(1).size)
  }

}
*/