package com.tritondigital.dojo

import org.scalatest.FunSuite


class Dojo extends FunSuite {

  // See https://stackoverflow.com/questions/3634023/should-i-use-lista-or-seqa-or-something-else
  // Bonne habitude: La signature la plus generale est generalement la meilleure (Seq vs List)

  // Attention a Seq vs List
  // Seq est une interface, mais Seq(1,2,3) est comme une List java
  // List(1,2,3) est une liste chainee


  test("Basic functions over all traversable types") {
    val list = List(1,2,3,4)

    assert(list.isEmpty == false, "not empty")
    assert(list.size == 4, "size of the collection")
    assert(list ++ list == List(1,2,3,4,1,2,3,4), "concatenate")
    assert(list.map(_ + 10) == List(11,12,13,14), "map a function") // map renvoit la meme "structure" avec le meme nombre d'elements, seul le type peut changer
    assert(list.filter(_ < 3) == List(1,2), "filter")

    // Dividing the container
    assert(list.partition(_ % 2 == 0) == (List(2,4), List(1,3)), "partition with a predicate")
    assert(list.groupBy(_ % 2) == Map(1 -> List(1,3), 0 -> List(2,4)), "group by")
    assert(list.splitAt(2) == (List(1,2), List(3,4)), "split after the second element")

    assert(list.forall(_ > 0), "all elements are positive")
    assert(list.exists(_ > 1), "the list contains at least one element superior to 1")
    assert(list.count(_ > 0) == 4, "the list contains 4 positive numbers")

    // Contains something?
    assert(list.contains(4), "list contains the number 4")
    assert(list.find(_ % 2 == 0) == Some(2), "first element that respects a criteria")

    // To produce a side effect foreach element (e.g print the value)
    list.foreach(x => print(x))
  }


  test("totality = 'this function will return something useful'") {
    val list = List(1,2,3,4)
    val empty = List()

    assert(list.head == 1, "head of a list")
    assert(list.headOption == Some(1), "head of a list, total") // The type system is telling the truth this time
    assert(empty.headOption == None, "head of a list, total")

    assert(list.last == 4, "last element of a list")
    assert(list.lastOption == Some(4), "last element of a list, total")
    assert(empty.lastOption == None, "last element of a list, total")
  }

  // Make illegal state unrepresentable
  /*mutable versions, when to use them*/

  test("conversions") {
    val list = List(1,2,3,4)
    val l: List[Int] = list.toList
    val seq: Seq[Int] = list.toSeq
    val array: Array[Int] = list.toArray
    val iterable: Iterable[Int] = list.toIterable
    val stream: Stream[Int] = list.toStream
  }

  test("streams") {
    val list = (1 to 10).toStream // Stream = Infinite List with lazy evaluation

    assert(list.take(2).toList == List(1,2), "take the two first elements")
    assert(list.takeWhile(x => x < 3).toList == List(1,2), "take while i < 3")
  }

  test("Turn a list into a dictionnary") {
    case class Person(id: Int, name: String)
    val people = List(Person(1,"a"), Person(2, "b"))

    assert(people.map(p => p.id -> p.name).toMap == Map(1 -> "a", 2 -> "b"), "id -> name")
  }


  test("zipping lists") {
    val list1 = List(1,2,3,4)
    val list2 = List(10,20,30,40,50)

    assert(list1.zip(list2) == List((1,10),(2,20),(3,30),(4,40)))

    assert((list1, list2).zipped.map(_ + _) == List(11,22,33,44), "zip then map")
  }


  test("Distribute a map over several CPUs") {
    val list = List(1,2,3,4)
    def plus1(x: Int): Int = x + 1

    assert(list.par.map(plus1) == list.map(plus1), "executing in parallel does not change the result")   // Warning: Use parallel collections only for independent transformations
  }


  // Folding
  // NB: The state in event sourcing is a foldLeft of the stream of events
  // NB: foldLeft is meant to be pure
  test("folding lists") {
    case class Station(id: Int)
    case class AddStation(stationId: Int)
    val events = List(1,2,3,4,3).map(AddStation)
    val initialState: Set[Station] = Set.empty

    val finalState = events.foldLeft(initialState)((state : Set[Station],event: AddStation) => state + Station(event.stationId))

    assert(finalState == List(1,2,3,4).map(Station).toSet)

    // How to make the fold run in constant space? Hint: lazy evaluation. What would happen with foldRight?
    // Warning: events.reduce* are partial functions! They fail on empty collections
  }


  // Functors
  // List is a functor because
  //  - it implements the map method
  //  - whatever the function you map, the shape and size of the container doesn't change
  test("Verify functor behaviour for the list type") {
    val list = Seq(1, 2, 3, 4)
    def plus10(x: Int): Int = x + 10
    def times2(x: Int): Int = x + 10
    assert(list.map(plus10).map(times2) == list.map(x => times2(plus10(x)))) // map (f . g) = map f . map g
    assert(list.map(x => x) == list) // map identity = identity
  }


  // Other functors
  test("Verify functor behaviour for the option/either type") {
    val opt: Option[Int] = Some(1)
    val none: Option[Int] = None
    assert(opt.map(x => x + 1) == Some(2))
    assert(none.map(x => x + 1) == None)

    val r: Either[String, Int] = Right(1)
    val l: Either[String, Int] = Left("error")
    assert(r.right.map(x => x + 1) == Right(2))
    assert(l.right.map(x => x + 1) == Left("error"))
  }


  // Set is not a functor
  test("Verify functor behaviour for the set type") {
    val set = Set(1, 2, 3, 4)
    case class AlwaysEqual(x: Int) {
      override def equals(other: Any) = true
    }
    assert(set.map(x => AlwaysEqual(x)).map(_.x) != set) // map (f . g) != map f . map g
  }


  test("Tricks") {
    val list: List[Option[String]] = List(Some("abc"), Some("def"), None, Some("yahoo"))

    assert(list.map(_.map(_ ++ "!")) == List(Some("abc!"), Some("def!"), None, Some("yahoo!")), "functor of a functor. map twice!") // Works the same with other functors (e.g Futures)

    assert(list.flatten == List("abc","def","yahoo"), "collect all the elements of the subcollections")
  }




  test("flatMap over lists is a cartesian product") {
    val nums = Seq(1, 2, 3)
    val letters = Seq("a","b","c")
    val product = for {
      i <- nums
      c <- letters
    } yield (i,c)
    assert(product == List((1,"a"), (1,"b"), (1,"c"), (2,"a"), (2,"b"), (2,"c"), (3,"a"), (3,"b"), (3,"c")))

    val product2 = nums.flatMap(i => letters.flatMap(c => List((i,c))))
    assert(product == product2)
  }

  /*
  override def init: Traversable[A]
  override def span(p: A => Boolean): (Traversable[A], Traversable[A])
  override def splitAt(n: Int): (Traversable[A], Traversable[A])

  override def sortWith(lt : (A,A) => Boolean): Traversable[A]
  override def mkString(sep: String): String

  getOrElse*/

}
