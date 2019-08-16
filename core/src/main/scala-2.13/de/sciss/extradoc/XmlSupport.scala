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

import scala.tools.nsc.doc.html.HtmlTags
import scala.xml.Text

object XmlSupport {
  type Elems = HtmlTags.Elems

  def NoElems: Elems = Nil

  def XmlMkString(ns: Elems): String = ns match {
    case Text("no summary matey") => ""
    case _ =>
//    require(ns.sizeIs == 1, ns.toString)
//    ns.head.toText
//      val s = Xhtml.toXhtml(ns).trim
      val s = ns.mkString // XXX TODO -- is this correct?
      if (s startsWith "<p>") {
        if (s.indexOf("</p>") == s.length - 4)
          s.substring(3, s.length - 4).trim
        else s
      } else s
  }
}
