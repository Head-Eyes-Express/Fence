package gg.fence

import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import gg.fence.guardian.Guardian

object Main {

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    val initialBehavior = ActorSystem(Guardian(), config.getString("app.actorSystemName"), config)
    initialBehavior ! Guardian.Hello
    initialBehavior ! Guardian.World
  }

}