/*
 *  JsonMultiFactory.scala
 *  (ExtraDoc)
 *
 *  This software is published under the BSD 2-clause license
 */

package de.sciss.extradoc.json

import scala.collection.mutable
import scala.tools.nsc.doc
import scala.tools.nsc.reporters.Reporter

class JsonMultiFactory(universe: doc.Universe, reporter: Reporter, explorer: Boolean)
  extends AbstractJsonFactory(universe, reporter) {

  // Global inlining is harmful for multi-page output because it increases
  // the size of extra objects which are included in many pages
  override val doInline             = false

  override val typeEntitiesAsHtml   = true
  override val compactFlags         = true
  override val removeSimpleBodyDocs = true
  override val simpleParamsAsString = true

  case class Page(no: Int, main: Int) {
    val objects   : mutable.Set   [Int] = mutable.Set   .empty
    val renumbered: mutable.Buffer[Int] = mutable.Buffer.empty

    lazy val renumberedMap: Map[Int, Int] =
      renumbered.zipWithIndex.toMap // don't access until "renumbered" is stable
  }

  def generate(): Unit = {
    if (explorer) {
      val p = "/de/sciss/extradoc/explorer"
      copyResource(p, "index.html")
      copyResource(p, "css/extradoc.css")
      copyResource(p, "js/cache.js")
      copyResource(p, "js/diagrams.js")
      copyResource(p, "js/extradoc.js")
      copyResource(p, "js/explorer.js")
      copyResource(p, "js/jit.js")
      copyResource(p, "js/jquery-1.4.2.min.js")
      copyResource(p, "js/jquery-ui-1.8.5.custom.min.js")
      copyResource(p, "js/jquery.history.js")
      copyResource(p, "images/ajax-loader-white.gif")
      copyResource(p, "images/search.png")
      copyResource(p, "images/tooltip-triangle.png")
    }

    val allModels = prepareModel(universe)

    aliasComments(allModels)

    val pages = findGlobal(allModels).toSeq.sorted
      .zipWithIndex.map { case (ord, idx) => (ord, Page(idx, ord)) }.toMap

    def findPage(ord: Int, j: JBase): Option[Page] = j match {
      case j: JObject =>
        val isPage    = pages.contains(ord)
        val isPackage = j.has("isPackage") || j.getOrElse("is", "").contains('p')
        def isObject  = j.has("isObject" ) || j.getOrElse("is", "").contains('b')

        lazy val parent = j.get("inTemplate").collect { case Link(t) => allModels(t) }

        def parentIsTemplate: Boolean = parent.collect { case par: JObject =>
          par.has("isTemplate") || par.getOrElse("is", "").contains('M')
        } .getOrElse(false)

        def isInParent = parent.collect { case par: JObject =>
          /*val valuesAndMethods = par("members", JArray.Empty).values filter {
            case Link(t) =>
              val tt = allModels(t)
              val ttIs = tt("is", "")
              tt !! "isVal" || tt !! "isMethod" || ttIs.contains("") || ttIs.contains("")
            case _ => false
          }*/
          par.getOrElse("members", JArray.Empty).values contains Link(ord)
        } .getOrElse(false)

        val companionPage = j.get("companion") map { case l: Link => pages.get(l.target) }
        // Don't map external packages to their parents
        if (ord >= 0 && isPackage && !isPage) None
        // Map auto-generated case class companion objects without a separate page to their classes
        else if (isObject && companionPage.isDefined && !isPage) companionPage.get
        // Treat members which were remapped but not compacted away as extras
        else if ((isDef(j) || isVal(j) || isAliasType(j)) && !isInParent && parentIsTemplate) None
        else j.get("inTemplate") match {
          case Some(Link(target)) =>
            pages.get(target).orElse(allModels.get(target).flatMap(ch => findPage(target, ch)))
          case Some(j: JObject) => findPage(-1, j)
          case _ /*None*/ => None
        }
      case _ => None
    }

    val extra = mutable.Set.empty[Int]
    allModels.foreach { case (ord, j) =>
      pages.get(ord).orElse(findPage(ord, j)).map(_.objects).getOrElse(extra) += ord
    }

    println(s"Mapping ${extra.size} extra objects to all pages that need them")
    var extraTotal = 0

    def mapExtras(p: Page, j: JBase): Unit = {
      j.foreachRec {
        _.links foreach { l =>
          if (extra.contains(l.target)) {
            if (!p.objects.contains(l.target)) {
              extraTotal += 1
              p.objects += l.target
              mapExtras(p, allModels(l.target))
            }
          }
        }
      }
    }

    pages.values.foreach { p =>
      p.objects.map(allModels).foreach { j => mapExtras(p, j) }
    }
    println(s"Total number of extra objects on all pages: $extraTotal")

    val keepHtmlLinks = mutable.Set.empty[Int]
    allModels.values.foreach {
      _.foreachRec {
        case j: JObject =>
          j.getOrElse("_links", JArray.Empty).values.foreach {
            keepHtmlLinks += _.asInstanceOf[Link].target
          }
        case _ =>
      }
    }

    println("Removing extra objects which are duplicated further up the linarization")
    var removedExtras = 0
    pages.values.foreach { p =>
      allModels(p.main).getOrElse("linearization", JArray.Empty).values.foreach {
        case Link(l) if l != p.main =>
          pages.get(l).foreach { p2 =>
            val s = p.objects.size
            p.objects --= (p2.objects intersect extra)
            removedExtras += s - p.objects.size
          }
        case _ =>
      }
    }
    println(s"Removed $removedExtras extra objects")

    println("Inlining objects on all pages")
    var totalInlined = 0
    val counts = mutable.Map.empty[Int, Int]
    allModels.values.foreach {
      _.foreachRec {
        _.links.foreach { l =>
          val j = allModels(l.target)
          if ((extra contains l.target) && !(keepHtmlLinks contains l.target) && !(isDef(j) || isVal(j)))
            counts += l.target -> (counts.getOrElse(l.target, 0) + 1)
        }
      }
    }
    for (p <- pages.values) {
      val toInline = counts.filter { case (_, c) => c <= 1 }.keys.toSet
      if (toInline.nonEmpty) {
        totalInlined += toInline.size
        val repl = toInline.map { i => (Link(i), allModels(i)) }.toMap
        p.objects --= toInline

        def replaceIn(j: JBase): Unit = {
          j.replaceLinks(repl)
          j.children.foreach(replaceIn)
        }

        allModels.filter { case (ord, _ /*j*/ ) => p.objects contains ord }.values.foreach(replaceIn)
      }
    }
    println(s"Inlined $totalInlined objects")

    println("Removing qualified names of defs, vals and alias types")
    allModels.values foreach {
      case j: JObject =>
        if (isDef(j) || isVal(j) || isAliasType(j)) {
          if (j.get("name").isEmpty && j.get("qName").isDefined) {
            j += "name" -> qNameToName(j.getOrElse("qName", ""))
          }
          j -= "qName"
        }
      case _ =>
    }

    val remappedIDs = mutable.Map.empty[Link, (Int, Int)]
    for (p <- pages.values) {
      p.renumbered += p.main
      remappedIDs += Link(p.main) -> ((p.no, 0))
      for (ord <- p.objects if ord != p.main) {
        remappedIDs += Link(ord) -> ((p.no, p.renumbered.size))
        p.renumbered += ord
      }
    }
    println(s"Writing p0.json to p${pages.size - 1}.json")
    val globalNames = mutable.Map.empty[String, String]

    def convertLink(p: Page)(l: Link): Any = {
      val localIdx        : Option[Int] = p.renumberedMap.get(l.target)
      val localOrParentIdx: Option[Any] = localIdx.orElse {
        val lin = allModels(p.main).getOrElse("linearization", JArray.Empty).values
        val parentIdx = lin.toSeq.view flatMap {
          case Link(t2) =>
            pages.get(t2).flatMap { p2 =>
              p2.renumberedMap.get(l.target).map {
                (p2.no, _)
              }
            }
          case _ => None
        }
        parentIdx.map { case (page, idx) => JArray(Seq(page, idx)) }.headOption
      }
      localOrParentIdx.getOrElse {
        val (page, idx) = remappedIDs(l)
        if (idx == 0 || page != p.no) {
          val jo = allModels(l.target)
          jo.get("name").orElse {
            jo.get("qName").map { q => qNameToName(q.asInstanceOf[String]) }
          } .foreach { case n: String =>
            globalNames += s"$page,$idx" -> n
          }
        }
        JArray(Seq(page, idx))
      }
    }

    for (p <- pages.values) {
      JsonWriter(siteRoot, s"p${p.no}.json").createArray { w =>
        for (ord <- p.renumbered) w.write(allModels(ord), convertLink(p))
      }
    }

    val pageObjects = pages.values map { p => (p.no, (allModels(p.main), p.main)) }
    val allPackages = pageObjects.filter { case (_, (j, _)) =>
      j.get("isPackage").getOrElse(false) == true || j.getOrElse("is", "").contains('p')
    }.toMap
    val linearPackages = allPackages.toSeq sortBy { case (_, (j, _)) => j.get("qName").get.asInstanceOf[String] }
    println("Writing global.json")
    val processedTemplates = mutable.Set.empty[Int]

    def processTemplates(jOrd: Int, j: JObject, jo: JObject): Unit = {
      j.get("members").foreach { case a: JArray =>
        val children = a.values.collect { case l: Link =>
          (l.target, allModels(l.target))
        } .filter { case (_, j: JObject) =>
          val up = j.get("inTemplate") .collect { case l: Link => l.target } .getOrElse(-1)
          val isTemplate = j.has("isTemplate") || j.getOrElse("is", "").contains('M')
          isTemplate && (up == -1 || up == jOrd)
        }
        val tlChildren = children.map { case (ord, j: JObject) =>
          val is = j.getOrElse("is", "")
          val kind =
            if      (j.has("isClass"  ) || is.contains('c')) 'c'
            else if (j.has("isTrait"  ) || is.contains('t')) 't'
            else if (j.has("isObject" ) || is.contains('b')) 'b'
            else '_'

          (ord, j, kind)
        }.filter { case (ord, _, kind) => pages.contains(ord) && (kind != '_') }.toSeq
        val sortedChildren = tlChildren.sortBy { case (_, j: JObject, kind) =>
          (j.getOrElse("qName", "").toLowerCase, kind)
        }
        jo.addOpt("e", JArray(sortedChildren.map { case (ord, chj, kind) =>
          val ch = new JObject
          ch += "p" -> pages(ord).no
          ch += "k" -> kind.toString
          if (!processedTemplates.contains(ord)) {
            processedTemplates += ord
            processTemplates(ord, chj, ch)
          }
          ch
        }))
      }
    }

    JsonWriter(siteRoot, "global.json") createObject { w =>
      w.write("names", JObject(globalNames), {
        _.target
      })
      w.write("packages", JArray(linearPackages map { case (no, (j: JObject, ord)) =>
        val jo = new JObject
        jo += "p" -> no
        jo += "n" -> j.get("qName").get.asInstanceOf[String]
        j.get("inTemplate") foreach { case l: Link => jo += "in" -> pages(l.target).no }
        processTemplates(ord, j, jo)
        jo
      }), {
        _.target
      })
      val settings = new JObject
      if (!universe.settings.doctitle     .isDefault) settings += "doctitle"      -> universe.settings.doctitle     .value
      if (!universe.settings.docversion   .isDefault) settings += "docversion"    -> universe.settings.docversion   .value
      if (!universe.settings.docsourceurl .isDefault) settings += "docsourceurl"  -> universe.settings.docsourceurl .value
      w.write("settings", settings, _.target)
    }
  }

  def aliasComments(allModels: mutable.Map[Int, JObject]): Unit = {
    println("Aliasing repeated comments")

    def forMembers(j: JObject)(f: (JObject, Link) => Unit): Unit = {
      j.getOrElse("members", JArray.Empty).values.foreach {
        case l: Link => f(allModels(l.target), l)
        case _ =>
      }
    }

    def findComment(c: JObject, templates: List[Link], notIn: Link): Option[Link] = templates match {
      case t :: ts => findComment(c, ts, notIn) orElse {
        var found: Option[Link] = None
        forMembers(allModels(t.target)) { (member, memberLink) =>
          if (memberLink != notIn) {
            member.get("comment").foreach {
              case mc: JObject =>
                if (found.isEmpty && mc == c) found = Some(memberLink)
              case _ =>
            }
          }
        }
        found
      }
      case Nil => None
    }

    var count = 0
    allModels.foreach {
      case (idx, j: JObject) =>
        val lin = (Link(idx) :: j.getOrElse("linearization", JArray.Empty).values.toList).asInstanceOf[List[Link]]
        forMembers(j) { (member, memberLink) =>
          var found: Option[Link] = None
          member.get("comment").foreach {
            case c: JObject =>
              findComment(c, lin, memberLink) foreach { l => found = Some(l) }
            case _ =>
          }
          found.foreach { l =>
            member += "commentIn" -> l
            member -= "comment"
            count += 1
          }
        }
      case _ =>
    }

    @scala.annotation.tailrec
    def chaseCommentIn(j: JObject, jLink: Option[Link]): Option[Link] = {
      j.get("commentIn") match {
        case Some(l: Link) => chaseCommentIn(allModels(l.target), Some(l))
        case _ => jLink
      }
    }

    allModels.foreach { case (_, j) =>
      forMembers(j) { (member, _) =>
        chaseCommentIn(member, None).foreach { l =>
          member += "commentIn" -> l
        }
      }
    }
    println(s"Aliased $count comments")
  }

  def isDef(j: JObject): Boolean = j.has("isDef") || j.getOrElse("is", "").contains('d')

  def isVal(j: JObject): Boolean = j.has("isVal") || j.getOrElse("is", "").contains('v') ||
    j.has("isLazyVal") || j.getOrElse("is", "").contains('l')

  def isAliasType(j: JObject): Boolean = j.has("isAliasType") || j.getOrElse("is", "").contains('a')
}
