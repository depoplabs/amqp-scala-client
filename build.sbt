name := "amqp-scala-client"

organization := "com.depop"

version := "2.2.0"

scalaVersion := "2.13.10"

crossScalaVersions := Seq("2.12.18", "2.13.10")

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


val akkaVersion   = "2.6.20"
libraryDependencies ++=
    Seq(
        "com.rabbitmq"           % "amqp-client"              % "4.0.2",
        "com.typesafe.akka"      %% "akka-actor"              % akkaVersion % "provided",
        "com.typesafe.akka"      %% "akka-slf4j"              % akkaVersion % "test",
        "com.typesafe.akka"      %% "akka-testkit"            % akkaVersion  % "test",
        "org.scala-lang.modules" %% "scala-collection-compat" % "2.10.0",
        "org.scalatest"          %% "scalatest"               % "3.2.16" % "test",
        "ch.qos.logback"         %  "logback-classic"         % "1.4.7" % "test",
        "junit"           	     % "junit"                    % "4.12" % "test"
    )
