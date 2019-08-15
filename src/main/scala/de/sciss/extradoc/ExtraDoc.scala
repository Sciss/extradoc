/* This source file is based on NSC -- new Scala compiler -- Copyright 2007-2010 LAMP/EPFL */

package de.sciss.extradoc

import java.io.File

import scala.collection.{mutable, Seq => CSeq}
import scala.reflect.internal.util.FakePos
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.{CompilerCommand, FatalError, Properties}

/** The main class for scaladoc, a front-end for the Scala compiler 
 *  that generates documentation from source files.
 */
object ExtraDoc extends CompilerSupport {

  val versionMsg: String =
    s"ExtraDoc - based on ScalaDoc ${Properties.versionString} -- ${Properties.copyrightString}"

  var reporter: ConsoleReporter = _
  
  def scalaFiles(base: File, name: String): CSeq[String] = {
    val b = mutable.Buffer.empty[String]
    def collect(f: File, s: String): Unit = {
      //println(s"Scanning ${f.getPath}")
      val fn = f.getName
      if(f.isDirectory) {
        if(fn == "." || !fn.startsWith(".")) {
          val files = f.listFiles
          if(files ne null) files foreach { ch => collect(ch, null) }
        }
      } else if(s ne null) b += s
      else if(fn endsWith ".scala") b += f.getPath
    }
    collect(base, name)
    b
  }

  def error(msg: String): Unit = {
    reporter.error(FakePos("scalac"), s"$msg\n  scalac -help  gives more information")
  }

  def process(args: Array[String]): Unit = {
    
    val docSettings = new ExtraDocSettings(error)
    docSettings.YpresentationAnyThread.value = true // because we use nsc.interactive.Global
    
    reporter = new ConsoleReporter(docSettings)
    
    val command =
      new CompilerCommand(args.toList, docSettings)
      
    if (!reporter.hasErrors) { // No need to continue if reading the command generated errors
      
      if (docSettings.version.value)
        reporter.info(null, versionMsg, force = true)
      else if (docSettings.help.value) {
        reporter.info(null, command.usageMsg, force = true)
      }
      else if (docSettings.Xhelp.value) 
        reporter.info(null, command.xusageMsg, force = true)
      else if (docSettings.Yhelp.value) 
        reporter.info(null, command.yusageMsg, force = true)
      else if (docSettings.showPlugins.value)
        reporter.warning(null, "Plugins are not available when using Scaladoc")
      else if (docSettings.showPhases.value)
        reporter.warning(null, "Phases are restricted when using Scaladoc")
      else try {

        // HH: dropped

//        if (docSettings.target.value == "msil")
//          msilLibPath foreach (x => docSettings.assemrefs.value += s"${pathSeparator}x"))
        
        val sourcePath = docSettings.sourcepath.value
        val expFiles = command.files.flatMap { fname: String =>
          val f = if(sourcePath == "") new File(fname) else new File(sourcePath, fname)
          scalaFiles(f, fname)
        }
        val docProcessor = new DocFactory(reporter, docSettings)
        //println(s"Found sources: ${expFiles.mkString(",")}")
        docProcessor.document(expFiles)
        
      }
      catch {
        case ex @ FatalError(msg) =>
          if (docSettings.debug.value) ex.printStackTrace()
          reporter.error(null, s"fatal error: $msg")
      }
      finally {
        summary(reporter)
      }
    }
  }


  def main(args: Array[String]): Unit = {
    process(args)
    sys.exit(if (reporter.hasErrors) 1 else 0)
  }
  
}
