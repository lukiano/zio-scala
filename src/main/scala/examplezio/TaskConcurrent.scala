package examplezio

import cats.effect.{Deferred, Fiber, GenConcurrent, Outcome, Poll, Ref, Unique}
import zio.{Exit, Promise, Task, ZIO, Fiber => ZioFiber, Ref => ZioRef}

class TaskConcurrent extends TaskMonadError with GenConcurrent[Task, Throwable] {

  @inline private def toOutcome[A](exit: Exit[Throwable, A]): Outcome[Task, Throwable, A] =
    exit match {
      case Exit.Success(value)                      =>
        Outcome.Succeeded(ZIO.succeed(value))
      case Exit.Failure(cause) if cause.isInterrupted =>
        Outcome.Canceled[Task, Throwable, A]()
      case Exit.Failure(cause)                      =>
        cause.failureOrCause match {
          case Left(error)  => Outcome.Errored(error)
          case Right(cause) => Outcome.Succeeded(ZIO.failCause(cause))
        }
    }
  private def toFiber[A](fiber: ZioFiber[Throwable, A]) = new Fiber[Task, Throwable, A] {
    override final val cancel: Task[Unit]           = fiber.interrupt.unit
    override final val join: Task[Outcome[Task, Throwable, A]] = fiber.await.map(toOutcome)
  }

  override def ref[A](a: A): Task[Ref[Task, A]] =
    ZioRef.make(a).map(new TaskRef(_))

  override def deferred[A]: Task[Deferred[Task, A]] =
    Promise.make[Throwable, A].map(new TaskDeferred(_))

  override final def start[A](fa: Task[A]): Task[Fiber[Task, Throwable, A]] =
    fa.interruptible.forkDaemon.map(toFiber)

  override def never[A]: Task[A] =
    ZIO.never

  override final def cede: Task[Unit] =
    ZIO.yieldNow

  override final def forceR[A, B](fa: Task[A])(fb: Task[B]): Task[B] =
    fa.foldCauseZIO(cause => if (cause.isInterrupted) ZIO.failCause(cause) else fb, _ => fb)

  override final def uncancelable[A](body: Poll[Task] => Task[A]): Task[A] =
    ZIO.uninterruptibleMask(body.compose(new TaskPoll(_)))

  override final def canceled: Task[Unit] =
    ZIO.interrupt

  override final def onCancel[A](fa: Task[A], fin: Task[Unit]): Task[A] =
    fa.onError(cause => fin.orDie.unless(cause.isFailure))

  override final def memoize[A](fa: Task[A]): Task[Task[A]] =
    fa.memoize

  override final def racePair[A, B](fa: Task[A], fb: Task[B]): Task[Either[(Outcome[Task, Throwable, A], Fiber[Task, Throwable, B]), (Fiber[Task, Throwable, A], Outcome[Task, Throwable, B])]] =
    (fa.interruptible raceWith fb.interruptible)(
      (exit, fiber) => Exit.Success(Left((toOutcome(exit), toFiber(fiber)))),
      (exit, fiber) => Exit.Success(Right((toFiber(fiber), toOutcome(exit))))
    )

  override final def both[A, B](fa: Task[A], fb: Task[B]): Task[(A, B)] =
    fa.interruptible zipPar fb.interruptible

  override final def guarantee[A](fa: Task[A], fin: Task[Unit]): Task[A] =
    fa.ensuring(fin.orDieWith(identity))

  override final def bracket[A, B](acquire: Task[A])(use: A => Task[B])(release: A => Task[Unit]): Task[B] =
    ZIO.scoped {
      ZIO.acquireRelease(acquire)(release.andThen(_.orDie)).flatMap(use)
    }

  override val unique: Task[Unique.Token] =
    ZIO.succeed(new Unique.Token)
}

