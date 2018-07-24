name := "akka-stream-zstd-example"

version := "0.1"

scalaVersion := "2.11.12"

// Add dependency of `spark-wikipedia-dump-loader` in GitHub
dependsOn(RootProject(uri("https://github.com/nwtgck/akka-stream-zstd.git#0e6eb379c82b914ace31f5f9f2399d58a99f324f")))
