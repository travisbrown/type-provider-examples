package typeproviders.rdfs.examples

import org.w3.banana._
import org.w3.banana.sesame.Sesame
import org.w3.banana.diesel._
import scala.language.experimental.macros

class VampiricTypeProviderExample[Rdf <: RDF](implicit ops: RDFOps[Rdf]) {
  import ops._
  import typeproviders.rdfs.anonymous._

  val dct = VampiricPrefixGenerator.fromSchema[Rdf]("/dctype.rdf")(
    "dct",
    "http://purl.org/dc/dcmitype/"
  )

  val dc = VampiricPrefixGenerator.fromSchema[Rdf]("/dcterms.rdf")(
    "dc",
    "http://purl.org/dc/terms/"
  )

  val frankensteinNotebookB = (
    URI("http://shelleygodwinarchive.org/data/ox/ox-ms_abinger_c57")
      .a(dct.Image)
      -- dc.title ->- "Frankenstein Draft Notebook B"
      -- dc.creator ->- URI("https://en.wikipedia.org/wiki/Mary_Shelley")
  )
}

