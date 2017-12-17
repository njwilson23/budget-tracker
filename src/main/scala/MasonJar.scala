package masonjar

import java.time.LocalDate

class MasonJar() {
    private var payments: List[Payment] = List()
    private var index: Int = -1

    private def getByIndex(index: Int, payments: List[Payment]): Option[Payment] = payments match {
        case List() => None
        case fst :: _ if fst.id.getOrElse(index+1) == index => Some(fst)
        case _ :: rest => getByIndex(index, rest)
    }

    def length: Int = payments.length

    def add(pmt: Payment): Int = {
        index = index + 1
        payments = Payment(pmt.date, pmt.payer, pmt.payee, pmt.amount, Some(index)) :: payments
        index
    }

    def index(index: Int): Option[Payment] = getByIndex(index, payments)

    def search(filter: CompositeFilter): List[Payment] = filter(payments)

    def allPayments: List[Payment] = payments

    def popPayment(index: Int): Option[Payment] = {
        val pmt = getByIndex(index, payments)
        payments = payments.filter(p => p.id.getOrElse(index + 1) != index)
        pmt
    }

    def sumAmounts(f: Filter): Double = payments.filter(f.test).map(_.amount).sum

    // Return amount owed to one entity by another entity, provided that the first entity
    // agrees to be responsible for a specific fraction of expenses
    def owed(debtor: String, lender: String, fractionHandledByLender: Double = 0.5): Payment = {
        val spentByLender = payments.filter(ip => ip.payer == lender & ip.payee != debtor).map(_.amount).sum
        val spentByDebtor = payments.filter(ip => ip.payer == debtor & ip.payee != lender).map(_.amount).sum
        val lenderToDebtor = sumAmounts(PaidBy(lender) + PaidTo(debtor))
        val debtorToLender = sumAmounts(PaidBy(debtor) + PaidTo(lender))

        Payment(
            date = LocalDate.now(),
            payer = debtor,
            payee = lender,
            amount = lenderToDebtor -
                debtorToLender +
                fractionHandledByLender * spentByLender -
                (1-fractionHandledByLender) * spentByDebtor
        )
    }

    def allPayers: List[String] = payments.map(_.payer).distinct.sorted

    private def combs2[T](lst: List[T]): List[(T,T)] = {
        lst match {
            case List() => List()
            case _ :: Nil => List()
            case a :: b :: rest => List((a, b)) ++ combs2(a::rest) ++ combs2(b::rest)
        }
    }

    def distribute(balances: List[Balance], entity: String, amount: Double): Payment = {
        if (amount > 0) {
            val maxBalance = balances.map(_.amount).max
            val ownerHighest = balances.drop(balances.indexWhere(_.amount == maxBalance)).head.owner
            Payment(LocalDate.now(), entity, ownerHighest, amount)
        } else {
            val minBalance = balances.map(_.amount).min
            val ownerLowest = balances.drop(balances.indexWhere(_.amount == minBalance)).head.owner
            Payment(LocalDate.now(), ownerLowest, entity, amount)
        }
    }

    // Return a sequence of payments that would resolve all debts
    def rebalance(balances: List[Balance]): List[Payment] = {
        val sumBalance = balances.map(_.amount).sum
        val meanBalance = sumBalance / balances.length
        balances match {
            case List() => List()
            case _ :: Nil => List()
            case Balance(owner, amt) :: rest =>
                if ((amt - meanBalance).abs < 0.01) rebalance(rest)
                else {
                    val pmt: Payment = distribute(rest, owner, meanBalance - amt).asPositive
                    val newRest = rest.map(bal =>
                        if (bal.owner == pmt.payee) Balance(bal.owner, bal.amount - pmt.amount)
                        else if (bal.owner == pmt.payer) Balance(bal.owner, bal.amount + pmt.amount)
                        else bal)
                    pmt :: rebalance(newRest)
                }
        }
    }

    // Return a list of payer relationships indicating who owes whom
    def resolveDebts(): List[Payment] = {
        val payers = allPayers
        val paid = payers.map(payer => {
            Balance(payer, payments.filter(_.payer == payer).map(_.amount).sum - payments.filter(_.payee == payer).map(_.amount).sum)
        })
        rebalance(paid)
    }

}


