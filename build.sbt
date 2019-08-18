lazy val baseName   = "ExtraDoc"
lazy val baseNameL  = baseName.toLowerCase()

lazy val commonSettings = Seq(
  version            := "0.1.0-SNAPSHOT",
  organization       := "de.sciss",
  scalaVersion       := "2.12.9",
  licenses           := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt")),
  homepage           := Some(url(s"https://github.com/Sciss/$baseNameL")),
  crossScalaVersions := Seq("2.13.0", "2.12.9"),
  scalacOptions     ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xlint", "-Xsource:2.13")
)

lazy val publishSettings = Seq(
  pomExtra := {
    val n = baseNameL
      <scm>
        <url>https://github.com/Sciss/{n}</url>
        <connection>scm:git:git@github.com:Sciss/{n}.git</connection>
      </scm>
      <developers>
        <developer>
          <id>szeiger</id>
          <name>Stefan Zeiger</name>
          <timezone>+1</timezone>
          <email>szeiger [at] novocode.com</email>
        </developer>
        <developer>
          <id>sciss</id>
          <name>Hanns Holger Rutz</name>
          <url>http://www.sciss.de</url>
        </developer>
      </developers>
  }
)

lazy val root = project.in(file("."))
  .aggregate(core, plugin)
  .settings(commonSettings)
  .settings(
    name := baseName,
  )

lazy val core = project.withId(s"$baseNameL-core").in(file("core"))
  .settings(commonSettings)
  .settings(
    name        := s"$baseName-Core",
    description := "A Scala API doc generator with JSON output",
    libraryDependencies ++= Seq(
      "org.scala-lang"          %  "scala-compiler" % scalaVersion.value,
      "org.scala-lang.modules"  %% "scala-xml"      % "1.2.0"
    )
  )

lazy val plugin = project.withId(s"sbt-$baseNameL").in(file("sbtPlugin"))
  .enablePlugins(ScriptedPlugin)
//  .dependsOn(core)
  .settings(commonSettings)
  .settings(
    name      := s"sbt-$baseName",  // note: overrides auto-derived name from `withId`!
    sbtPlugin := true,
    // cf. https://www.scala-sbt.org/1.x/docs/Testing-sbt-plugins.html
    scriptedLaunchOpts ++= Seq("-Xmx1024M", s"-Dplugin.version=${version.value}"),
    scriptedBufferLog := false
  )

lazy val dotterTest = project.in(file("dotterTest"))
  .dependsOn(core)
  .settings(commonSettings)
  .settings(
    name := s"$baseName-dotter-test",
    libraryDependencies ++= Seq(
      "de.sciss"          %% "dotterweide-ui"     % "0.2.3",
      "de.sciss"          %% "dotterweide-scala"  % "0.2.3",
      "de.sciss"          %% "scalacollider"      % "1.28.4",
      "com.typesafe.play" %% "play-json"          % "2.7.4",
    )
  )