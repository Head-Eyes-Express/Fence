package gg.fence.http

import gg.fence.http.ApiService.{Request, Response}

import scala.concurrent.Future

trait ApiService {

  def serve(request: Request): Future[Response]

}

object ApiService {

  sealed trait Request
  final case object Unit extends Request

  sealed trait Response
  final case object Hello extends Response

  def apply(): ApiService = (request: Request) => request match {
    case Unit =>
      println("Fuck you")
      Future.successful(Hello)
  }

}