# Running Apache Kafka locally

The application publishes booking events to the topic `ticket.events` and consumes them again (notifications).
Kafka is optional: with `app.kafka.enabled=false` (the default) the app needs no broker and events wait in the
`outbox_event` table until Kafka is switched on.

## Option A: Docker (recommended)

```bash
docker compose up -d kafka kafka-ui        # broker for your machine on localhost:9094, UI on http://localhost:8090
mvn spring-boot:run -Dspring-boot.run.profiles=local
```
or simply `./run-local.sh` (Windows: `run-local.bat`), which does both and waits until Kafka is ready.

Why two ports: containers talk to Kafka on `kafka:9092`, your machine on `localhost:9094` (see `docker-compose.yml`).

## Option B: Kafka installed on your machine (no Docker)

1. Download Kafka 3.8 or newer from https://kafka.apache.org/downloads and unpack it.
2. Start it in KRaft mode (no ZooKeeper):
   ```bash
   KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
   bin/kafka-storage.sh format -t "$KAFKA_CLUSTER_ID" -c config/kraft/server.properties
   bin/kafka-server-start.sh config/kraft/server.properties        # listens on localhost:9092
   ```
3. Run the application against it:
   ```bash
   KAFKA_BOOTSTRAP=localhost:9092 mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

## Check that it works

1. Open Swagger UI (http://localhost:8080/swagger-ui.html), call `POST /api/v1/auth/login` with
   `{"email":"demo@example.com","password":"Demo@12345"}`, click **Authorize** and paste the token.
2. Call `POST /api/v1/bookings` (customer id from the login response, a show id from
   `GET /api/v1/movies/{id}/theatres`), then `POST /api/v1/payments/simulator/{bookingId}/pay`.
3. Watch topic `ticket.events` in Kafka UI (http://localhost:8090) or on the command line:
   ```bash
   docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 \
     --topic ticket.events --from-beginning
   ```
4. `GET /api/v1/customers/{id}/notifications` shows the message created by the consumer.
5. A message that fails three times lands on `ticket.events.DLT`.

## Settings (all can be overridden with environment variables)

| Property | Environment variable | Default |
|---|---|---|
| `app.kafka.enabled` | `APP_KAFKA_ENABLED` | `false` (the `local` profile sets `true`) |
| `spring.kafka.bootstrap-servers` | `KAFKA_BOOTSTRAP` | `localhost:9092` in the base config, `localhost:9094` in the `local` profile |
| `app.kafka.replicas` | `KAFKA_REPLICAS` | `1` (use `3` on a cluster) |
| `app.kafka.relay-delay-ms` | `APP_KAFKA_RELAY_DELAY_MS` | `1000` |

Troubleshooting: if the app logs "Could not publish outbox event", the broker is not reachable on the configured
address; bookings still work and the events are sent as soon as Kafka is back. To start from scratch:
`docker compose down -v`.
