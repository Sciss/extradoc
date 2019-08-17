package de.sciss.extradoc

import sbt.Keys._
import sbt.ScopeFilter.{ConfigurationFilter, ProjectFilter}
import sbt._
import sbt.plugins.JvmPlugin

object ExtraDocPlugin extends AutoPlugin {

  override def trigger  : PluginTrigger = allRequirements
  override def requires : Plugins       = JvmPlugin

  object autoImport {
    val extradoc                    = taskKey   [File]                ("Creates extradoc for all aggregates.")
    val extradocAllSources          = taskKey   [Seq[Seq[File]]]      ("All sources.")
    val extradocAllClasspaths       = taskKey   [Seq[Classpath]]      ("All classpaths.")
    val extradocAllAPIMappings      = taskKey   [Seq[Map[File, URL]]] ("All API mappings.")
    val extradocScopeFilter         = settingKey[ScopeFilter]         ("Controls sources to be included in extradoc.")
    val extradocProjectFilter       = settingKey[ProjectFilter]       ("Controls projects to be included in extradoc.")
    val extradocConfigurationFilter = settingKey[ConfigurationFilter] ("Controls configurations to be included in extradoc.")

    lazy val Extradoc = config("extradoc").extend(Compile)
  }

  import autoImport.{Extradoc => Cfg, _}

  override lazy val projectSettings: Seq[Setting[_]] =
    inConfig(Cfg)(Defaults.configSettings) ++ baseExtradocTasks(Compile) ++ Seq(
      extradoc in Compile := (extradoc in Cfg).value
    )

  def baseExtradocTasks(sc: Configuration): Seq[Setting[_]] = Seq(
    target in extradoc := crossTarget.value / "extradoc",
    extradocAllSources in extradoc := allScalaSources.value,
    extradoc := RunExtraDoc(
      streams.value.cacheDirectory,
      (compilers in extradoc).value,
      (sources        in extradoc).value,
      (fullClasspath  in extradoc).value,
      (scalacOptions  in extradoc).value,
      (javacOptions   in extradoc).value,
      (apiMappings    in extradoc).value,
      (maxErrors      in extradoc).value,
      (target         in extradoc).value,
      configuration.value,
      streams.value,
      (sourcePositionMappers in extradoc).value
    ),
    compilers     in extradoc := (compilers in sc).value,
    sources       in extradoc := (extradocAllSources in extradoc).value.flatten.sortBy { _.getAbsolutePath },
    scalacOptions in extradoc := (scalacOptions in (sc, doc)).value,
    javacOptions  in extradoc := (javacOptions  in (sc, doc)).value,
    fullClasspath in extradoc := (extradocAllClasspaths in extradoc).value.flatten.distinct.sortBy { _.data.getName },
    extradocAllClasspaths in extradoc := allClasspathsTask.value,
    apiMappings in extradoc := {
      val all     = (extradocAllAPIMappings in extradoc).value
      val allList = all.map(_.toList)
      allList.flatten.distinct.toMap
    },
    extradocAllAPIMappings in extradoc := allAPIMappingsTask.value,
    maxErrors in extradoc := (maxErrors in (sc, doc)).value,
    extradocScopeFilter   in extradoc := ScopeFilter(
      (extradocProjectFilter        in extradoc).value,
      (extradocConfigurationFilter  in extradoc).value
    ),
    extradocProjectFilter in extradoc := inAnyProject,
    extradocConfigurationFilter in extradoc := inConfigurations(Compile)
  )

//  def ExtraDocTask(c: Configuration, sc: Configuration): Seq[sbt.Def.Setting[_]] =
//    inConfig(c)(Defaults.configSettings ++ baseExtradocTasks(sc)) ++ Seq(
//      extradoc in sc ++= Seq((doc in c).value)
//    )

  lazy val allClasspathsTask = Def.taskDyn {
    val f = (extradocScopeFilter in extradoc).value
    dependencyClasspath.all(f)
  }

  lazy val allAPIMappingsTask = Def.taskDyn {
    val f = (extradocScopeFilter in extradoc).value
    (apiMappings in (Compile, doc)).all(f)
  }

  lazy val allScalaSources = Def.taskDyn {
    val f = (extradocScopeFilter in extradoc).value
    sources.all(f)
  }

  override lazy val buildSettings : Seq[Setting[_]] = Seq()
  override lazy val globalSettings: Seq[Setting[_]] = Seq()
}
