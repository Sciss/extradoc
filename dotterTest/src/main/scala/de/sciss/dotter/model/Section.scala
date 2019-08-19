package de.sciss.dotter.model

//object Section {
//  //  implicit object reads extends Reads[Section] {
//  //    def reads(json: JsValue): JsResult[Section] = json match {
//  //      case JsString(s) => ...
//  //      case _ => ..
//  //    }
//  //  }
//}
final case class Section(page: Int, section: Int)
