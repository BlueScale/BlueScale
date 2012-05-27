import com.twitter.sbt._

name := "BlueScaleServer"

organization := "BlueScale"

version := ".3"

scalaVersion := "2.9.1"

scalaSource in Compile := new File("src")

scalaSource in Test := new File("test")

libraryDependencies += "com.novocode" % "junit-interface" % "0.7" % "test->default"

parallelExecution in Test := false


