package gg.fence

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import gg.fence.guardian.Guardian
import gg.fence.http.{ApiService, Server}

import scala.concurrent.Future

object Main {

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    val httpConfig = config.getConfig("app.http")
    val httpHost = httpConfig.getString("host")
    val httpPort = httpConfig.getInt("port")
    val initialBehavior = ActorSystem(Guardian(), config.getString("app.actorSystemName"), config)
    val apiService = ApiService()
    val server = Server(initialBehavior.classicSystem, apiService)
    server.start(httpHost, httpPort)
  }

}