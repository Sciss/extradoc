/*
 *  ExtraDocSettings.scala
 *  (ExtraDoc)
 *
 *  This software is published under the BSD 2-clause license
 */

package de.sciss.extradoc

import scala.tools.nsc.doc

/** Overrides `-doc-format` to add support for json, json-multi, and explorer. */
class ExtraDocSettings(error: String => Unit) extends doc.Settings(error) {

  override def ChoiceSetting(name: String, helpArg: String, descr: String, choices: List[String],
                             default: String, choicesHelp: List[String]): ChoiceSetting = {
    val choices1 = if (name != "-doc-format") choices else List("html", "json", "json-multi", "explorer")
    super.ChoiceSetting(name, helpArg = helpArg, descr = descr, choices = choices1,
      default = default, choicesHelp = choicesHelp)
  }
}
