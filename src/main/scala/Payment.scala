package masonjar

import io.circe._
import java.time.LocalDate

sealed trait Expenditure

case class Transfer(date: LocalDate, payer: String, payee: String, amount: Double, id: Option[Int] = None) extends Expenditure

case class Payment(date: LocalDate, payer: String, payee: String, amount: Double, id: Option[Int] = None) extends Expenditure {
    def asPositive: Payment = {
        if (amount >= 0) Payment(date, payer, payee, amount)
        else Payment(date, payee, payer, -amount)
    }

    def test(predicate: Rule): Boolean = predicate(this)
}

case class PaymentID(id: Int)

object PaymentImplicits {

    implicit val encodePayment: Encoder[Payment] = new Encoder[Payment] {
        final def apply(pmt: Payment): Json = Json.obj(
            ("payer", Json.fromString(pmt.payer)),
            ("payee", Json.fromString(pmt.payee)),
            ("amount", Json.fromDouble(pmt.amount) getOrElse Json.fromString("NA")),
            ("date", Json.fromString(pmt.date.toString)),
            ("id", Json.fromString(pmt.id.getOrElse(-999).toString))
        )
    }
    implicit val decodePayment: Decoder[Payment] = new Decoder[Payment] {
        final def apply(c: HCursor): Decoder.Result[Payment] = for {
            payer <- c.downField("payer").as[String]
            payee <- c.downField("payee").as[String]
            dateStr <- c.downField("date").as[String]
            amount <- c.downField("amount").as[Double]
            id <- c.getOrElse[Int]("id")(-999)
        } yield Payment(LocalDate.parse(dateStr), payer, payee, amount, Some(id))
    }

}

