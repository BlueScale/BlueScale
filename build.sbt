name := "BlueScale"

version := ".3"

scalaVersion := "2.9.1"

sourceDirectories in Compile := Seq(file("scala"))

sourceDirectories in Test += new File("test")

libraryDependencies += "com.novocode" % "junit-interface" % "0.7" % "test->default"

parallelExecution in Test := false

