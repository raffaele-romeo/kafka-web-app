package com.kafka.application.service

import cats.effect.*
import io.circe.generic.auto.*
import io.circe.parser.*
import scala.io.Source
import io.circe.Json
import com.kafka.application.domain.ListJson

object JsonReader {
  def readFileFromResources(filename: String): Resource[IO, List[Json]] = {
    for {
      jsonString <- readJsonFile(filename)
      jsonList <- parseJson(jsonString).toResource
    } yield jsonList
  }

  private def readJsonFile(filename: String): Resource[IO, String] = {
    Resource
      .fromAutoCloseable(IO.blocking(Source.fromResource(filename)))
      .map(source => source.mkString)
  }

  private def parseJson(json: String): IO[List[Json]] =
    IO.fromEither {
      decode[ListJson](json).map(_.ctRoot)
    }
}
