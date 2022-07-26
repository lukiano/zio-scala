package examplezio

import cats.effect.Poll
import zio.{Task, ZIO}

final class TaskPoll(restore: ZIO.InterruptibilityRestorer) extends Poll[Task] {
  override def apply[A](fa: Task[A]): Task[A] = restore(fa)
}
