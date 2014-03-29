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
  def fromResource[Rdf <: RDF](path: String, base: String)(implicit
    ops: RDFOps[Rdf],
    reader: RDFReader[Rdf, RDFXML],
    sparqlOps: SparqlOps[Rdf],
    sparqlGraph: SparqlGraph[Rdf]
  ): Try[SchemaParser[Rdf]] =
    Option(getClass.getResourceAsStream(path)).fold[Try[SchemaParser[Rdf]]](
      Failure(new RuntimeException(s"Invalid resource path: $path."))
    ) { stream =>
      reader.read(stream, base).map { graph =>
        new SchemaParser[Rdf](graph, ops.URI(base)) 
      }
    }
}

/** A simple helper class that reads properties and classes from an RDF graph
  * representing an RDF schema.
  */
class SchemaParser[Rdf <: RDF](graph: Rdf#Graph, baseUri: Rdf#URI)(implicit
  val ops: RDFOps[Rdf],
  val sparqlOps: SparqlOps[Rdf],
  val sparqlGraph: SparqlGraph[Rdf] 
) {
  import ops._
  import sparqlOps._

  import scala.concurrent.ExecutionContext.Implicits.global

  val engine = sparqlGraph(graph)

  def toName(uri: Rdf#URI): String = {
    val relativized = baseUri.relativize(uri).getString
    if (relativized.charAt(0) == '#') relativized.substring(1) else relativized
  }

  def getNames(query: String): Future[List[String]] =
    engine.executeSelect(SelectQuery(query)).map(
      _.toIterable.map { solution =>
        solution("uri").flatMap(_.as[Rdf#URI].map(toName))
      }.collect {
        case Success(name) => name
      }.toList
    )

  /** Banana's asynchronous API for Sparql queries is nice, but we don't
    * really need it here.
    */
  def getNamesBlocking(query: String): List[String] =
    Await.result(getNames(query), Duration.Inf)

  def classNames: List[String] = getNamesBlocking(
    """
      PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
      SELECT DISTINCT ?uri WHERE {
        ?uri a rdfs:Class
      }
    """
  )

  def propertyNames: List[String] = getNamesBlocking(
    """
      PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
      SELECT DISTINCT ?uri WHERE {
        ?uri a rdf:Property
      }
    """
  )
}

