import sbt._, Keys._

object TypeProviderExamples extends Build {
  import BuildSettings._

  lazy val rdfsCommon: Project = Project(
    "rdfs-common",
    file("rdfs-common"),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= bananaDependencies
    )
  ) 

  lazy val rdfsPublic: Project = Project(
    "rdfs-public",
    file("rdfs-public"),
    settings = macroProjectSettings ++ Seq(
      libraryDependencies ++= sesameDependencies
    )
  ).dependsOn(rdfsCommon)

  lazy val rdfsAnonymous: Project = Project(
    "rdfs-anonymous",
    file("rdfs-anonymous"),
    settings = macroProjectSettings ++ Seq(
      libraryDependencies ++= sesameDependencies
    )
  ).dependsOn(rdfsCommon)

  lazy val rdfs: Project = Project(
    "rdfs",
    file("rdfs"),
    settings = buildSettings
  ).dependsOn(rdfsPublic, rdfsAnonymous)
}

object BuildSettings {
  val paradiseVersion = "2.0.0-M3"
  val paradiseDependency =
    "org.scalamacros" % "paradise" % paradiseVersion cross CrossVersion.full

  val buildSettings = Defaults.defaultSettings ++ Seq(
    version := "0.0.0-SNAPSHOT",
    scalaVersion := "2.10.4",
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked"
    ),
    resolvers ++= Seq(
      Resolver.sonatypeRepo("snapshots"),
      Resolver.sonatypeRepo("releases")
    ),
    addCompilerPlugin(paradiseDependency)
  )

  val bananaDependencies = Seq(
    "org.w3" %% "banana-rdf" % "0.4"
  )

  val sesameDependencies = Seq(
    "org.w3" %% "banana-sesame" % "0.4"
  )

  val macroProjectSettings = buildSettings ++ Seq(
    libraryDependencies <+= (scalaVersion)(
      "org.scala-lang" % "scala-reflect" % _
    ),
    libraryDependencies ++= (
      if (scalaVersion.value.startsWith("2.10")) List(paradiseDependency) else Nil
    )
  )
}

