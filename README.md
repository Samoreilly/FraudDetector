# Real-Time Fraud Detection Service

# Overview
This project implements a real-time fraud detection backend service using Spring Boot, Kafka, and Redis. It processes user transactions asynchronously via Kafka, caches recent transactions in Redis to enable temporal validation, and provides real-time status updates to clients using Server-Sent Events (SSE).

# Features

Asynchronous transaction processing via Kafka topics.
Redis based caching of recent user transactions with automatic expiry.
Transaction spam prevention by allowing 1 transaction every 5 seconds.
Rate limiting based by client IP and session.
Real-time transaction status updates via Kafka and SSE.
Session management and client IP tracking for enhanced fraud prevention.

# Tech stack
Java 17+ 
Spring Boot
Apache Kafka
Redis via Spring Data Redis
Jackson JSON serialization
Server side events for realtime updates

# Prerequisites
Java Development Kit (JDK) 17 or later
Apache Kafka (https://kafka.apache.org/downloads)
Redis Server (sudo apt install redis-server) or through App center if your on Ubuntu Linux
Maven or Gradle build tool

# Kafka setup

- Go into your kafka directory
cd ~/kafka/kafka_2.13-3.9.1
- Generate a UUID
bin/kafka-storage.sh random-uuid

- Paste your UUID here
bin/kafka-storage.sh format -t ..YOUR UUID HERE.. -c config/kraft/server.properties

- Start server
bin/kafka-server-start.sh config/kraft/server.properties

# Redis setup

Run redis-cli in bash

# Application.properties

spring.application.name=FraudDetector
spring.kafka.bootstrap-servers=localhost:9092
spring.cache.type=redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
logging.level.org.springframework.cache=DEBUG
logging.level.org.springframework.data.redis.cache=DEBUG

spring.jackson.serialization.write-dates-as-timestamps=false

# How It Works
- Client submits transactions via /api/tr.
- The transaction is sent to Kafka "transactions" topic asynchronously.
- TransactionService consumes transactions from Kafka, retrieves recent transactions from Redis, and validates timing to prevent rapid repeats.
- Status updates are sent back on the "out-transactions" Kafka topic keyed by session ID.
- Clients receive live updates via SSE from /api/streams/results.
- Redis caches recent transactions per user/session with a 10-minute expiry that refreshes on each new transaction.
- ContributingContributions are welcome.
- Please fork the repository and submit pull requests with clear descriptions.

