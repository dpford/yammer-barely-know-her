package controllers

import java.util.UUID

import play.api._
import play.api.http.HeaderNames
import play.api.libs.ws.WS
import play.api.mvc._
import util.OAuth2
import util.Helpers._
import util.OAuth2._
import play.api.libs.json.{JsObject, JsArray, JsValue, Json}

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Application extends Controller {

  def index = Action { implicit request =>
    val oauth2 = new OAuth2(Play.current)
    val callbackUrl = util.routes.OAuth2.callback(None).absoluteURL()
    val redirectUrl = oauth2.getAuthorizationUrl(callbackUrl)
    Ok(views.html.index("Your new application is ready.", redirectUrl)).
      withSession("oauth-state" -> redirectUrl)
  }

  def quiz = Action.async { implicit request =>
    implicit val app = Play.current
    request.session.get("oauth-token").fold(Future.successful(Unauthorized("No way Jose"))) { authToken =>
      WS.url("https://www.yammer.com/api/v1/users.json").
        withHeaders(HeaderNames.AUTHORIZATION -> s"Bearer $authToken").
        get().map { response =>
        def getProfilePic(url: String, width: String, height: String) = {
          url.replace("{width}x{height}", s"$width" + "x" + s"$height")
        }
        val json: JsArray = Json.parse(response.body).as[JsArray]
        val chosenOnes:JsArray = getRandomGroup(json, new JsArray, 4)
        val personToGuess: String = getPersonToGuess(chosenOnes)
        val imageUrls: mutable.ListBuffer[String] = extractImageUrls(chosenOnes)
        for (a <- 0 to imageUrls.length - 1) {
          imageUrls(a) = getProfilePic(imageUrls(a), "250", "300")
        }
        Ok(views.html.quiz(imageUrls, personToGuess))
      }
    }
  }

}