name := "amqp-scala-client"

organization := "com.depop"

version := "3.0.0"

scalaVersion := "2.13.13"

crossScalaVersions := Seq("2.12.18", "2.13.13")

scalacOptions  ++= Seq("-feature", "-language:postfixOps")

scalacOptions  ++= Seq("-unchecked", "-deprecation")

//useGpg := true

//credentials += Credentials(Path.userHome / ".ivy2" / ".spacelift-credentials")

//usePgpKeyHex("561D5885877866DF")

publishMavenStyle := true

publishTo := 
{
  val nexus = "https://depop.jfrog.io/artifactory/depop-scala-local/"
  if (isSnapshot.value)
    Some("snapshots" at nexus)
  else
    Some("releases"  at nexus)
}

publishArtifact in Test := false

publishConfiguration := publishConfiguration.value.withOverwrite(true)

//pomIncludeRepository := { _ => false }


val pekkoVersion   = "1.0.2"
libraryDependencies ++=
    Seq(
        "com.rabbitmq"           % "amqp-client"              % "4.8.0",
        "org.apache.pekko"       %% "pekko-actor"             % pekkoVersion % "provided",
        "org.apache.pekko"       %% "pekko-slf4j"             % pekkoVersion % "test",
        "org.apache.pekko"       %% "pekko-testkit"           % pekkoVersion % "test",
        "org.scala-lang.modules" %% "scala-collection-compat" % "2.10.0",
        "org.scalatest"          %% "scalatest"               % "3.2.18" % "test",
        "ch.qos.logback"         %  "logback-classic"         % "1.4.7" % "test"
    )
