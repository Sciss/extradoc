package de.sciss.extradoc

import java.io.File

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

    info(s"RunExtraDoc(sources = $sources, cp = $cp, apiMappings = $apiMappings, out = $out)")

//    val home = sys.props("user.home")
//    val sv                = util.Properties.versionNumberString
//    //    val major             = if (sv.startsWith("2.12")) "2.12" else "2.13"
//
//    val pScalaLib      = s"$home/.ivy2/cache/org.scala-lang/scala-library/jars/scala-library-$sv.jar"
//    //    val pScalaCompiler = s"$home/.ivy2/cache/org.scala-lang/scala-compiler/jars/scala-compiler-$sv.jar"
//    //    val pScalaReflect  = s"$home/.ivy2/cache/org.scala-lang/scala-reflect/jars/scala-reflect-$sv.jar"
//    //    val pScalaXml      = s"$home/.ivy2/cache/org.scala-lang.modules/scala-xml_$major/bundles/scala-xml_$major-1.2.0.jar"
//    //    val paths = Seq(pScalaLib, pScalaCompiler, pScalaReflect, pScalaXml)


//    val paths = Seq[String](pScalaLib)

    val classPathS  = cp.map(_.data.getPath).mkString(File.pathSeparator)
    val pathOutS    = out.getPath
    val sourcesS    = sources.map(_.getPath)

    val callArgs = Seq(
      "-doc-format:explorer",
      "-classpath", classPathS,
      "-d", pathOutS
    ) ++ sourcesS

    out.mkdirs()

    println(callArgs.mkString("extradoc: ", " ", ""))

    val ok = ExtraDoc.process(callArgs.toArray)
//    if (!ok) ...

    out // new File("nada")
  }
}
