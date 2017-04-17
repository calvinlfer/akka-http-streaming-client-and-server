package com.github.calvin
import java.util.UUID

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.stream.scaladsl.{Flow, Framing, Source}
import akka.util.ByteString

import scala.util.{Failure, Success}
import scala.concurrent.duration._

object ExampleServer extends App {
  def logFlow[A]: Flow[A, A, NotUsed] = Flow[A].map { x =>
    println(x)
    x
  }

  val route = path("batch") {
    post {
      entity(as[HttpEntity]) { entity =>
        val resultSource: Source[ByteString, Any] = entity.dataBytes
          .via(Framing.delimiter(ByteString("\n"), 120))
          .map(_.utf8String.trim)
          .map(each => UUID.nameUUIDFromBytes(each.getBytes()).toString + "\n")
          .via(logFlow)
          .map(ByteString(_))

        complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, resultSource))
      }
    }
  }

  val anotherRoute = path("batch2") {
    get {
      complete(
        HttpEntity(
          ContentTypes.`text/plain(UTF-8)`,
          Source(1 to 1000000)
            .throttle(10, 1.second, 10, ThrottleMode.Shaping)
            .map(_.toString + "\n")
            .map(ByteString(_))
        )
      )
    }
  }

  implicit val system = ActorSystem("test-akka-http-server")
  implicit val mat = ActorMaterializer()
  implicit val ec = system.dispatcher

  Http().bindAndHandle(route ~ anotherRoute, "localhost", 9999).onComplete {
    case Success(result) => println(result.localAddress)
    case Failure(error) => println(error)
  }
}
