package examplezio

import cats.effect.Deferred
import zio.{Promise, Task, ZIO}

final class TaskDeferred[A](promise: Promise[Throwable, A]) extends Deferred[Task, A] {

  override val get: Task[A] =
    promise.await

  override def complete(a: A): Task[Boolean] =
    promise.succeed(a)

  override val tryGet: Task[Option[A]] =
    promise.isDone.flatMap {
      case true  => get.asSome
      case false => ZIO.none
    }
}

