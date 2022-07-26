package examplezio

import cats.StackSafeMonad
import zio.{Exit, Task, ZIO}

class TaskMonad extends StackSafeMonad[Task] {

  override final def flatMap[A, B](fa: Task[A])(f: A => Task[B]): Task[B] =
    fa.flatMap(f)

  override final def map[A, B](fa: Task[A])(f: A => B): Task[B] =
    fa.map(f)

  /**
   * Keeps calling `f` until a `scala.util.Right[B]` is returned.
   *
   * Based on Phil Freeman's
   * [[http://functorial.com/stack-safety-for-free/index.pdf Stack Safety for Free]].
   *
   * Implementations of this method should use constant stack space relative to `f`.
   */
  override final def tailRecM[A, B](a: A)(f: A => Task[Either[A, B]]): Task[B] =
    ZIO.suspend(f(a)).flatMap {
      case Left(l)  => tailRecM(l)(f)
      case Right(r) => pure(r)
    }

  override final def pure[A](x: A): Task[A] =
    Exit.Success(x)

  override final def flatTap[A, B](fa: Task[A])(f: A => Task[B]): Task[A] =
    fa.tap(f)

  override final def widen[A, B >: A](fa: Task[A]): Task[B] =
    fa

  override final def map2[A, B, Z](fa: Task[A], fb: Task[B])(f: (A, B) => Z): Task[Z] =
    fa.zipWith(fb)(f)

  override final def as[A, B](fa: Task[A], b: B): Task[B] =
    fa.as(b)

  override final def whenA[A](cond: Boolean)(f: => Task[A]): Task[Unit] =
    ZIO.suspendSucceed(if (cond) f.map(_ => ()) else unit)

  override final def unit: Task[Unit] =
    ZIO.unit
}
