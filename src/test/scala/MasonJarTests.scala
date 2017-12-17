import org.scalatest.{FlatSpec, Matchers}
import java.time.LocalDate

import masonjar._

class MasonJarTests extends FlatSpec with Matchers {

    "A MasonJar" should "accept payments" in {
        val masonJar = new MasonJar()
        val idx1 = masonJar.add(Payment(LocalDate.of(2012, 10, 11), "Alice", "Britannia Sushi", 12.50))
        idx1 should be(0)
        masonJar.length should be(1)
        val idx2 = masonJar.add(Payment(LocalDate.of(2012, 10, 13), "Alice", "Santa Barbara", 23.71))
        idx2 should be(1)
        masonJar.length should be(2)
    }

    "A MasonJar" should "return previously deposited payments" in {
        val masonJar = new MasonJar()
        val idx = masonJar.add(Payment(LocalDate.of(2015, 4, 7), "Alice", "Britannia Sushi", 12.50))
        masonJar.add(Payment(LocalDate.of(2015, 4, 7), "Alice", "Santa Barbara", 23.71))

        val payee = masonJar.index(idx).map(_.payee) getOrElse "missing"
        payee should be("Britannia Sushi")
        masonJar.length should be(2)

        val payee2 = masonJar.pop(idx).map(_.payee) getOrElse "missing"
        payee2 should be("Britannia Sushi")
        masonJar.length should be(1)
    }

    "A MasonJar" should "return None when the payment doesn't exist" in {
        val masonJar = new MasonJar()
        masonJar.add(Payment(LocalDate.of(2015, 4, 7), "Alice", "Britannia Sushi", 12.50))
        val idx = masonJar.add(Payment(LocalDate.of(2015, 4, 7), "Alice", "Britannia Sushi", 12.50))
        masonJar.add(Payment(LocalDate.of(2015, 4, 7), "Alice", "Britannia Sushi", 12.50))

        // create a hole
        masonJar.pop(idx)
        masonJar.pop(idx) should be (None)
    }

    "A MasonJar" should "be summable by criteria" in {
        val masonJar = new MasonJar()
        masonJar.add(Payment(LocalDate.of(2015, 4, 7), "Alice", "Bob", 1.00))
        masonJar.add(Payment(LocalDate.of(2015, 4, 9), "Bob", "Alice", 2.00))
        masonJar.add(Payment(LocalDate.of(2015, 4, 10), "Bob", "Charlize", 3.00))
        masonJar.add(Payment(LocalDate.of(2015, 4, 10), "Charlize", "Alice", 4.00))

        masonJar.tally(PaidBy("Bob")) should be (5.0)
        masonJar.tally(PaidTo("Alice")) should be (6.0)
        masonJar.tally(PaidAfter(LocalDate.of(2015, 4, 8))) should be (9.0)
        masonJar.tally(AtLeast(3.0)) should be (7.0)
    }

    "A MasonJar" should "permit filtering by date ranges" in {
        val masonJar = new MasonJar()
        masonJar.add(Payment(LocalDate.of(2015, 4, 7), "Alice", "Bob", 1.00))
        masonJar.add(Payment(LocalDate.of(2015, 4, 9), "Bob", "Alice", 2.00))
        masonJar.add(Payment(LocalDate.of(2015, 4, 10), "Bob", "Charlize", 3.00))
        masonJar.add(Payment(LocalDate.of(2015, 4, 12), "Charlize", "Alice", 4.00))

        masonJar.search(CompositeFilter(List(PaidAfter(LocalDate.of(2015, 4, 9))))).length should be (2)
        masonJar.search(CompositeFilter(List(PaidBefore(LocalDate.of(2015, 4, 9))))).length should be (1)
        masonJar.search(PaidAfter(LocalDate.of(2015, 4, 8)) + PaidBefore(LocalDate.of(2015, 4, 11))).length should be (2)
    }

