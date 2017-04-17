package com.github.calvin

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

object ExampleClient extends App {
  implicit val system = ActorSystem("example")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val fileSource: Source[ByteString, Future[IOResult]] = FileIO.fromPath(Paths.get("example.txt"))

  val futureHttpResponse = Http().singleRequest(
    HttpRequest(method = POST, uri = "http://localhost:9999/batch", entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, fileSource))
  ).foreach(response =>
    response.entity.dataBytes
      .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 256))
      .map(_.utf8String.trim)
      .throttle(5, 1.second, 5, ThrottleMode.Shaping)
      .runForeach(println)
      .foreach(_ => system.terminate())
  )

  val futureHttpResponse2 = Http().singleRequest(
    HttpRequest(method = GET, uri = "http://localhost:9999/batch2")
  ).foreach(response =>
    response.entity.dataBytes
      .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 256))
      .map(_.utf8String.trim)
      .runForeach(println)
  )
}
