package masonjar

import io.circe._

case class Imbalance(debtor: String, lender: String, amount: Double)

object ImbalanceImplicits {

    implicit val encodeImbalance: Encoder[Imbalance] = new Encoder[Imbalance] {
        final def apply(pmt: Imbalance): Json = Json.obj(
            ("lender", Json.fromString(pmt.lender)),
            ("debtor", Json.fromString(pmt.debtor)),
            ("amount", Json.fromDouble(pmt.amount) getOrElse Json.fromString("NA"))
        )
    }
    implicit val decodeImbalance: Decoder[Imbalance] = new Decoder[Imbalance] {
        final def apply(c: HCursor): Decoder.Result[Imbalance] = for {
            lender <- c.downField("payer").as[String]
            debtor <- c.downField("payee").as[String]
            amount <- c.downField("amount").as[Double]
        } yield Imbalance(debtor, lender, amount)
    }

}
