addSbtPlugin("me.lessis" % "bintray-sbt" % "0.1.2")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.2.5")
libraryDependencies <+= sbtVersion ("org.scala-sbt" % "scripted-plugin" % _)
