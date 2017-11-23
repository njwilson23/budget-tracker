import org.scalatest.{FlatSpec, Matchers}

import java.util.{Date}
import masonjar.{MasonJar, Payment, PaymentFilter}

class MasonJarTests extends FlatSpec with Matchers {

    "A MasonJar" should "accept payments" in {
        val masonJar = new MasonJar()
        val idx1 = masonJar.addPayment(Payment(new Date(0), "Alice", "Britannia Sushi", 12.50))
        idx1 should be(0)
        masonJar.length should be(1)
        val idx2 = masonJar.addPayment(Payment(new Date(0), "Alice", "Santa Barbara", 23.71))
        idx2 should be(1)
        masonJar.length should be(2)
    }

    "A MasonJar" should "return previously deposited payments" in {
        val masonJar = new MasonJar()
        val idx = masonJar.addPayment(Payment(new Date(0), "Alice", "Britannia Sushi", 12.50))
        masonJar.addPayment(Payment(new Date(0), "Alice", "Santa Barbara", 23.71))

        val payee = masonJar.getPayment(idx).map(_.payee) getOrElse "missing"
        payee should be("Britannia Sushi")
        masonJar.length should be(2)

        val payee2 = masonJar.popPayment(idx).map(_.payee) getOrElse "missing"
        payee2 should be("Britannia Sushi")
        masonJar.length should be(1)
    }

    "A MasonJar" should "throw IndexOutOfBounds when the payment doesn't exist" in {
        val masonJar = new MasonJar()
        masonJar.addPayment(Payment(new Date(0), "Alice", "Britannia Sushi", 12.50))
        val idx = masonJar.addPayment(Payment(new Date(0), "Alice", "Britannia Sushi", 12.50))
        masonJar.addPayment(Payment(new Date(0), "Alice", "Britannia Sushi", 12.50))

        // create a hole
        masonJar.popPayment(idx)

        masonJar.popPayment(idx) should be (None)


    }

    "A MasonJar" should "be summable by payer and by payee" in {
        val masonJar = new MasonJar()
        masonJar.addPayment(Payment(new Date(0), "Alice", "Bob", 1.00))
        masonJar.addPayment(Payment(new Date(1), "Bob", "Alice", 2.00))
        masonJar.addPayment(Payment(new Date(2), "Bob", "Charlize", 3.00))
        masonJar.addPayment(Payment(new Date(3), "Charlize", "Alice", 4.00))

        masonJar.sumAmounts(new PaymentFilter(payer = Some("Bob"))) should be (5.0)
        masonJar.sumAmounts(new PaymentFilter(payee = Some("Alice"))) should be (6.0)
        //masonJar.sumAmounts(new PaymentFilter(date = Some((dt: Int) => (new Date(2)).before(dt) & dt.before(new Date(4))))) should be (4.0)
        masonJar.sumAmounts(new PaymentFilter(amount = Some((amt: Double) => amt >= 3.0))) should be (7.0)
    }

}
