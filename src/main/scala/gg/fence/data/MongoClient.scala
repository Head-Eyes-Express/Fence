package gg.fence.data
import akka.contrib.persistence.mongodb.Bson
import gg.fence.data.MongoClient.{JsTransformer, JsValue}
import org.mongodb.scala.bson.{BsonDocument, BsonValue}
// import org.bson.{BsonArray, BsonBoolean, BsonInt64, BsonNull, BsonObjectId, BsonString, BsonValue}
import org.mongodb.scala._
import org.mongodb.scala.bson.{BsonArray, BsonBoolean, BsonDecimal128, BsonObjectId, BsonString, BsonNull}
import org.mongodb.scala.{Observer => DbObserver}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.jdk.CollectionConverters._
import scala.util.Success

trait MongoClient {
  def getData[A]()(implicit transformer: JsTransformer[A]): Future[List[A]]
  def getDataByName[A](name: String)(implicit transformer: JsTransformer[A]): Future[List[A]]
  def addData[A](data: A)(implicit transformer: JsTransformer[A]): Future[Unit]
}

object MongoClient {

  sealed trait JsValue
  final case object JsNull extends JsValue
  final case class JsString(value: String) extends JsValue
  final case class JsNumber(value: BigDecimal) extends JsValue
  final case class JsBoolean(value: Boolean) extends JsValue
  final case class JsArray(value: Array[JsValue]) extends JsValue
  final case class JsObject(value: Map[String, JsValue]) extends JsValue

  /** a typeclass that knows how to transform an [[A]] into a [[JsValue]]
   *
   * @tparam A the type to be transformed
   */
  trait JsTransformer[A] {
    def toJsValue(data: A): JsValue
    def fromJsValue(data: JsValue): A
  }


  /**
   * Implementation of an [[org.mongodb.scala.Observer]] that is Promise-based
   * Aggregates all results coming from the [[org.mongodb.scala.Observable]]
   *
   * Promise is only completed once [[onComplete]] or [[onError]] is called
   *
   * @param resultPromise the [[Promise]] to be completed
   *
   * @tparam A            the type of data the observable is emitting
   */
  private[this] class Observer[A](resultPromise: Promise[Seq[A]]) extends DbObserver[A] {

    @volatile
    private[this] var sequenceOfResult: Vector[A] = Vector.empty[A]

    override def onNext(result: A): Unit =
      sequenceOfResult = sequenceOfResult.appended(result)

    override def onError(e: Throwable): Unit =
      resultPromise.failure(e)

    override def onComplete(): Unit =
      resultPromise.complete(Success(sequenceOfResult))
  }


  def apply(client: org.mongodb.scala.MongoClient, collectionRegistry: Map[Class[_],String])(implicit ec: ExecutionContext): MongoClient = new MongoClient {

    // what the fuck does this mean
    // something something Atlas
    // System.setProperty("org.mongodb.async.type", "netty")

    val db: MongoDatabase = client.getDatabase("fence-db")


    /** Converts a [[BsonValue]] to a [[JsValue]]
     *  If it encounters a [[org.mongodb.scala.bson.BsonArray]] or [[org.mongodb.scala.bson.BsonDocument]]
     *  then it will recurse into transformation
     *
     *  @param value the [[BsonValue]] to be converted to JsValue
     *  @return [[JsValue]]
     */
    private def toJsValue(value: BsonValue): JsValue = {
      if(value.isNull) {
        JsNull
      } else if(value.isBoolean) {
        JsBoolean(value.asBoolean().getValue)
      } else if(value.isArray) {
        JsArray(value.asArray().getValues.asScala.toArray.map(toJsValue))
      } else if(value.isNumber) {
        JsNumber(value.asNumber().decimal128Value().bigDecimalValue())
      } else if(value.isString) {
        JsString(value.asString().getValue)
      } else {
        JsObject(value.asDocument().asScala.toMap.map { case (key, documentValue) =>
          key -> toJsValue(documentValue)
        })
      }
    }

    private def fromJsValue(jsValue: JsValue): BsonValue = jsValue match {
      case JsNull => BsonNull()
      case JsString(value) => BsonString(value)
      case JsNumber(value) => BsonDecimal128(value)
      case JsBoolean(value) => BsonBoolean(value)
      case JsArray(value) => BsonArray.fromIterable(value.map(fromJsValue))
      case JsObject(value) => BsonDocument(value.map{ case (k, v) =>
        k -> fromJsValue(v)
      })
    }


    override def getData[A]()(implicit transformer:  JsTransformer[A]): Future[List[A]] = {
      val promise: Promise[Seq[Document]] = Promise()                              // Create a Promise to be used in the Observer
      val observer = new Observer[Document](promise)                               // Create the Observer that subscribes to the collection Observable
      val collectionName = collectionRegistry(classOf[A])                          // Get the collection name of the data we are getting from the registry
      val collection: MongoCollection[Document] = db.getCollection(collectionName) // Get the collection
      collection.find().subscribe(observer)                                        // Make the observer subscribe to the documents emitted by the observable
      promise.future.map { listOfDocument =>
        listOfDocument.map { document =>
          val bsonJsValue = JsObject(document.map { case (key, value) =>           // Iterate on the BsonValue the client gives us
            key -> toJsValue(value)                                                // Transform Java's bullshit BsonValue into something useful in our application which is a JsValue
          }.toMap)                                                                 // Since Document is an Iterable of Key-Value pair, it's a Map and we transform it into a Map
          transformer.fromJsValue(bsonJsValue)                                     // Transform JsValue to an A
        }.toList                                                                   // Convert the Sequence of A to a List of A
      }
    }

    override def getDataByName[A](name: String)(implicit transformer: JsTransformer[A]): Future[List[A]] = {
      val promise: Promise[Seq[Document]] = Promise()
      val observer = new Observer[Document](promise)
      val collectionName = collectionRegistry(classOf[A])
      val collection: MongoCollection[Document] = db.getCollection(collectionName)
    }

    override def addData[A](data: A)(implicit transformer: JsTransformer[A]): Future[Unit] = {
      val promise: Promise[Seq[Document]] = Promise()
      val observer = new Observer[Document](promise)
      val collectionName = collectionRegistry(classOf[A])
      val collection: MongoCollection[Document] = db.getCollection(collectionName)
      val bsonData = fromJsValue(transformer.toJsValue(data))
      collection.insertOne(bsonData)
    }
  }
}
