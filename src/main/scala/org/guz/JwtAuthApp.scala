package org.guz

import cats.effect.*
import org.http4s.*
import org.http4s.server.*
import org.http4s.implicits.*
import org.http4s.dsl.io.*
import org.http4s.ember.server.*
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

object JwtAuthApp extends IOApp.Simple {

  val localhostUrl = "http://localhost:8080"
  val defaultUser = "bobby"
  val defaultPerms = "basic"

  val guardedRoutes = AuthedRoutes.of[User, IO] {
    case GET -> Root / "secret" as user =>
      Ok(s"Secret: $user")
  }

  val jwtRoutes = AuthedRoutes.of[User, IO] {
    case GET -> Root / "welcome" as user => // localhost:8080/welcome/[user]
      Ok(s"Ciao $user").map(_.addCookie(ResponseCookie("token", token)))
  }

  val searchFunc: String => IO[Option[(User, String)]] = { case "bobby" =>
    for {
      user <- IO.pure(User(1L, "bobby"))
      hash <- Md5HashedAuthStore
        .precomputeHash[IO](defaultUser, localhostUrl, "org.guz")
    } yield Some(user, hash)
  }

  val authStore = Md5HashedAuthStore(searchFunc)

  val middleware: IO[AuthMiddleware[IO, User]] =
    DigestAuth.applyF[IO, User](localhostUrl, authStore)

  val middlewareResource = Resource.eval(middleware)

  case class TokenPayload(user: String, permsLevel: String)

  object TokenPayload {

    given decoder: Decoder[TokenPayload] = Decoder.instance { hCursor =>
      for {
        user <- hCursor.get[String]("user")
        permsLevel <- hCursor.get[String]("level")
      } yield {
        println(s"user: $user")
        println(s"permsLevel: $permsLevel")
        TokenPayload(user, permsLevel)
      }
    }

  }

  def claim(user: String, permsLevel: String) = JwtClaim(
    content = s"""
    |{
    |"user": "$user",
    |"level": "$permsLevel"
    |}
    """.stripMargin,
    expiration =
      Some(Instant.now().plusSeconds(10 * 24 * 60 * 60).getEpochSecond),
    issuedAt = Some(Instant.now().getEpochSecond)
  )

  val key = "keystoredsomewheresafe"
  val algo = JwtAlgorithm.HS256

  val token = JwtCirce.encode(
    claim(defaultUser, defaultPerms),
    key,
    algo
  ) // jwt built manually

  // database
  val database = Map(
    "bobby" -> User(1L, defaultUser)
  )

  val authorizeFunction: JwtToken => JwtClaim => IO[Option[User]] =
    token =>
      claim =>
        println(s"claim: $claim.content")
        decode[TokenPayload](claim.content) match {
          case Left(_)        => IO(None)
          case Right(payload) => 
            println(s"payload: $payload")
            IO(database.get(payload.user))
        }

  val jwtAuthMiddleware =
    JwtAuthMiddleware[IO, User](JwtAuth.hmac(key, algo), authorizeFunction)

  val routerResource = middlewareResource.map { mw =>
    Router(
      "/login" -> mw(jwtRoutes),
      "/guarded" -> jwtAuthMiddleware(guardedRoutes)
    )
  }

  val jwtServer = for {
    router <- routerResource
    server <- EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(router.orNotFound)
      .build
  } yield server

  override def run =
    jwtServer.use(_ => IO.never).void

}
