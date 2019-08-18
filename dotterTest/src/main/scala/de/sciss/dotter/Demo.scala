package de.sciss.dotter

import java.awt.Dimension

import dotterweide.Example
import dotterweide.build.Module
import dotterweide.ide.MainFrame
import dotterweide.languages.scala.ScalaLanguage

import scala.swing.event.WindowClosed
import scala.swing.{Swing, Window}

object Demo {
  private val language =
    new ScalaLanguage(prelude = "object Main {\n", postlude = "\n}", examples = List(
      Example("Analog Bubbles", 'a',
      """import de.sciss.synth._
        |import ugen._
        |import Ops.stringToControl
        |
        |val f1 = "freq1".kr(0.4)
        |val f2 = "freq2".kr(8.0)
        |val d  = "detune".kr(0.90375)
        |val f  = LFSaw.ar(f1).mulAdd(24, LFSaw.ar(Seq(f2, f2 * d)).mulAdd(3, 80)).midiCps // glissando function
        |val x  = CombN.ar(SinOsc.ar(f) * 0.04, 0.2, 0.2, 4) // echoing sine wave
        |Out.ar(0, x)
        |
        |""".stripMargin)
    ),
      impliedPrefixes = List("scala.collection"))

  def main(args: Array[String]): Unit = {
    Swing.onEDT(run())
  }

  def run(): Unit = {
    val code  = language.examples.headOption.fold("")(_.code)
    val frame = new MainFrame(language, code)
    frame.preferredSize = new Dimension(874, 696)
    open(frame)
    frame.listenTo(frame)
    frame.reactions += {
      case WindowClosed(_) => sys.exit()
    }

    val docModule = Module("org.scala-lang", "scala-library", language.scalaVersion)
    new ScalaDocLookUp(language, frame, docModule = docModule)
  }

  private def open(window: Window): Unit = {
    window.pack()
    window.centerOnScreen()
    window.open()
  }
}
