# Kafka Web App

## Loading data to Kafka
1. Start kafka 
```shell
docker-compose up -d
```

2. Create a topic
```shell
docker exec -it <kafka-container-name> kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --replication-factor 1 \
  --partitions 3 \
  --topic random-people-data \
  --config cleanup.policy=delete
``` 

3. Run Main prgram in kafka-data-loader 
```shell
sbt kafkaDataLoader/run
```

4. Check data is in topic
```
docker exec -it <kafka-container-name> kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic random-people-data \
  --from-beginning
```

## Web app
This application is a web service built with Scala 3, http4s and Cats Effect 3 that consumes messages from Kafka. 
It provides an API for retrieving messages from a Kafka topic, starting from a specified offset (or from the beginning), with the option to define the number of messages to fetch. If not specified, the API defaults to fetching a single message.

The application uses:
- http4s for handling HTTP requests
- Cats Effect 3
- Kafka's assign and seek methods to manually control message consumption

Currently, the API exposes:
- GET /topic/[topic_name]/[offset]?count=N - Fetches messages from Kafka starting from the specified offset.

To start up the app
```shell
sbt kafkaWebApp/run
```

### Missing Features Compared to a Production-Ready App
To further enhance the application and make it production-ready, the following would be necessary:  

### **1. Unit Tests**  
- Implement thorough unit tests to validate the functionality of core components. Specifically, unit tests are needed for AppRoutes to:
  - Verify that exceptions are correctly mapped to the right HTTP error responses.
  - Ensure the correct method is called based on the presence or absence of an offset:
    - Calls consumeFromBeginning when no offset is provided.
    - Calls consumeFromOffset when an offset is provided.
  - Validate that a valid JSON response is returned with consumed messages.
  - Ensure errors are handled gracefully, returning appropriate HTTP responses.

### **2. Integration Tests**  
Set up integration tests using Docker Compose to verify functionality when integrating with Kafka. Specifically, KafkaConsumerService needs to be tested against a real Kafka instance since it depends on an actual Kafka consumer.
Test different scenarios to verify behavior from various offsets, including:
- Consuming from the beginning of the topic.
- Consuming from a specified offset.
- Handling cases where the offset is out of range.
- Ensuring messages are properly deserialized and returned as expected.
- Verifying error handling when Kafka is unreachable or the topic does not exist

### **3. End-To-End Tests** 
Implement end-to-end (E2E) tests by running the full application inside Docker Compose along with a Kafka instance.
These tests should verify that AppRoutes properly integrates with KafkaConsumerService and interacts with Kafka correctly.
Scenarios to cover:
- Sending messages to Kafka and verifying they can be retrieved through the HTTP API.
- Ensuring the API correctly returns messages when requested from different offsets.

### **4. Robust Error Handling**  
- Raise different exceptions based on different scenarios and map them to the right http error response.  
- Currently, the application defaults to an **Internal Server Error** in case of failures.  

### **5. Data Validation**  
- Schema registry to have defined schema between producer and consumer

### **6. Retry Logic**  
- Implement retries with exponential backoff to handle transient failures gracefully.  

### **7. Timeout Configuration**  
- Define appropriate request and connection timeouts to prevent hanging requests.  

### **8. Monitoring & Observability**  
- Improve logging and instrument the application for distributed tracing and metrics.  

### **9. Configuration Management**  
- Externalize configurations for Kafka bootstrap servers and topic's name, enabling support for environment-based settings.  
