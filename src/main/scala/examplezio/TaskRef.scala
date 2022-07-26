package examplezio

import java.util.concurrent.atomic.AtomicBoolean

import cats.data.State
import cats.effect.Ref
import zio.{Exit, Ref => ZioRef, Task, ZIO}


final class TaskRef[A](ref: ZioRef[A]) extends Ref[Task, A] {

  override def access: Task[(A, A => Task[Boolean])] =
    get.map { current =>
      val called                   = new AtomicBoolean(false)
      def setter(a: A): Task[Boolean] =
        ZIO.suspendSucceed {
          if (called.getAndSet(true)) {
            Exit.Success(false)
          } else {
            ref.modify { updated =>
              if (current == updated) (true, a)
              else (false, updated)
            }
          }
        }

      (current, setter)
    }

  override def tryUpdate(f: A => A): Task[Boolean] =
    update(f).as(true)

  override def tryModify[B](f: A => (A, B)): Task[Option[B]] =
    modify(f).asSome

  override def update(f: A => A): Task[Unit] =
    ref.update(f)

  override def modify[B](f: A => (A, B)): Task[B] =
    ref.modify(f(_).swap)

  override def tryModifyState[B](state: State[A, B]): Task[Option[B]] =
    modifyState(state).asSome

  override def modifyState[B](state: State[A, B]): Task[B] =
    modify(state.run(_).value)

  override def set(a: A): Task[Unit] =
    ref.set(a)

  override def get: Task[A] =
    ref.get
}