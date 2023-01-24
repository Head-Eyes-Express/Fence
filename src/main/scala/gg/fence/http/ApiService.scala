package gg.fence.http

import gg.fence.http.ApiService.{Request, Response}
import gg.fence.data.DataRetriever
import gg.fence.data.DataRetriever.Item

import scala.concurrent.{ExecutionContext, Future}

trait ApiService {

  def serve(request: Request): Future[Response]

}

object ApiService {

  sealed trait Request
  final case object Unit extends Request

  sealed trait Response
  final case object Hello extends Response

  final case class Final[A](item: List[A]) extends Response


  def apply(dataRetriever: DataRetriever[Item])(implicit ec: ExecutionContext): ApiService = (request: Request) => request match {
    case Unit =>
      println("Fuck you")
      dataRetriever.get(69).map(x => Final(x))
     // Future.successful(Hello)
  }

}