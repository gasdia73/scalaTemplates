val scala3Version = "3.3.6"

val Http4sVersion = "0.23.18"

val JwtHttp4sVersion = "1.2.0"

val JwtScalaVersion = "9.3.0"

val catsEffectVersion = "3.5.2"

val CirisVersion = "3.2.0"

val weaverVersion = "0.8.3"

val catsEffect = "org.typelevel" %% "cats-effect" % catsEffectVersion

val http4sDsl = "org.http4s" %% "http4s-dsl" % Http4sVersion

val emberServer = "org.http4s" %% "http4s-ember-server" % Http4sVersion

val emberClient = "org.http4s" %% "http4s-ember-client" % Http4sVersion

val jwtHttp4s = "dev.profunktor" %% "http4s-jwt-auth" % JwtHttp4sVersion

val jwtScala = "com.github.jwt-scala" %% "jwt-core" % JwtScalaVersion

val jwtCirce = "com.github.jwt-scala" %% "jwt-circe" % JwtScalaVersion

val ciris = "is.cir" %% "ciris" % CirisVersion

val cirisCirce = "is.cir" %% "ciris-circe" % CirisVersion

val weaver = "com.disneystreaming" %% "weaver-cats" % weaverVersion % Test

lazy val protobuf =
  project
    .in(file("protobuf"))
    .settings(
      name := "protobuf",
      scalaVersion := scala3Version
    )
    .enablePlugins(Fs2Grpc)

lazy val root = (project in file("."))
  .settings(
    name := "authentication",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      emberServer,
      emberClient,
      http4sDsl,
      jwtHttp4s,
      jwtScala,
      jwtCirce,
      catsEffect,
      ciris,
      cirisCirce,
      weaver,
      "io.grpc" % "grpc-netty-shaded" % scalapb.compiler.Version.grpcJavaVersion
    ),
    testFrameworks += new TestFramework("weaver.framework.CatsEffect")
  )
  .dependsOn(protobuf)
