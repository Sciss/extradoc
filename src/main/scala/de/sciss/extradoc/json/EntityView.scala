/*
 *  EntityView.scala
 *  (ExtraDoc)
 *
 *  This software is published under the BSD 2-clause license
 */

package de.sciss.extradoc.json

import scala.tools.nsc.doc.base.{LinkTo, LinkToExternalTpl, LinkToMember, LinkToTpl, Tooltip}
import scala.tools.nsc.doc.model.{Entity, ParameterEntity, TypeEntity}

object EntityView {
  implicit object entity extends EntityView[Entity] {
    def nameOption  (peer: Entity): Option[String] = Option(peer.name)
    def qNameOption (peer: Entity): Option[String] = Option(peer.qualifiedName)
  }

  implicit object typeEntity extends EntityView[TypeEntity] {
    def nameOption  (peer: TypeEntity): Option[String] = Option(peer.name)
    def qNameOption (peer: TypeEntity): Option[String] = None
  }

  implicit object parameterEntity extends EntityView[ParameterEntity] {
    def nameOption  (peer: ParameterEntity): Option[String] = Option(peer.name)
    def qNameOption (peer: ParameterEntity): Option[String] = None
  }

  implicit object linkTo extends EntityView[LinkTo] {
    def nameOption  (peer: LinkTo): Option[String] = peer match {
      case LinkToMember     (m: Entity, _)  => Some(m.name)
      case LinkToTpl        (t: Entity)     => Some(t.name)
      case LinkToExternalTpl(name, _, _)    => Option(name)
      case Tooltip          (name)          => Option(name)
      case _ =>
        println(s"linkTo.nameOption unclear: $peer")
        None
    }

    def qNameOption (peer: LinkTo): Option[String] = peer match {
      case LinkToMember     (m: Entity, _)  => Some(m.qualifiedName)
      case LinkToTpl        (t: Entity)     => Some(t.qualifiedName)
      case _ => None
    }
  }
}
trait EntityView[-A] {
  def nameOption  (peer: A): Option[String]
  def qNameOption (peer: A): Option[String]
}
