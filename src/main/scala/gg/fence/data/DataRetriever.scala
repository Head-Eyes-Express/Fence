package gg.fence.data
import gg.fence.data.MongoClient
import gg.fence.data.MongoClient.{JsNumber, JsString, JsTransformer}

import scala.concurrent.Future


trait DataRetriever[A]{
      def get(amount: Int):Future[List[A]]
      def getByName(name: String):Future[Option[A]]
}

object DataRetriever {

  case class Item(id: String, name: String, price: BigDecimal)

  private implicit val itemJsTransformer: JsTransformer[Item] = new JsTransformer[Item] {
            override def toJsValue(data: Item): MongoClient.JsObject =  {
              val itemMap = Map(
                "id" -> JsString(data.id),
                "name" -> JsString(data.name),
                "price" -> JsNumber(data.price)
              )
              MongoClient.JsObject(itemMap)
            }

            override def fromJsValue(data: MongoClient.JsObject): Item = {
              val jsId = data.value("id")
              val jsName = data.value("name")
              val jsPrice = data.value("price")
              (jsId, jsName, jsPrice) match {
                case (JsString(id), JsString(name), JsNumber(price)) => Item(id, name, price)
                case _ => throw new Exception("Data retrieved does not fit the model") // Redesign the API for JS to A value
              }
            }
  }

  def itemRetriever(client: MongoClient):DataRetriever[Item] = new DataRetriever[Item] {
    override def get(amount: Int): Future[List[Item]] =
      client.getData[Item]()

    override def getByName(name: String): Future[Option[Item]] =
      Future.successful(None)
  }
}

