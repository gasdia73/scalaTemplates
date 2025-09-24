package org.guz.auth

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
import org.guz.auth.User

object BasicAuthApp extends IOApp.Simple {

  val localhostUrl = "http://localhost:8080"
  val defaultUser = "bobby"
  val defaultPerms = "basic"

  /* basic auth:
    Request[IO] => IO[Either[String, User]]
    same as Kleisli[IO, Request[IO], Either[String, User]]

    Kleisli[F, A, B] equivalent to A => F[B]
   */

  val basicAuth = Kleisli.apply[IO, Request[IO], Either[String, User]] {
    request =>
      val headers = request.headers.get[Authorization]
      headers match
        case Some(Authorization(BasicCredentials(creds))) =>
          IO(Right(User(1L, creds._1)))
        // check password against db

        case Some(_) => IO(Left("No basic creds"))
        case None    => IO(Left("Nope!!"))
  }

  val onFailure: AuthedRoutes[String, IO] = Kleisli {
    (req: AuthedRequest[IO, String]) =>
      OptionT.pure[IO](Response[IO](status = Status.Unauthorized))
  }

  val basicAuthMiddleware: AuthMiddleware[IO, User] =
    AuthMiddleware(basicAuth, onFailure)

  val authedRoutes = AuthedRoutes.of[User, IO] {
    case GET -> Root / "welcome" as user => // localhost:8080/welcome/[user]
      Ok(s"Ciao $user")
  }

  val guardedRoutes = AuthedRoutes.of[User, IO] {
    case GET -> Root / "secret" as user =>
      Ok(s"Secret: $user")
  }

  val basicAuthServer =
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(basicAuthMiddleware(authedRoutes).orNotFound)
      .build

  override def run =
    basicAuthServer.use(_ => IO.never).void

}
