import org.scalatest.{FlatSpec, Matchers}

import java.time.LocalDate
import masonjar.{MasonJar, Payment, PaymentFilter}

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

        masonJar.sumAmounts(new PaymentFilter(payer = Some("Bob"))) should be (5.0)
        masonJar.sumAmounts(new PaymentFilter(payee = Some("Alice"))) should be (6.0)
        masonJar.sumAmounts(new PaymentFilter(date = Some((dt: LocalDate) => LocalDate.of(2015, 4, 8).isBefore(dt)))) should be (9.0)
        masonJar.sumAmounts(new PaymentFilter(amount = Some((amt: Double) => amt >= 3.0))) should be (7.0)
    }

    "A MasonJar" should "be summable by using PaymentFilter.suchThat" in {
        val masonJar = new MasonJar()
        masonJar.addPayment(Payment(LocalDate.of(2015, 4, 7), "Alice", "Bob", 1.00))
        masonJar.addPayment(Payment(LocalDate.of(2015, 4, 9), "Bob", "Alice", 2.00))
        masonJar.addPayment(Payment(LocalDate.of(2015, 4, 10), "Bob", "Charlize", 3.00))
        masonJar.addPayment(Payment(LocalDate.of(2015, 4, 10), "Charlize", "Alice", 4.00))

        masonJar.sumAmounts(PaymentFilter.suchThat(payer = Some("Bob"))) should be (5.0)
        masonJar.sumAmounts(PaymentFilter.suchThat(payee = Some("Alice"))) should be (6.0)
        masonJar.sumAmounts(PaymentFilter.suchThat(after = Some(LocalDate.of(2015, 4, 8)))) should be (9.0)
        masonJar.sumAmounts(PaymentFilter.suchThat(atLeast = Some(2.99))) should be (7.0)
    }

}
