/*
 *  CompilerSupport.scala
 *  (ExtraDoc)
 *
 *  This software is published under the BSD 2-clause license
 */

package de.sciss.extradoc

import scala.tools.nsc.reporters.ConsoleReporter

trait CompilerSupport {
  protected def summary(r: ConsoleReporter): Unit =
    r.finish()
}
