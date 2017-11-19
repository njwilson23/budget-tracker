package server

import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import fs2.{Stream, Task}
import org.http4s._
import org.http4s.dsl._
import org.http4s.circe._
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.util.StreamApp
import masonjar.{MasonJar, Payment, PaymentID}

object Main extends StreamApp {

    // State object
    val payments = new MasonJar()

    // Define a matcher to extract payment IDs from URLs
    object IdQueryParamMatcher extends QueryParamDecoderMatcher[Int]("id")

    // Extract a payment option as a string or return an empty string
    def getPaymentById(id: Int): String = payments.getPayment(id).map(_.toString) getOrElse s"no such payment (id=$id)"

    // Service handling insertion, retrieval, and deletion of payment records
    val paymentService = HttpService {

        case request @ POST -> Root / "payments" / "add" =>
            // Somehow 400 (BadRequest) automatically emitted when the JSON is invalid
            for {
                pmt <- request.as(jsonOf[Payment])
                resp <- Ok(payments.addPayment(pmt).toString)
            } yield resp

        case GET -> Root / "payments" :? IdQueryParamMatcher(id) => Ok(getPaymentById(id))

        case GET -> Root / "payments" / "count" => Ok(payments.length.toString)

        case request @ POST -> Root / "payments" / "delete" =>
            for {
                data <- request.as(jsonOf[PaymentID])
                resp <- payments.popPayment(data.id) match {
                    case None => NotFound(data.id.toString)
                    case Some(pmt) => Ok(pmt.asJson)
                }
            } yield resp

        case GET -> Root / "payments" / _ => Forbidden()
    }

    override def stream(args: List[String]): Stream[Task, Nothing] = {
        BlazeBuilder
            .bindHttp(8080, "localhost")
            .mountService(paymentService, "/")
            .serve
    }

}
