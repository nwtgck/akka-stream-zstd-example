import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import io.github.nwtgck.akka_stream_zstd.ZstdFlow

import scala.util.{Failure, Success}

object Main {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("my-system")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    import system.dispatcher

    // Create a simple source
    val source = Source.single(ByteString("hello, hello, hello, hello, hello, hello, world"))

    val fut = source
      // Compress
      .via(ZstdFlow.compress(level = 5))
      // Decompress
      .via(ZstdFlow.decompress())
      // Concatenate
      .runWith(Sink.fold(ByteString.empty)(_ ++ _))


    fut.onComplete{
      case Success(finalByteString) =>
        // NOTE: finalByteString should be the same as the original
        println(s"Final byte string: ${finalByteString}")
        system.terminate()
      case Failure(e) =>
        e.printStackTrace()
        system.terminate()
    }
  }
}
