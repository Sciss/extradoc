/*
 *  JsonBuilder.scala
 *  (ExtraDoc)
 *
 *  This software is published under the BSD 2-clause license
 */

package de.sciss.extradoc.json

import de.sciss.extradoc.XmlSupport._

import scala.collection.{mutable, Map => CMap, Seq => CSeq}
import scala.language.implicitConversions
import scala.reflect.ClassManifest
import scala.reflect.internal.Reporter
import scala.tools.nsc.doc.base.{LinkToMember, LinkToTpl, comment => cm}
import scala.tools.nsc.doc.model.MemberTemplateEntity
import scala.tools.nsc.doc.{model => m}
import scala.xml.{Elem, Node, NodeBuffer, NodeSeq, Text, Xhtml}

abstract class JsonBuilder { builder =>

  val typeEntitiesAsHtml  : Boolean
  val compactFlags        : Boolean
  val removeSimpleBodyDocs: Boolean

  val mergeInheritedMembers = true

  def reporter: Reporter

  def global[E <: AnyRef](e: E)(f: E => JObject)(implicit view: EntityView[E]): Link

  def as[T](o: AnyRef)(f: T => Unit)(implicit m: ClassManifest[T]): Unit =
    if (m.erasure.isInstance(o)) f(o.asInstanceOf[T])

  def asOrElse[T](o: AnyRef)(f: T => Unit)(e: => Unit)(implicit m: ClassManifest[T]): Unit =
    if (m.erasure.isInstance(o)) f(o.asInstanceOf[T]) else e

  class CollectingJsonPage extends JsonPage {
    val links = new JArray

    def ref(e: m.TemplateEntity): String = {
      links += global(e)(createEntity)
      "#"
    }

    protected def docletReporter: Reporter = builder.reporter
  }

  def createJson(f: JsonPage => Elems): JObject = {
    val gen = new CollectingJsonPage
    val ns  = f(gen)
    val j   = new JObject
    j.addOpt("_links", gen.links)
    j += "_html" -> XmlMkString(ns) // gen.mkString(ns)
    j
  }

  implicit def nodeSeqFromList(in: CSeq[Node]): NodeSeq = NodeSeq.fromSeq(in)

  def createBody  (b: cm.Body   ): JObject = createJson(_.bodyToHtml  (b))
  def createBlock (b: cm.Block  ): JObject = createJson(_.blockToHtml (b))
  def createInline(i: cm.Inline ): JObject = createJson(_.inlineToHtml(i))

  /** Returns a tuple consisting of (main-doc, vParams-doc, tParams-doc) */
  def createComment(c: cm.Comment): (JObject, CMap[String, JObject], CMap[String, JObject]) = {
    val j             = new JObject
    val bodyDoc       = createBody(c.body)
    val bodyIsEmpty   = bodyDoc  .get("_html").getOrElse("") == ""
    val shortDoc      = createInline(c.short)
    val shortIsEmpty  = shortDoc .get("_html").getOrElse("") == ""

    if (!shortIsEmpty) j += "short" -> shortDoc

    val skipBody = bodyIsEmpty || (removeSimpleBodyDocs && {
      val bodyHtml      = bodyDoc .get("_html").getOrElse("")
      val shortHtml     = shortDoc.get("_html").getOrElse("")
      val bodyIsSimple  = bodyHtml == shortHtml

      bodyDoc.get("_links").isEmpty && shortDoc.get("_links").isEmpty && bodyIsSimple
    })
    if (!skipBody) {
      j += "body" -> bodyDoc
    }

    j.addOpt("authors", JArray(c.authors.map(createBody)))
    j.addOpt("see"    , JArray(c.see    .map(createBody)))

    c.result.foreach(b => j.addOpt("result", createBody(b)))

    j.addOpt("throws", JObject(c.throws.map { case (k, v) => k -> createBody(v) }))

    val vParams = c.valueParams .map { case (k, v) => k -> createBody(v) }
    val tParams = c.typeParams  .map { case (k, v) => k -> createBody(v) }

    c.version.foreach(b => j.addOpt("version", createBody(b)))
    c.since  .foreach(b => j.addOpt("since"  , createBody(b)))

    j.addOpt("todo"   , JArray(c.todo.map(createBody)))

    c.deprecated .foreach(b => j.addOpt("deprecated", createBody(b)))

    j.addOpt("note"   , JArray(c.note    .map(createBody)))
    j.addOpt("example", JArray(c.example .map(createBody)))

    (j, vParams, tParams)
  }

