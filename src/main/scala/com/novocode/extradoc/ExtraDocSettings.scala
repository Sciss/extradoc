package com.novocode.extradoc

import scala.tools.nsc._

class ExtraDocSettings(error: String => Unit) extends {

  // XXX TODO:

//  override def ChoiceSetting(name: String, descr: String, choices: List[String], default: String) = {
//    if(name == "-doc-format") super.ChoiceSetting(name, descr, List("html", "json", "json-multi", "explorer"), default)
//    else super.ChoiceSetting(name, descr, choices, default)
//  }

  override val docformat: ChoiceSetting = ChoiceSetting (
    "-doc-format",
    "format",
    "Selects in which format documentation is rendered.",
    List("html", "json", "json-multi", "explorer"),
    "html"
  )

//  def enhancedDocFormat: Setting = ChoiceSetting (
//    "-doc-format",
//    "format",
//    "Selects in which format documentation is rendered.",
//    List("html", "json", "json-multi", "explorer"),
//    "html"
//  )

//  // otherwise we run into NPE due to initialization order
//  override val isScaladocSpecific: String => Boolean = scaladocSpecific map (_.name)
//
//  // XXX TODO: how to specify the return type? bloody projection here
//  override def scaladocSpecific =
//    super.scaladocSpecific - docformat + enhancedDocFormat
} with doc.Settings(error)
