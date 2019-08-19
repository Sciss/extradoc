package de.sciss.dotter.model

import play.api.libs.json.{JsPath, Reads}
import play.api.libs.functional.syntax._

import scala.collection.immutable.{IndexedSeq => Vec}

object Member {
  implicit val reads: Reads[Member] = (
    (JsPath \ "name"  ).readNullable[String] and
    (JsPath \ "qName" ).readNullable[String]
  )(Member(_, _, Nil, Nil, None))
}
case class Member(name: Option[String], qName: Option[String],
                  parents: List[Int], subClasses: List[Section],
                  companion: Option[Section]
                 )

object Page {
  implicit val reads: Reads[Page] = JsPath.read[Vec[Member]].map(Page.apply)
}
case class Page(members: Vec[Member])
