package examplezio

import cats.MonadError
import zio.{Task, ZIO}

class TaskMonadError extends TaskMonad with MonadError[Task, Throwable] {

  override final def handleErrorWith[A](fa: Task[A])(f: Throwable => Task[A]): Task[A] =
    fa.catchAll(f)

  override final def recoverWith[A](fa: Task[A])(pf: PartialFunction[Throwable, Task[A]]): Task[A] =
    fa.catchSome(pf)

  override final def raiseError[A](t: Throwable): Task[A] =
    ZIO.fail(t)

  override final def attempt[A](fa: Task[A]): Task[Either[Throwable, A]] =
    fa.either

  override final def adaptError[A](fa: Task[A])(pf: PartialFunction[Throwable, Throwable]): Task[A] =
    fa.mapError(pf.orElse { case error => error })
}
