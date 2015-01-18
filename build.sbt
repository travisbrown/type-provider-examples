lazy val rdfsCommon = project.in(file("rdfs-common"))
  .settings(moduleName := "rdfs-common")
  .settings(buildSettings: _*)
  .settings(libraryDependencies += "org.w3" %% "banana_jvm" % "0.7.1")

lazy val rdfsPublic = project.in(file("rdfs-public"))
  .settings(moduleName := "rdfs-public")
  .settings(buildSettings: _*)
  .settings(macroProjectSettings: _*)
  .settings(libraryDependencies += sesameDependency)
  .dependsOn(rdfsCommon)

lazy val rdfsAnonymous = project.in(file("rdfs-anonymous"))
  .settings(moduleName := "rdfs-anonymous")
  .settings(buildSettings: _*)
  .settings(macroProjectSettings: _*)
  .settings(libraryDependencies += sesameDependency)
  .dependsOn(rdfsCommon)

lazy val sesameDependency = "org.w3" %% "sesame" % "0.7.1"
lazy val paradiseDependency =
  "org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full

lazy val rdfs = project.in(file("rdfs"))
  .settings(buildSettings: _*)
  .settings(
    /** See this Stack Overflow question and answer for some discussion of
      * why we need this line: http://stackoverflow.com/q/17134244/334519
      */
    unmanagedClasspath in Compile <++= unmanagedResources in Compile
  )
  .dependsOn(rdfsPublic, rdfsAnonymous)

lazy val buildSettings = Seq(
  version := "0.0.0-SNAPSHOT",
  scalaVersion := "2.11.5",
  crossScalaVersions := Seq("2.10.4", "2.11.5"),
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-unchecked"
  ),
  resolvers ++= Seq(
    Resolver.sonatypeRepo("snapshots"),
    Resolver.sonatypeRepo("releases")
  ),

  /** We need the Macro Paradise plugin both to support the macro
    * annotations used in the public type provider implementation and to
    * allow us to use quasiquotes in both implementations. The anonymous
    * type providers could easily (although much less concisely) be
    * implemented without the plugin.
    */
  addCompilerPlugin(paradiseDependency)
)

lazy val macroProjectSettings = Seq(
  libraryDependencies <+= (scalaVersion)(
    "org.scala-lang" % "scala-reflect" % _
  ),
  libraryDependencies ++= (
    if (scalaVersion.value.startsWith("2.10")) List(paradiseDependency) else Nil
  )
)
