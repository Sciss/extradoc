package de.sciss.dotter

import dotterweide.build.Module
import dotterweide.editor.controller.LookUpTypeAction
import dotterweide.editor.{Async, Color, Editor}
import dotterweide.ide.MainFrame
import dotterweide.languages.scala.ScalaLanguage
import dotterweide.languages.scala.node.{ClassType, ModuleType, PackageType, ScalaType}
import dotterweide.node.NodeType

import scala.swing.{BorderPanel, Frame}

class ScalaDocLookUp(language: ScalaLanguage, frame: MainFrame, docModule: Module) {

  private[this] val panel       = frame.panel
//  private[this] val baseDir     = DocUtil.defaultUnpackDir(cacheDir, docModule)
//  private[this] val ready       = new File(baseDir, "ready")

  private[this] val ErrorColor  = new Color(200, 0, 0)
  private[this] val WarnColor   = new Color(160, 80, 0)

  private[this] implicit val async: Async = panel.async

  panel.editors.foreach { ed =>
    ed.addAction(new ActionImpl(ed))
  }

  private def printWarn(s: String): Unit =
    frame.console.print(s + "\n", WarnColor)

  private def printError(s: String): Unit =
    frame.console.print(s + "\n", ErrorColor)

  private object browser {
    private lazy val panel: BorderPanel = new BorderPanel {

    }

    def openURI(s: String): Unit = {

    }

    private lazy val webFrame: Frame = new Frame {
//      title     = baseTitle
      contents  = panel
      bounds    = {
        val gc    = frame.peer.getGraphicsConfiguration
        val r     = gc.getBounds
        val x2    = r.x + r.width
        r.width   = math.min(r.width/2, 960)
        r.x       = x2 - r.width
        val h     = r.height
        r.height  = math.min(r.height, 960)
        r.y       = r.y + (h - r.height)/2
        r
      }
      open()
    }

    def dispose(): Unit = webFrame.dispose()

    def title: String = webFrame.title
    def title_=(value: String): Unit = {
      webFrame.title = value
      if (!webFrame.visible) webFrame.visible = true
    }
  }

  private val docGlobal = ReadModelTest.readGlobal().get

  private def docForType(tpe: NodeType): Unit = tpe match {
    case sd: ScalaType =>
      val (pkgPath: Option[String], className: Option[String]) = sd match {
        case ClassType   (pkg, name)  => (Some(pkg.presentation), Some(name)) // pkg.scalaDocPath()
        case ModuleType  (pkg, name)  => (Some(pkg.presentation), Some(name)) // pkg.scalaDocPath()
        case PackageType (name)       => (Some(name), None)
        case _                        => (None, None)
      }
      pkgPath match {
        case Some(path) =>
          val pkgOpt = docGlobal.packageMap.get(path)
          pkgOpt match {
            case Some(pkg) =>
              val pgIdxOpt = className match {
                case Some(name) => pkg.entryMap.get(name).map(_.page)
                case None       => Some(pkg.page)
              }
              println(s"Doc page is $pgIdxOpt")
              val pgOpt = pgIdxOpt.flatMap(ReadModelTest.readPage)
              pgOpt match {
                case Some(p) =>
                  p.members.headOption.foreach(_.comment.foreach { c =>
                    println("SHORT COMMENT:")
                    println(c.short)
                  })
                  println("MEMBERS:")
                  p.members.foreach(m => println(m.name.orElse(m.qName).getOrElse("?")))

                case None =>
                  printWarn(s"No page information for ${pkg.page}")
              }

            case None =>
              printWarn(s"No package information for ${sd.presentation}")
          }

        case None =>
          printWarn(s"Could not determine scala-doc path for ${sd.presentation}")
      }

    case _ =>
      printWarn("No scala type found")
  }

  private class ActionImpl(ed: Editor)
    extends LookUpTypeAction(ed.document, ed.terminal, ed.data, language.adviser) {

    override def run(tpeOpt: Option[NodeType]): Unit = {
      super.run(tpeOpt)
      tpeOpt.foreach(docForType)
    }
  }
}