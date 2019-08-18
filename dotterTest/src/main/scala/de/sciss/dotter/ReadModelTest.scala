package de.sciss.dotter

import java.io.{File, FileInputStream}

import de.sciss.dotter.model.Global
import play.api.libs.json.{JsError, JsResult, JsSuccess, Json}

object ReadModelTest {
  def main(args: Array[String]): Unit = {
    run()
  }

  def run(): Option[Global] = {
    val fIn = new File("sbtPlugin/src/sbt-test/sbt-extradoc/simple/target/scala-2.12/extradoc/global.json").getAbsolutePath
    println(fIn)
    val is = new FileInputStream(fIn)
    try {
      val json = Json.parse(is)
      val globalResult: JsResult[Global] = json.validate[Global]
//      println(s"Ok? ${globalResult.isSuccess}")
      globalResult match {
        case JsError(errors) =>
          println("Parsing global failed:")
          errors.foreach(println)
          None
        case JsSuccess(g, _) =>
          Some(g)
      }

    } finally {
      is.close()
    }
  }
}
