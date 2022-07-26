package examplezio

import cats.effect.{Async, Resource}
import cats.effect.std.Dispatcher
import zio.{Console, Exit, ExitCode, FiberFailure, Scope, Task, ZIO, ZIOAppArgs}
import zio.managed.*
import zio.logging.*
//import zio.logging.slf4j.Slf4jLogger

import scala.concurrent.ExecutionContext

// import io.github.vigoo.zioaws.netty

import zio.interop.catz._
import zio.interop.catz.implicits._

// import zhttp.http._
// import zhttp.service.Server

import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Server

object Main extends zio.ZIOAppDefault {

//   def run(args: List[String]) = {
//     val httpClient = netty.default
//     myAppLogic.exitCode
//   }

//   val env =
//     Logging.console(
//       logLevel = LogLevel.Info,
//       format = LogFormat.ColoredLogFormat()
//     ) >>> Logging.withRootLoggerName("my-component")

//   val myAppLogic =
//     for {
//       _    <- putStrLn("Hello! What is your name?")
//       name <- getStrLn
//       _    <- putStrLn(s"Hello, ${name}, welcome to ZIO!")
//     } yield ()

    // Create HTTP route
  // val app = Http.collect {
  //   case Method.GET -> Root / "text" => Response.text("Hello World!")
  //   case Method.GET -> Root / "json" => Response.jsonString("""{"greetings": "Hello World!"}""")
  // }

  // val foo = for {
  //   _ <- log.info("Starting operation")
  //   time <- ZIO.effectTotal(System.currentTimeMillis())
  // } yield Response.text(time.toString)

  // val cooler = Http.collectM {
  //   case Method.GET -> Root / "foo" => foo
  // }

//   val logFormat = "[correlation-id = %s] %s"

//  val env =
//    Slf4jLogger.make{(context, message) =>
//        val correlationId = LogAnnotation.CorrelationId.render(
//          context.get(LogAnnotation.CorrelationId)
//        )
//        message
//    }

  private val dsl = Http4sDsl[Task]
  import dsl._

  // implicit val taskAsync: TaskAsync = new TaskAsync
  private lazy val helloWorldService = HttpRoutes
    .of[Task] {
      case GET -> Root / "hello" => Ok("Hello, Joe")
    }
    .orNotFound

  private def http4Server(ec: ExecutionContext): Resource[Task, Server] =
    BlazeServerBuilder[Task].withExecutionContext(ec)
      .bindHttp(8080, "localhost")
      .withHttpApp(helloWorldService)
      .resource

  @inline private def toExitCase(exit: Exit[Any, Any]): Resource.ExitCase =
    exit match {
      case Exit.Success(_) =>
        Resource.ExitCase.Succeeded
      case Exit.Failure(cause) if cause.isInterrupted =>
        Resource.ExitCase.Canceled
      case Exit.Failure(cause) =>
        cause.failureOrCause match {
          case Left(error: Throwable) => Resource.ExitCase.Errored(error)
          case _                      => Resource.ExitCase.Errored(FiberFailure(cause))
        }
    }

  def go[A](resource: Resource[Task, A]): ZManaged[Any, Throwable, A] =
    resource match {
      case Resource.Allocate(res) =>
        lazy val allocatedResource = implicitly[Async[Task]].uncancelable(res) // : Task[(Any, Resource.ExitCase => Task[Unit])]
        lazy val foo = allocatedResource map { // : Task[Reservation[Any, Nothing, ?]]
          case (b, release) => Reservation(Exit.Success(b), error => release(toExitCase(error)).orDie)
        }
        ZManaged.fromReservationZIO(foo)

      case Resource.Bind(source, fs) =>
        for {
          a <- go(source)
          b <- go(fs(a))
        } yield b

      case Resource.Eval(fa) => ZManaged.fromZIO(fa)

      case Resource.Pure(a) => ZManaged(Exit.Success((ZManaged.Finalizer.noop, a)))
    }

  def buildServer(ec: ExecutionContext): ZManaged[Any, Throwable, Server] =
    go(http4Server(ec))

  def runServer(ec: ExecutionContext): Task[ExitCode] =
    buildServer(ec)
      .useForever
      .foldCauseZIO(
        err => Console.printLine(err.prettyPrint).as(ExitCode.failure),
        _ => ZIO.succeed(ExitCode.success)
      )

  // Run it like any simple app
  override def run: ZIO[Environment with ZIOAppArgs with Scope, Any, Any] = {
    // val httpClient = netty.default
    // Server.start(8080, cooler <> app).provideCustomLayer(env).exitCode
    Dispatcher(implicitly[Async[Task]]).use { implicit dispatcher =>
      for {
        _ <- ZIO.runtime
        // _ <- taskAsync.executionContext.flatMap(runServer)
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

