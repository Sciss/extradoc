package de.sciss.dotter.model

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsError, JsPath, JsResult, JsString, JsSuccess, JsValue, Reads}

import scala.collection.immutable.{IndexedSeq => Vec}

object Section {
//  implicit object reads extends Reads[Section] {
//    def reads(json: JsValue): JsResult[Section] = json match {
//      case JsString(s) => ...
//      case _ => ..
//    }
//  }
}
final case class Section(page: Int, section: Int)

object Settings {
  implicit object reads extends Reads[Settings] {
    def reads(json: JsValue): JsResult[Settings] =
      JsSuccess(Settings()) // XXX TODO
  }
}
case class Settings()

object EntryKind {
  case object Object  extends EntryKind
  case object Class   extends EntryKind
  case object Trait   extends EntryKind

  implicit object reads extends Reads[EntryKind] {
    def reads(json: JsValue): JsResult[EntryKind] = json match {
      case JsString("b") => JsSuccess(Object)
      case JsString("c") => JsSuccess(Class )
      case JsString("t") => JsSuccess(Trait )
      case _ => JsError(s"Not a valid entry kind $json")
    }
  }
}
sealed trait EntryKind

object Entry {
  implicit val reads: Reads[Entry] = (
    (JsPath \ "k").read[EntryKind] and
    (JsPath \ "p").read[Int]
  )(Entry.apply _)
}
final case class Entry(kind: EntryKind, page: Int)

object Package {
  implicit val reads: Reads[Package] = (
    (JsPath \ "n" ).read[String] and
    (JsPath \ "p" ).read[Int] and
    (JsPath \ "in").read[Int] and
    (JsPath \ "e" ).readWithDefault[Vec[Entry]](Vec.empty)
  )(Package.apply _)
}
final case class Package(name: String, page: Int, in: Int, children: Vec[Entry])

object Global {
  private implicit object readsSections extends Reads[Map[Section, String]] {
    def reads(json: JsValue): JsResult[Map[Section, String]] =
      JsSuccess(Map.empty)  // XXX TODO
  }

  implicit val reads: Reads[Global] = (
    (JsPath \ "names"   ).read[Map[Section, String]] and
    (JsPath \ "packages").read[Vec[Package]] and
    (JsPath \ "settings").read[Settings]
  )(Global.apply _)
}
final case class Global(names: Map[Section, String], packages: Vec[Package], settings: Settings) {
  lazy val packageMap: Map[String, Package] = packages.iterator.map { p =>
    p.name -> p
  } .toMap
}