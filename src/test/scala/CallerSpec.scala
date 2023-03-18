// import CallerSpec._
import akka.testkit._
import akka.actor.ActorSystem
import akka.http.scaladsl.HttpExt
import gg.fence.data.ApiCaller
import gg.fence.data.DataRetriever.Item
import org.scalamock.scalatest.MockFactory
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.{AnyWordSpec, AnyWordSpecLike}

import scala.concurrent.ExecutionContext


class CallerSpec extends TestKit(ActorSystem("CallerSpec")) with AnyWordSpecLike with OptionValues with ScalaFutures with Matchers with MockFactory {
  implicit val ec: ExecutionContext = system.dispatcher
  private val httpClient = mock[HttpExt]
  private val caller = ApiCaller(httpClient)

  "CallerSpec" when {
    "using getitemprice" should {
      "return first item in the list when getting by id" in {
        caller.getItemPrice() should be
      }
      "return both items passed in constructor when getting item prices" in {
       caller.getItemPrice("gayshit") should be
      }
    }
  }

}

//object CallerSpec {
//
//  val item1 = Item()
//  val item2 = Item()
//  val items = List(item1, item2)
//}
