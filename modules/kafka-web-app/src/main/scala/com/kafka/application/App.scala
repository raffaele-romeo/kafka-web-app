package com.kafka.application

import cats.effect.IOApp
import cats.effect.ExitCode
import cats.effect.IO
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import cats.effect.kernel.Resource
import org.http4s.ember.server.EmberServerBuilder
import com.kafka.application.service.KafkaConsumerServiceLive
import com.kafka.application.api.AppRoutes
import com.comcast.ip4s.*
import org.http4s.HttpApp

object App extends IOApp {
  private val bootstrapServers = "localhost:9092"

  given LoggerFactory[IO] = Slf4jFactory.create[IO]

  private val logger = LoggerFactory[IO].getLogger

  override def run(args: List[String]): IO[ExitCode] = {
    val kafkaConsumerService = new KafkaConsumerServiceLive(
      bootstrapServers: String
    )
    val httpApp = AppRoutes.routes(kafkaConsumerService).orNotFound

    logger.info("Starting the app") *> server(httpApp).use(_ => IO.never)
  }

  private def server(
      httpApp: HttpApp[IO]
  ): Resource[IO, org.http4s.server.Server] =
    EmberServerBuilder
      .default[IO]
      .withPort(port"8080")
      .withHttpApp(httpApp)
      .build
}
