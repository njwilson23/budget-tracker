import org.scalatest.{FlatSpec, Matchers}
import java.time.LocalDate

import io.circe.syntax._

import masonjar._
import masonjar.PaymentImplicits._

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

        masonJar.search(PaidAfter(LocalDate.of(2015, 4, 9))).length should be (2)
        masonJar.search(PaidBefore(LocalDate.of(2015, 4, 9))).length should be (1)
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

    "A MasonJar" should "allocate debts among multiple payers" in {
        val masonJar = new MasonJar()
        masonJar.add(Payment(LocalDate.of(2015, 4, 7), "Alice", "Rental", 709.83))
        masonJar.add(Payment(LocalDate.of(2015, 4, 7), "Alice", "Gas", 60.00))
        masonJar.add(Payment(LocalDate.of(2015, 4, 9), "Bob", "Groceries", 120.00))
        masonJar.add(Payment(LocalDate.of(2015, 4, 10), "Cassandra", "", 0.00))

        val debts = masonJar.resolveDebts
        debts.length should be (2)
        debts.filter(_.payer == "Bob").map(_.amount).sum should be (176.61)
        debts.filter(_.payer == "Cassandra").map(_.amount).sum should be (473.22)
    }

}

class EncodingTests extends FlatSpec with Matchers {

    "A Payment" should "be encodable as JSON" in {
        val payment = Payment(LocalDate.of(2018, 1, 1), "Alice", "Bob", 5.0)
        //PaymentImplicits.encodePayment(payment)
        payment.asJson.toString.length should be (102)
    }

    "A list of Payments" should "be encodable as JSON" in {
        val payments = List(
            Payment(LocalDate.of(2018, 1, 1), "Alice", "Bob", 5.0),
            Payment(LocalDate.of(2018, 1, 1), "Alice", "Charlie", 7.0)
        )
        payments.asJson.toString.length should be (242)
    }

}

class FilterTests extends FlatSpec with Matchers {

    "A UnaryFilter" should "filter a List of Payments" in {

        val payments = List(
            Payment(LocalDate.of(2017, 12, 1), "Alice", "Bob", 1.0),
            Payment(LocalDate.of(2017, 12, 2), "Alice", "Bob", 0.5),
            Payment(LocalDate.of(2017, 12, 4), "Alice", "Dani", 2.0),
            Payment(LocalDate.of(2017, 12, 4), "Bob", "Charlize", 4.25)
        ).filter(_.test(PaidBy("Alice")))

        payments.length should be (3)
    }

    "A UnaryFilter" should "be composable" in {

        val payments = List(
            Payment(LocalDate.of(2017, 12, 1), "Alice", "Bob", 1.0),
            Payment(LocalDate.of(2017, 12, 2), "Alice", "Bob", 0.5),
            Payment(LocalDate.of(2017, 12, 4), "Alice", "Dani", 2.0),
            Payment(LocalDate.of(2017, 12, 4), "Bob", "Charlize", 4.25)
        ).filter(_.test(PaidBy("Alice") + AtLeast(1.0)))

        payments.length should be (2)
    }

    "A UnaryFilter" should "be associative" in {

        val payments = List(
            Payment(LocalDate.of(2017, 12, 1), "Alice", "Bob", 1.0),
            Payment(LocalDate.of(2017, 12, 2), "Alice", "Bob", 0.5),
            Payment(LocalDate.of(2017, 12, 4), "Alice", "Dani", 2.0),
            Payment(LocalDate.of(2017, 12, 4), "Bob", "Charlize", 4.25)
        )

        val subset1 = payments.filter(_.test(PaidBy("Alice") + (AtLeast(0.25) + PaidTo("Bob"))))
        val subset2 = payments.filter(_.test((PaidBy("Alice") + AtLeast(0.25)) + PaidTo("Bob")))

        subset1.zip(subset2).map(a => a._1 == a._2).map(if (_) 1 else 0).sum should be (2)
    }

}
