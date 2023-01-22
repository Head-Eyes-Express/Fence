import Dependencies._

This / name := "fence"

This / version := "pre-alpha"

This / organization := "gg.fence"

This / scalaVersion := "2.13.10"

This / libraryDependencies ++= akkaLibs ++ testLibs ++ Seq(logback)