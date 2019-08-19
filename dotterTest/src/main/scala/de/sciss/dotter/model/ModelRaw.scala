package de.sciss.dotter.model

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsError, JsObject, JsPath, JsResult, JsString, JsSuccess, JsValue, Reads}

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.util.{Failure, Success, Try}

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

object EntryRaw {
  implicit val reads: Reads[EntryRaw] = (
    (JsPath \ "k").read[EntryKind] and
    (JsPath \ "p").read[Int]
  )(EntryRaw.apply _)
}
final case class EntryRaw(kind: EntryKind, page: Int)

object PackageRaw {
  implicit val reads: Reads[PackageRaw] = (
    (JsPath \ "n" ).read[String] and
    (JsPath \ "p" ).read[Int] and
    (JsPath \ "in").read[Int] and
    (JsPath \ "e" ).readWithDefault[Vec[EntryRaw]](Vec.empty)
  )(PackageRaw.apply _)
}
final case class PackageRaw(name: String, page: Int, in: Int, entries: Vec[EntryRaw])

final case class SectionRaw(page: Int, section: Int)

object GlobalRaw {
  private implicit object readsSections extends Reads[Map[SectionRaw, String]] {
    def reads(json: JsValue): JsResult[Map[SectionRaw, String]] = json match {
      case JsObject(map) =>
        val b   = Map.newBuilder[SectionRaw, String]
        val it  = map.iterator
        while (it.hasNext) {
          val eRes: JsResult[(SectionRaw, String)] = it.next() match {
            case (k, JsString(v)) =>
              val i = k.indexOf(",")
              if (i < 0) {
                val msg = s"global.names - key is not a section pair: '$k'"
                // println(s"COMMA EXPECTED: $msg")
                JsError(msg)
              }
              else {
                // XXX TODO damn, this is awful, is there no simpler way?
                (for {
                  page <- Try(k.substring(0, i  ).toInt)
                  sec  <- Try(k.substring(i + 1 ).toInt)
                } yield SectionRaw(page, sec)) match {
                  case Success(sec) => JsSuccess(sec -> v)
                  case Failure(ex)  =>
                    val msg = s"global.names - key is not a section pair: '$k'"
                    // println(s"PARSE SECTION: $msg")
                    JsError(msg)
                }
              }

            case (_, v) =>
              val msg = s"global.names - value is not a JSON string: $v"
              // println(s"STRING EXPECTED: $msg")
              JsError(msg)
          }
          eRes match {
            case JsSuccess(pair, _) => b += pair
            case f: JsError         => return f
          }
        }
        JsSuccess(b.result())

      case _ => JsError("global.names - not a JSON object")
    }
  }

  implicit val reads: Reads[GlobalRaw] = (
    (JsPath \ "names"   ).read[Map[SectionRaw, String]] and
    (JsPath \ "packages").read[Vec[PackageRaw]] and
    (JsPath \ "settings").read[Settings]
  )(GlobalRaw.apply _)
}
final case class GlobalRaw(names: Map[SectionRaw, String], packages: Vec[PackageRaw], settings: Settings) {
//  lazy val packageMap: Map[String, PackageRaw] = packages.iterator.map { p =>
//    p.name -> p
//  } .toMap

  def build(): Global = {
    val pkg = packages.map { pRaw =>
      val entries = pRaw.entries.map { eRaw =>
        val es    = SectionRaw(page = eRaw.page, section = 0)
        val eName = names.getOrElse(es, throw new IllegalStateException(s"Package ${pRaw.name}, entry ${eRaw.page}"))
        Entry(eName, kind = eRaw.kind, page = eRaw.page)
      }
      Package(pRaw.name, page = pRaw.page, in = pRaw.in, entries = entries)
    }
    Global(pkg)
  }
}

/////

object MemberRaw {
  implicit val reads: Reads[MemberRaw] = (
    (JsPath \ "name"  ).readNullable[String] and
    (JsPath \ "qName" ).readNullable[String]
  )(MemberRaw(_, _, Nil, Nil, None))
}
case class MemberRaw(name: Option[String], qName: Option[String],
                     parents: List[Int], subClasses: List[SectionRaw],
                     companion: Option[SectionRaw]
                    )

object PageRaw {
  implicit val reads: Reads[PageRaw] = JsPath.read[Vec[MemberRaw]].map(PageRaw.apply)
}
case class PageRaw(members: Vec[MemberRaw])
