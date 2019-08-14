package com.novocode.extradoc

import scala.tools.nsc.reporters.ConsoleReporter

trait CompilerSupport {
  protected def summary(r: ConsoleReporter): Unit =
    r.printSummary()
}