    "A MasonJar" should "indicate the payment required to settle balances" in {
        val masonJar = new MasonJar()
        masonJar.add(Payment(LocalDate.of(2015, 4, 7), "Alice", "Store", 1.00))
        masonJar.add(Payment(LocalDate.of(2015, 4, 9), "Bob", "Store", 2.00))
        masonJar.add(Payment(LocalDate.of(2015, 4, 10), "Bob", "Store", 3.00))

        val settlement = masonJar.owed("Alice", "Bob")
        settlement.payer should be ("Alice")
        settlement.payee should be ("Bob")
        settlement.amount should be (2.0)

        masonJar.add(Payment(LocalDate.of(2015, 4, 12), "Alice", "Bob", 3.00))

        val settlement2 = masonJar.owed("Alice", "Bob")
        settlement2.payer should be ("Alice")
        settlement2.payee should be ("Bob")
        settlement2.amount should be (-1.0)
    }

    "A MasonJar" should "determine debts among multiple payers" in {
        val masonJar = new MasonJar()
        masonJar.add(Payment(LocalDate.of(2015, 4, 7), "Alice", "Store", 1.00))
        masonJar.add(Payment(LocalDate.of(2015, 4, 9), "Bob", "Store", 2.00))
        masonJar.add(Payment(LocalDate.of(2015, 4, 10), "Cassandra", "Store", 3.00))

        val debts = masonJar.resolveDebts()
        val balanceA = 1.0 + debts.filter(_.payer == "Alice").map(_.amount).sum - debts.filter(_.payee == "Alice").map(_.amount).sum
        val balanceB = 2.0 + debts.filter(_.payer == "Bob").map(_.amount).sum - debts.filter(_.payee == "Bob").map(_.amount).sum
        val balanceC = 3.0 + debts.filter(_.payer == "Cassandra").map(_.amount).sum - debts.filter(_.payee == "Cassandra").map(_.amount).sum
        balanceA should be (balanceB)
        balanceB should be (balanceC)
    }

}

class FilterTests extends FlatSpec with Matchers {

    "A UnaryFilter" should "filter a List of Payments" in {

        val payments = List(
            Payment(LocalDate.of(2017, 12, 1), "Alice", "Bob", 1.0),
            Payment(LocalDate.of(2017, 12, 2), "Alice", "Bob", 0.5),
            Payment(LocalDate.of(2017, 12, 4), "Alice", "Dani", 2.0),
            Payment(LocalDate.of(2017, 12, 4), "Bob", "Charlize", 4.25)
        ).filter(PaidBy("Alice").test)

        payments.length should be (3)
    }

    "A UnaryFilter" should "be composable" in {

        val payments = List(
            Payment(LocalDate.of(2017, 12, 1), "Alice", "Bob", 1.0),
            Payment(LocalDate.of(2017, 12, 2), "Alice", "Bob", 0.5),
            Payment(LocalDate.of(2017, 12, 4), "Alice", "Dani", 2.0),
            Payment(LocalDate.of(2017, 12, 4), "Bob", "Charlize", 4.25)
        ).filter((PaidBy("Alice") + AtLeast(1.0)).test)

        payments.length should be (2)
    }

    "A UnaryFilter" should "be associative" in {

        val payments = List(
            Payment(LocalDate.of(2017, 12, 1), "Alice", "Bob", 1.0),
            Payment(LocalDate.of(2017, 12, 2), "Alice", "Bob", 0.5),
            Payment(LocalDate.of(2017, 12, 4), "Alice", "Dani", 2.0),
            Payment(LocalDate.of(2017, 12, 4), "Bob", "Charlize", 4.25)
        )

        val subset1 = payments.filter((PaidBy("Alice") + (AtLeast(0.25) + PaidTo("Bob"))).test)
        val subset2 = payments.filter(((PaidBy("Alice") + AtLeast(0.25)) + PaidTo("Bob")).test)

        subset1.zip(subset2).map(a => a._1 == a._2).map(if (_) 1 else 0).sum should be (2)
    }

}
