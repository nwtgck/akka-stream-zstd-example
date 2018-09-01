name := "akka-stream-zstd-example"

version := "0.1"

scalaVersion := "2.11.12"

// Add dependency of `akka-stream-zstd.git` on GitHub
dependsOn(RootProject(uri("https://github.com/nwtgck/akka-stream-zstd.git#v0.1.1")))
