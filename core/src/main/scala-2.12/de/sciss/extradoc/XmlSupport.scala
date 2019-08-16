/*
 *  XmlSupport.scala
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

package de.sciss.extradoc

import scala.xml.{NodeSeq, Xhtml}

object XmlSupport {
  type Elems = NodeSeq

  def NoElems: Elems = NodeSeq.Empty

  def XmlMkString(ns: Elems): String = {
    val s = Xhtml.toXhtml(ns).trim
    if (s.startsWith("<p>") && s.indexOf("</p>") == s.length - 4) {
      s.substring(3, s.length - 4).trim
    } else {
      s
    }
  }
}
