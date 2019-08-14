lazy val commonSettings = Seq(
  name               := "Extradoc",
  scalaVersion       := "2.12.9",
  crossScalaVersions := Seq("2.13.0", "2.12.9"),
  scalacOptions     ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xlint", "-Xsource:2.13")
)

lazy val root = project.in(file("."))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang"          %  "scala-compiler" % scalaVersion.value,
      "org.scala-lang.modules"  %% "scala-xml"      % "1.2.0"
    )
  )
