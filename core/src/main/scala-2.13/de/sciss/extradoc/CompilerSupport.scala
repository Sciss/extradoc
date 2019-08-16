/*
 *  CompilerSupport.scala
 *  (ExtraDoc)
 *
 *  Copyright (c) 2019 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.extradoc

import scala.tools.nsc.reporters.ConsoleReporter

trait CompilerSupport {
  protected def summary(r: ConsoleReporter): Unit =
    r.finish()
}
