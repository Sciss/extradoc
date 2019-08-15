/*
 *  JsonFactory.scala
 *  (ExtraDoc)
 *
 *  This software is published under the BSD 2-clause license
 */

package de.sciss.extradoc.json

import scala.tools.nsc.doc
import scala.tools.nsc.reporters.Reporter

class JsonFactory(universe: doc.Universe, reporter: Reporter)
  extends AbstractJsonFactory(universe, reporter) {

  def generate(): Unit = {
    val allModels = prepareModel(universe)
    println("Writing scaladoc.json")
    JsonWriter(siteRoot, "scaladoc.json") createArray { w =>
      for ((_ /*ord*/ , m) <- allModels.toSeq.sortBy(_._1)) {
        w.write(m, {
          _.target
        })
      }
    }
  }
}
