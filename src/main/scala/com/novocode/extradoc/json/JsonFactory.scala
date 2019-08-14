package com.novocode.extradoc.json

import scala.tools.nsc.doc._

class JsonFactory(universe: Universe) extends AbstractJsonFactory(universe) {

  def generate(universe: Universe): Unit = {
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
