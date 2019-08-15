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
      case LinkToMember     (_, _)        => None // XXX TODO what could Mbr be?
      case LinkToTpl        (_)           => None // XXX TODO what could Tpl be?
      case LinkToExternalTpl(name, _, _)  => Option(name)
      case Tooltip          (name)        => Option(name)
    }

    def qNameOption (peer: LinkTo): Option[String] = None
  }
}
trait EntityView[-A] {
  def nameOption  (peer: A): Option[String]
  def qNameOption (peer: A): Option[String]
}
