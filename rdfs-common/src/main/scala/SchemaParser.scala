package typeproviders.rdfs

import org.w3.banana._
import org.w3.banana.diesel._
import org.w3.banana.syntax._
import scala.util.{ Failure, Success, Try }

/** Provides a constructor that reads an RDF graph representing an RDF schema
  * from a resource.
  */
trait SchemaParserModule extends RDFModule with RDFOpsModule
  with RDFXMLReaderModule
  with SparqlOpsModule with SparqlGraphModule {
  def fromResource(path: String): Try[SchemaParser] =
    Option(getClass.getResourceAsStream(path)).fold[Try[SchemaParser]](
      Failure(new RuntimeException(s"Invalid resource path: $path."))
    ) { stream =>
      rdfXMLReader.read(stream, "").map { graph =>
        new SchemaParser(graph) 
      }
    }

  /** A simple helper class that reads properties and classes from an RDF graph
    * representing an RDF schema.
    */
  class SchemaParser(graph: Rdf#Graph) {
    import ops._
    import sparqlOps._
    import sparqlGraph.sparqlEngineSyntax._

    def toName(baseUri: Rdf#URI)(uri: Rdf#URI): String = {
      val rel = baseUri.relativize(uri).getString
      if (rel.charAt(0) == '#') rel.substring(1) else rel
    }

    def getUris(
      query: String,
      field: String,
      mappings: Map[String, Rdf#URI]
    ): List[Rdf#URI] = {
      val result = for {
        select <- parseSelect(query)
        solutions <- graph.executeSelect(select, mappings)
      } yield solutions.toIterable.map { solution =>
        solution(field).flatMap(_.as[Rdf#URI])
      }.collect {
        case Success(name) => name
      }.toList
  
      result.getOrElse(Nil)
    }

    def inferBaseUri: Option[Rdf#URI] = {
      val query = """
        PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        SELECT DISTINCT ?definer WHERE { {
          ?class rdfs:isDefinedBy ?definer .
          ?class a rdfs:Class
        } UNION {
          ?property rdfs:isDefinedBy ?definer .
          ?property a rdf:Property
        } }
      """

      getUris(query, "definer", Map.empty) match {
        case List(definer) => Some(definer)
        /** If we can't find a candidate or have multiple candidates we probably
          * just want to bail out.
          */
        case _ => None
      }
    }

    def getNames(query: String, baseUri: Rdf#URI): List[String] =
      getUris(query, "uri", Map("base" -> baseUri)).map(toName(baseUri))

    def classNames(baseUri: Rdf#URI): List[String] = getNames(
      """
        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        SELECT DISTINCT ?uri WHERE {
          ?uri a rdfs:Class .
          ?uri rdfs:isDefinedBy ?base
        }
      """,
      baseUri
    )

    def propertyNames(baseUri: Rdf#URI): List[String] = getNames(
      """
        PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        SELECT DISTINCT ?uri WHERE {
          ?uri a rdf:Property .
          ?uri rdfs:isDefinedBy ?base
        }
      """,
      baseUri
    )
  }
}
