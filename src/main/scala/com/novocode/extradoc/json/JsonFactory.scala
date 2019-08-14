package com.novocode.extradoc.json

import scala.tools.nsc.doc._
import scala.tools.nsc.reporters.Reporter

class JsonFactory(universe: Universe, reporter: Reporter)
  extends AbstractJsonFactory(universe, reporter) {

  def generate(): Unit = {
    val allModels = prepareModel(universe)
    println("Writing scaladoc.json")
    JsonWriter(siteRoot, "scaladoc.json") createArray { w =>
      for ((_ /*ord*/ , m) <- allModels.toSeq.sortBy(_._1)) {
        w.write(m, {
          _.target
        })
      }
    }
  }
}
