package gg.fence.data
import org.mongodb.scala.model
import scala.concurrent.Future

trait DataRetriever[A]{
      def get(amount: Int):Future[List[A]]
      def getByName(name: String):Future[Option[A]]
}

object DataRetriever{
      case class Item(Id: String,name: String,Price: Double)
      def ItemDataRetriever():DataRetriever[Item] = new DataRetriever[Item] {
            override def get(amount: Int): Future[List[Item]] = Future.successful(List(Item("asdasd","retard",69)))

            override def getByName(name: String): Future[Option[Item]] = Future.successful(None)
      }

}

