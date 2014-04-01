package typeproviders.rdfs.anonymous

import org.w3.banana._
import org.w3.banana.sesame.Sesame
import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.Context
import typeproviders.rdfs.SchemaParser

/** Another implementation that combines vampire methods with some tricks to
  * dig around in the enclosing context to allow us to avoid having to provide
  * a prefix name (we just reuse the name of the variable).
  */
object AdvancedPrefixGenerator {
  class body(tree: Any) extends StaticAnnotation

  def selectField_impl(c: Context) = c.Expr(
    c.macroApplication.symbol.annotations.filter(
      _.tpe <:< c.typeOf[body]
    ).head.scalaArgs.head
  )

  def fromSchema[Rdf <: RDF](
    path: String
  )(
    uri: String
  )(implicit ops: RDFOps[Rdf]) = macro fromSchema_impl[Rdf]

  def fromSchema_impl[Rdf <: RDF: c.WeakTypeTag](c: Context)(
    path: c.Expr[String]
  )(
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

    val prefix = c.enclosingClass match {
      case ClassDef(_, _, _, Template(_, _, body)) => body.collectFirst {
        case q"val $name = $tree" if tree.pos == c.enclosingPosition =>
          name.decoded
      }.getOrElse(
        bail("You must assign the prefix to a val.")
      )
      case _ => bail("You must use this macro in a class definition.")
    }

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
      q"""
        @VampiricPrefixGenerator.body(ops.URI($name))
        def ${newTermName(name)}: Rdf#URI =
          macro VampiricPrefixGenerator.selectField_impl
      """
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

