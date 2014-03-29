package typeproviders.rdfs.public

import org.w3.banana._
import org.w3.banana.sesame.Sesame
import scala.language.experimental.macros
import scala.reflect.macros.Context
import scala.annotation.StaticAnnotation
import typeproviders.rdfs.SchemaParser

class fromSchema(path: String) extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro PrefixGenerator.fromSchema_impl
}

object PrefixGenerator {
  def fromSchema_impl(c: Context)(annottees: c.Expr[Any]*) = {
    import c.universe._

    def bail(message: String) = c.abort(c.enclosingPosition, message)

    val path = c.macroApplication match {
      case Apply(Select(Apply(_, List(Literal(Constant(s: String)))), _), _) => s
      case _ => bail(
        "You must provide a literal resource path for schema parsing."
      ) 
    }

    annottees.map(_.tree) match {
      case List(q"""object $name extends $parent { ..$body }""") =>
        parent match {
          case Apply(
            AppliedTypeTree(prefixBuilder, List(rdf)),
            List(Literal(Constant(prefix: String)), Literal(Constant(uri: String)))
          ) =>
            val schemaParser = SchemaParser.fromResource[Sesame](path, uri).getOrElse(
              bail(s"Invalid schema: $path.")
            )

            val names = schemaParser.classNames ++ schemaParser.propertyNames

            val defs = names.map { name =>
              q"""val ${newTermName(name)} = apply($name)"""
            }

            c.Expr[Any](
              q"""object $name extends $parent { ..${defs ::: body} }"""
            )
          case _ => bail(
            "You must supply a prefix and URI to the constructor." 
          )
        }


      case _ => bail(
        "You can only create a prefix from an object definition."
      )
    }
  }
}

