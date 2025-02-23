package com.kafka.application

import cats.effect.{ExitCode, IO, IOApp}
import com.kafka.application.service.JsonReader
import java.util.Properties
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import cats.effect.kernel.Resource
import cats.implicits.*
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import io.circe.Json

object Main extends IOApp {
  private val Filename = "random-people-data.json"
  private val TopicName = "random-people-data"
  private val bootstrapServers = "localhost:9092"

  given logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] = {
    val program = for {
      jsonList <- JsonReader.readFileFromResources(Filename)
      produceRecords = jsonList.flatMap(makeProducerRecord)
      producer <- makeKafkaProducer()
      _ <- produceRecords
        .traverse(record => IO.delay(producer.send(record)))
        .toResource
      _ <- logger.info("All data loaded into Kafka").toResource
    } yield ()

    program.use(_ => IO(ExitCode.Success))
  }

  private def makeProducerRecord(
      json: Json
  ): Option[ProducerRecord[String, String]] = {
    val key = json.hcursor.downField("_id").as[String]

    key match
      case Left(_) => None
      case Right(id) =>
        Some(
          new ProducerRecord[String, String](
            TopicName,
            id,
            json.noSpaces
          )
        )
  }

  private def makeKafkaProducer() = {
    val properties: Properties = new Properties()
    properties.setProperty(
      ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
      bootstrapServers
    )
    properties.setProperty(
      ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
      classOf[StringSerializer].getName
    )
    properties.setProperty(
      ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
      classOf[StringSerializer].getName
    )
    Resource.make(IO.delay(new KafkaProducer[String, String](properties)))(
      producer =>
        IO.blocking {
          producer.flush()
          producer.close()
        }
    )
  }
}
