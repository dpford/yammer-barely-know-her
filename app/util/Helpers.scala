package util

import play.api.libs.json.{JsValue, JsArray}
import scala.collection.mutable

object Helpers {
  def getRandomGroup(sourceArray: JsArray, finalArray: JsArray, count: Int): JsArray = {
    if (count < 1) {
      return finalArray
    }
    val r = scala.util.Random
    val randomIndexNum: Int = r.nextInt(sourceArray.value.size)
    val person: JsValue = sourceArray(randomIndexNum)
    getRandomGroup(sourceArray, finalArray.append(person), count - 1)

  }

  def extractImageUrls(source: JsArray) = {
    val profileUrls: mutable.ListBuffer[String] = mutable.ListBuffer()
    for ( a  <- 0 to source.value.length - 1) {
      profileUrls += (source(a) \ "mugshot_url_template").as[String]
    }
    profileUrls
  }

  def getPersonToGuess(source: JsArray) = {
    val r = scala.util.Random
    val randomIndexNum: Int = r.nextInt(source.value.size)
    (source(randomIndexNum) \ "full_name").as[String]
  }


}