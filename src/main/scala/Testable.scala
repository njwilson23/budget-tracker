package masonjar

import java.time.LocalDate

trait Testable {
    def test(pmt: Payment): Boolean
    def unary_!(): Testable = p => !test(p)
}

abstract class UnaryFilter extends Testable {
    def +(that: Testable): CompositeFilter = CompositeFilter(List(this, that))
}

final case class PaidAfter(dt: LocalDate) extends UnaryFilter {
    def test(pmt: Payment): Boolean = pmt.date.isAfter(dt)
}

final case class PaidBefore(dt: LocalDate) extends UnaryFilter {
    def test(pmt: Payment): Boolean = pmt.date.isBefore(dt)
}

final case class AtLeast(amt: Double) extends UnaryFilter {
    def test(pmt: Payment): Boolean = (pmt.amount - amt) >= -0.005
}

final case class AtMost(amt: Double) extends UnaryFilter {
    def test(pmt: Payment): Boolean = (pmt.amount - amt) <= 0.005
}

final case class PaidBy(payer: String) extends UnaryFilter {
    def test(pmt: Payment): Boolean = pmt.payer == payer
}

final case class PaidTo(payee: String) extends UnaryFilter {
    def test(pmt: Payment): Boolean = pmt.payee == payee
}

final case class PaymentIDEquals(id: Int) extends UnaryFilter {
    def test(pmt: Payment): Boolean = pmt.id.getOrElse(id + 1) == id
}

case class CompositeFilter(steps: List[Testable]) extends Testable {
    val rules: List[Testable] = steps
    def test(pmt: Payment): Boolean = steps.map(_.test(pmt)).reduce(_ & _)
    def +(filter: Testable): CompositeFilter = CompositeFilter(filter :: steps)
}
