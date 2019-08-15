/*
 *  XmlSupport.scala
 *  (ExtraDoc)
 *
 *  This software is published under the BSD 2-clause license
 */

package de.sciss.extradoc

import scala.xml.{NodeSeq, Text, Xhtml}

object XmlSupport {
  type Elems = NodeSeq

  def NoElems: Elems = NodeSeq.Empty

  def XmlMkString(ns: Elems): String = ns match {
    case Text("no summary matey") => ""
    case _ =>
      val s = Xhtml.toXhtml(ns).trim
      if (s startsWith "<p>") {
        if (s.indexOf("</p>") == s.length - 4)
          s.substring(3, s.length - 4).trim
        else s
      } else s
  }
}
