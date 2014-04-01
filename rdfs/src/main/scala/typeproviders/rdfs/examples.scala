package typeproviders.rdfs.examples

import org.w3.banana._
import org.w3.banana.sesame.Sesame
import org.w3.banana.diesel._

class AnonymousTypeProviderExample[Rdf <: RDF](implicit ops: RDFOps[Rdf]) {
  import ops._
  import typeproviders.rdfs.anonymous._

  val dct = PrefixGenerator.fromSchema[Rdf]("/dctype.rdf")

  val dc = PrefixGenerator.fromSchema[Rdf]("/dcterms.rdf")

  val frankensteinNotebookB = (
    URI("http://shelleygodwinarchive.org/data/ox/ox-ms_abinger_c57")
      .a(dct.Image)
      -- dc.title ->- "Frankenstein Draft Notebook B"
      -- dc.creator ->- URI("https://en.wikipedia.org/wiki/Mary_Shelley")
  )
}

class PublicTypeProviderExample[Rdf <: RDF](implicit ops: RDFOps[Rdf]) {
  import ops._
  import typeproviders.rdfs.public._

  @fromSchema("/dctype.rdf") object dct extends PrefixBuilder[Rdf]

  @fromSchema("/dcterms.rdf") object dc extends PrefixBuilder[Rdf]

  val frankensteinNotebookB = (
    URI("http://shelleygodwinarchive.org/data/ox/ox-ms_abinger_c57")
      .a(dct.Image)
      -- dc.title ->- "Frankenstein Draft Notebook B"
      -- dc.creator ->- URI("https://en.wikipedia.org/wiki/Mary_Shelley")
  )
}

