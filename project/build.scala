import sbt._
import Keys._
import com.twitter.sbt._

object BlueScaleBuild extends Build {
    import BuildSettings._

    //I can't believe someone thought this was good syntax!!!!
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

    val netty = "org.jboss.netty" % "netty" % "3.2.2.Final" % "compile"

    val slf4j = "org.slf4j" % "slf4j-api" % "1.6.1" % "compile"

    val jbossresolver = Seq("respository.jboss.org" at "http://repository.jboss.org/nexus/content/group/public")

    val dependencies = Seq (
        scalatest, netty, slf4j  )

  val buildSettings = Defaults.defaultSettings ++ Seq (
    resolvers    := jbossresolver,
    name         := buildName,
    organization := buildOrganization,
    version      := buildVersion,
    scalaVersion := buildScalaVersion,
    libraryDependencies ++= dependencies,
    parallelExecution := false
  )

}

/*
class MySQLEventEmitter(info:ProjectInfo) extends DefaultProject(info){
	val jbossRepository        = "Jboss Repository" at "http://repository.jboss.org/nexus/content/groups/public"
	val jbossRepository2       = "jboss Maven2 repo" at "http://repository.jboss.com/maven2"
	...

	val netty = "org.jboss.netty" % "netty" % "3.2.2.Final"
	val scalatest = "org.scalatest" % "scalatest" % "1.2" % "test"
	...
}
	
*/
