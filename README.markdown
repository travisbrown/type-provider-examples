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
class DCPrefix[Rdf <: RDF](implicit ops: RDFOps[Rdf])
  extends PrefixBuilder("dc", "http://purl.org/dc/terms/")(ops) {
  val title = apply("title")
  val creator = apply("creator")
  // and so on...
}
```

And then somewhere else in our project we'd write the following:

``` scala
object dc extends DCPrefix[Rdf]
```

Which would allow the usage above.

This isn't too bad, but in some cases our vocabularies can be quite large (the
Dublin Core terms vocabulary defines 77 properties, for example), which can make manually
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

The syntax looks quite natural—we simply call a method with some arguments
(in this case a single argument—the path to the schema resource).

``` scala
val dct = PrefixGenerator.fromSchema[Rdf]("/dctype.rdf")

val dc = PrefixGenerator.fromSchema[Rdf]("/dcterms.rdf")
```

The macro does impose some additional constraints on the user of the type
provider, however. In particular, the path argument must be a string literal,
and it must point to a valid RDF Schema resource on the build classpath. If
either of these constraints is not satisfied, the type provider will fail with
a compile-time error.

The inferred types of `dct` and `dc`  in this example are structural types
that allow the usage demonstrated in the definition of `frankensteinNotebookB`
above. More specifically, the generated code for the second line will look
something like the following:

``` scala
val dc = {
  class Prefix2 extends PrefixBuilder("dc", "http://purl.org/dc/terms/") {
    val title = apply("title")
    val creator = apply("creator")
    // and so on...
  }
  new Prefix2 {}
}
```

I.e., we're defining a class inside a block and then instantiating an
anonymous subclass of that class (see [this Stack Overflow
answer](http://stackoverflow.com/a/18485004/334519) and [this earlier
question](http://stackoverflow.com/q/14370842/334519) for some discussion of
why this two-step process is necessary). We can't see the class itself outside
of the block, but we can see its methods on the structural type that will be
inferred for `dc`.

Note that the type provider has also inferred the schema URI from the RDF
Schema file (the second argument to the `PrefixBuilder` constructor) and has
picked a reasonable short name for the `Prefix` (the first argument).

Please see the comments in [the
implementation](https://github.com/travisbrown/type-provider-examples/blob/master/rdfs-anonymous/src/main/scala/typeproviders/rdfs/anonymous/PrefixGenerator.scala)
for more detail about how exactly this approach works.

Public type providers
---------------------

The public approach uses [macro annotations](http://docs.scala-lang.org/overviews/macros/annotations.html),
which allow us to expand the body of an annotated object definition.

``` scala
@fromSchema("/dctype.rdf") object dct extends PrefixBuilder[Rdf]

@fromSchema("/dcterms.rdf") object dc extends PrefixBuilder[Rdf]
```

These definitions support the same usage as the anonymous examples above,
but `dct` and `dc` are full-fledged objects, not instances of structural types.
The generated code looks fairly similar:

``` scala
object dc extends PrefixBuilder[Rdf]("dc", "http://purl.org/dc/terms/") {
  val title = apply("title")
  val creator = apply("creator")
  // and so on...
}
```

The [implementation](https://github.com/travisbrown/type-provider-examples/blob/master/rdfs-public/src/main/scala/typeproviders/rdfs/public/PrefixGenerator.scala)
is also pretty similar to the [anonymous type provider
implementation](https://github.com/travisbrown/type-provider-examples/blob/master/rdfs-anonymous/src/main/scala/typeproviders/rdfs/anonymous/PrefixGenerator.scala).

Vampire methods
---------------

One of the disadvantages of using structural types in Scala is that they involve
reflective access, which means you have to deal with warnings (and a hit to
performance). For example, when you compile the example project you'll see the
following:

```
[warn] ...reflective access of structural type member value title should be enabled
[warn] by making the implicit value scala.language.reflectiveCalls visible.
[warn]       -- dc.title ->- "Frankenstein Draft Notebook B"
[warn]             ^
```

It's possible, however, to use ["vampire
methods"](http://meta.plasm.us/posts/2013/07/12/vampire-methods-for-structural-types/)
to avoid this penalty. Vampire methods are macro methods on the anonymous
class that read their values from some location at compile time (in this case
we're using a static annotation on the method; if this sounds confusing,
that's because it is).

(Note that in Scala 2.10.4 the use of vampire methods will still result in a
reflective access warning, but this [has been fixed in
2.10.5](https://github.com/scala/scala/pull/3602).)

While we provide [an implementation](https://github.com/travisbrown/type-provider-examples/blob/master/rdfs-anonymous/src/main/scala/typeproviders/rdfs/anonymous/VampiricPrefixGenerator.scala)
of our example using vampire methods here, in general it's probably better to
avoid the added complexity, unless you know for a fact that the performance of
calls to methods on the structural type is a problem in your application.

Licenses
--------

Portions of this software may use RDF schemas copyright © 2011
[DCMI](http://dublincore.org/), the Dublin Core Metadata Initiative.
These are licensed under the [Creative Commons 3.0
Attribution](http://creativecommons.org/licenses/by/3.0/) license.

