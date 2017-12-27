package masonjar

import java.time.LocalDate
import FilterImplicits._

class MasonJar() {

    private var payments: List[Payment] = List()
    private var index: Int = -1

    def length: Int = payments.length

    def add(pmt: Payment): Int = {
        index = index + 1
        payments = Payment(pmt.date, pmt.payer, pmt.payee, pmt.amount, Some(index)) :: payments
        index
    }

    def index(index: Int): Option[Payment] = getByIndex(index, payments)

    def search(f: Filter): List[Payment] = payments.filter(_.test(f))

    def allPayments: List[Payment] = payments

    def pop(index: Int): Option[Payment] = {
        val pmt = getByIndex(index, payments)
        val predicate = CompositeFilter(PaymentIDEquals(index))
        payments = payments.filter(!_.test(predicate))
        pmt
    }

    // Return amount owed to one entity by another entity, provided that the first entity
    // agrees to be responsible for a specific fraction of expenses
    def owed(debtor: String, lender: String, fractionHandledByLender: Double = 0.5): Payment = {
        val spentByLender = tally(PaidBy(lender) + !PaidTo(debtor))
        val spentByDebtor = tally(PaidBy(debtor) + !PaidTo(lender))
        val lenderToDebtor = tally(PaidBy(lender) + PaidTo(debtor))
        val debtorToLender = tally(PaidBy(debtor) + PaidTo(lender))

        Payment(
            date = LocalDate.now(),
            payer = debtor,
            payee = lender,
            amount = lenderToDebtor -
                debtorToLender +
                fractionHandledByLender * spentByLender -
                (1 - fractionHandledByLender) * spentByDebtor
        )
    }

    // Sum payments meeting particular criteria
    def tally(pred: Filter): Double = payments.filter(_.test(pred)).map(_.amount).sum

    // Distribute a payment or debit from an entity amongst a list of balances.
    //
    // In this implementation, a positive transaction is applied to the lowest balance, while a negative transaction is
    // applied to the largest balance
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

    // Return a sequence of payments that would resolve all deficits and surpluses across multiple balances
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

    def allPayers: List[String] = payments.map(_.payer).distinct.sorted

    // Return a list of payer relationships indicating who owes whom
    def resolveDebts(): List[Payment] = {
        val paid = allPayers.map(payer => {
            Balance(payer,
                payments.filter(_.payer == payer).map(_.amount).sum -
                    payments.filter(_.payee == payer).map(_.amount).sum)
        })
        rebalance(paid)
    }

    private def getByIndex(index: Int, payments: List[Payment]): Option[Payment] = payments match {
        case List() => None
        case fst :: _ if fst.id.getOrElse(index + 1) == index => Some(fst)
        case _ :: rest => getByIndex(index, rest)
    }

}


