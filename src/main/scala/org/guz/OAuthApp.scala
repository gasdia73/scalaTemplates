package org.guz

import cats.effect.*
import org.http4s.*
import org.http4s.server.*
import org.http4s.implicits.*
import org.http4s.dsl.io.*
import com.comcast.ip4s.*
import cats.data.Kleisli
import org.http4s.headers.Authorization
import cats.data.OptionT
import org.http4s.server.middleware.authentication.DigestAuth.Md5HashedAuthStore
import org.http4s.server.middleware.authentication.DigestAuth
import pdi.jwt.*
import java.time.Instant
import dev.profunktor.auth.*
import dev.profunktor.auth.jwt.*
import io.circe.*
import io.circe.parser.*
import ciris.*
import ciris.circe.*
import java.nio.file.Paths
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.Accept
import org.http4s.server.middleware.ErrorAction
import org.http4s.server.middleware.ErrorHandling

object OAuthApp extends IOApp.Simple {

  val localhostUrl = "http://localhost:8080"
  val defaultUser = "bobby"
  val defaultPerms = "basic"

  val appConfig =
    file(Paths.get("src/main/resources/appConfig.json")).as[AppConfig]

  def fetchToken(code: String, config: AppConfig): IO[Option[String]] = {
    val form = UrlForm(
      "client_id" -> config.key,
      "client_secret" -> config.secret.value,
      "code" -> code
    )

    val req = Request[IO](
      Method.POST,
      uri"https://github.com/login/oauth/access_token",
      headers = Headers(Accept(MediaType.application.json))
    ).withEntity(form)
    println(s"request: $req")

    EmberClientBuilder
      .default[IO]
      .withMaxResponseHeaderSize(1024 * 1024)
      .build
      .use(client => client.expect[String](req))
      .map(jsonString => {
        println(s"decoding: $jsonString")
        decode[GitHubTokenResponse](jsonString)
      })
      .map {
        case Left(e)              => None
        case Right(tokenResponse) => Some(tokenResponse.accessToken)
      }
  }

  def fetchUserInfo(token: String): IO[String] = {
    val req = Request[IO](
      Method.GET,
      uri"https://api.github.com/user/emails",
      headers = Headers(
        Accept(MediaType.application.json),
        Authorization(Credentials.Token(AuthScheme.Bearer, token))
      )
    )

    EmberClientBuilder
      .default[IO]
      .build
      .use(client => client.expect[String](req))
      .map { response =>
        decode[List[GitHubUser]](response).toOption.flatMap(_.find(_.primary)) match {
          case Some(usr) => s"Welcome ${usr.email}!"
          case None        => "Authentication failed!" 
        }
      }
  }

  def getOAuthResult(code: String, config: AppConfig): IO[String] = {
    for {
      maybeToken <- fetchToken(code, config)
      result <- maybeToken match {
        case Some(token) => fetchUserInfo(token)
        case None        => IO("Authentication failed!")
      }
    } yield result
  }

  val dsl = Http4sDsl[IO]
  import dsl.*

  def routes(config: AppConfig): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ GET -> Root / "home" =>
      StaticFile
        .fromString("src/main/resources/html/index.html", Some(req))
        .getOrElseF(NotFound())
    // localhost:8080/callback?code=...
    case GET -> Root / "callback" :? GitHubTokenQueryParamMatcher(code) =>
      println(s"callback code: $code")
      getOAuthResult(code, config).flatMap(result => Ok(result))
  }

  def errorHandler(t: Throwable, msg: => String): OptionT[IO, Unit] =
    OptionT.liftF(
      IO.println(msg) >>
        IO.println(t) >>
        IO(t.printStackTrace())
    )

  def routesWithErrorLogging(config: AppConfig) = ErrorHandling.Recover.total(
    ErrorAction.log(
      routes(config),
      messageFailureLogAction = errorHandler,
      serviceErrorLogAction = errorHandler
    )
  )

  override def run: IO[Unit] = for {
    config <- appConfig.load[IO]
    server <- EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(routesWithErrorLogging(config).orNotFound)
      .build
      .use(_ => IO(println("Server up!")) *> IO.never)
  } yield ()

}

object GitHubTokenQueryParamMatcher
    extends QueryParamDecoderMatcher[String]("code")

case class GitHubUser(email: String, primary: Boolean, verified: Boolean) derives Decoder

case class GitHubTokenResponse(
    accessToken: String,
    tokenType: String,
    scope: String
)

object GitHubTokenResponse {

  given decoder: Decoder[GitHubTokenResponse] = Decoder.instance { hCursor =>
    for {
      accessToken <- hCursor.get[String]("access_token")
      tokenType <- hCursor.get[String]("token_type")
      scope <- hCursor.get[String]("scope")
    } yield GitHubTokenResponse(accessToken, tokenType, scope)
  }

}

case class AppConfig(key: String, secret: Secret[String])

object AppConfig {

  given appDecoder: Decoder[AppConfig] = Decoder.instance { hCursor =>
    for {
      key <- hCursor.get[String]("key")
      secret <- hCursor.get[String]("secret")
    } yield AppConfig(key, Secret(secret))
  }

  given appConfigDecoder: ConfigDecoder[String, AppConfig] = circeConfigDecoder(
    "AppConfig"
  )

}
