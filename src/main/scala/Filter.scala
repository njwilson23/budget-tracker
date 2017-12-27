// Experimental nicer filters

package masonjar
import java.time.LocalDate

object FilterImplicits {
    implicit val ruleToCompositeFilter: Rule => CompositeFilter = rule => CompositeFilter(rule, IdentityFilter)
}

sealed trait Filter {
    def +(other: CompositeFilter): CompositeFilter
}

final case object IdentityFilter extends Filter {
    def +(other: CompositeFilter): CompositeFilter = other
}

final case class CompositeFilter(head: Rule, tail: Filter = IdentityFilter) extends Filter {
    def +(other: CompositeFilter): CompositeFilter = head + (tail + other)
}

trait Rule {
    def apply(p: Payment): Boolean
    def unary_!(): Rule = !this.apply(_)
    def +(other: CompositeFilter): CompositeFilter = CompositeFilter(this, other)
}

final case class PaidAfter(dt: LocalDate) extends Rule {
    def apply(pmt: Payment): Boolean = pmt.date.isAfter(dt)
}

final case class PaidBefore(dt: LocalDate) extends Rule {
    def apply(pmt: Payment): Boolean = pmt.date.isBefore(dt)
}

final case class AtLeast(amt: Double) extends Rule {
    def apply(pmt: Payment): Boolean = (pmt.amount - amt) >= -0.005
}

final case class AtMost(amt: Double) extends Rule {
    def apply(pmt: Payment): Boolean = (pmt.amount - amt) <= 0.005
}

final case class PaidBy(payer: String) extends Rule {
    def apply(pmt: Payment): Boolean = pmt.payer == payer
}

final case class PaidTo(payee: String) extends Rule {
    def apply(pmt: Payment): Boolean = pmt.payee == payee
}

final case class PaymentIDEquals(id: Int) extends Rule {
    def apply(pmt: Payment): Boolean = pmt.id.getOrElse(id + 1) == id
}
