package gg.fence.data
import akka.protobufv3.internal.ByteString
import org.mongodb.scala._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.model.Sorts._

import scala.concurrent.Future

trait MongoClient {
  def getData[A]()(implicit transformer: Json => A): Future[List[A]]
  def getDatabyName[A](name: String)(implicit transformer: Json => A): Future[List[A]]
  def addData[A](data: A)(implicit transformer: A => Json): Future[Unit]
}
case class Json(data: Array[Byte])


object MongoClient {


  def apply(client: org.mongodb.scala.MongoClient, collectionRegistry: Map[Class[_],String]): MongoClient = new MongoClient {

    // what the fuck does this mean
    // something something Atlas
    // System.setProperty("org.mongodb.async.type", "netty")

    val db: MongoDatabase = client.getDatabase("fence-db")

    override def getData[A]()(implicit transformer: Json => A): Future[List[A]] = {
      // retrieves data, converts mongo document into a list of A
      val collection: MongoCollection[Document] = db.getCollection(collectionRegistry.apply(classOf[A]))
      val res = collection.find().collect().toFuture()
      res
    }

    override def getDatabyName[A](name: String)(implicit transformer: Json => A): Future[List[A]] = {
      val collection: MongoCollection[Document] = db.getCollection(collectionRegistry.apply(classOf[A]))
    }

    override def addData[A](data: A)(implicit transformer: A => Json): Future[Unit] = {
      val collection: MongoCollection[Document] = db.getCollection(collectionRegistry.apply(classOf[A]))
    }

  }
}
