libraryDependencies += "dev.zio" %% "zio" % "2.0.0"

libraryDependencies += "dev.zio" %% "zio-aws-netty" % "5.17.234.1"
libraryDependencies += "dev.zio" %% "zio-aws-dynamodb" % "5.17.234.1"
libraryDependencies += "dev.zio" %% "zio-aws-dynamodbstreams" % "5.17.234.1"

libraryDependencies += "dev.zio" %% "zio-aws-sqs" % "5.17.234.1"

libraryDependencies += "dev.zio" %% "zio-logging-slf4j" % "2.0.1"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.11"

libraryDependencies += "org.http4s" %% "http4s-blaze-server" % "0.23.12"
libraryDependencies += "org.http4s" %% "http4s-dsl" % "0.23.12"

libraryDependencies += "dev.zio"    %% "zio-interop-cats" % "3.3.0"

libraryDependencies += "dev.zio"    %% "zio-json" % "0.3.0-RC10"
//libraryDependencies += "dev.zio"    %% "zio-json-interop-http4s" % "0.3.0-RC10"

libraryDependencies += "dev.zio"    %% "zio-managed" % "2.0.0"
// scalaVersion := "2.13.8"
scalaVersion := "3.1.3"

// scalacOptions ++= Seq("-Xlint:_")