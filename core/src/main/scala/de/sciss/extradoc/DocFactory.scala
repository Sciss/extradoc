/*
 *  JsonWriter.scala
 *  (ExtraDoc)
 *
 *  This class originates from the original extradoc project, and
 *  was based based on NSC -- new Scala compiler -- Copyright 2007-2010 LAMP/EPFL.
 *  New adopted work published under LGPL.
 *
 *  This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.extradoc

import scala.tools.nsc.doc
import scala.tools.nsc.doc.{model => m}
import scala.tools.nsc.reporters.Reporter

/** A documentation processor controls the process of generating Scala documentation, which is as follows.
  *
  * * A simplified compiler instance (with only the front-end phases enabled) is created, and additional
  *   ''sourceless'' comments are registered.
  * * Documentable files are compiled, thereby filling the compiler's symbol table.
  * * A documentation model is extracted from the post-compilation symbol table.
  * * A generator is used to transform the model into the correct final format (HTML).
  *
  * A processor contains a single compiler instantiated from the processor's `settings`. Each call to `document`
  * uses the same compiler instance with the same symbol table. In particular, this implies that the scaladoc site
  * obtained from a call to `run` will contain documentation about files compiled during previous calls to the same
  * processor's `run` method.
  *
  * @param reporter The reporter to which both documentation and compilation errors will be reported.
  * @param settings The settings to be used by the documenter and compiler for generating documentation.
  * 
  * @author Gilles Dubochet
  */
class DocFactory(val reporter: Reporter, val settings: doc.Settings) { processor =>

  /** The unique compiler instance used by this processor and constructed from its `settings`. */
  object compiler extends doc.ScaladocGlobal(settings, reporter) {

    lazy val addSourceless: Unit = {
      // XXX TODO -- what are sourceless comments ?

//      val sless = new SourcelessComments { val global = compiler }
//      docComments ++= sless.comments
    }
  }

  /** Creates a scaladoc site for all symbols defined in this call's `files`, as well as those defined in `files` of
    * previous calls to the same processor.
    * @param files The list of paths (relative to the compiler's source path, or absolute) of files to document.
    */
  def document(files: List[String]): Unit = {
    new compiler.Run().compile(files)
    compiler.addSourceless
    if (!reporter.hasErrors) {
      val modelFactory =
        new m.ModelFactory(compiler, settings)
          with scala.tools.nsc.doc.model.ModelFactoryImplicitSupport
          with scala.tools.nsc.doc.model.ModelFactoryTypeSupport
          with scala.tools.nsc.doc.model.diagram.DiagramFactory
          with scala.tools.nsc.doc.model.CommentFactory
          with scala.tools.nsc.doc.model.TreeFactory
          with scala.tools.nsc.doc.model.MemberLookup

      val docModel = modelFactory.makeModel.getOrElse(throw new IllegalStateException("docModel is empty")) // HH
      println(s"model contains ${modelFactory.templatesCount} documentable templates")
      settings.docformat.value match {
        case "html"       => new doc.html.HtmlFactory  (docModel, reporter)                   .generate()
        case "json"       => new json.JsonFactory      (docModel, reporter)                   .generate()
        case "json-multi" => new json.JsonMultiFactory (docModel, reporter, explorer = false) .generate()
        case "explorer"   => new json.JsonMultiFactory (docModel, reporter, explorer = true ) .generate()
	    }
    }
  }
}
