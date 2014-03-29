package typeproviders.rdfs.anonymous

import org.w3.banana._
import org.w3.banana.sesame.Sesame
import scala.language.experimental.macros
import scala.reflect.macros.Context
import typeproviders.rdfs.SchemaParser

object PrefixGenerator {
  def fromSchema[Rdf <: RDF](
    path: String
  )(
    prefix: String,
    uri: String
  )(implicit ops: RDFOps[Rdf]) = macro fromSchema_impl[Rdf]

  def fromSchema_impl[Rdf <: RDF: c.WeakTypeTag](c: Context)(
    path: c.Expr[String]
  )(
    prefix: c.Expr[String],
    uri: c.Expr[String]
  )(ops: c.Expr[RDFOps[Rdf]]) = {
    import c.universe._

    def bail(message: String) = c.abort(c.enclosingPosition, message)

    val pathLiteral = path.tree match {
      case Literal(Constant(s: String)) => s
      case _ => bail(
        "You must provide a literal resource path for schema parsing."
      )
    }

    val prefixLiteral = prefix.tree match {
      case Literal(Constant(s: String)) => s
      case _ => bail("You must provide a literal prefix.")
    }

    val uriLiteral = uri.tree match {
      case Literal(Constant(s: String)) => s
      case _ => bail("You must provide a literal URI.")
    }

    val schemaParser = SchemaParser.fromResource[Sesame](
      pathLiteral,
      uriLiteral
    ).getOrElse(
      bail(s"Invalid schema: $pathLiteral.")
    )

    val names = schemaParser.classNames ++ schemaParser.propertyNames

    val defs = names.map { name =>
      q"""val ${newTermName(name)} = apply($name)"""
    }

    c.Expr[PrefixBuilder[Rdf]](
      q"""
        class Prefix extends
          org.w3.banana.PrefixBuilder($prefixLiteral, $uriLiteral)($ops) {
          ..$defs
        }
        new Prefix {}
      """
    )
  }
}

