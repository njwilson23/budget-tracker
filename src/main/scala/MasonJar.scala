package masonjar

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
        if (payments.length == 0) None
        else payments.head match {
            case (i, pmt) if i == index => Some(pmt)
            case (i, _) if i != index => _getPaymentByIndex(index, payments.tail)
        }
    }

    def getPayment(index: Int): Option[Payment] = _getPaymentByIndex(index, payments)

    def popPayment(index: Int): Option[Payment] = {
        val pmt = _getPaymentByIndex(index, payments)
        if (pmt.isDefined) {
            payments = payments.filter(_._1 != index)
        }
        pmt
    }

    def sumAmounts(start: Option[Int] = None,
                   end: Option[Int] = None,
                   payer: Option[String] = None,
                   payee: Option[String] = None): Double = {

        def trueOrNone(opt: Option[Boolean]): Boolean = opt match {
            case Some(tf) => tf
            case None => true
        }

        def qualifier(pmt: Payment): Boolean = {
            trueOrNone(start.map(_ <= pmt.date)) &
                trueOrNone(end.map(pmt.date < _)) &
                trueOrNone(payer.map(_ == pmt.payer)) &
                trueOrNone(payee.map(_ == pmt.payee))
        }

        val data = payments.filter(_._2.amount > 10)

        payments
            .filter(it => qualifier(it._2))
            .map(_._2.amount)
            .sum
    }

}


