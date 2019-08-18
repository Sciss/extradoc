package de.sciss.extradoc

import sbt.Keys._
import sbt.ScopeFilter.{ConfigurationFilter, ProjectFilter}
import sbt._
import sbt.plugins.JvmPlugin

object ExtraDocPlugin extends AutoPlugin {
  private val extraDocVersion = "0.1.0-SNAPSHOT"

  override def trigger  : PluginTrigger = allRequirements
  override def requires : Plugins       = JvmPlugin

  // https://github.com/sbt/sbt-web/blob/a04454faf8ea3b66092afd40da645dae9b8c211e/src/main/scala/com/typesafe/sbt/web/SbtWeb.scala#L145

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

  override def projectConfigurations: Seq[Configuration] =
    super.projectConfigurations :+ Cfg

  override lazy val projectSettings: Seq[Setting[_]] =
    inConfig(Cfg)(Defaults.configSettings ++ baseExtradocTasks(Compile)) ++ Seq(
      extradoc in Compile := (extradoc in Cfg).value,
      libraryDependencies ++= Seq(
        "de.sciss" %% "extradoc-core" % extraDocVersion % Cfg,
      )
    )

  def baseExtradocTasks(sc: Configuration): Seq[Setting[_]] = Seq(
    target in extradoc := crossTarget.value / "extradoc",
    extradocAllSources in extradoc := allScalaSources.value,
    extradoc := RunExtraDoc(
      cache         = streams.value.cacheDirectory,
      compilers     = (compilers in extradoc).value,
      plugInCp      = (managedClasspath in Cfg).value,
      sources       = (sources        in extradoc).value,
      projectCp     = (fullClasspath  in extradoc).value,
      scalacOptions = (scalacOptions  in extradoc).value,
      javacOptions  = (javacOptions   in extradoc).value,
      apiMappings   = (apiMappings    in extradoc).value,
      maxErrors     = (maxErrors      in extradoc).value,
      out           = (target         in extradoc).value,
      config        = configuration.value,
      streams       = streams.value,
      srcPosMap     = (sourcePositionMappers in extradoc).value
    ),
    compilers     in extradoc := (compilers in sc).value,
    sources       in extradoc := (extradocAllSources in extradoc).value.flatten.sortBy(_.getAbsolutePath),
    scalacOptions in extradoc := (scalacOptions in (sc, doc)).value,
    javacOptions  in extradoc := (javacOptions  in (sc, doc)).value,
    fullClasspath in extradoc := (extradocAllClasspaths in extradoc).value.flatten.distinct.sortBy(_.data.getName),
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

//  override lazy val buildSettings : Seq[Setting[_]] = Seq()
//  override lazy val globalSettings: Seq[Setting[_]] = Seq()
}
