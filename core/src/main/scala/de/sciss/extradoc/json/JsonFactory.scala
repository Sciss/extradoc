/*
 *  JsonFactory.scala
 *  (ExtraDoc)
 *
 *  This class originates from the original extradoc project and
 *  was published under the BSD 2-clause license.
 *  New adopted work published under LGPL.
 *
 *  This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
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
