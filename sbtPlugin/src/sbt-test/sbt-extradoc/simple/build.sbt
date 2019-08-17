lazy val baseName        = "ScalaCollider"
lazy val baseNameL       = baseName.toLowerCase

lazy val BASE_VERSION    = "1.28.4"
lazy val PROJECT_VERSION = BASE_VERSION

lazy val deps = new {
  val audioFile          = "1.5.3"
//  val scalaColliderSwing = "1.41.0"
  val scalaColliderUGens = "1.19.5"
  val scalaOsc           = "1.2.0"
}

lazy val lScalaCollider       = RootProject(uri(s"git://github.com/Sciss/$baseName.git#v${BASE_VERSION}"))
lazy val lAudioFile           = RootProject(uri(s"git://github.com/Sciss/AudioFile.git#v${deps.audioFile}"))
//lazy val lScalaColliderSwing  = RootProject(uri(s"git://github.com/Sciss/ScalaColliderSwing.git#v${deps.scalaColliderSwing}"))
lazy val lScalaColliderUGens  = RootProject(uri(s"git://github.com/Sciss/ScalaColliderUGens.git#v${deps.scalaColliderUGens}"))
lazy val lScalaOsc            = RootProject(uri(s"git://github.com/Sciss/ScalaOSC.git#v${deps.scalaOsc}"))

lazy val lList = Seq(lAudioFile, lScalaCollider, lScalaColliderUGens, /*lScalaColliderSwing,*/ lScalaOsc)
//lazy val lList = Seq(lAudioFile)

scalaVersion in ThisBuild := "2.12.9"

lazy val extradocSettings = Seq(
//  mappings in packageDoc in Compile := (mappings in (ExtraDoc, packageDoc)).value,
  scalacOptions in (Compile, doc) ++= Seq(
    "-skip-packages", Seq(
      "de.sciss.osc.impl",
      "de.sciss.synth.impl",
      "snippets"
    ).mkString(":"),
    "-doc-title", s"${baseName} ${PROJECT_VERSION} API"
  )
)

val site = project.withId(s"$baseNameL-site").in(file("."))
  .enablePlugins(ExtraDocPlugin) // ParadoxSitePlugin, GhpagesPlugin, ScalaUnidocPlugin, SiteScaladocPlugin)
  .settings(extradocSettings)
  .settings(
    name                 := baseName, // IMPORTANT: `name` is used by GhpagesPlugin, must base base, not s"$baseName-Site"!
    version              := PROJECT_VERSION,
//    siteSubdirName in SiteScaladoc    := "latest/api",
//    git.remoteRepo       := s"git@github.com:Sciss/$baseName.git",
//    git.gitCurrentBranch := "master",
//    paradoxTheme         := Some(builtinParadoxTheme("generic")),
//    paradoxProperties in Paradox ++= Map(
//      "snippet.base_dir"        -> s"${baseDirectory.value}/snippets/src/main",
//      "swingversion"            -> deps.scalaColliderSwing,
//      "extref.swingdl.base_url" -> s"https://github.com/Sciss/ScalaColliderSwing/releases/download/v${deps.scalaColliderSwing}/ScalaColliderSwing_${deps.scalaColliderSwing}%s"
//    )
  )
  .aggregate(lList: _*)
