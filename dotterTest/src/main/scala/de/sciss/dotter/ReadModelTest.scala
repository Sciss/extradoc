package de.sciss.dotter

import java.io.{File, FileInputStream}

import de.sciss.dotter.model.{Global, GlobalRaw, PageRaw}
import play.api.libs.json.{JsError, JsResult, JsSuccess, Json}

object ReadModelTest {
  def main(args: Array[String]): Unit = {
    val t0  = System.currentTimeMillis()
    val opt = readGlobal()
    val t1  = System.currentTimeMillis()
    if (opt.isDefined) println(s"Ok. Took ${t1 - t0}ms.")
  }

  val base: File = new File("sbtPlugin/src/sbt-test/sbt-extradoc/simple/target/scala-2.12/extradoc")

  def readPage(idx: Int): Option[PageRaw] = {
    val fIn = new File(base, s"p$idx.json")
    val is  = new FileInputStream(fIn)
    try {
      val json = Json.parse(is)
      val globalResult: JsResult[PageRaw] = json.validate[PageRaw]
      globalResult match {
        case JsError(errors) =>
          println("Parsing 'page' failed:")
          errors.foreach(println)
          None
        case JsSuccess(g, _) =>
          Some(g)
      }

    } finally {
      is.close()
    }
  }

  def readGlobal(): Option[Global] = {
    val fIn = new File(base, "global.json")
    val is  = new FileInputStream(fIn)
    try {
      val json = Json.parse(is)
      val globalResult: JsResult[GlobalRaw] = json.validate[GlobalRaw]
      globalResult match {
        case JsError(errors) =>
          println("Parsing 'global' failed:")
          errors.foreach(println)
          None
        case JsSuccess(g, _) =>
          Some(g.build())
      }

    } finally {
      is.close()
    }
  }
}
