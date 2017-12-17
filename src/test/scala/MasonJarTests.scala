import org.scalatest.{FlatSpec, Matchers}
import java.time.LocalDate

import masonjar._
import org.specs2.text.AddedLine

class MasonJarTests extends FlatSpec with Matchers {

    "A MasonJar" should "accept payments" in {
        val masonJar = new MasonJar()
        val idx1 = masonJar.addPayment(Payment(LocalDate.of(2012, 10, 11), "Alice", "Britannia Sushi", 12.50))
        idx1 should be(0)
        masonJar.length should be(1)
        val idx2 = masonJar.addPayment(Payment(LocalDate.of(2012, 10, 13), "Alice", "Santa Barbara", 23.71))
        idx2 should be(1)
        masonJar.length should be(2)
    }

    "A MasonJar" should "return previously deposited payments" in {
        val masonJar = new MasonJar()
        val idx = masonJar.addPayment(Payment(LocalDate.of(2015, 4, 7), "Alice", "Britannia Sushi", 12.50))
        masonJar.addPayment(Payment(LocalDate.of(2015, 4, 7), "Alice", "Santa Barbara", 23.71))

        val payee = masonJar.getPayment(idx).map(_.payee) getOrElse "missing"
        payee should be("Britannia Sushi")
        masonJar.length should be(2)

        val payee2 = masonJar.popPayment(idx).map(_.payee) getOrElse "missing"
        payee2 should be("Britannia Sushi")
        masonJar.length should be(1)
    }

    "A MasonJar" should "throw IndexOutOfBounds when the payment doesn't exist" in {
        val masonJar = new MasonJar()
        masonJar.addPayment(Payment(LocalDate.of(2015, 4, 7), "Alice", "Britannia Sushi", 12.50))
        val idx = masonJar.addPayment(Payment(LocalDate.of(2015, 4, 7), "Alice", "Britannia Sushi", 12.50))
        masonJar.addPayment(Payment(LocalDate.of(2015, 4, 7), "Alice", "Britannia Sushi", 12.50))

        // create a hole
        masonJar.popPayment(idx)

        masonJar.popPayment(idx) should be (None)


    }

    "A MasonJar" should "be summable by criteria" in {
        val masonJar = new MasonJar()
        masonJar.addPayment(Payment(LocalDate.of(2015, 4, 7), "Alice", "Bob", 1.00))
        masonJar.addPayment(Payment(LocalDate.of(2015, 4, 9), "Bob", "Alice", 2.00))
        masonJar.addPayment(Payment(LocalDate.of(2015, 4, 10), "Bob", "Charlize", 3.00))
        masonJar.addPayment(Payment(LocalDate.of(2015, 4, 10), "Charlize", "Alice", 4.00))

        masonJar.sumAmounts(new Filter(List(PaidBy("Bob")))) should be (5.0)
        masonJar.sumAmounts(new Filter(List(PaidTo("Alice")))) should be (6.0)
        masonJar.sumAmounts(new Filter(List(PaidAfter(LocalDate.of(2015, 4, 8))))) should be (9.0)
        masonJar.sumAmounts(new Filter(List(AtLeast(3.0)))) should be (7.0)
    }

    "A MasonJar" should "permit filtering by date ranges" in {
        val masonJar = new MasonJar()
        masonJar.addPayment(Payment(LocalDate.of(2015, 4, 7), "Alice", "Bob", 1.00))
        masonJar.addPayment(Payment(LocalDate.of(2015, 4, 9), "Bob", "Alice", 2.00))
        masonJar.addPayment(Payment(LocalDate.of(2015, 4, 10), "Bob", "Charlize", 3.00))
        masonJar.addPayment(Payment(LocalDate.of(2015, 4, 12), "Charlize", "Alice", 4.00))

        masonJar.getPayments(new Filter(List(PaidAfter(LocalDate.of(2015, 4, 9))))).length should be (2)
        masonJar.getPayments(new Filter(List(PaidBefore(LocalDate.of(2015, 4, 9))))).length should be (1)
        masonJar.getPayments(PaidAfter(LocalDate.of(2015, 4, 8)) + PaidBefore(LocalDate.of(2015, 4, 11))).length should be (2)
    }

    "A MasonJar" should "indicate the payment required to settle balances" in {
        val masonJar = new MasonJar()
        masonJar.addPayment(Payment(LocalDate.of(2015, 4, 7), "Alice", "Store", 1.00))
        masonJar.addPayment(Payment(LocalDate.of(2015, 4, 9), "Bob", "Store", 2.00))
        masonJar.addPayment(Payment(LocalDate.of(2015, 4, 10), "Bob", "Store", 3.00))

        val settlement = masonJar.owed("Alice", "Bob")
        settlement.payer should be ("Alice")
        settlement.payee should be ("Bob")
        settlement.amount should be (2.0)

        masonJar.addPayment(Payment(LocalDate.of(2015, 4, 12), "Alice", "Bob", 3.00))

        val settlement2 = masonJar.owed("Alice", "Bob")
        settlement2.payer should be ("Alice")
        settlement2.payee should be ("Bob")
        settlement2.amount should be (-1.0)
    }

    "A MasonJar" should "determine debts among multiple payers" in {
        val masonJar = new MasonJar()
        masonJar.addPayment(Payment(LocalDate.of(2015, 4, 7), "Alice", "Store", 1.00))
        masonJar.addPayment(Payment(LocalDate.of(2015, 4, 9), "Bob", "Store", 2.00))
        masonJar.addPayment(Payment(LocalDate.of(2015, 4, 10), "Cassandra", "Store", 3.00))

        val debts = masonJar.resolveDebts()
        val balanceA = 1.0 + debts.filter(_.payer == "Alice").map(_.amount).sum - debts.filter(_.payee == "Alice").map(_.amount).sum
        val balanceB = 2.0 + debts.filter(_.payer == "Bob").map(_.amount).sum - debts.filter(_.payee == "Bob").map(_.amount).sum
        val balanceC = 3.0 + debts.filter(_.payer == "Cassandra").map(_.amount).sum - debts.filter(_.payee == "Cassandra").map(_.amount).sum
        balanceA should be (balanceB)
        balanceB should be (balanceC)
    }

}
