package masonjar

import java.time.LocalDate

trait Filter {
    def test(pmt: Payment): Boolean
}

trait UnaryFilter extends Filter {
    def +(that: UnaryFilter): CompositeFilter = CompositeFilter(List(this, that))

    def test(pmt: Payment): Boolean
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

case class CompositeFilter(steps: List[Filter]) extends Filter {

    val rules: List[Filter] = steps

    def test(pmt: Payment): Boolean = steps.map(_.test(pmt)).reduce(_ & _)

    def apply(payments: List[Payment]): List[Payment] = payments.filter(test)

    def +(filter: CompositeFilter): CompositeFilter = CompositeFilter(steps ++ filter.rules)

    def ::(step: UnaryFilter): CompositeFilter = CompositeFilter(step :: steps)
}
