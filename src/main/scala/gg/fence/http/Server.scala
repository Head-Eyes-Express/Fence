package gg.fence.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives.complete
import gg.fence.http.ApiService.{Request, Response}

import scala.concurrent.{ExecutionContext, Future}


/** Serves an HTTP Gateway to the outside world for the application*/
trait Server {

  /** Starts the http server with defined host and port
   *
   * @param host the hostname of the API
   * @param port which port it is going to be served at
   * @return Future that starts the server
   */
  def start(host: String, port: Int): Future[Unit]

  /** Stops the http server if it is started
   *  Doesn't do shit if the HTTP server is not started
   * @return Future that stops the server
   */
  def stop(): Future[Unit]
}

object Server {

  def apply(as: ActorSystem, apiService: ApiService): Server = new Server {

    private var serverBinding: Http.ServerBinding = null

    private implicit val ec: ExecutionContext = as.dispatcher

    private def toDomainRequest(akkaRequest: HttpRequest): Request =
      ApiService.Unit

    private def fromDomainResponse(response: Response): HttpResponse =
      HttpResponse()

    /** Starts the http server with defined host and port
     *
     * @param host the hostname of the API
     * @param port which port it is going to be served at
     * @return Future that starts the server
     */
    override def start(host: String, port: Int): Future[Unit] =
      Http(as).newServerAt(host, port).bind { httpRequest =>
        apiService.serve(toDomainRequest(httpRequest)).map(fromDomainResponse)
      }.map  { binding =>
        serverBinding = binding
      }

    /** Stops the http server if it is started
     * Doesn't do shit if the HTTP server is not started
     *
     * @return Future that stops the server
     */
    override def stop(): Future[Unit] =
      serverBinding.unbind().map(_ => ())
  }
}
