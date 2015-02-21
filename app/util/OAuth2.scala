package util

import play.api.Application
import play.api.Play
import play.api.http.{MimeTypes, HeaderNames}
import play.api.libs.ws.WS
import play.api.mvc.{Results, Action, Controller}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class OAuth2(application: Application) {
  //lazy val githubAuthId = application.configuration.getString("github.client.id").get
  //lazy val githubAuthSecret = application.configuration.getString("github.client.secret").get
  val yammerAuthId = application.configuration.getString("yammer.client.id").get
  val yammerAuthSecret = application.configuration.getString("yammer.client.secret").get

  //def getAuthorizationUrl(redirectUri: String, scope: String, state: String): String = {
  //  val baseUrl = application.configuration.getString("github.redirect.url").get
  //  baseUrl.format(githubAuthId, redirectUri, scope, state)
  //}
  def getAuthorizationUrl(redirectUri: String): String = {
    val baseUrl = application.configuration.getString("yammer.redirect.url").get
    baseUrl.format(yammerAuthId, redirectUri)
  }

  //  def getToken(code: String): Future[String] = {
  //    val tokenResponse = WS.url("https://github.com/login/oauth/access_token")(application).
  //      withQueryString("client_id" -> githubAuthId,
  //        "client_secret" -> githubAuthSecret,
  //        "code" -> code).
  //      withHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON).
  //      post(Results.EmptyContent())
  //
  //    tokenResponse.flatMap { response =>
  //      (response.json \ "access_token").asOpt[String].fold(Future.failed[String](new IllegalStateException("Sod off!"))) { accessToken =>
  //        Future.successful(accessToken)
  //      }
  //    }
  //  }
  def getToken(code: String): Future[String] = {
    val tokenResponse = WS.url("https://www.yammer.com/oauth2/access_token.json")(application).
      withQueryString("client_id" -> yammerAuthId,
        "client_secret" -> yammerAuthSecret,
        "code" -> code).
      withHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON).
      post(Results.EmptyContent())

    tokenResponse.flatMap { response =>
      (response.json \ "access_token" \ "token").asOpt[String].fold(Future.failed[String](new IllegalStateException("Sod off!"))) { accessToken =>
        Future.successful(accessToken)
      }
    }
  }
}

  object OAuth2 extends Controller {
    lazy val oauth2 = new OAuth2(Play.current)

    def callback(codeOpt: Option[String] = None) = Action.async { implicit request =>
      (for {
        code <- codeOpt
        oauthState <- request.session.get("oauth-state")
      } yield {
        if (code != None) {
          oauth2.getToken(code).map { accessToken =>
            Redirect(util.routes.OAuth2.success()).withSession("oauth-token" -> accessToken)
          }.recover {
            case ex: IllegalStateException => Unauthorized(ex.getMessage)
          }
        }
        else {
          Future.successful(BadRequest("Invalid yammer login"))
        }
      }).getOrElse(Future.successful(BadRequest("No parameters supplied")))
    }

    //  def success() = Action.async { request =>
    //    implicit val app = Play.current
    //    request.session.get("oauth-token").fold(Future.successful(Unauthorized("No way Jose"))) { authToken =>
    //      WS.url("https://api.github.com/user/repos").
    //        withHeaders(HeaderNames.AUTHORIZATION -> s"token $authToken").
    //        get().map { response =>
    //          Ok(response.json)
    //        }
    //    }
    //  }
    def success() = Action.async { request =>
      implicit val app = Play.current
      request.session.get("oauth-token").fold(Future.successful(Unauthorized("No way Jose"))) { authToken =>
        WS.url("https://www.yammer.com/api/v1/messages/following.json").
          withHeaders(HeaderNames.AUTHORIZATION -> s"Bearer $authToken").
          get().map { response =>
          Ok(response.json)
        }
      }
    }


}