package com.novocode.extradoc

import scala.tools.nsc._

class ExtraDocSettings(error: String => Unit) extends doc.Settings(error) {

  // XXX TODO:

//  override def ChoiceSetting(name: String, descr: String, choices: List[String], default: String) = {
//    if(name == "-doc-format") super.ChoiceSetting(name, descr, List("html", "json", "json-multi", "explorer"), default)
//    else super.ChoiceSetting(name, descr, choices, default)
//  }
}
