lazy val root = project.in(file("."))
  .aggregate(rdfsCommon, rdfsPublic, rdfsAnonymous, rdfs)

lazy val rdfsCommon = project.in(file("rdfs-common"))
  .settings(moduleName := "rdfs-common")
  .settings(buildSettings)
  .settings(libraryDependencies += "org.w3" %% "banana-rdf" % "0.8.2-SNAPSHOT")

lazy val rdfsPublic = project.in(file("rdfs-public"))
  .settings(moduleName := "rdfs-public")
  .settings(buildSettings ++ macroProjectSettings)
  .settings(libraryDependencies += sesameDependency)
  .dependsOn(rdfsCommon)

lazy val rdfsAnonymous = project.in(file("rdfs-anonymous"))
  .settings(moduleName := "rdfs-anonymous")
  .settings(buildSettings ++ macroProjectSettings)
  .settings(libraryDependencies += sesameDependency)
  .dependsOn(rdfsCommon)

lazy val rdfs = project.in(file("rdfs"))
  .settings(buildSettings)
  .settings(
    /** See this Stack Overflow question and answer for some discussion of
      * why we need this line: http://stackoverflow.com/q/17134244/334519
      */
    unmanagedClasspath in Compile <++= unmanagedResources in Compile
  )
  .dependsOn(rdfsPublic, rdfsAnonymous)

lazy val buildSettings = Seq(
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.11.7",
  crossScalaVersions := Seq("2.10.5", "2.11.7"),
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-unchecked"
  ),
  resolvers ++= Seq(
    Resolver.sonatypeRepo("snapshots"),
    Resolver.sonatypeRepo("releases"),
    "bblfish" at "http://bblfish.net/work/repo/snapshots/"
  ),

  /** We need the Macro Paradise plugin both to support the macro
    * annotations used in the public type provider implementation and to
    * allow us to use quasiquotes in both implementations. The anonymous
    * type providers could easily (although much less concisely) be
    * implemented without the plugin.
    */
  addCompilerPlugin(paradiseDependency)
)

lazy val sesameDependency = "org.w3" %% "banana-sesame" % "0.8.2-SNAPSHOT"
lazy val paradiseDependency =
  "org.scalamacros" % "paradise" % "2.1.0-M5" cross CrossVersion.full

lazy val macroProjectSettings = Seq(
  libraryDependencies <+= (scalaVersion)(
    "org.scala-lang" % "scala-reflect" % _
  ),
  libraryDependencies ++= (
    if (scalaVersion.value.startsWith("2.10")) List(paradiseDependency) else Nil
  )
)
