lazy val sparkVersion = "2.2.0"
lazy val framelessVersion = "0.4.0"
lazy val scalaCheckVersion = "1.13.+"

lazy val root = project
  .in(file("."))
  .settings(name := "minimalExample")
  .settings(minimalExampleSettings: _*)
  .settings(warnUnusedImport: _*)
  .settings(commonAssemblyMergeStrategy: _*)
  .settings(commonAssemblyShadeRules: _*)
  .settings(minimalExampleREPL: _*)

lazy val minimalExampleSettings = Seq(
  organization := "org.example",
  scalaVersion := "2.11.11-bin-typelevel-4",
  scalacOptions ++= commonScalacOptions,
  scalaOrganization := "org.typelevel",
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oDF"),
  libraryDependencies ++= Seq(
    "org.typelevel" %% "frameless-dataset" % framelessVersion,
    "org.apache.spark" %% "spark-core" % sparkVersion,
    "org.apache.spark" %% "spark-sql" % sparkVersion,
    "org.scalacheck" %% "scalacheck" % scalaCheckVersion
  ),
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots"),
    "apache-snapshots" at "http://repository.apache.org/snapshots/"
  ),
  fork in Test := false,
  parallelExecution in Test := false,
  unmanagedClasspath in Compile ++= (unmanagedResources in Compile).value,
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3"),
  addCompilerPlugin("org.scalamacros" %% "paradise" % "2.1.0" cross CrossVersion.patch)
)

lazy val commonScalacOptions = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint:-missing-interpolator,_",
  "-Yinline-warnings",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ypartial-unification",
  "-language:existentials",
  "-language:experimental.macros",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-Xfuture"
)

lazy val warnUnusedImport = Seq(
  scalacOptions ++= Seq("-Ywarn-unused-import"),
  scalacOptions in (Compile, console) ~= {
    _.filterNot("-Ywarn-unused-import" == _)
  },
  scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value
)

lazy val commonAssemblyMergeStrategy = Seq(
  assemblyMergeStrategy in assembly := {
    case m if m.toLowerCase.endsWith("manifest.mf") => MergeStrategy.discard
    case m if m.startsWith("META-INF") => MergeStrategy.discard
    case PathList("org", "aopalliance", xs @ _ *) => MergeStrategy.last
    case PathList("javax", "inject", xs @ _ *) => MergeStrategy.last
    case PathList("javax", "servlet", xs @ _ *) => MergeStrategy.last
    case PathList("javax", "activation", xs @ _ *) => MergeStrategy.last
    case PathList("org", "apache", xs @ _ *) => MergeStrategy.last
    case PathList("org", "jboss", xs @ _ *) => MergeStrategy.last
    case "about.html" => MergeStrategy.rename
    case "overview.html" => MergeStrategy.rename
    case "application.conf" => MergeStrategy.concat
    case "log4j.properties" => MergeStrategy.concat
    case "plugin.properties" => MergeStrategy.last
    case "reference.conf" => MergeStrategy.concat
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }
)

lazy val commonAssemblyShadeRules = Seq(
  assemblyShadeRules in assembly := Seq(
    ShadeRule.rename("shapeless.**" -> "new_shapeless.@1").inAll
  )
)

def makeColorConsole() = {
  val ansi = System.getProperty("sbt.log.noformat", "false") != "true"
  if (ansi) System.setProperty("scala.color", "true")
}

lazy val minimalExampleREPL = Seq(
  initialize ~= { _ =>
    makeColorConsole()
  },
  initialCommands in console :=
    """
      |import org.apache.spark.{SparkConf, SparkContext}
      |import org.apache.spark.sql.SparkSession
      |import org.apache.spark.sql.functions._
      |
      |val conf = new SparkConf()
      |  .setMaster("local[*]")
      |  .setAppName("minimal-example-repl")
      |  .setAll("spark.ui.enabled" -> "true" ::
      |    "spark.sql.crossJoin.enabled" -> "true" :: Nil)
      |val spark = SparkSession.builder().config(conf).appName("REPL").getOrCreate()
      |
      |import spark.implicits._
      |
      |spark.sparkContext.setLogLevel("WARN")
      |
      |implicit val sqlContext = spark.sqlContext
    """.stripMargin,
  cleanupCommands in console :=
    """
      |spark.stop()
    """.stripMargin
)
