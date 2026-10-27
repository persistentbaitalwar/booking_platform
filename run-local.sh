#!/usr/bin/env bash
# Starts the backend locally (in-memory H2 database).
#   ./run-local.sh           with Kafka (starts Kafka and Kafka UI in Docker first)
#   ./run-local.sh nokafka   without Kafka (events stay in the outbox table)
set -euo pipefail
cd "$(dirname "$0")"

if [ "${1:-kafka}" != "nokafka" ]; then
  command -v docker >/dev/null 2>&1 || { echo "Docker is needed for Kafka. Use './run-local.sh nokafka' to start without it."; exit 1; }
  docker compose up -d kafka kafka-ui
  echo "Waiting for Kafka to become ready..."
  until docker compose exec -T kafka /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092 >/dev/null 2>&1; do sleep 2; done
  export KAFKA_BOOTSTRAP=localhost:9094
  echo "Kafka is ready. Kafka UI: http://localhost:8090"
fi

echo "Demo sign-in: demo@example.com / Demo@12345 (customer), partner@example.com / Partner@12345 (partner), admin@example.com / Admin@123456789"
echo "Swagger UI:  http://localhost:8080/swagger-ui.html"
echo "H2 console:  http://localhost:8080/h2-console  (JDBC URL jdbc:h2:mem:movies, user sa, empty password)"
echo "React UI:    cd frontend && npm install && npm run dev  ->  http://localhost:5173"
if [ "${1:-kafka}" = "nokafka" ]; then
  mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dapp.seed-demo-data=true"
else
  mvn spring-boot:run -Dspring-boot.run.profiles=local
fi
