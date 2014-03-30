Scala type provider examples
============================

This repository contains type provider examples that will be discussed by
[Eugene Burmako](https://twitter.com/xeno_by) and [Travis
Brown](https://twitter.com/travisbrown) at the [2014 Scalar
Conference](http://scalar-conf.com/).

Motivation
----------

We'll take as a running example the construction of RDF graphs using
[Banana RDF](https://github.com/w3c/banana-rdf), a Scala RDF library
developed by the [World Wide Web Consortium](http://www.w3.org/).

Banana provides a clear and concise embedded DSL for building RDF graphs.
For example, we might describe the second draft notebook of Mary Shelley's
_Frankenstein_ as follows:

``` scala
val frankensteinNotebookB = (
  URI("http://shelleygodwinarchive.org/data/ox/ox-ms_abinger_c57")
    .a(dct.Text)
    -- dc.title ->- "Frankenstein Draft Notebook B"
    -- dc.creator ->- URI("https://en.wikipedia.org/wiki/Mary_Shelley")
)
```

`dct` and `dc` here are `org.w3.banana.Prefix` objects that represent the
[Dublin Core Metadata Initiative](http://dublincore.org/)'s types and terms
vocabularies.

The following is an example of how you'd define a `Prefix` class in Banana:

``` scala
class DCPrefix[Rdf <: RDF](ops: RDFOps[Rdf])
  extends PrefixBuilder("dc", "http://purl.org/dc/terms/")(ops) {
  val title = apply("title")
  val creator = apply("creator")
  ...
}
```

And then somewhere else in our project we'd write the following:

``` scala
object dc extends DCPrefix[Rdf]
```

Which would allow the usage above.

This isn't too bad, but in some cases our vocabularies can be quite large (the
terms vocabulary defines 77 properties, for example), which can make manually
creating `Prefix` classes inconvenient and error-prone. Creating these classes
manually can be especially frustrating when we have access to a
machine-readable description of the vocabulary in the form of an [RDF
Schema](http://www.w3.org/TR/rdf-schema/). The terms schema, for example, is
[published on the web](http://dublincore.org/2008/01/14/dcterms.rdf) by the
DCMI under a Creative Commons license.

Type providers allow us to avoid the boilerplate of translating these schemas
into `Prefix` class definitions manually. This repository includes two
macro-based implementations of type providers in Scala, one demonstrating the
"public" approach, in which the body of a publicly-visible class is provided
by the macro, and the other demonstrating the "anonymous" approach, where the
macro defines and instantiates an anonymous class that is visible to the rest
of the program as a structural type.

Anonymous type providers
------------------------

The anonymous approach is supported by `def` macros, which means that it can be
used in Scala 2.10 without additional compiler plugins (although note that the
example implementation provided here uses quasiquotes and therefore does require
the [Macro Paradise plugin](http://docs.scala-lang.org/overviews/macros/paradise.html)).

The syntax looks quite natural—we simply call a method with some arguments (in
this case there are two parameter lists, with the first taking a path to the schema
resource, and the second taking the arguments for the `PrefixBuilder` constructor):

``` scala
val dct = PrefixGenerator.fromSchema[Rdf]("/dctype.rdf")(
  "dct",
  "http://purl.org/dc/dcmitype/"
)

val dc = PrefixGenerator.fromSchema[Rdf]("/dcterms.rdf")(
  "dc",
  "http://purl.org/dc/terms/"
)
```

The inferred types of `dct` and `dc` are now structural types that allow the
usage demonstrated in the definition of `frankensteinNotebookB` above.

Public type providers
---------------------

The public approach uses [macro annotations](http://docs.scala-lang.org/overviews/macros/annotations.html),
which allow us to expand the body of an annotated object definition.

``` scala
  @fromSchema("/dctype.rdf") object dct extends PrefixBuilder[Rdf](
    "dct",
    "http://purl.org/dc/dcmitype/"
  )

  @fromSchema("/dcterms.rdf") object dc extends PrefixBuilder[Rdf](
    "dc",
    "http://purl.org/dc/terms/"
  )
```

These definitions support the same usage as the anonymous examples above,
but `dct` and `dc` are full-fledged objects, not instances of structural types.

Licenses
--------

Portions of this software may use RDF schemas copyright © 2011
[DCMI](http://dublincore.org/), the Dublin Core Metadata Initiative.
These are licensed under the [Creative Commons 3.0
Attribution](http://creativecommons.org/licenses/by/3.0/) license.

