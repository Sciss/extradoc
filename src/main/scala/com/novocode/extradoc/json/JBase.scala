package com.novocode.extradoc.json

import java.io.{StringWriter, Writer}

import scala.collection.{mutable, Iterable => CIterable, Iterator => CIterator, Traversable => CTraversable, Map => CMap}
import scala.reflect.ClassManifest

abstract class CanBeValue[-T] {
  def isEmpty(v: T): Boolean

  def writeValue(v: T, out: Writer, resolveLink: Link => Any): Unit
}

object CanBeValue {
  implicit val jBaseCanBeValue: CanBeValue[JBase] = new CanBeValue[JBase] {
    def isEmpty(v: JBase): Boolean = v.isEmpty

    def writeValue(v: JBase, out: Writer, resolveLink: Link => Any): Unit =
      v.writeTo(out, resolveLink)
  }
  implicit val intCanBeValue: CanBeValue[Int] = new CanBeValue[Int] {
    def isEmpty(v: Int) = false

    def writeValue(v: Int, out: Writer, resolveLink: Link => Any): Unit =
      out write v.toString
  }
  implicit val stringCanBeValue: CanBeValue[String] = new CanBeValue[String] {
    def isEmpty(v: String): Boolean = v == ""

    def writeValue(v: String, out: Writer, resolveLink: Link => Any): Unit =
      JBase.quote(v, out)
  }
  implicit val booleanCanBeValue: CanBeValue[Boolean] = new CanBeValue[Boolean] {
    def isEmpty(v: Boolean): Boolean = !v

    def writeValue(v: Boolean, out: Writer, resolveLink: Link => Any): Unit =
      out.write(v.toString)
  }
  implicit val linkCanBeValue: CanBeValue[Link] = new CanBeValue[Link] {
    def isEmpty(v: Link) = false

    def writeValue(v: Link, out: Writer, resolveLink: Link => Any): Unit = {
      val resolved = resolveLink(v)
      recoverFor(resolved).writeValue(resolved, out, resolveLink)
    }
  }

  def recoverFor(v: Any): CanBeValue[Any] = v match {
    case _: JBase   => jBaseCanBeValue  .asInstanceOf[CanBeValue[Any]]
    case _: Int     => intCanBeValue    .asInstanceOf[CanBeValue[Any]]
    case _: String  => stringCanBeValue .asInstanceOf[CanBeValue[Any]]
    case _: Boolean => booleanCanBeValue.asInstanceOf[CanBeValue[Any]]
    case _: Link    => linkCanBeValue   .asInstanceOf[CanBeValue[Any]]
  }
}

sealed abstract class JBase {
  def writeTo(out: Writer, resolveLink: Link => Any): Unit

  def isEmpty: Boolean

  def links: CIterable[Link]

  def children: CIterable[JBase]

  def replaceLinks[T: CanBeValue](repl: CMap[Link, T]): Unit

  def foreachRec(f: JBase => Unit): Unit = {
    f(this)
    children.foreach(_.foreachRec(f))
  }

  override def toString: String = {
    val wr = new StringWriter
    writeTo(wr, _.target)
    wr.toString
  }
}

object JBase {
  def quote(s: String, wr: Writer): Unit = {
    wr.write('"')
    val len = s.length
    var i = 0
    while (i < len) {
      val c = s.charAt(i)
      if      (c == '"' ) wr.write("\\\"")
      else if (c == '\\') wr.write("\\\\")
      else if (c == '\r') wr.write("\\r")
      else if (c == '\n') wr.write("\\n")
      else if (c >= 32 && c <= 127) wr.write(c)
      else wr.write(s"\\u${"%04X".format(c.toInt)}")
      i += 1
    }
    wr.write('"')
  }
}

sealed class JObject extends JBase {
  private val m = mutable.Map.empty[String, Any]

  def +=[V](t: (String, V))(implicit cv: CanBeValue[V]): Unit = {
    //if(m contains t._1) throw new RuntimeException("Cannot overwrite field "+t._1)
    m += t
  }

  /** Adds an entry optionally, if the value is non-empty */
  def addOpt[V](t: (String, V))(implicit cv: CanBeValue[V]): Unit =
    if (!cv.isEmpty(t._2)) this += t

  def -= (k: String): Option[Any] = m.remove(k)

  def isEmpty: Boolean = m.isEmpty

  def writeTo(out: Writer, resolveLink: Link => Any): Unit = {
    out.write('{')
    var first = true
    for ((k, v) <- m) {
      if (first) first = false else out write ','
      JBase.quote(k, out)
      out.write(':')
      CanBeValue.recoverFor(v).writeValue(v, out, resolveLink)
    }
    out.write('}')
  }

  override def equals(o: Any): Boolean = o match {
    case j: JObject => m == j.m
    case _ => false
  }

