package gg.fence.test


import CallerSpec._
import gg.fence.data.ApiCaller
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.{AnyWordSpec, AsyncWordSpec}

import scala.concurrent.Future

class CallerSpec extends AnyWordSpec with OptionValues with ScalaFutures with Matchers {

  private val stub = Caller.stub(items)

  "CallerSpec" when {
    "using stub" should {
      "return first item in the list when getting by id" in {
        stub.getItemPrice("doesn'tmatterhadsex").futureValue.value shouldBe item1
      }
      "return both items passed in constructor when getting item prices" in {
        stub.getItemPrices.futureValue shouldBe items
      }
    }
  }

}

object CallerSpec {

  val item1 = Item()
  val item2 = Item()
  val items = List(item1, item2)
}
