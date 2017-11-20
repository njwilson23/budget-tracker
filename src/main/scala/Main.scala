package server

import java.io.File

import io.circe.generic.auto._
import io.circe.syntax._
import fs2.{Stream, Task}
import fs2.interop.cats._
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

        // how do these lines work?
        case request @ GET -> Root => StaticFile.fromFile(new File("frontend/masonjar/build/index.html"), Some(request)).getOrElseF(NotFound())
        case request @ GET -> Root / "static" / dirname / filename  =>
            StaticFile.fromFile(new File(s"frontend/masonjar/build/static/$dirname/$filename"),
                                Some(request)).getOrElseF(NotFound())

        case GET -> Root / "payments" :? IdQueryParamMatcher(id) => Ok(getPaymentById(id))

        case GET -> Root / "payments" / "count" => Ok(payments.length.toString)

        // this is temporary, and will be replaced by improving the /payments endpoint
        case GET -> Root / "payments" / "all" =>
            val paymentList: List[(Int, Payment)] = payments
                .getAllPayments
                .sortWith(_._1 < _._1)
            Ok(paymentList.asJson)

        case request @ POST -> Root / "payments" / "add" =>
            // Somehow 400 (BadRequest) automatically emitted when the JSON is invalid
            for {
                pmt <- request.as(jsonOf[Payment])
                resp <- Ok(payments.addPayment(pmt).toString)
            } yield resp

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
