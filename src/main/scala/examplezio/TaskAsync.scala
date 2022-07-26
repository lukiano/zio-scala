package examplezio

import scala.concurrent.{ExecutionContext, Future}

import zio.{Exit, Promise, Task, ZIO}
import cats.effect.Async
import cats.effect.kernel.{Cont, Sync, Unique}

class TaskAsync extends TaskTemporal with Async[Task] {

  override final def evalOn[A](fa: Task[A], ec: ExecutionContext): Task[A] =
    fa.onExecutionContext(ec)

  override final val executionContext: Task[ExecutionContext] =
    ZIO.executor.map(_.asExecutionContext)

  override final val unique: Task[Unique.Token] =
    Exit.Success(new Unique.Token)

  override final def cont[K, Q](body: Cont[Task, K, Q]): Task[Q] =
  Async.defaultCont(body)(this)

  override final def suspend[A](hint: Sync.Type)(thunk: => A): Task[A] = hint match {
    case Sync.Type.Delay                                           => ZIO.attempt(thunk)
    case Sync.Type.Blocking                                        => ZIO.attemptBlocking(thunk)
    case Sync.Type.InterruptibleOnce | Sync.Type.InterruptibleMany => ZIO.attemptBlockingInterrupt(thunk)
  }

  override final def defer[A](thunk: => Task[A]): Task[A] =
    ZIO.suspend(thunk)

  override final def async[A](k: (Either[Throwable, A] => Unit) => Task[Option[Task[Unit]]]): Task[A] =
    Promise.make[Nothing, Unit].flatMap { promise =>
    ZIO.asyncZIO { register =>
      k(either => register(promise.await *> ZIO.fromEither(either))) *> promise.succeed(())
    }
  }

  override final def async_[A](k: (Either[Throwable, A] => Unit) => Unit): Task[A] =
    ZIO.async(register => k(register.compose(fromEither)))

  override final def fromFuture[A](fut: Task[Future[A]]): Task[A] =
    fut.flatMap(f => ZIO.fromFuture(_ => f))

  override final def never[A]: Task[A] =
    ZIO.never
}