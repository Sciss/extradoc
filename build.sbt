lazy val commonSettings = Seq(
  scalaVersion := "2.12.9"
)

lazy val root = project.in(file("."))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" %  "scala-compiler" % scalaVersion.value,
    )
  )
