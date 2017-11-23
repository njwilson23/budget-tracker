package masonjar

import io.circe._
import java.util.{Date}

case class Payment(date: Date, payer: String, payee: String, amount: Double)

case class PaymentID(id: Int)

object PaymentImplicits {

    implicit val encodePayment: Encoder[Payment] = new Encoder[Payment] {
        final def apply(pmt: Payment): Json = Json.obj(
            ("payer", Json.fromString(pmt.payer)),
            ("payee", Json.fromString(pmt.payee)),
            ("amount", Json.fromDouble(pmt.amount) getOrElse Json.fromString("NA")),
            ("date", Json.fromString(pmt.date.toString))
        )
    }
    implicit val decodePayment: Decoder[Payment] = new Decoder[Payment] {
        final def apply(c: HCursor): Decoder.Result[Payment] = for {
            payer <- c.downField("payer").as[String]
            payee <- c.downField("payee").as[String]
            dateStr <- c.downField("date").as[String]
            amount <- c.downField("amount").as[Double]
        } yield Payment(new Date(dateStr), payer, payee, amount)
    }

}

