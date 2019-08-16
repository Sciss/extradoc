/*
 *  JsonPage.scala
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

import de.sciss.extradoc.XmlSupport._

import scala.tools.nsc.doc.html
import scala.tools.nsc.doc.model.TemplateEntity

abstract class JsonPage extends html.HtmlPage {
  def path: List[String] = Nil

  protected def title: String = ""

  protected def headers : Elems = NoElems // NodeSeq.Empty
  def           body    : Elems = NoElems // NodeSeq.Empty

  def ref(e: TemplateEntity): String

  // HH: in XmlSupport

//  def mkString(ns: Elems): String = ns match {
//    case Text("no summary matey") => ""
//    case _ =>
//      val s = Xhtml.toXhtml(ns).trim
//      if (s startsWith "<p>") {
//        if (s.indexOf("</p>") == s.length - 4)
//          s.substring(3, s.length - 4).trim
//        else s
//      } else s
//  }

  override def relativeLinkTo(destClass: TemplateEntity): String = s"#${ref(destClass)}"
}
