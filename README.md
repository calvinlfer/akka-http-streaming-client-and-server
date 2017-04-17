# Akka HTTP Examples on Streaming Requests and Responses

This project is a showcase of 2 simple examples on how to perform streaming requests and responses with Akka HTTP. 
This does not make use of [Entity Streaming Support](http://doc.akka.io/docs/akka-http/10.0.5/scala/http/routing-dsl/source-streaming-support.html#source-streaming)
and interacts with ByteStrings and hand-parses text using `Framing.delimiter` in order to go from a `ByteString` into 
something that is easier to work with (like `String`).

There are two examples: 

- A GET endpoint that returns a streaming text response (`/batch2`) 
- A POST endpoint that requires streaming text as input and produces streaming text as output, it takes a string as 
input and produces the UUID equivalent of the string as output (`/batch`)

The client shows how to interact with both endpoints. It pulls some example strings from a file and sends them through 
to the server and prints out the UUIDs.

If you want to store the data in a file, you can do something like: 
```scala
import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest}
import akka.stream.scaladsl.{FileIO, Framing, Source}
import akka.stream.{ActorMaterializer, IOResult, ThrottleMode}
import akka.util.ByteString

import scala.concurrent.Future
import scala.concurrent.duration._

  implicit val system = ActorSystem("example")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val fileSource: Source[ByteString, Future[IOResult]] = FileIO.fromPath(Paths.get("example.txt"))

  // Read data from file -> API -> output to file
  val placeOutputInFile = Http().singleRequest(
    HttpRequest(method = POST, uri = "http://localhost:9999/batch", entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, fileSource)))
    .flatMap(response =>
        response.entity.dataBytes
          .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 256))
          .runWith(FileIO.toPath(Paths.get("output.txt"))))
    .foreach(_ => println("finished writing data to file"))
```

Please note that you should check the status of the response before attempting to interact with the body.

Ensure you use large enough frame-lengths when parsing data otherwise you end up with scenarios where the data won't
stream correctly.


If you are wondering how to attempt the same style of streaming endpoint with Play Framework, it looks like this:
```scala
import akka.stream.scaladsl.{Flow, Framing}
import akka.util.ByteString
import play.api.libs.streams.Accumulator
import play.api.mvc._

class StreamsController extends Controller {
  private def streamParser = BodyParser { _ =>
    def logFlow[A]: Flow[A, A, NotUsed] = Flow[A].map { x =>
      println(x)
      x
    }

    Accumulator.source[ByteString].map(sourceOfByteString =>
      Right(sourceOfByteString.via(Framing.delimiter(ByteString("\n"), 128)).map(_.utf8String.trim).via(logFlow))
    )
  }

 // expose this via the routes
  def batchConversion = Action(streamParser) { req =>
    Ok.chunked {
      req.body.map(each => UUID.nameUUIDFromBytes(each.getBytes).toString + "\n")
    }
  }
}
```

Writing an equivalent client in Node.JS looks like this (provided you use the `request` package):
```ecmascript 6
const fs = require('fs');
const request = require('request');

fs.createReadStream('example.txt')
    .pipe(request.post('http://localhost:9000/batch'))
    .pipe(process.stdout);
```

And if you want to write to a file (`output.txt`)
```ecmascript 6
const fs = require('fs');
const request = require('request');

fs.createReadStream('example.txt')
    .pipe(request.post('http://localhost:9000/batch'))
    .pipe(fs.createWriteStream('output.txt'));
```
