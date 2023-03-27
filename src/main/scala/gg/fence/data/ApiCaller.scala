package gg.fence.data
import akka.actor.ActorSystem
import akka.http.scaladsl.{Http, HttpExt}
import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.unmarshalling.Unmarshal
import gg.fence.data.ApiCaller.ExternalItem
import io.circe.generic.auto._
import caliban._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.syntax.EncoderOps
import io.circe.parser.decode

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
// todo: create a http request to an external api
// track the callTime and updatedAt
// something something caliban
// test the shit out


trait ApiCaller{
  def getItemPrice(): Future[List[ExternalItem]]
  def getItemPrice(id: String): Future[Option[ExternalItem]]
}
object ApiCaller extends FailFastCirceSupport{

  case class ExternalItem()

  def stub(items: List[ExternalItem]): ApiCaller = new ApiCaller {
    override def getItemPrice(): Future[List[ExternalItem]] = {
      Future.successful(items)
    }

    override def getItemPrice(id: String): Future[Option[ExternalItem]] = {
      Future.successful(items.headOption)

    }

  }
  def apply(httpClient: HttpExt)(implicit ec : ExecutionContext, system: ActorSystem ): ApiCaller =
    new ApiCaller {
      override def getItemPrice(): Future[List[ExternalItem]] = {
        val header = HttpHeader.parse("AUTH-Token", "bede6545d506c4f00a56")
        header match {
          case ParsingResult.Ok(parsedHeader, errors) => {
           val request = HttpRequest(
              method = HttpMethods.GET,
              uri = "https://api.tarkov-changes.com/v1/grenades",
              headers = List(parsedHeader)
            )
             // val response = Http().singleRequest(request) // Defines a real client on every call (harder to be controllable)
              val responseV2  =  httpClient.singleRequest(request)  // A client is provided once and is used many times by the caller, can be a mock/stub/etc (makes things controllable)
              responseV2.flatMap(res => Unmarshal(res).to[List[ExternalItem]])
          }
          case ParsingResult.Error(error) => {
            Future.failed(new Exception("gay: "+ error))
          }
        }
      }


      override def getItemPrice(id: String): Future[Option[ExternalItem]] = {
        val header = HttpHeader.parse("AUTH-Token", "bede6545d506c4f00a56")
        header match {
          case ParsingResult.Ok(parsedHeader, errors) => {
            val request = HttpRequest(
              method = HttpMethods.GET,
              uri = "https://api.tarkov-changes.com/v1/grenades",
              headers = List(parsedHeader)
            )
            val response = httpClient.singleRequest(request)
            response.flatMap(res => Unmarshal(res).to[List[ExternalItem]].map(list => list.headOption))
          }
          case ParsingResult.Error(error) => {
            Future.failed(new Exception("gay: "+ error))
          }
        }
      }
    }
}
