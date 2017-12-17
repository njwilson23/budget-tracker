package masonjar

import java.time.LocalDate

trait FilterRule {
    def +(that: FilterRule): Filter = new Filter(List(this, that))

    def test(pmt: Payment): Boolean
}

final case class PaidAfter(dt: LocalDate) extends FilterRule {
    def test(pmt: Payment): Boolean = pmt.date.isAfter(dt)
}

final case class PaidBefore(dt: LocalDate) extends FilterRule {
    def test(pmt: Payment): Boolean = pmt.date.isBefore(dt)
}

final case class AtLeast(amt: Double) extends FilterRule {
    def test(pmt: Payment): Boolean = (pmt.amount - amt) >= -0.005
}

final case class AtMost(amt: Double) extends FilterRule {
    def test(pmt: Payment): Boolean = (pmt.amount - amt) <= 0.005
}

final case class PaidBy(payer: String) extends FilterRule {
    def test(pmt: Payment): Boolean = pmt.payer == payer
}

final case class PaidTo(payee: String) extends FilterRule {
    def test(pmt: Payment): Boolean = pmt.payee == payee
}

class Filter(steps: List[FilterRule]) {

    val rules = steps

    def apply(payments: List[Payment]): List[Payment] = payments.filter(p => steps.map(_.test(p)).reduce(_ & _))

    def +(filter: Filter): Filter = new Filter(steps ++ filter.rules)

    def ::(step: FilterRule): Filter = new Filter(step :: steps)
}
