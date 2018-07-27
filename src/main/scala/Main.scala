import java.io.File
import java.nio.file.Path

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, IOResult}
import akka.stream.scaladsl.{Compression, FileIO, Flow, Sink, Source}
import akka.util.ByteString
import io.github.nwtgck.akka_stream_zstd.ZstdFlow

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object Main {
  def main(args: Array[String]): Unit = {
    example1()

    runBenchMark()
  }

  def example1(): Unit = {
    implicit val system: ActorSystem = ActorSystem("my-system")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    import system.dispatcher

    val filePath: Path = new File("out.zst").toPath

    // Create a simple source
    val source = Source.single(ByteString("hello, hello, hello, hello, hello, hello, world"))

    // Future of store
    val storingFut = source
      // Compress
      .via(ZstdFlow.compress())
      // Store to file
      .runWith(FileIO.toPath(filePath))

    // Wait for store
    Await.ready(storingFut, 3.seconds)


    // Future of restoring
    val restoringFut = FileIO.fromPath(filePath)
      // Decompress
      .via(ZstdFlow.decompress())
      // Concatenate into one ByteString
      .runWith(Sink.fold(ByteString.empty)(_ ++ _))
      // Convert ByteString to String
      .map(_.utf8String)

    // Wait for restring and get
    val decompressedStr: String = Await.result(restoringFut, 3.seconds)

    println(s"Decompressed: '${decompressedStr}'")
    system.terminate()
  }

  // Calculate and print simple benchmark
  def printSimpleBenchmark(name: String, times: Int = 1)(f: => Unit): Unit = {
    println(s"================= ${name} =================")
    val passedTimes: Seq[Long] =
      for(_ <- 1 to times) yield {
        val start = System.currentTimeMillis()
        f
        val end = System.currentTimeMillis()
        end - start
      }
    println(s"Time: ${passedTimes.sum.toDouble / times} msec")

  }

  def runBenchMark(): Unit = {
    implicit val system: ActorSystem = ActorSystem("my-system")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    import system.dispatcher

    // Compress
    def compressAndStoreFut(srcFilePath: String, dstFilePath: String, compressFlow: Flow[ByteString, ByteString, _]): Future[_] = {
      // Future of store
      FileIO.fromPath(new File(srcFilePath).toPath)
        // Compress
        .via(compressFlow)
        // Store to file
        .runWith(FileIO.toPath(new File(dstFilePath).toPath))
    }

    // Decompress
    def decompressAndStoreFut(srcFilePath: String, dstFilePath:String, decompressFlow: Flow[ByteString, ByteString, _]): Future[_] = {
      FileIO.fromPath(new File(srcFilePath).toPath)
        // Decompress
        .via(decompressFlow)
        // Store to file
        .runWith(FileIO.toPath(new File(dstFilePath).toPath))
    }


    val timeout = 3.seconds

    printSimpleBenchmark("zstd compress", times = 10) {
      // Wait for store
      Await.ready(compressAndStoreFut(
        srcFilePath = "./files/dickens",
        dstFilePath = "./files/out.zst",
        compressFlow = ZstdFlow.compress()
      ), timeout)
    }

    printSimpleBenchmark("zstd decompress", times = 10) {
      // Wait for restring
      Await.ready(decompressAndStoreFut(
        srcFilePath    = "./files/out.zst",
        dstFilePath    = "./files/decompressed",
        decompressFlow = ZstdFlow.decompress()
      ), timeout)
    }

    printSimpleBenchmark("zlib compress", times = 10) {
      // Wait for store
      Await.ready(compressAndStoreFut(
        srcFilePath = "./files/dickens",
        dstFilePath = "./files/out.gzip",
        compressFlow = Compression.gzip
      ), timeout)
    }

    printSimpleBenchmark("zlib decompress", times = 10) {
      // Wait for restring
      Await.ready(decompressAndStoreFut(
        srcFilePath    = "./files/out.gzip",
        dstFilePath    = "./files/decompressed",
        decompressFlow = Compression.gunzip()
      ), timeout)
    }

    system.terminate()
  }
}
