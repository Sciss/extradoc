/*
 *  EntityHash.scala
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

import scala.tools.nsc.doc.model.NoDocTemplate

final case class EntityHash[+E <: AnyRef /*Entity*/](e: E) extends Function0[E] {

  override def hashCode: Int = e match {
    case t: NoDocTemplate => t.qualifiedName.hashCode
    case _                => System.identityHashCode(e)
  }

  override def equals(o: Any): Boolean = o match {
    case EntityHash(e2) => (e, e2) match {
      case (a, b) if a eq b => true
      case (t1: NoDocTemplate, t2: NoDocTemplate) =>
        (  t1.isPackage     == t2.isPackage)      &&
          (t1.isRootPackage == t2.isRootPackage)  &&
          (t1.isTrait       == t2.isTrait)        &&
          (t1.isClass       == t2.isClass)        &&
          (t1.isObject      == t2.isObject)       &&
          (t1.qualifiedName == t2.qualifiedName)

      case _ => false
    }
    case _ => false
  }

  override def toString: String = s"EntityHash($e)"

  def apply(): E = e
}