  def createTypeEntity(t: m.TypeEntity): JObject = {
    val j = new JObject
    if (typeEntitiesAsHtml) {
      val b     = new NodeBuffer
      val links = mutable.Buffer.empty[Link]
      val name  = t.name
      var pos   = 0
      t.refEntity.foreach { case (start, (ref0, len)) =>
        def perform[E <: AnyRef : EntityView](ref: E): Unit = {
          if (pos < start) b += Text(name.substring(pos, start))
          links += global(ref)(createEntity _)
          b += Elem(null, "a", xml.Null, xml.TopScope, minimizeEmpty = false,
            child = Text(name.substring(start, start + len)))
          pos = start + len
        }
        ref0 match {
          case LinkToTpl    (tpl: m.Entity)   => perform(tpl)
          case LinkToMember (m: m.Entity, _ /*t*/)  =>
//            println(s"LinkToMember($m, $t)")
            perform(m)
          case _                              => perform(ref0)
        }
      }
      if (pos < name.length) b += Text(name.substring(pos))
      j += "_xname" -> Xhtml.toXhtml(b)
      j.addOpt("_refs", JArray(links))
    } else {
      j += "name" -> t.name
      j.addOpt("refEntity", JArray(t.refEntity.map {
        case (start, (ref, len)) =>
          val vv = new JObject
          vv += "s" -> start
          vv += "l" -> len
          vv += "e" -> global(ref)(createEntity _)
          vv
      }))
    }
    j
  }

