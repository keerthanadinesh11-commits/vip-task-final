# Kafka Integration — TaskFlow

## What was added

The **task-service → notification-service** communication was replaced from
synchronous Feign HTTP calls to **asynchronous Kafka messaging**.

### Architecture

```
task-service                         notification-service
    │                                        │
    │  (create/assign/complete task)         │
    │                                        │
    ▼                                        │
NotificationKafkaProducer  ──Kafka──►  NotificationKafkaConsumer
    │                       topic:           │
    │                  task-notifications    ▼
    │                                  NotificationService
    │                                        │
    │                                   MySQL DB
    ▼
  (task saved to DB — no waiting for notification!)
```

### Topic

| Topic               | Partitions | Key        | Value                    |
|---------------------|-----------|------------|--------------------------|
| `task-notifications` | 3         | `userId`   | JSON `NotificationEventDto` |

Keying by `userId` guarantees **ordered delivery per user**.

## Files Changed / Added

### task-service
| File | Change |
|------|--------|
| `pom.xml` | + `spring-kafka` dependency |
| `src/.../kafka/KafkaProducerConfig.java` | **NEW** — Kafka producer bean config |
| `src/.../kafka/KafkaTopicConfig.java` | **NEW** — auto-creates the topic |
| `src/.../kafka/NotificationKafkaProducer.java` | **NEW** — publishes `NotificationDto` events |
| `src/.../service/TaskService.java` | Replaced `NotificationClient` (Feign) with `NotificationKafkaProducer` |
| `src/.../resources/application.yml` | + Kafka bootstrap-servers config |

### notification-service
| File | Change |
|------|--------|
| `pom.xml` | + `spring-kafka` dependency |
| `src/.../kafka/KafkaConsumerConfig.java` | **NEW** — Kafka consumer bean config |
| `src/.../kafka/NotificationKafkaConsumer.java` | **NEW** — listens to topic, persists notifications |
| `src/.../dto/NotificationEventDto.java` | **NEW** — Kafka payload DTO |
| `src/.../resources/application.yml` | + Kafka consumer config |

### Root
| File | Change |
|------|--------|
| `docker-compose.yml` | **NEW** — Zookeeper + Kafka + Kafka UI |

> **Note:** The existing `NotificationClient` (Feign) and
> `NotificationInternalController` are kept for backwards compatibility
> but are no longer called by `TaskService`. You can remove them once
> you've verified Kafka works end-to-end.

## Running Kafka Locally

### 1 — Start Kafka with Docker

```bash
cd taskflow-parent
docker compose up -d
```

This starts:
- **Zookeeper** on port `2181`
- **Kafka broker** on port `9092` (host) / `29092` (container-to-container)
- **Kafka UI** at http://localhost:8090

### 2 — Start the microservices (in order)

```bash
# Terminal 1
cd eureka-server && mvn spring-boot:run

# Terminal 2
cd notification-service && mvn spring-boot:run

# Terminal 3
cd task-service && mvn spring-boot:run
```

### 3 — Test

Create or assign a task via the API. The notification will be:
1. Published to the `task-notifications` Kafka topic by `task-service`
2. Consumed and persisted by `notification-service`

View messages in Kafka UI → http://localhost:8090

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker address |

For Docker-to-Docker communication (e.g. if services are also containerised),
set `KAFKA_BOOTSTRAP_SERVERS=kafka:29092`.

## Benefits Over Feign

| | Feign (before) | Kafka (now) |
|-|----------------|-------------|
| Coupling | Tight (sync HTTP) | Loose (async) |
| Availability | Both services must be up | notification-service can be down |
| Task latency | Adds network RTT | No added latency |
| Retry | Manual | Kafka consumer retries automatically |
| Observability | None | Kafka UI, consumer lag metrics |
