import sbt._
import Keys._
import com.twitter.sbt._

object BlueScaleBuild extends Build {
    import BuildSettings._
    /* 
    val bsproject =
        Project("BlueScale",
        file ("."),
        settings = (PackageDist.newSettings ++ buildSettings ))
    */
    val bsproject = 
        Project("BlueScale",
        file(".")) settings(StandardProject.newSettings : _*) settings(buildSettings : _*)
    
}

object BuildSettings {
    val buildName = "BlueScaleServer"

    val buildOrganization = "BlueScale"

    val buildVersion = ".8"

    val buildScalaVersion = "2.9.1"

    val parallelExection = false
    
    val scalatest =  "org.scalatest" %% "scalatest" % "1.8" % "test"


    val dependencies = Seq (
        scalatest  )

  val buildSettings = Defaults.defaultSettings ++ Seq (
    name         := buildName,
    organization := buildOrganization,
    version      := buildVersion,
    scalaVersion := buildScalaVersion,
    libraryDependencies ++= dependencies,
    parallelExecution := false
  )

}
