package typeproviders.rdfs.anonymous

import org.w3.banana._
import org.w3.banana.sesame.Sesame
import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.Context
import typeproviders.rdfs.SchemaParser

/** An alternative implementation of the anonymous type provider that uses
  * "vampire" methods to avoid reflective calls on the structural type.
  */
object VampiricPrefixGenerator extends AnonymousTypeProviderUtils {
  class body(tree: Any) extends StaticAnnotation

  /** A helper method that allows a macro method to read an annotation on
    * itself. We'll use this below in the macro methods on the anonymous class
    * we're defining.
    */
  def selectField_impl(c: Context) = c.Expr(
    c.macroApplication.symbol.annotations.filter(
      _.tpe <:< c.typeOf[body]
    ).head.scalaArgs.head
  )

  def fromSchema[Rdf <: RDF](path: String)(implicit ops: RDFOps[Rdf]) =
    macro fromSchema_impl[Rdf]

  def fromSchema_impl[Rdf <: RDF: c.WeakTypeTag](c: Context)
    (path: c.Expr[String])
    (ops: c.Expr[RDFOps[Rdf]]) = {
    import c.universe._

    /** The first several steps look exactly the same as they do in the
      * non-vampiric version.
      */
    def bail(message: String) = c.abort(c.enclosingPosition, message)

    val pathLiteral = path.tree match {
      case Literal(Constant(s: String)) => s
      case _ => bail(
        "You must provide a literal resource path for schema parsing."
      )
    }

    val prefixName = assignedValName(c).getOrElse(
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

    /** Here's where we diverge from the non-vampiric anonymous approach.
      * Instead of nice simple vals, our terms are going to be macro methods. 
      */
    val defs = names.map { name =>
      q"""
        @VampiricPrefixGenerator.body(ops.URI($name))
        def ${newTermName(name)}: Rdf#URI =
          macro VampiricPrefixGenerator.selectField_impl
      """
    }

    val className = newTypeName(c.fresh("Prefix"))

    /** This is again exactly the same as in the non-vampiric version, and the
      * the inferred structural type of the instance we return will also be
      * the same, apart from the fact that we're using methods instead of vals.
      */ 
    c.Expr[PrefixBuilder[Rdf]](
      q"""
        class $className extends
          org.w3.banana.PrefixBuilder($prefixName, $baseUriString)($ops) {
          ..$defs
        }
        new $className {}
      """
    )
  }
}

