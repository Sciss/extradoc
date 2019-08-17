package de.sciss.extradoc

import java.io.File

import sbt.Keys.{Classpath, TaskStreams}
import sbt.{Configuration, File, Fork, ForkOptions, URL}
import xsbti.compile.Compilers

object RunExtraDoc {
  def apply(cache         : File,
            compilers     : Compilers,
            plugInCp      : Classpath,
            sources       : Seq[File],
            projectCp     : Classpath,
            scalacOptions : Seq[String],
            javacOptions  : Seq[String],
            apiMappings   : Map[File, URL],
            maxErrors     : Int,
            out           : File,
            config        : Configuration,
            streams       : TaskStreams,
            srcPosMap     : Seq[xsbti.Position => Option[xsbti.Position]]
           ): File = {

    import streams.log.info

    info(s"RunExtraDocn - plugin classpath = $plugInCp")
//    info(s"RunExtraDoc(sources = $sources, cp = $cp, apiMappings = $apiMappings, out = $out)")
    info(s"RunExtraDoc - invocation classpath = $projectCp")

    info(s"RunExtraDoc - output path = $out")

    val pluginCpS   = plugInCp .map(_.data.getPath).mkString(File.pathSeparator)
    val projectCpS  = projectCp.map(_.data.getPath).mkString(File.pathSeparator)
//    val cpS         = (plugInCp ++ projectCp).map(_.data.getPath).mkString(File.pathSeparator)

    val pathOutS    = out.getPath
    val sourcesS    = sources.map(_.getPath)

    val extraDocArgs = Seq(
      "-doc-format:explorer",
      "-classpath", projectCpS,
      "-d", pathOutS
    ) ++ sourcesS

    out.mkdirs()

    val forkOpt   = ForkOptions()
    val javaArgs  = Seq(
      "-cp", pluginCpS, "de.sciss.extradoc.ExtraDoc"
    )
    val exitCote  = Fork.java(forkOpt, javaArgs ++ extraDocArgs)
    info(s"exit-code: $exitCote")

//    println(callArgs.mkString("extradoc: ", " ", ""))

//    val ok = ExtraDoc.process(callArgs.toArray)
//    if (!ok) ...

    out // new File("nada")
  }
}
