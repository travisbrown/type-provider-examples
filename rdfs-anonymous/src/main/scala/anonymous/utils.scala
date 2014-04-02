package typeproviders.rdfs.anonymous

import scala.reflect.macros.Context

trait AnonymousTypeProviderUtils {
  def assignedValName(c: Context): Option[String] = {
    import c.universe._

    /** Note that this API is deprecated in 2.11. Banana RDF requires a prefix
      * name (mostly for internal use and pretty printing), and it's convenient
      * to read it off the name of the val the output of the macro is assigned
      * to, but we could also dig around in the schema XML or pick a value some
      * other way.
      */
    c.enclosingClass match {
      case ClassDef(_, _, _, Template(_, _, body)) => body.collectFirst {
        case q"val $name = $tree" if tree.pos == c.enclosingPosition =>
          name.decoded
      }
      case _ => None
    }
  }
}