  def createEntity[E <: AnyRef](e: E)(implicit view: EntityView[E]): JObject = {
    val j = new JObject
    
//    j += "inTemplate" -> global(e.inTemplate)(createEntity _)
    e match { // HH
      case e1: m.Entity => j += "inTemplate" -> global(e1.inTemplate)(createEntity _)
      case _            =>  // ignore
    }

    // "toRoot" is own ID plus recursively toRoot of inTemplate
    //j += "toRoot" -> JArray(e.toRoot.map(e => global(e)(createEntity _)))
    val qName = view.qNameOption(e).orNull
    var name  = view.nameOption (e).orNull
    if (qName ne null) {
      val sep1  = qName.lastIndexOf('#')
      val sep2  = qName.lastIndexOf('.')
      val sep   = if (sep1 > sep2) sep1 else sep2
      if (sep > 0 && qName.substring(sep + 1) == name || sep == -1 && qName == name) name = null
      j += "qName" -> qName
    }
    if (name ne null) j += "name" -> name

    var isPackageOrClassOrTraitOrObject = false
    var isClassOrTrait                  = false

    as[m.TemplateEntity](e) { t =>
      isPackageOrClassOrTraitOrObject = t.isPackage || t.isClass || t.isTrait || t.isObject || t.isRootPackage
      isClassOrTrait = t.isClass || t.isTrait
      if (compactFlags) {
        if (t.isPackage     ) set(j, 'p')
        if (t.isRootPackage ) set(j, 'r')
        if (t.isTrait       ) set(j, 't')
        if (t.isClass       ) set(j, 'c')
        if (t.isObject      ) set(j, 'b')
        if (t.isDocTemplate ) set(j, 'D')
      } else {
        if (t.isPackage     ) j += "isPackage"      -> true
        if (t.isRootPackage ) j += "isRootPackage"  -> true
        if (t.isTrait       ) j += "isTrait"        -> true
        if (t.isClass       ) j += "isClass"        -> true
        if (t.isObject      ) j += "isObject"       -> true
        if (t.isDocTemplate ) j += "isDocTemplate"  -> true
      }
    }
    //as[NoDocTemplate](e) { t => j += "isNoDocTemplate" -> true }
    var vParams: CMap[String, JObject] = CMap.empty
    var tParams: CMap[String, JObject] = CMap.empty

    as[m.MemberEntity](e) { me =>
      me.comment.foreach { c =>
        val (comment, v, t) = createComment(c)
        j += "comment" -> comment
        vParams = v
        tParams = t
      }
      val vis = me.visibility
      if (compactFlags) {
        if (vis.isProtected) set(j, 'o')
        if (vis.isPublic   ) set(j, 'u')
      } else {
        if (vis.isProtected) j += "isProtected"  -> true
        if (vis.isPublic   ) j += "isPublic"     -> true
      }

      // XXX TODO: owner is `Option[Entity] in 2.12, and `Entity` in 2.13

//      as[PrivateInTemplate  ](m.visibility) { p =>
//        p.owner.foreach { owner =>
//          j += "visibleIn" -> global(owner)(createEntity _)
//        }
//      }
//
//      as[ProtectedInTemplate](m.visibility) { p =>
//        p.owner.foreach { owner =>
//          j += "visibleIn" -> global(owner)(createEntity _)
//        }
//      }

      if (mergeInheritedMembers) {
        // XXX TODO --- is this equivalent? or do we need to further check mte.parentTypes?
//        if (!me.inheritedFrom.isEmpty)
        asOrElse[MemberTemplateEntity](me) { _ =>
          /* Remove "inheritedFrom", replace "inTemplate" with first from
             "inDefinitionTemplates" and replace "qName" with "definitionName"
             to make this inherited member definition identical to the original
             one so it can be compacted away and remapped to the correct
             page. */
          val originalOwnerLink = global(me.inDefinitionTemplates.head)(createEntity _)
          j += "inTemplate" -> originalOwnerLink
          j += "qName"      -> me.definitionName
          /* If the member is visible in its inTemplate, it must have been
             inDefinitionTemplates.first at the point of its definition, so we
             rewrite it that way. */
          if (j.getOrElse("visibleIn", Link(-1)).target != -1) {
            j += "visibleIn" -> originalOwnerLink
          }
          // inDefinitionTemplate.head has already become inTemplate
          j.addOpt("alsoIn", JArray(me.inDefinitionTemplates.tail.map(e => global(e)(createEntity))))
        } /*else*/ {
          // filter out inTemplate
          j.addOpt("alsoIn",
            JArray(me.inDefinitionTemplates.filter(_ != me.inTemplate).map(e => global(e)(createEntity)))
          )
        }
        // definitionName is always identical to qName, so leave it out
      } else {
        // HH
        as[MemberTemplateEntity](me) { mte =>
          j.addOpt("inheritedFrom", JArray(mte.parentTypes.map { case (_ /*eTmp*/, eTpe) =>
            global(eTpe)(createEntity _)
          }))
        }
//        j.addOpt("inheritedFrom", JArray(me.inheritedFrom.map(e => global(e)(createEntity))))
        j.addOpt("definitionName", me.definitionName)
      }

      me.flags.map(createBlock).foreach { fj =>
        fj.get("_html") match {
          case Some("<p>sealed</p>") =>
            if (compactFlags) set(j, 's') else j += "isSealed"    -> true
          case Some("<p>abstract</p>") =>
            if (compactFlags) set(j, 'B') else j += "isAbstract"  -> true
          case Some("<p>final</p>") =>
            if (compactFlags) set(j, 'f') else j += "isFinal"     -> true
          case _ =>
        }
      }

      me.deprecation.foreach { d => j += "deprecation" -> createBody(d) }

      if (!me.isAliasType && !isPackageOrClassOrTraitOrObject) {
        j += "resultType" -> createTypeEntity(me.resultType)
      }

      val isTemplate = me.isInstanceOf[MemberTemplateEntity]

      if (compactFlags) {
        if (me.isDef          ) set(j, 'd')
        if (me.isVal          ) set(j, 'v')
        if (me.isLazyVal      ) set(j, 'l')
        if (me.isVar          ) set(j, 'V')
        // XXX TODO
//        if (m.isImplicit    ) set(j, 'm')
        if (me.isConstructor  ) set(j, 'n')
        if (me.isAliasType    ) set(j, 'a')
        if (me.isAbstractType ) set(j, 'A')
        // HH
//        if (m.isTemplate    ) set(j, 'M')
        if (isTemplate        ) set(j, 'M')
      } else {
        if (me.isDef          ) j += "isDef"          -> true
        if (me.isVal          ) j += "isVal"          -> true
        if (me.isLazyVal      ) j += "isLazyVal"      -> true
        if (me.isVar          ) j += "isVar"          -> true
        // XXX TODO
//        if (m.isImplicit    ) j += "isImplicit"     -> true
        if (me.isConstructor  ) j += "isConstructor"  -> true
        if (me.isAliasType    ) j += "isAliasType"    -> true
        if (me.isAbstractType ) j += "isAbstractType" -> true
        // HH
//        if (m.isTemplate    ) j += "isTemplate"     -> true
        if (isTemplate        ) j += "isTemplate"     -> true
      }
    } // as[MemberEntity](e)

    as[m.DocTemplateEntity](e) { t =>
      t.sourceUrl.foreach(u => j.addOpt("sourceUrl", u.toString))
      j.addOpt("typeParams", createTypeParams(t.typeParams, tParams))
//      t.parentType .foreach { p => j += "parentType" -> createTypeEntity(p) }
      t.parentTypes.foreach { case (_ /*pTemp*/, pType) =>
        j += "parentType" -> createTypeEntity(pType)  // XXX TODO correct?
      }

      // "parentTemplates" is not needed and has been removed in Scala trunk (2.9)
      //j addOpt "parentTemplates" -> JArray(t.parentTemplates.map(e => global(e)(createEntity _)))

//      j addOpt "linearization" -> JArray(t.linearization .map(e => global(e)(createEntity)))
      j.addOpt("linearization", JArray(t.linearizationTemplates.map(e => global(e)(createEntity))))  // XXX TODO correct?

//      j addOpt "subClasses"    -> JArray(t.subClasses    .map(e => global(e)(createEntity)))
      // HH
      j.addOpt("subClasses", JArray(t.directSubClasses.map(e => global(e)(createEntity))))

      // "members" is constructors + templates + methods + values + abstractTypes + aliasTypes + packages
      val members = t.members // .filter(m => m.visibility.isProtected || m.visibility.isPublic)
      j.addOpt("members", JArray(members.map(e => global(e)(createEntity))))

//      j.addOpt("templates", JArray(t.templates.map(e => global(e)(createEntity _))))
//      j.addOpt("methods", JArray(t.methods.map(e => global(e)(createEntity _))))
//      j.addOpt("values", JArray(t.values.map(e => global(e)(createEntity _))))
//      j.addOpt("abstractTypes", JArray(t.abstractTypes.map(e => global(e)(createEntity _))))
//      j.addOpt("aliasTypes", JArray(t.aliasTypes.map(e => global(e)(createEntity _))))

      t.companion.foreach { p => j += "companion" -> global(p)(createEntity) }
    }

    as[m.Trait](e) { t =>
      j.addOpt("valueParams", createValueParams(t.valueParams, vParams))
    }

    as[m.Class](e) { c =>
      //j addOpt "constructors" -> JArray(c.constructors.map(e => global(e)(createEntity _)))
      if (c.isCaseClass) {
        if (compactFlags) set(j, 'C')
        else j += "isCaseClass" -> true
      }
    }

    //as[Package](e) { p => j addOpt "packages" -> JArray(p.packages.map(e => global(e)(createEntity _))) }
    as[m.NonTemplateMemberEntity](e) { n =>
      if (n.isUseCase) {
        if (compactFlags) set(j, 'U')
        else j += "isUseCase" -> true
      }
    }

    as[m.Def](e) { d =>
      j.addOpt("typeParams" , createTypeParams (d.typeParams , tParams))
      j.addOpt("valueParams", createValueParams(d.valueParams, vParams))
    }

    as[m.Constructor](e) { c =>
      if (c.isPrimary) {
        if (compactFlags) set(j, 'P')
        else j += "isPrimary" -> true
      }
      j.addOpt("valueParams", createValueParams(c.valueParams, vParams))
    }

    as[m.AbstractType](e) { a =>
      a.lo.foreach(t => j.addOpt("lo", createTypeEntity(t)))
      a.hi.foreach(t => j.addOpt("hi", createTypeEntity(t)))
    }

    as[m.AliasType](e) { a =>
      j += "alias" -> createTypeEntity(a.alias)
    }

//    as[ParameterEntity](e) { p =>
//      if (!compactFlags) {
//        // These two are not represented with compact flags
//        if (p.isTypeParam ) j += "isTypeParam" -> true
//        if (p.isValueParam) j += "isValueParam" -> true
//      }
//      j -= "inTemplate"
//    }

    as[m.ParameterEntity](e) { p =>
      if (!compactFlags) {
        // These two are not represented with compact flags
        as[m.TypeParam](p) { _ =>
          j += "isTypeParam" -> true
        }
        as[m.ValueParam](p) { _ =>
          j += "isValueParam" -> true
        }
      }
      j -= "inTemplate"
    }

    as[m.TypeParam](e) { t =>
      j.addOpt("variance", t.variance)
      t.lo.foreach(t => j.addOpt("lo", createTypeEntity(t)))
      t.hi.foreach(t => j.addOpt("hi", createTypeEntity(t)))
    }

    as[m.ValueParam](e) { v =>
      j += "resultType" -> createTypeEntity(v.resultType)

      v.defaultValue.foreach { s =>
        j += "defaultValue" -> s.expression // XXX TODO correct?
      }
      if (v.isImplicit) {
        if (compactFlags) set(j, 'm')
        else j += "isImplicit" -> true
      }
    }

    // Traits have empty dummy valueParams, and for classes they are
    // duplicates from the primary constructor
    if (isClassOrTrait) j -= "valueParams"
    j
  }

