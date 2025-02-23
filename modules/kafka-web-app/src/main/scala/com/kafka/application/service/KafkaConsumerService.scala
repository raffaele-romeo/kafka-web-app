package com.kafka.application.service

import cats.effect.*
import org.apache.kafka.clients.consumer.*
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import io.circe.parser.*
import io.circe.generic.auto.*

import java.util.Properties
import scala.jdk.CollectionConverters.*
import com.kafka.application.domain.Person
import scala.collection.mutable
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

trait KafkaConsumerService {
  def consumeFromOffset(
      topicName: String,
      offset: Long,
      numberOfMessagesToRead: Int
  ): IO[List[Person]]

  def consumeFromBeginning(
      topicName: String,
      numberOfMessagesToRead: Int
  ): IO[List[Person]]
}

class KafkaConsumerServiceLive(bootstrapServers: String)
    extends KafkaConsumerService {

  given logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  def consumeFromOffset(
      topicName: String,
      offset: Long,
      numberOfMessagesToRead: Int
  ): IO[List[Person]] = {
    KafkaConsumerServiceLive
      .makeKafkaConsumer(bootstrapServers)
      .use { consumer =>
        for {
          partitions <- getTopicPartitions(consumer, topicName)
          _ <- IO.blocking {
            consumer.assign(partitions.asJava)
            partitions.foreach(partition => consumer.seek(partition, offset))
          }
          result <- take(consumer, numberOfMessagesToRead)
        } yield result
      }
      .onError(e =>
        logger.error(e)(
          s"Error while consuming topic $topicName from offset $offset"
        )
      )
  }

  def consumeFromBeginning(
      topicName: String,
      numberOfMessagesToRead: Int
  ): IO[List[Person]] = {
    KafkaConsumerServiceLive
      .makeKafkaConsumer(bootstrapServers)
      .use { consumer =>
        for {
          partitions <- getTopicPartitions(consumer, topicName)
          _ <- IO.blocking {
            consumer.assign(partitions.asJava)
            consumer.seekToBeginning(partitions.asJava)
          }
          result <- take(consumer, numberOfMessagesToRead)
        } yield result
      }
      .onError(e =>
        logger.error(e)(
          s"Error while consuming topic $topicName from beginning"
        )
      )
  }

  private def take(
      consumer: Consumer[String, String],
      numberOfMessagesToRead: Int
  ): IO[List[Person]] = {
    IO.blocking {
      val messages = mutable.ListBuffer.empty[Person]

      while (messages.size < numberOfMessagesToRead) {
        val records = consumer
          .poll(java.time.Duration.ofMillis(100))
          .asScala
          .toList
          .flatMap(record => decode[Person](record.value).toOption)

        messages.addAll(records)
      }

      messages.take(numberOfMessagesToRead).toList
    }.onError(e =>
      logger.error(e)(
        "Error while polling for messages"
      )
    )
  }

  private def getTopicPartitions(
      consumer: KafkaConsumer[String, String],
      topicName: String
  ): IO[List[TopicPartition]] = {
    logger.info(s"Getting partitions info for topic $topicName") *>
      IO.blocking {
        consumer
          .partitionsFor(topicName)
          .asScala
          .map(info => new TopicPartition(topicName, info.partition()))
          .toList
      }
      .flatMap {
        case Nil =>
          IO.raiseError(new RuntimeException(s"Topic $topicName does not exist"))
        case partitions => IO.pure(partitions)
      }
      .onError(e =>
        logger.error(e)(
          s"Error while getting partitions for topic $topicName"
        )
      )
    }
}

object KafkaConsumerServiceLive {
  private def makeKafkaConsumer(bootstrapServers: String) = {
    val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    props.put(
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
      classOf[StringDeserializer].getName
    )
    props.put(
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
      classOf[StringDeserializer].getName
    )
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")

    Resource.make(IO.blocking(new KafkaConsumer[String, String](props)))(
      consumer => IO.blocking(consumer.close())
    )
  }
}
