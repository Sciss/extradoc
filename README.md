# ExtraDoc

A variant of the scala-doc documentation tool that can output JSON format.
Original code for Scala 2.8 by [Stefan Zeiger](https://github.com/szeiger/extradoc),
updated for Scala 2.12 and 2.13 by Hanns Holger Rutz. Note: not all problems in the
migration have been fixed yet, this is experimental. License is BSD 2-clause.

Below is the original read-me.

---

An extended version of Scala's scaladoc command. It has all the features (and
the command line syntax) of the original Scaladoc, plus:

- Generate documentation in a single JSON file with a format resembling the
  structure of the scala.tools.nsc.doc.model.* classes (-doc-format:json)

- Generate documentation in multiple JSON files, one per doc template like
  in scaladoc's HTML output (-doc-format:json-multi)

- Generate documentation in multiple JSON files plus the browser-based
  Extradoc Explorer (-doc-format:explorer)

- Instead of only Scala source files you can also specify directories on the
  command line to include all *.scala files inside (recursively)

Usage: There's a bin/extradoc.bat script for Windows that can be used after
building the project with "sbt compile". Make sure that the "scala" command
from Scala 2.8.0 is on your path. Or run the main class
com.novocode.extradoc.ExtraDoc in some other way. Note the current version of
Extradoc requires Scala 2.8.0. It will *not* work with otherwise API-compatible
versions like 2.8.1 because it relies on internal compiler code.

Example: Building scaladoc.json for the Scala library (run from the base dir
of the Scala source code; put everything on one line):

  extradoc -doc-format:json -P:continuations:enable \
    src\actors src\library\scala src\swing src\continuations\library

For Scala 2.8.0, this results in a JSON file of 18 MB which compresses very
nicely down to less than 800 kB (with 7zip). Extradoc requires quite a bit of
memory to build its JSON models. You should have a heap size of at least 1.5
GB (2 GB recommended) to build the Scala library documentation in JSON format.

Example: Building an Extradoc Explorer site for the Scala library, with a
proper title and linked sources (assuming the output directory doc-test
already exists; put everything on one line):

  extradoc -doc-format:explorer -P:continuations:enable \
    -doc-title "Scala Library" \
    -doc-version 2.8.0 \
    -doc-source-url \
    http://lampsvn.epfl.ch/trac/scala/browser/scala/tags/R_2_8_0_final \
    -d doc-test \
    src\actors src\library\scala src\swing src\continuations\library

See LICENSE.txt for licensing conditions (BSD-style). Dependencies have their
own licenses (BSD, MIT, Apache, Scla). Patches are gladly accepted from their
original author. Along with any patches, please state that the patch is your
original work and that you license the work to the Extradoc project under the
project's open source license.

The current release is Extradoc 0.1 for Scala 2.8.0. Binaries can be found at
http://scala-tools.org/repo-releases/com/novocode/extradoc_2.8.0/0.1/ or
referenced with Maven/Ivy.
