lazy val baseName   = "ExtraDoc"
lazy val baseNameL  = baseName.toLowerCase()

lazy val commonSettings = Seq(
  name               := baseName,
  organization       := "de.sciss",
  scalaVersion       := "2.12.9",
  description        := "A Scala API doc generator with JSON output",
  licenses           := Seq("Two-clause BSD-style license" -> url("https://github.com/Sciss/extradoc/blob/master/LICENSE")),
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
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang"          %  "scala-compiler" % scalaVersion.value,
      "org.scala-lang.modules"  %% "scala-xml"      % "1.2.0"
    )
  )
