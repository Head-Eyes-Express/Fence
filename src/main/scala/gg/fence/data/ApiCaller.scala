package gg.fence.data

import gg.fence.data.ApiCaller.ExternalItem
import scala.concurrent.Future

trait ApiCaller{
  def getItemPrice(): Future[List[ExternalItem]]
  def getItemPrice(id: String): Future[Option[ExternalItem]]
}
object ApiCaller {
  case class ExternalItem()

  def stub(items: List[ExternalItem]): ApiCaller = new ApiCaller {
    override def getItemPrice(): Future[List[ExternalItem]] = ???
    override def getItemPrice(id: String): Future[Option[ExternalItem]] = ???
  }
}
