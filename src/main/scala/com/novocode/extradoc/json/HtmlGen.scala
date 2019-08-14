package com.novocode.extradoc.json

import com.novocode.extradoc.XmlSupport._

import scala.tools.nsc.doc.html
import scala.tools.nsc.doc.model.TemplateEntity

abstract class HtmlGen extends html.HtmlPage {
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
