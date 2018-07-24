import java.io.File
import java.nio.file.Path

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Sink, Source}
import akka.util.ByteString
import io.github.nwtgck.akka_stream_zstd.ZstdFlow

import scala.util.{Failure, Success}

object Main {
  def main(args: Array[String]): Unit = {
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


    // Future of restoring
    val restoringFut = FileIO.fromPath(filePath)
      // Decompress
      .via(ZstdFlow.decompress())
      // Concatenate into one ByteString
      .runWith(Sink.fold(ByteString.empty)(_ ++ _))
      // Convert ByteString to String
      .map(_.utf8String)


    (for {
      _   <- storingFut
      str <- restoringFut
    } yield str).onComplete{
      case Success(str) =>
        println(s"Decompressed: '${str}'")
        system.terminate()
      case Failure(e) =>
        e.printStackTrace()
        system.terminate()
    }
  }
}
