package de.sciss.extradoc

import sbt.Keys.{Classpath, TaskStreams}
import sbt.{Configuration, File, URL}
import xsbti.compile.Compilers

object RunExtraDoc {
  // This is straight out of docTaskSettings in Defaults.scala.
  def apply(cache       : File,
            compilers   : Compilers,
            sources     : Seq[File],
            cp          : Classpath,
            scalaOptions: Seq[String],
            javaOptions : Seq[String],
            apiMappings : Map[File, URL],
            maxErrors   : Int,
            out         : File,
            config      : Configuration,
            streams     : TaskStreams,
            srcPosMap   : Seq[xsbti.Position => Option[xsbti.Position]]
           ): File = {

    import streams.log.info

    info(s"RunExtraDoc(cache = $cache, compilers = $compilers, sources = $sources, cp = $cp, apiMappings = $apiMappings)")

    new File("nada")
  }
}