  def createValueParams(vp: List[List[m.ValueParam]], docs: CMap[String, JObject]): JArray = {
    JArray(vp.map(l => JArray(l.map(e => global(e) { e =>
      val j = createEntity(e)
      docs.get(e.name).foreach { doc => j += "doc" -> doc }
      j
    }))))
  }

  def createTypeParams(tp: List[m.TypeParam], docs: CMap[String, JObject]): JArray = {
    JArray(tp.map(e => global(e) { e =>
      val j = createEntity(e)
      docs.get(e.name).foreach { doc => j += "doc" -> doc }
      j
    }))
  }

  /**
   * Sets a flag in the "is" field.
   *
   *  - o: isProtected
   *  - u: isPublic
   *  - p: isPackage
   *  - r: isRootPackage
   *  - t: isTrait
   *  - c: isClass
   *  - b: isObject
   *  - D: isDocTemplate
   *  - d: isDef
   *  - v: isVal
   *  - l: isLazyVal
   *  - V: isVar
   *  - m: isImplicit
   *  - n: isConstructor
   *  - a: isAliasType
   *  - A: isAbstractType
   *  - M: isTemplate
   *  - C: isCaseClass
   *  - U: isUseCase
   *  - P: isPrimary
   *  - s: isSealed
   *  - B: isAbstract
   *  - f: isFinal
   */
  def set(j: JObject, flag: Char): Unit = {
    j += "is" -> (j.get("is").getOrElse("") + flag.toString)
  }
}
