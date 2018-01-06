package server

import java.io.File
import java.time.LocalDate

import fs2.interop.cats._
import fs2.{Stream, Task}
import io.circe.generic.auto._
import io.circe.syntax._
import masonjar.PaymentImplicits._
import masonjar._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl._
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.middleware._
import org.http4s.util.StreamApp

object Server extends StreamApp {

    // State object
    val payments = new MasonJar()

    implicit val dateQueryParamDecoder: QueryParamDecoder[LocalDate] = QueryParamDecoder[String].map(LocalDate.parse)

    // Define a matcher to extract payment IDs from URLs
    object AfterDateQueryParam extends QueryParamDecoderMatcher[LocalDate]("after")
    object BeforeDateQueryParam extends QueryParamDecoderMatcher[LocalDate]("before")

    object LenderQueryParam extends QueryParamDecoderMatcher[String]("from")
    object DebtorQueryParam extends QueryParamDecoderMatcher[String]("to")
    object SplitFractionQueryParam extends QueryParamDecoderMatcher[Double]("splitFraction")

    // Extract a payment option as a string or return an empty string
    def getPaymentById(id: Int): String = payments.index(id).map(_.toString) getOrElse s"no such payment (id=$id)"

    // Service handling insertion, retrieval, and deletion of payment records
    val paymentService = HttpService {

        // how do these lines work?
        case request @ GET ->
            Root => StaticFile.fromFile(new File("frontend/masonjar/build/index.html"), Some(request)).getOrElseF(NotFound())

        case request @ GET -> Root / "static" / dirName / fileName  =>
            StaticFile.fromFile(new File(s"frontend/masonjar/build/static/$dirName/$fileName"),
                                Some(request)).getOrElseF(NotFound())

        case GET -> Root / "payments" :? AfterDateQueryParam(after) +& BeforeDateQueryParam(before) =>
            val paymentList: List[Payment] = payments
                .search(PaidAfter(after) + PaidBefore(before))
                .sortWith(_.id.getOrElse(-1) < _.id.getOrElse(-1))
            Ok(paymentList.asJson)

        case GET -> Root / "payments" / "count" => Ok(payments.length.toString)

        case GET -> Root / "payments" / "owed" :? LenderQueryParam(from) +& DebtorQueryParam(to) =>
            Ok(payments.owed(from, to).asJson)

        case GET -> Root / "payments" / "owed" :? LenderQueryParam(from) +& DebtorQueryParam(to) +& SplitFractionQueryParam(split) =>
            Ok(payments.owed(from, to, split).asJson)

        case GET -> Root / "resolve" => Ok(payments.resolveDebts.asJson)

        case request @ POST -> Root / "payments" / "add" =>
            // Somehow 400 (BadRequest) automatically emitted when the JSON is invalid
            for {
                pmt <- request.as(jsonOf[Payment])
                resp <- Ok(payments.add(pmt).toString)
            } yield resp

        case request @ POST -> Root / "payments" / "delete" =>
            for {
                data <- request.as(jsonOf[PaymentID])
                resp <- payments.pop(data.id) match {
                    case None => NotFound(data.id.toString)
                    case Some(pmt) => Ok(pmt.asJson)
                }
            } yield resp

        case GET -> Root / "payments" / IntVar(paymentId) => Ok(getPaymentById(paymentId))

        case GET -> Root / "payments" / _ => Forbidden()
    }

    override def stream(args: List[String]): Stream[Task, Nothing] = {
        BlazeBuilder
            .bindHttp(8080, "localhost")
            .mountService(CORS(paymentService), "/")
            .serve
    }

}
