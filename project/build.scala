import sbt._
import Keys._
import sbtbuildinfo.Plugin._
import com.earldouglas.xsbtwebplugin.WebPlugin
import com.earldouglas.xsbtwebplugin.WebPlugin.container
import com.earldouglas.xsbtwebplugin.PluginKeys._

object ValintaTulosServiceBuild extends Build {
  val Organization = "fi.vm.sade"
  val Name = "valinta-tulos-service"
  val Version = "0.1.0-SNAPSHOT"
  val JavaVersion = "1.8"
  val ScalaVersion = "2.11.4"
  val ScalatraVersion = "2.3.0.RC3"
  val TomcatVersion = "7.0.22"
  val SpringVersion = "3.2.9.RELEASE"

  if(!System.getProperty("java.version").startsWith(JavaVersion)) {
    throw new IllegalStateException("Wrong java version (required " + JavaVersion + "): " + System.getProperty("java.version"))
  }
  lazy val project = Project (
    "valinta-tulos-service",
    file("."),
    settings = Defaults.coreDefaultSettings ++ WebPlugin.webSettings ++ buildInfoSettings
      ++ Seq(
      organization := Organization,
      name := Name,
      version := Version,
      scalaVersion := ScalaVersion,
      javacOptions ++= Seq("-source", JavaVersion, "-target", JavaVersion),
      scalacOptions ++= Seq("-target:jvm-1.7", "-deprecation"),
      resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository",
      resolvers += Classpaths.typesafeReleases,
      resolvers += "oph-sade-artifactory-releases" at "http://penaali.hard.ware.fi/artifactory/oph-sade-release-local",
      resolvers += "oph-sade-artifactory-snapshots" at "http://penaali.hard.ware.fi/artifactory/oph-sade-snapshot-local",
      sourceGenerators in Compile <+= buildInfo,
      parallelExecution in Test := false,
      buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
      buildInfoPackage := "fi.vm.sade.valintatulosservice",
      libraryDependencies ++= Seq(
        "org.scalatra" %% "scalatra" % ScalatraVersion,
        "org.scalatra" %% "scalatra-json" % ScalatraVersion,
        "org.scalatra" %% "scalatra-swagger" % ScalatraVersion,
        "org.scalatra" %% "scalatra-specs2" % ScalatraVersion % "test",
        "junit" % "junit" % "4.11" % "test",
        "com.typesafe" % "config" % "1.2.1",
        "javax.servlet" % "javax.servlet-api" % "3.0.1" % "provided",
        "org.apache.tomcat.embed" % "tomcat-embed-core"         % TomcatVersion % "container;test",
        "org.apache.tomcat.embed" % "tomcat-embed-logging-juli" % TomcatVersion % "container;test",
        "org.apache.tomcat.embed" % "tomcat-embed-jasper"       % TomcatVersion % "container;test",
        "org.json4s" %% "json4s-jackson" % "3.2.10",
        "org.json4s" %% "json4s-ext" % "3.2.10",
        "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.4.1",
        "org.scalatra.scalate" %% "scalate-core" % "1.7.0",
        "org.springframework" % "spring-jms" % SpringVersion, // <- patch for spring-core-3.1.3 transitive dep
        "org.springframework" % "spring-core" % SpringVersion,
        "org.springframework" % "spring-context" % SpringVersion,
        "de.flapdoodle.embed" % "de.flapdoodle.embed.mongo" % "1.46.0",
        "org.mongodb" %% "casbah" % "2.7.3",
        "org.mongodb.morphia" % "morphia" % "0.108",
        "fi.vm.sade.sijoittelu" % "sijoittelu-tulos-service" % "1.0-SNAPSHOT" excludeAll(
          ExclusionRule(organization = "org.json4s"),
          ExclusionRule(organization = "org.mongodb"),
          ExclusionRule(organization = "org.mongodb.morphia"),
          ExclusionRule(organization = "com.wordnik"),
          ExclusionRule(organization = "com.sun.jersey"),
          ExclusionRule(organization = "com.thoughtworks.proxytoys"),
          ExclusionRule(organization = "cglib")
        ),
        "fi.vm.sade.valintaperusteet" % "valintaperusteet" % "1.0-SNAPSHOT",
        "fi.vm.sade.sijoittelu" % "sijoittelu-algoritmi-domain" % "1.0-SNAPSHOT",
        "com.google.guava" % "guava" % "15.0",
        "org.scalaj" %% "scalaj-http" % "0.3.15",
        "fi.vm.sade" %% "scala-security" % "0.1.0-SNAPSHOT"
      ),
      artifactName <<= (name in (Compile, packageWar)) { projectName =>
        (config: ScalaVersion, module: ModuleID, artifact: Artifact) =>
          var newName = projectName
          if (module.revision.nonEmpty) {
            newName += "-" + module.revision
          }
          newName + "." + artifact.extension
      },
      artifactPath in (Compile, packageWar) ~= { defaultPath =>
        file("target") / defaultPath.getName
      },
      testOptions in Test := Seq(Tests.Filter(s => s.endsWith("Test") || s.endsWith("Spec"))),
      testOptions in Test += Tests.Argument("junitxml", "console")
    ) ++ container.deploy(
      "/valinta-tulos-service" -> projectRef
    )
  ).settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)
  lazy val projectRef: ProjectReference = project

}
