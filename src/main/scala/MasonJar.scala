package masonjar

import java.time.LocalDate

class PaymentFilter(date: Option[LocalDate => Boolean] = None,
                    payer: Option[String] = None,
                    payee: Option[String] = None,
                    amount: Option[Double => Boolean] = None) {

    private def trueOrNone(opt: Option[Boolean]): Boolean = opt match {
        case Some(tf) => tf
        case None => true
    }

    private def qualifier(pmt: Payment): Boolean = trueOrNone(date.map(_.apply(pmt.date))) &
                                                   trueOrNone(payer.map(_ == pmt.payer)) &
                                                   trueOrNone(payee.map(_ == pmt.payee)) &
                                                   trueOrNone(amount.map(_.apply(pmt.amount)))

    def apply(payments: List[(Int, Payment)]): List[(Int, Payment)] = payments.filter(it => qualifier(it._2))
}

object PaymentFilter {
    def suchThat(after: Option[LocalDate] = None,
                 before: Option[LocalDate] = None,
                 atLeast: Option[Double] = None,
                 under: Option[Double] = None,
                 payer: Option[String] = None,
                 payee: Option[String] = None): PaymentFilter = {

        new PaymentFilter(
            payer = payer,
            payee = payee,
            amount = Some((amt: Double) => (atLeast.isEmpty || amt >= atLeast.get) && (under.isEmpty || amt < under.get)),
            date = Some((dt: LocalDate) => (after.isEmpty || dt.isAfter(after.get)) && (before.isEmpty || dt.isBefore(before.get)))
        )
    }
}

class MasonJar() {
    private var payments: List[(Int, Payment)] = List()
    private var index: Int = -1

    def length: Int = payments.length

    def addPayment(pmt: Payment): Int = {
        index = index + 1
        payments = (index, pmt) :: payments
        index
    }

    def _getPaymentByIndex(index: Int, payments: List[(Int, Payment)]): Option[Payment] ={
        if (payments.isEmpty) None
        else payments.head match {
            case (i, pmt) if i == index => Some(pmt)
            case (i, _) if i != index => _getPaymentByIndex(index, payments.tail)
        }
    }

    def getPayment(index: Int): Option[Payment] = _getPaymentByIndex(index, payments)

    def getPayments(f: PaymentFilter): List[(Int, Payment)] = f(payments)

    def getAllPayments: List[(Int, Payment)] = payments

    def popPayment(index: Int): Option[Payment] = {
        val pmt = _getPaymentByIndex(index, payments)
        if (pmt.isDefined) {
            payments = payments.filter(_._1 != index)
        }
        pmt
    }

    def aggregate[T](f: PaymentFilter)(agg: List[(Int, Payment)] => T): T = agg(f.apply(payments))
    def sumAmounts(f: PaymentFilter): Double = aggregate(f)((lst: List[(Int, Payment)]) => lst.map(_._2.amount).sum)

    // Return amount owed to one entity by another entity, provided that the first entity
    // agrees to be responsible for a specific fraction of expenses
    def owed(lender: String, debtor: String, fractionHandledByLender: Double): Imbalance = {
        val spentByLender = payments.filter(ip => ip._2.payer == lender & ip._2.payee != debtor).map(_._2.amount).sum
        val spentByDebtor = payments.filter(ip => ip._2.payer == debtor & ip._2.payee != lender).map(_._2.amount).sum
        val lenderToDebtor = sumAmounts(new PaymentFilter(payer=Some(lender), payee=Some(debtor)))
        val debtorToLender = sumAmounts(new PaymentFilter(payer=Some(debtor), payee=Some(lender)))
        val totalSpent = spentByDebtor + spentByLender

        Imbalance(lender, debtor, 0.5 * (totalSpent) + lenderToDebtor - debtorToLender)
    }

}


