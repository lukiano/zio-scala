package examplezio

import scala.concurrent.duration.{FiniteDuration, MILLISECONDS, NANOSECONDS}

import cats.effect.GenTemporal
import zio.Clock.{currentTime, nanoTime}
import zio.{Duration, Task, ZIO}

class TaskTemporal extends TaskConcurrent with GenTemporal[Task, Throwable] {

  override final def sleep(time: FiniteDuration): Task[Unit] =
    ZIO.sleep(Duration.fromScala(time))

  override final val monotonic: Task[FiniteDuration] =
    nanoTime.map(FiniteDuration(_, NANOSECONDS))

  override final val realTime: Task[FiniteDuration] =
    currentTime(MILLISECONDS).map(FiniteDuration(_, MILLISECONDS))
}