  override def hashCode: Int = m.hashCode

  def links: CIterable[Link] = m.values.filter(_.isInstanceOf[Link]).asInstanceOf[CIterable[Link]]

  def children: CIterable[JBase] = m.values.filter(_.isInstanceOf[JBase]).asInstanceOf[CIterable[JBase]]

  def replaceLinks[T: CanBeValue](repl: CMap[Link, T]): Unit = m transform { case (_ /*k*/, v) =>
    v match {
      case l: Link => repl.getOrElse(l, l)
      case _ => v
    }
  }

  def apply(key: String): Option[Any] = m.get(key)

  def apply[T: ClassManifest](key: String, default: T): T = m.get(key) match {
    case Some(v) if implicitly[ClassManifest[T]].erasure.isInstance(v) => v.asInstanceOf[T]
    case _ => default
  }

  def keys: CIterator[String] = m.keys.iterator

  def !!(k: String): Boolean = m.get(k) match {
    case None | Some("") | Some(0) | Some(false) => false
    case _ => true
  }
}

object JObject {
  def apply: JObject = new JObject

  def apply[V: CanBeValue](t: CTraversable[(String, V)]): JObject = {
    val o = new JObject
    t foreach { case (k, v) => o += k -> v }
    o
  }

  val Empty: JObject = new JObject {
    override def +=[V](t: (String, V))(implicit cv: CanBeValue[V]): Unit =
      throw new RuntimeException("Cannot add to JObject.Empty")
  }
}

sealed class JArray extends JBase {
  private val a = mutable.Buffer.empty[Any]

  def += [T](v: T)(implicit cv: CanBeValue[T]): Unit = a += v

  def addOpt[T](v: T)(implicit cv: CanBeValue[T]): Unit = if (!cv.isEmpty(v)) a += v

  def isEmpty: Boolean = a.isEmpty

  def writeTo(out: Writer, resolveLink: Link => Any): Unit = {
    out write '['
    var first = true
    for (v <- a) {
      if (first) first = false else out write ','
      CanBeValue.recoverFor(v).writeValue(v, out, resolveLink)
    }
    out write ']'
  }

  override def equals(o: Any): Boolean = o match {
    case j: JArray => a == j.a
    case _ => false
  }

  override def hashCode: Int = a.hashCode

  def links: CIterable[Link] = a.filter(_.isInstanceOf[Link]).asInstanceOf[CIterable[Link]]

  def children: CIterable[JBase] = a.filter(_.isInstanceOf[JBase]).asInstanceOf[CIterable[JBase]]

  def replaceLinks[T: CanBeValue](repl: CMap[Link, T]): Unit = {
    val len = a.length
    var i = 0
    while (i < len) {
      a(i) match {
        case l: Link  => repl.get(l).foreach(a(i) = _)
        case _        =>
      }
      i += 1
    }
  }

  def values: CIterator[Any] = a.iterator

  def length: Int = a.length

  def transform(f: (Int, Any) => Any): Unit = {
    var i = 0
    while (i < a.length) {
      a(i) = f(i, a(i))
      i += 1
    }
  }
}

object JArray {
  def apply: JArray = new JArray

  def apply[T: CanBeValue](t: CTraversable[T]): JArray = {
    val a = new JArray
    t foreach { j => a += j }
    a
  }

  val Empty: JArray = new JArray {
    override def += [T](v: T)(implicit cv: CanBeValue[T]): Unit =
      throw new RuntimeException("Cannot add to JArray.Empty")
  }
}

case class Link(target: Int) {
  override def toString: String = target.toString
}

case class LimitedEquality(j: JBase, keys: String*) {
  override def hashCode: Int = 0 //TODO optimize

  override def equals(o: Any): Boolean = o match {
    case LimitedEquality(o) => LimitedEquality.isEqual(j, o, keys: _*)
    case _ => false
  }

}

object LimitedEquality {
  def isEqual(a: Any, b: Any, keys: String*): Boolean = (a, b) match {
    case (null, null) => true
    case (null, _)    => false
    case (_, null)    => false

    case (a: JArray, b: JArray) =>
      a.length == b.length && (a.values zip b.values).forall { case (v, w) => isEqual(v, w, keys: _*) }

    case (a: JObject, b: JObject) =>
      keys forall { k =>
        (a(k), b(k)) match {
          case (None, None) => true
          case (None, _) => false
          case (_, None) => false
          case (Some(v), Some(w)) => isEqual(v, w, keys: _*)
        }
      }

    case (a: String , b: String ) => a == b
    case (a: Int    , b: Int    ) => a == b
    case (a: Boolean, b: Boolean) => a == b
    case (Link(a)   , Link(b)   ) => a == b

    case _ => false
  }
}
