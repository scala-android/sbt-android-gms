# sbt-android-gms

A plugin to process google-services.json for sbt-android

## Usage

add to `project/plugins.sbt`:

    addSbtPlugin("org.scala-android" % "sbt-android-gms" % "0.2")

add to `build.sbt`:

    googleServicesSettings

## Description

Performs the same function as `com.google.gms:google-services:3.0.0`

1. adds `firebase-core` or `play-services-measurement` to your project,
   depending on version `play-services` version used
2. processes `google-services.json` for various api keys and settings and
   generates resources in the correct locations as expected by the Google
   services.
