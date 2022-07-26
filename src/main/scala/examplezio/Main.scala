package examplezio

import cats.effect.{Async, Resource}
import cats.effect.std.Dispatcher
import zio.{Console, Exit, ExitCode, FiberFailure, LogLevel, Scope, Task, ZIO, ZIOAppArgs, ZLayer}
import zio.logging.LogFormat
import zio.managed.*
import zio.logging.backend.SLF4J

import scala.concurrent.ExecutionContext
import zio.interop.catz.*
import zio.interop.catz.implicits.*
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits.*
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Server

object Main extends zio.ZIOAppDefault {

  val env: ZLayer[Any, Nothing, Unit] =
    SLF4J.slf4j(
      logLevel = LogLevel.Info,
      format = LogFormat.colored
    ) // >>> Logging.withRootLoggerName("my-component")

  private val dsl = Http4sDsl[Task]
  import dsl._

  private lazy val helloWorldService = HttpRoutes
    .of[Task] {
      case GET -> Root / "hello" => Ok("Hello, Joe")
    }
    .orNotFound

  // Run it like any simple app
  override def run: ZIO[Environment with ZIOAppArgs with Scope, Any, Any] = {
    Dispatcher(implicitly[Async[Task]]).use { implicit dispatcher =>
      for {
        _ <- ZIO.runtime
        _ <- BlazeServerBuilder[Task]
          .bindHttp(8080, "localhost")
          .withHttpApp(helloWorldService)
          .resource
          .toManaged
          .useForever
          .foldCauseZIO(
            err => Console.printLine(err.prettyPrint).as(ExitCode.failure),
            _ => ZIO.succeed(ExitCode.success)
          )
      } yield ()
    }
  }
}

