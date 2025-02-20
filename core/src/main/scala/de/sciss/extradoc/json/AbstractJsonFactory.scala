/*
 *  AbstractJsonFactory.scala
 *  (ExtraDoc)
 *
 *  This class originates from the original extradoc project and
 *  was published under the BSD 2-clause license.
 *  New adopted work published under LGPL.
 *
 *  This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.extradoc.json

import java.io.{InputStream, File => JFile}

import scala.collection.mutable
import scala.tools.nsc.doc
import scala.tools.nsc.io.{Directory, Streamable}
import scala.tools.nsc.reporters.Reporter

abstract class AbstractJsonFactory(val universe: doc.Universe, val reporter: Reporter) {
  self =>

  val doInline              = true
  val typeEntitiesAsHtml    = false
  val compactFlags          = false
  val removeSimpleBodyDocs  = false
  val simpleParamsAsString  = false

  def prepareModel(universe: doc.Universe): mutable.Map[Int, JObject] = {
    println("Building JSON model")

    val (allModels, allModelsReverse) = buildModels(universe)

    if (simpleParamsAsString) inlineSimpleParams(allModels, allModelsReverse)

    while (allModels.size > allModelsReverse.size) {
      compact(allModels, allModelsReverse)
    }

    if (doInline) inline(allModels)

    if (allModels.keys.max + 1 != allModels.size) {
      renumber(allModels)
      if (allModels.keys.max + 1 != allModels.size) {
        throw new RuntimeException(s"Renumbering failed: Max key ${allModels.keys.max} for size ${allModels.size}")
      }
    }

    val (verOk, _) = verify(allModels)
    if (!verOk) throw new RuntimeException("Model verification failed")

    allModels
  }

  /** Builds a map that indexes.
    * Returns the mapping in both directions.
    */
  def buildModels(universe: doc.Universe): (mutable.Map[Int, JObject], mutable.Map[JObject, Int]) = {
    val globalEntityOrdinals  = mutable.Map.empty[EntityHash[AnyRef /*Entity*/], Int]
    val allModels             = mutable.Map.empty[Int, JObject]
    val allModelsReverse      = mutable.Map.empty[JObject, Int]
    val builder: JsonBuilder  = new JsonBuilder {
      val typeEntitiesAsHtml  : Boolean = self.typeEntitiesAsHtml
      val compactFlags        : Boolean = self.compactFlags
      val removeSimpleBodyDocs: Boolean = self.removeSimpleBodyDocs

      val reporter: Reporter  = self.reporter

      def global[E <: AnyRef](e: E)(f: E => JObject)(implicit view: EntityView[E]): Link = {
//        require (!e.isInstanceOf[LinkTo] || e.isInstanceOf[Tooltip], e)
        globalEntityOrdinals.get(EntityHash(e)) match {
          case Some(ord) => Link(ord)
          case None =>
            val ord = globalEntityOrdinals.size
            globalEntityOrdinals += EntityHash(e) -> ord
            val o = f(e)
            if (ord + 1 == globalEntityOrdinals.size) {
              // No dependent entities were built by f, so there cannot be any references to ord yet
              allModels += ord -> o
              allModelsReverse.get(o) match {
                case Some(oldOrd) =>
                  globalEntityOrdinals.remove(EntityHash(e))
                  Link(oldOrd)
                case None =>
                  allModels        += ord -> o
                  allModelsReverse += o -> ord
                  Link(ord)
              }
            } else {
              allModels        += ord -> o
              allModelsReverse += o -> ord
              Link(ord)
            }
        }
      }
    }
    builder.global(universe.rootPackage)(builder.createEntity)
    println(s"Built ${allModels.size} global objects (${allModelsReverse.size} unique)")
    (allModels, allModelsReverse)
  }

  def verify(m: mutable.Map[Int, JObject]): (Boolean, Int) = {
    println("Verifying JSON model")
    var ok = true
    var count = 0
    val verified = mutable.Set.empty[Int]

    def f(ord: Int, j: JBase): Unit = {
      if (ord == -1 || !verified.contains(ord)) {
        if (ord != -1) verified += ord
        for (ch <- j.links) {
          count += 1
          m.get(ch.target) match {
            case Some(j) =>
              f(ch.target, j)
            case None =>
              println(s"Model verification error: Link target ${ch.target} not found")
              ok = false
          }
        }
        for (ch <- j.children) f(-1, ch)
      }
    }

    for ((ord, j) <- m) f(ord, j)
    println(s"Verified $count links and ${m.size} global objects")
    (ok, count)
  }

  def compact(allModels: mutable.Map[Int, JObject], allModelsReverse: mutable.Map[JObject, Int]): Unit = {
    val duplicates = allModels.keys.toSet -- allModelsReverse.values
    val repl = duplicates.map { i => (Link(i), Link(allModelsReverse(allModels(i)))) }.toMap
    println(s"Replacing duplicates: $repl")
    allModels --= duplicates

    def replaceIn(j: JBase): Unit = {
      j.replaceLinks(repl)
      j.children.foreach(replaceIn)
    }

    allModels.values.foreach(replaceIn)

    allModelsReverse.clear()
    for ((ord, j) <- allModels) allModelsReverse += j -> ord
    println(s"Compacted to ${allModels.size} global objects (${allModelsReverse.size} unique)")
  }

  def inlineSimpleParams(allModels: mutable.Map[Int, JObject], allModelsReverse: mutable.Map[JObject, Int]): Unit = {
    def simple(l: Int): Option[String] = {
      val j = allModels(l)
      if ((j.keys.toSet -- Set("name", "qName")).isEmpty)
        nameFor(j).filter(_.length < 7)
      else None
    }

    allModels.values.foreach { j =>
      j.getOrElse("typeParams", JArray.Empty).transform {
        case (_, l@Link(t)) => simple(t).getOrElse(l)
        case (_, o) => o
      }

//      j.getOrElse("valueParams", JArray.Empty).values.foreach {
//        case a: JArray =>
//          a.transform {
//            case (_, l @ Link(t)) => simple(t).getOrElse(l)
//            case (_, o) => o
//          }
//        case _ =>
//      }
    }
    allModelsReverse.clear()
    for ((ord, j) <- allModels) allModelsReverse += j -> ord
    println(s"Compacted to ${allModels.size} global objects (${allModelsReverse.size} unique)")
  }

  def renumber(allModels: mutable.Map[Int, JObject]): Unit = {
    println("Renumbering objects")
    val repl = allModels.keys.toSeq.sorted.zipWithIndex.toMap
    val linkRepl = repl.map { case (k, v) => (Link(k), Link(v)) }

    def replaceIn(j: JBase): Unit = {
      j.replaceLinks(linkRepl)
      j.children.foreach(replaceIn)
    }

    allModels.values.foreach(replaceIn)

    val newM = allModels.toSeq.map { case (ord, j) => (repl(ord), j) }
    allModels.clear()
    allModels ++= newM
  }

  def findGlobal(allModels: mutable.Map[Int, JObject]): mutable.Set[Int] = {
    val global = mutable.Set.empty[Int]

    def f(ord: Int): Unit = {
      if (!global.contains(ord)) {
        val j = allModels(ord)
        if (j.has("isTemplate") || j.getOrElse("is", "").contains('M')) {
          global += ord
          j.getOrElse("members", JArray.Empty).values.foreach {
            case Link(target) => f(target)
            case _ =>
          }
        }
      }
    }

    f(0)
    global
  }

  def inline(allModels: mutable.Map[Int, JObject]): Unit = {
    println("Finding objects to inline")
    val keep = findGlobal(allModels)
    allModels.values.foreach {
      _.foreachRec {
        case j: JObject =>
          j.getOrElse("_links", JArray.Empty).values.foreach {
            keep += _.asInstanceOf[Link].target
          }
        case _ =>
      }
    }
    println(s"Protecting ${keep.size} objects")
    val counts = mutable.Map.empty[Int, Int]
    allModels.values.foreach {
      _.foreachRec {
        _.links.foreach { l =>
          counts += l.target -> (counts.getOrElse(l.target, 0) + 1)
        }
      }
    }
    val toInline = counts.filter { case (_, c) => c <= 1 } .keys.toSet -- keep
    if (toInline.nonEmpty) {
      println(s"Inlining/eliminating ${toInline.size} objects")
      val repl = toInline.map { i => (Link(i), allModels(i)) }.toMap
      allModels --= toInline

      def replaceIn(j: JBase): Unit = {
        j.replaceLinks(repl)
        j.children.foreach(replaceIn)
      }

      allModels.values.foreach(replaceIn)
    }
  }

  lazy val siteRoot: JFile = new JFile(universe.settings.outdir.value)

  def copyResource(resPath: String, subPath: String): Unit = {
    val bytes = new Streamable.Bytes {
      val path = s"$resPath/$subPath"
      val inputStream: InputStream = getClass.getResourceAsStream(path)
      assert(inputStream != null, path)
    }.toByteArray
    val dest = Directory(siteRoot) / subPath
    dest.parent.createDirectory()
    val out = dest.toFile.bufferedOutput()
    try out.write(bytes, 0, bytes.length)
    finally out.close()
  }

  def qNameToName(qName: String): String = {
    val (s1, s2) = (qName.lastIndexOf('#'), qName.lastIndexOf('.'))
    val sep = if (s1 > s2) s1 else s2
    qName.substring(sep + 1)
  }

  def nameFor(j: JObject): Option[String] =
    j.get("name").orElse {
      j.get("qName").map { q => qNameToName(q.asInstanceOf[String]) }
    }.asInstanceOf[Option[String]]
}
