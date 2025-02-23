package com.kafka.application.api

import com.kafka.application.service.KafkaConsumerService
import org.http4s.HttpRoutes
import cats.effect.IO
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher
import com.kafka.application.domain.People
import io.circe.syntax.*
import org.http4s.circe.*

object CountQueryParamMatcher
    extends OptionalQueryParamDecoderMatcher[Int]("count")

object AppRoutes {
  def routes(kafkaConsumerService: KafkaConsumerService): HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl.*

    given logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

    val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
      case GET -> Root / "topic" / topicName / offsetStr :? CountQueryParamMatcher(
            countOpt
          ) =>
        val offset = offsetStr.toLongOption
        val count = countOpt.getOrElse(1)

        //TODO Server Sent Events for streaming approach
        offset match
          case None =>
            logger.info(
              s"Starting to consume ${topicName} from beginning and retrieve $count messages"
            ) *>
              kafkaConsumerService
                .consumeFromBeginning(topicName, count)
                .flatMap { peopleList =>
                  Ok(People(peopleList).asJson)
                }
          case Some(offset) =>
            logger.info(
              s"Starting to consume ${topicName} from offset $offset and retrieve $count messages"
            ) *>
              kafkaConsumerService
                .consumeFromOffset(topicName, offset, count)
                .flatMap { peopleList =>
                  Ok(People(peopleList).asJson)
                }
    }

    // TODO Handle error
    routes
  }
}
