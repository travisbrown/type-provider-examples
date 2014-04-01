package typeproviders.rdfs.anonymous

import org.w3.banana._
import org.w3.banana.sesame.Sesame
import scala.language.experimental.macros
import scala.reflect.macros.Context
import typeproviders.rdfs.SchemaParser

object PrefixGenerator extends AnonymousTypeProviderUtils {
  def fromSchema[Rdf <: RDF](
    path: String
  )(implicit ops: RDFOps[Rdf]) = macro fromSchema_impl[Rdf]

  def fromSchema_impl[Rdf <: RDF: c.WeakTypeTag](c: Context)(
    path: c.Expr[String]
  )(ops: c.Expr[RDFOps[Rdf]]) = {
    import c.universe._

    def bail(message: String) = c.abort(c.enclosingPosition, message)

    val pathLiteral = path.tree match {
      case Literal(Constant(s: String)) => s
      case _ => bail(
        "You must provide a literal resource path for schema parsing."
      )
    }

    val prefix = assignedValName(c).getOrElse(
      bail("You must assign the output of the macro to a value.")
    )

    val schemaParser = SchemaParser.fromResource[Sesame](
      pathLiteral
    ).getOrElse(
      bail(s"Invalid schema: $pathLiteral.")
    )

    val baseUri = schemaParser.inferBaseUri.getOrElse(
      bail("Could not identify a unique schema URI.")
    )
    
    val baseUriString = RDFOps[Sesame].fromUri(baseUri)

    val names =
      schemaParser.classNames(baseUri) ++
      schemaParser.propertyNames(baseUri)

    val defs = names.map { name =>
      q"""val ${newTermName(name)} = apply($name)"""
    }

    c.Expr[PrefixBuilder[Rdf]](
      q"""
        class Prefix extends
          org.w3.banana.PrefixBuilder($prefix, $baseUriString)($ops) {
          ..$defs
        }
        new Prefix {}
      """
    )
  }
}

