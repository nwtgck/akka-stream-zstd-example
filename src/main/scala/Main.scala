import java.io.File
import java.nio.file.Path

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Sink, Source}
import akka.util.ByteString
import io.github.nwtgck.akka_stream_zstd.ZstdFlow

import scala.concurrent.Await
import scala.concurrent.duration._

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
}
