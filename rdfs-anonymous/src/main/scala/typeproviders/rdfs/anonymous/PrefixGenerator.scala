package typeproviders.rdfs.anonymous

import org.w3.banana._
import org.w3.banana.sesame.Sesame
import scala.language.experimental.macros
import scala.reflect.macros.Context
import typeproviders.rdfs.SchemaParser

object PrefixGenerator extends AnonymousTypeProviderUtils {
  def fromSchema[Rdf <: RDF](path: String)(implicit ops: RDFOps[Rdf]) =
    macro fromSchema_impl[Rdf]

  def fromSchema_impl[Rdf <: RDF: c.WeakTypeTag](c: Context)
    (path: c.Expr[String])
    (ops: c.Expr[RDFOps[Rdf]]) = {
    import c.universe._

    def bail(message: String) = c.abort(c.enclosingPosition, message)

    /** First we deconstruct the path expression, which has to be a string
      * literal, since we need it at compile time.
      */
    val pathLiteral = path.tree match {
      case Literal(Constant(s: String)) => s
      case _ => bail(
        "You must provide a literal resource path for schema parsing."
      )
    }

    /** Next we figure out a reasonable short prefix name (which will fill a
      * field on the resulting [[org.w3.banana.Prefix]] object that Banana
      * will use for pretty printing). See
      * [[typeproviders.rdfs.anonymous.AnonymousTypeProviderUtils]] for some
      * discussion of how this is implemented.
      */
    val prefixName = assignedValName(c).getOrElse(
      bail("You must assign the output of the macro to a value.")
    )

    /** Now we can actually read the schema resource. We're using the Sesame
      * backend for Banana, but that's only relevant now (at compile time);
      * the macro user can pick whichever backend they want.
      */
    val schemaParser = SchemaParser.fromResource[Sesame](
      pathLiteral
    ).getOrElse(
      bail(s"Invalid schema: $pathLiteral.")
    )

    /** We need to know the base URI for the schema. See the comments in
      * [[typeproviders.rdfs.SchemaParser]] for more information about how
      * this is determined from the RDF graph.
      */
    val baseUri = schemaParser.inferBaseUri.getOrElse(
      bail("Could not identify a unique schema URI.")
    )

    /** Now we have the URI as an instance of Sesame#URI, but we also need it
      * as a string for the [[org.w3.banana.PrefixBuilder]] constructor.
      */
    val baseUriString = RDFOps[Sesame].fromUri(baseUri)

    /** Next we read all class and property names out of the schema. Banana
      * doesn't distinguish between the two here, so we just combine them.
      */
    val names =
      schemaParser.classNames(baseUri) ++
      schemaParser.propertyNames(baseUri)

    /** Now for the part where we actually generate the definitions that we
      * want to show up on the structural type instance we're building.
      */
    val defs = names.map { name =>
      q"""val ${newTermName(name)} = apply($name)"""
    }

    /** And now we define our anonymous class and instantiate it. See
      * [[http://stackoverflow.com/a/18485004/334519 this Stack Overflow
      * answer]] for some discussion of why we need both a local class
      * definition and an anonymous class.
      */
    c.Expr[PrefixBuilder[Rdf]](
      q"""
        class Prefix extends
          org.w3.banana.PrefixBuilder($prefixName, $baseUriString)($ops) {
          ..$defs
        }
        new Prefix {}
      """
    )
  }
}

