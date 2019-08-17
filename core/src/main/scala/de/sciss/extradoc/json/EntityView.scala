/*
 *  EntityView.scala
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

import scala.tools.nsc.doc.base.{LinkTo, LinkToExternalTpl, LinkToMember, LinkToTpl}
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
      case LinkToMember     (m: Entity, t: Entity)  =>
//        if (!t.isInstanceOf[Entity]) println(s"Not an entity: $t")
        // XXX TODO which one?
        Some(m.name)
//        Some(t.name)
      case LinkToTpl        (t: Entity)     => Some(t.name)
      case LinkToExternalTpl(name, _, _)    => Option(name)
//      case Tooltip          (name)          => Option(name)
      case _ =>
//        println(s"linkTo.nameOption unclear: $peer")
        None
    }

    def qNameOption (peer: LinkTo): Option[String] = peer match {
      case LinkToMember     (m: Entity, t: Entity) =>
        // XXX TODO which one?
        Some(m.qualifiedName)
//        Some(t.qualifiedName)
      case LinkToTpl        (t: Entity)     => Some(t.qualifiedName)
      case _ =>
//        println(s"linkTo.qNameOption unclear: $peer")
        None
    }
  }
}
trait EntityView[-A] {
  def nameOption  (peer: A): Option[String]
  def qNameOption (peer: A): Option[String]
}
