package typeproviders.rdfs

import org.w3.banana._
import org.w3.banana.diesel._
import org.w3.banana.syntax._
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration
import scala.util.{ Failure, Success, Try }

/** Provides a constructor that reads an RDF graph representing an RDF schema
  * from a resource.
  */
object SchemaParser {
  def fromResource[Rdf <: RDF](path: String)(implicit
    ops: RDFOps[Rdf],
    reader: RDFReader[Rdf, RDFXML],
    sparqlOps: SparqlOps[Rdf],
    sparqlGraph: SparqlGraph[Rdf]
  ): Try[SchemaParser[Rdf]] =
    Option(getClass.getResourceAsStream(path)).fold[Try[SchemaParser[Rdf]]](
      Failure(new RuntimeException(s"Invalid resource path: $path."))
    ) { stream =>
      reader.read(stream, "").map { graph =>
        new SchemaParser[Rdf](graph) 
      }
    }
}

/** A simple helper class that reads properties and classes from an RDF graph
  * representing an RDF schema.
  */
class SchemaParser[Rdf <: RDF](graph: Rdf#Graph)(implicit
  val ops: RDFOps[Rdf],
  val sparqlOps: SparqlOps[Rdf],
  val sparqlGraph: SparqlGraph[Rdf] 
) {
  import ops._
  import sparqlOps._

  import scala.concurrent.ExecutionContext.Implicits.global

  val engine = sparqlGraph(graph)

  def toName(baseUri: Rdf#URI)(uri: Rdf#URI): String = {
    val relativized = baseUri.relativize(uri).getString
    if (relativized.charAt(0) == '#') relativized.substring(1) else relativized
  }

  def getUris(
    query: String,
    field: String,
    mappings: Map[String, Rdf#URI]
  ): List[Rdf#URI] = {
    val results = engine.executeSelect(SelectQuery(query), mappings).map(
      _.toIterable.map { solution =>
        solution(field).flatMap(_.as[Rdf#URI])
      }.collect {
        case Success(name) => name
      }.toList
    )
  
    /** Banana's asynchronous API for Sparql queries is nice, but we don't
      * really need it here.
      */
    Await.result(results, Duration.Inf)
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

