package gg.fence.data
import akka.contrib.persistence.mongodb.Bson
import gg.fence.data.MongoClient.JsTransformer
import org.mongodb.scala.bson.{BsonDocument, BsonValue}
import org.mongodb.scala._
import org.mongodb.scala.bson.{BsonArray, BsonBoolean, BsonDecimal128, BsonString, BsonNull}
import org.mongodb.scala.{Observer => DbObserver}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.jdk.CollectionConverters._
import scala.util.Success


/** An Intermediary API that does not expose the current library used by the application */
trait MongoClient {
  def getData[A]()(implicit transformer: JsTransformer[A]): Future[List[A]]
  def getDataById[A](name: String)(implicit transformer: JsTransformer[A]): Future[Option[A]]
  def addData[A](data: A)(implicit transformer: JsTransformer[A]): Future[Unit]
}

object MongoClient {

  /** An application representation of JSON Values that does not use library definitions */
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
    def toJsValue(data: A): JsObject
    def fromJsValue(data: JsObject): A
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


  def apply(
             client: org.mongodb.scala.MongoClient,
             collectionRegistry: Map[Class[_],String]
           )(implicit ec: ExecutionContext): MongoClient = new MongoClient {

    private val db: MongoDatabase = client.getDatabase("fence-db")

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
      case jsObj: JsObject => fromJsObject(jsObj)
    }

    private def fromJsObject(jsObject: JsObject): BsonDocument = {
      val jsValueMap = jsObject.value.map { case (key, jsValue) =>
        key -> (jsValue match {
          case obj: JsObject => fromJsObject(obj)
          case other => fromJsValue(other)
        })
      }
      BsonDocument(jsValueMap)
    }

    private def observeResults(collection: MongoCollection[Document]): Future[List[Document]] = {
      val promise: Promise[Seq[Document]] = Promise()     // Create a Promise to be used in the Observer
      val observer = new Observer[Document](promise)      // Create the Observer that subscribes to the collection Observable
      collection.find().subscribe(observer)               // Make the observer subscribe to the documents emitted by the observable
      promise.future.map(_.toList)
    }


    override def getData[A]()(implicit transformer:  JsTransformer[A]): Future[List[A]] = {
      val collectionName = collectionRegistry(classOf[A])                          // Get the collection name of the data we are getting from the registry
      val mongoCollection = db.getCollection(collectionName)                       // Get the Mongo Collection for the data we are getting
      val result = observeResults(mongoCollection)
      result.map { listOfDocument =>
        listOfDocument.map { document =>
          val bsonJsValue = JsObject(document.map { case (key, value) =>           // Iterate on the BsonValue the client gives us
            key -> toJsValue(value)                                                // Transform Java's bullshit BsonValue into something useful in our application which is a JsValue
          }.toMap)                                                                 // Since Document is an Iterable of Key-Value pair, it's a Map and we transform it into a Map
          transformer.fromJsValue(bsonJsValue)                                     // Transform JsValue to an A
        }
      }
    }

    override def getDataById[A](name: String)(implicit transformer: JsTransformer[A]): Future[Option[A]] = {
      val promise: Promise[Seq[Document]] = Promise()
      val observer = new Observer[Document](promise)
      val collectionName = collectionRegistry(classOf[A])
      val mongocollection = db.getCollection(collectionName)
      mongocollection.find()
    }

    override def addData[A](data: A)(implicit transformer: JsTransformer[A]): Future[Unit] = {
      val collectionName = collectionRegistry(classOf[A])
      val jsValueOfData = transformer.toJsValue(data)
      val bsonDocument = fromJsObject(jsValueOfData)
      db.getCollection(collectionName).insertOne(bsonDocument).toFuture().map(_ => ())
    }
  }
}
