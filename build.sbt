organization := "no.arktekk"

scalaVersion := "2.10.4"

crossScalaVersions := Seq("2.10.4", "2.11.2", "2.12.5")

scalacOptions := Seq("-deprecation", "-language:_")

description := "anti-xml"

name := "anti-xml"

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.11.6" % "test",
  "org.specs2" %% "specs2" % "2.4.17" % "test"
)

libraryDependencies <++= (scalaBinaryVersion) { (sv) => sv match {
    case "2.11" | "2.12" => Seq("org.scala-lang.modules" %% "scala-xml" % "1.1.0" % "provided")
    case _ => Nil
  }
}
      
initialCommands in console := """import com.codecommit.antixml._"""

scalacOptions in Compile in doc <++= (unmanagedSourceDirectories in Compile) map {
  (usd) =>
    val scalaSrc: File = (usd filter {
      _.toString endsWith "scala"
    }).head
    Seq(
      "-sourcepath", scalaSrc.toString,
      "-doc-source-url", "https://github.com/arktekk/anti-xml/tree/master/src/main/scala€{FILE_PATH}.scala"
    )
}
