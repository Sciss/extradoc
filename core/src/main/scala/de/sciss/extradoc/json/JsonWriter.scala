/*
 *  JsonWriter.scala
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

import java.io.{BufferedWriter, FileOutputStream, OutputStreamWriter, File => JFile}

// XXX TODO --- why ISO-8859-1 and not UTF-8 ?
case class JsonWriter(dir: JFile, fName: String) {

  private[this] var bwr: BufferedWriter = null
  private[this] var first = true

  object ObjectWriter {
    def write(k: String, v: JBase, resolveLink: Link => Any): Unit = {
      if (first) first = false else bwr write ",\n"
      bwr.write("\"" + k + "\":")
      v.writeTo(bwr, resolveLink)
    }
  }

  object ArrayWriter {
    def write(v: JBase, resolveLink: Link => Any): Unit = {
      if (first) first = false else bwr.write(",\n")
      v.writeTo(bwr, resolveLink)
    }
  }

  def createObject(f: ObjectWriter.type => Unit): Unit = {
    val fOut = new FileOutputStream(new JFile(dir, fName))
    try {
      bwr = new BufferedWriter(new OutputStreamWriter(fOut, "ISO-8859-1"), 1024 * 1024)
      bwr.write('{')
      f(ObjectWriter)
      bwr.write("}\n")
      bwr.flush()
    } finally {
      fOut.close()
    }
  }

  def createArray(f: ArrayWriter.type => Unit): Unit = {
    val fOut = new FileOutputStream(new JFile(dir, fName))
    try {
      bwr = new BufferedWriter(new OutputStreamWriter(fOut, "ISO-8859-1"), 1024 * 1024)
      bwr.write('[')
      f(ArrayWriter)
      bwr.write("]\n")
      bwr.flush()
    } finally {
      fOut.close()
    }
  }
}
