// Experimental nicer filters

package masonjar
import java.time.LocalDate

object TypeAliases {
    type Predicate = (Payment) => Boolean
}

trait Rule {
    def apply(payment: Payment): Boolean
    def +(that: Filter): Filter
    def unary_!(): Filter
}

object Filter {
    def apply(predicates: TypeAliases.Predicate*) = new Filter(predicates.toList)
}

class Filter(val predicates: List[TypeAliases.Predicate]) extends Rule {
    def apply(payment: Payment): Boolean = predicates.map(_.apply(payment)).reduce(_ && _)
    def +(that: Filter): Filter = Filter(this.predicates ++ that.predicates: _*)
    def unary_!(): Filter = Filter((payment) => !this.apply(payment))
}

object PaidAfter {
    def apply(dt: LocalDate): Filter = Filter((payment: Payment) => payment.date.isAfter(dt))
}

object PaidBefore {
    def apply(dt: LocalDate): Filter = Filter((payment: Payment) => payment.date.isBefore(dt))
}

object AtLeast {
    def apply(amt: Double): Filter = Filter((payment: Payment) => (payment.amount - amt) >= -0.005)
}

object AtMost {
    def apply(amt: Double): Filter = Filter((payment: Payment) => (payment.amount - amt) <= 0.005)
}

object PaidBy {
    def apply(payer: String): Filter = Filter((payment: Payment) => payment.payer == payer)
}

object PaidTo {
    def apply(payee: String): Filter = Filter((payment: Payment) => payment.payee == payee)
}

object PaymentIDEquals {
    def apply(id: Int): Filter = Filter((payment: Payment) => payment.id.getOrElse(id + 1) == id)
}

