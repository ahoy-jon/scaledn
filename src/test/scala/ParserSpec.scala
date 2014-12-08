import org.scalatest._

import scala.util.{Try, Success, Failure}

class ParserSpec extends FlatSpec with Matchers with TryValues {
  
  "EDNParser" should "parse Nil" in {
    EDNParser("""nil""").Nil.run().success.value should be (EDNNil)
    EDNParser("""nil    
      \n""").Nil.run().success.value should be (EDNNil)
    EDNParser("""Nil""").Nil.run() should be ('failure)
  }

  it should "parse Boolean" in {
    EDNParser("""true""").Boolean.run().success.value should be (true)
    EDNParser("""True""").Boolean.run() should be ('failure)
    EDNParser("""true
        \n""").Boolean.run().success.value should be (true)
    EDNParser("""false
        \t""").Boolean.run().success.value should be (false)
  }

  it should "parse String" in {
    EDNParser("\"coucou\"").String.run().success.value should be ("coucou")
    EDNParser("\"coucou\n\b\t\f\b\\\"foo\\\" 'bar' \"").String.run().success.value should be ("coucou\n\b\t\f\b\"foo\" 'bar' ")

    EDNParser("\\newline").Char.run().success.value should be ('\n')
    EDNParser("\\return").Char.run().success.value should be ('\r')
    EDNParser("\\space").Char.run().success.value should be (' ')
    EDNParser("\\tab").Char.run().success.value should be ('\t')
    EDNParser("\\\\").Char.run().success.value should be ('\\')
    EDNParser("\\u00D5").Char.run().success.value should be ('\u00D5')
    EDNParser("\\u00D5 ").Char.run().success.value should be ('\u00D5')
  }

  it should "parse Symbol" in {
    EDNParser("""toto""").Symbol.run().success.value should be (EDNSymbol("toto"))
    EDNParser("""foo/bar""").Symbol.run().success.value should be (EDNSymbol("foo/bar", Some("foo")))
    EDNParser("""1foo""").Symbol.run() should be ('failure)
    EDNParser("""-1foo""").Symbol.run() should be ('failure)
    EDNParser("""+1foo""").Symbol.run() should be ('failure)
    EDNParser(""".1foo""").Symbol.run() should be ('failure)
    EDNParser("""foo&>-<.bar:#$%""").Symbol.run().success.value should be (EDNSymbol("foo&>-<.bar:#$%"))
    EDNParser("""foo&><bar$%/xyz""").Symbol.run().success.value should be (EDNSymbol("foo&><bar$%/xyz", Some("foo&><bar$%")))
    EDNParser("""xyz/foo&><bar$%""").Symbol.run().success.value should be (EDNSymbol("xyz/foo&><bar$%", Some("xyz")))
    EDNParser("""::""").Symbol.run() should be ('failure)
    EDNParser("""::foo""").Symbol.run() should be ('failure)
    EDNParser(""":/""").Symbol.run() should be ('failure)
    EDNParser("""foo/bar""").Symbol.run().success.value should be (EDNSymbol("foo/bar", Some("foo")))
    EDNParser("""foo/bar """).Symbol.run().success.value should be (EDNSymbol("foo/bar", Some("foo")))
  }

  it should "parse Keyword" in {
    EDNParser(""":foo""").Keyword.run().success.value should be (EDNKeyword(EDNSymbol("foo")))
    EDNParser("""foo""").Keyword.run() should be ('failure)
    EDNParser(""":foo/bar""").Keyword.run().success.value should be (EDNKeyword(EDNSymbol("foo/bar", Some("foo"))))
    EDNParser(""":foo/bar """).Keyword.run().success.value should be (EDNKeyword(EDNSymbol("foo/bar", Some("foo"))))
  }

  it should "parse Long" in {
    EDNParser("""1""").Long.run().success.value should be (1L)
    EDNParser("""1N""").Long.run().success.value should be (BigInt("1"))
    EDNParser("""0""").Long.run().success.value should be (0L)
    EDNParser("""-0""").Long.run().success.value should be (0L)
    EDNParser("""+0""").Long.run().success.value should be (0L)
    EDNParser("""+1""").Long.run().success.value should be (1L)
    EDNParser("""-1""").Long.run().success.value should be (-1L)
    EDNParser("""-123""").Long.run().success.value should be (-123L)
    EDNParser("""+123""").Long.run().success.value should be (123L)
    EDNParser("""-123456789123N""").Long.run().success.value should be (BigInt("-123456789123"))
  }

  it should "parse Double" in {
    EDNParser("""1""").Double.run() should be ('failure)
    EDNParser("""1.23567""").Double.run().success.value should be (1.23567)
    EDNParser("""+1e23""").Double.run().success.value should be (1e23)
    EDNParser("""-1.234e23""").Double.run().success.value should be (-1.234e23)
    EDNParser("""1.23567M""").Double.run().success.value should be (BigDecimal("1.23567"))
    EDNParser("""1e234M""").Double.run().success.value should be (BigDecimal("1e234"))
    EDNParser("""1.234e567M""").Double.run().success.value should be (BigDecimal("1.234e567"))
  }

  it should "parse List" in {
    EDNParser("""(1, "foo", :foo/bar)""").List.run().success.value should be (List(1L, "foo", EDNKeyword(EDNSymbol("foo/bar", Some("foo")))))
    EDNParser("""(1 "foo" :foo/bar)""").List.run().success.value should be (List(1L, "foo", EDNKeyword(EDNSymbol("foo/bar", Some("foo")))))
    EDNParser("""(1 "foo,bar" :foo/bar)""").List.run().success.value should be (List(1L, "foo,bar", EDNKeyword(EDNSymbol("foo/bar", Some("foo")))))
  }

  it should "parse Vector" in {
    EDNParser("""[1, "foo", :foo/bar]""").Vector.run().success.value should be (Vector(1L, "foo", EDNKeyword(EDNSymbol("foo/bar", Some("foo")))))
    EDNParser("""[1 "foo" :foo/bar]""").Vector.run().success.value should be (Vector(1L, "foo", EDNKeyword(EDNSymbol("foo/bar", Some("foo")))))

    EDNParser("""[db.part/db]""").Vector.run().success.value should be (Vector(EDNSymbol("db.part/db", Some("db.part"))))
  }

  it should "parse Set" in {
    EDNParser("""#{1, "foo", :foo/bar}""").Set.run().success.value should be (Set(1L, "foo", EDNKeyword(EDNSymbol("foo/bar", Some("foo")))))
    EDNParser("""#{1 "foo" :foo/bar}""").Set.run().success.value should be (Set(1L, "foo", EDNKeyword(EDNSymbol("foo/bar", Some("foo")))))
  }

  it should "parse Map" in {
    EDNParser("""{1 "foo", "bar" 1.234M, :foo/bar [1,2,3]}""").Map.run().success.value should be (
      Map(
        1L -> "foo", 
        "bar" -> BigDecimal("1.234"),
        EDNKeyword(EDNSymbol("foo/bar", Some("foo"))) -> Vector(1, 2, 3)
      )
    )

    EDNParser("""{1 "foo" "bar" 1.234M :foo/bar [1,2,3]}""").Map.run().success.value should be (
      Map(
        1L -> "foo", 
        "bar" -> BigDecimal("1.234"),
        EDNKeyword(EDNSymbol("foo/bar", Some("foo"))) -> Vector(1, 2, 3)
      )
    )
  }

  it should "skip comments" in {
    val parser = EDNParser("""
      ; 1 balbal dsdkfjsdlfj sdfkjds lfdsjlkf 
      {1 "foo" "bar" 1.234M :foo/bar [1,2,3]} ; 2 lfkjlkfjdskfjd
      ; 3 balbal dsdkfjsdlfj sdfkjds lfdsjlkf
    """).Root.run().success.value should be (
      Vector(
        Map(
          1L -> "foo", 
          "bar" -> BigDecimal("1.234"),
          EDNKeyword(EDNSymbol("foo/bar", Some("foo"))) -> Vector(1, 2, 3)
        )
      )
    )

  }

  it should "discard" in {
    EDNParser("""#_foo/bar""").Discard.run() should be ('success)
    EDNParser("""#_ :foo/bar""").Discard.run() should be ('success)
  }

  it should "parse full" in {
    EDNParser("""{1 "foo", "bar" 1.234M, :foo/bar [1,2,3] :bar/foo""").Root.run() should be ('failure)
    EDNParser("""{1 "foo", "bar" 1.234M, :foo/bar [1,2,3]} :bar/foo""").Root.run().success.value should be (
      Vector(
        Map(
          1L -> "foo", 
          "bar" -> BigDecimal("1.234"),
          EDNKeyword(EDNSymbol("foo/bar", Some("foo"))) -> Vector(1, 2, 3)
        ),

        EDNKeyword(EDNSymbol("bar/foo", Some("bar")))
      )
    )

    // match {
    //   case Success(t) => println("SUCCESS:"+t)
    //   case Failure(f : org.parboiled2.ParseError) => println("PARSE:"+parser.formatError(f))
    // }
  }

  it should "parse full with discard" in {
    EDNParser("""{1 "foo", "bar" 1.234M, :foo/bar [1,2,3]} #_foo/bar :bar/foo""").Root.run().success.value should be (
      Vector(
        Map(
          1L -> "foo", 
          "bar" -> BigDecimal("1.234"),
          EDNKeyword(EDNSymbol("foo/bar", Some("foo"))) -> Vector(1, 2, 3)
        ),

        EDNKeyword(EDNSymbol("bar/foo", Some("bar")))
      )
    )

    // match {
    //   case Success(t) => println("SUCCESS:"+t)
    //   case Failure(f : org.parboiled2.ParseError) => println("PARSE:"+parser.formatError(f))
    // }
  }

  it should "parse bigger" in {
    val str = """
  [{:db/id #db/id [db.part/db]
  :db/ident :object/name
  :db/doc "Name of a Solar System object."
  :db/valueType :db.type/string
  :db/index true
  :db/cardinality :db.cardinality/one
  :db.install/_attribute :db.part/db}
 {:db/id #db/id [db.part/db]
  :db/ident :object/meanRadius
  :db/doc "Mean radius of an object."
  :db/index true
  :db/valueType :db.type/double
  :db/cardinality :db.cardinality/one
  :db.install/_attribute :db.part/db}
 {:db/id #db/id [db.part/db]
  :db/ident :data/source
  :db/doc "Source of the data in a transaction."
  :db/valueType :db.type/string
  :db/index true
  :db/cardinality :db.cardinality/one
  :db.install/_attribute :db.part/db}]
[{:db/id #db/id [db.part/tx]
  :db/doc "Solar system objects bigger than Pluto."}
 {:db/id #db/id [db.part/tx]
  :data/source "http://en.wikipedia.org/wiki/List_of_Solar_System_objects_by_size"}
 {:db/id #db/id [db.part/user]
  :object/name "Sun"
  :object/meanRadius 696000.0}
 {:db/id #db/id [db.part/user]
  :object/name "Jupiter"
  :object/meanRadius 69911.0}
 {:db/id #db/id [db.part/user]
  :object/name "Saturn"
  :object/meanRadius 58232.0}
 {:db/id #db/id [db.part/user]
  :object/name "Uranus"
  :object/meanRadius 25362.0}
 {:db/id #db/id [db.part/user]
  :object/name "Neptune"
  :object/meanRadius 24622.0}
 {:db/id #db/id [db.part/user]
  :object/name "Earth"
  :object/meanRadius 6371.0}
 {:db/id #db/id [db.part/user]
  :object/name "Venus"
  :object/meanRadius 6051.8}
 {:db/id #db/id [db.part/user]
  :object/name "Mars"
  :object/meanRadius 3390.0}
 {:db/id #db/id [db.part/user]
  :object/name "Ganymede"
  :object/meanRadius 2631.2}
 {:db/id #db/id [db.part/user]
  :object/name "Titan"
  :object/meanRadius 2576.0}
 {:db/id #db/id [db.part/user]
  :object/name "Mercury"
  :object/meanRadius 2439.7}
 {:db/id #db/id [db.part/user]
  :object/name "Callisto"
  :object/meanRadius 2410.3}
 {:db/id #db/id [db.part/user]
  :object/name "Io"
  :object/meanRadius 1821.5}
 {:db/id #db/id [db.part/user]
  :object/name "Moon"
  :object/meanRadius 1737.1}
 {:db/id #db/id [db.part/user]
  :object/name "Europa"
  :object/meanRadius 1561.0}
 {:db/id #db/id [db.part/user]
  :object/name "Triton"
  :object/meanRadius 1353.4}
 {:db/id #db/id [db.part/user]
  :object/name "Eris"
  :object/meanRadius 1163.0}]
    """

    val parser = EDNParser(str)
    parser.Root.run() match {
      case Success(t) => println("SUCCESS:"+t)
      case Failure(f : org.parboiled2.ParseError) => println("PARSE:"+parser.formatError(f))
    }
  }
}