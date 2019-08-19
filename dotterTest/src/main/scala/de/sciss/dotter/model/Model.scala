package de.sciss.dotter.model

import scala.collection.immutable.{IndexedSeq => Vec}

final case class Entry(name: String, kind: EntryKind, page: Int)

final case class Package(name: String, page: Int, in: Int, entries: Vec[Entry]) {
  val entryMap: Map[String, Entry] = entries.iterator.map(e => e.name -> e).toMap
}

final case class Global(packages: Vec[Package]) {
  val packageMap: Map[String, Package] = packages.iterator.map(p => p.name -> p).toMap
}
