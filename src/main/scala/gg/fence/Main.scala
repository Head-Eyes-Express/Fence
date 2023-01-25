package gg.fence

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import gg.fence.data.DataRetriever
import gg.fence.guardian.Guardian
import gg.fence.http.{ApiService, Server}
import org.mongodb.scala.{MongoClient, MongoDatabase}

import scala.concurrent.{ExecutionContext, Future}

object Main {

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    val httpConfig = config.getConfig("app.http")
    val httpHost = httpConfig.getString("host")
    val httpPort = httpConfig.getInt("port")
    val initialBehavior = ActorSystem(Guardian(), config.getString("app.actorSystemName"), config)
    implicit val ec: ExecutionContext = initialBehavior.executionContext
    val dataRetriever = DataRetriever.ItemDataRetriever()
    val apiService = ApiService(dataRetriever)
    val server = Server(initialBehavior.classicSystem, apiService)
    server.start(httpHost, httpPort)
  }

}