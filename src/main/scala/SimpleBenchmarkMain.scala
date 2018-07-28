import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Compression, FileIO, Flow}
import akka.util.ByteString
import io.github.nwtgck.akka_stream_zstd.ZstdFlow

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object SimpleBenchmarkMain {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("my-system")
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val timeout = 30.seconds

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

  // Compress
  def compressAndStoreFut(srcFilePath: String, dstFilePath: String, compressFlow: Flow[ByteString, ByteString, _])(implicit materializer: ActorMaterializer): Future[_] = {
    // Future of store
    FileIO.fromPath(Paths.get(srcFilePath))
      // Compress
      .via(compressFlow)
      // Store to file
      .runWith(FileIO.toPath(Paths.get(dstFilePath)))
  }

  // Decompress
  def decompressAndStoreFut(srcFilePath: String, dstFilePath:String, decompressFlow: Flow[ByteString, ByteString, _])(implicit materializer: ActorMaterializer): Future[_] = {
    FileIO.fromPath(Paths.get(srcFilePath))
      // Decompress
      .via(decompressFlow)
      // Store to file
      .runWith(FileIO.toPath(Paths.get(dstFilePath)))
  }
}
