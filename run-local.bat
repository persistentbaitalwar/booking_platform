@echo off
rem Starts the backend locally (in-memory H2 database).
rem   run-local.bat           with Kafka (starts Kafka and Kafka UI in Docker first)
rem   run-local.bat nokafka   without Kafka
cd /d "%~dp0"
if /I "%1"=="nokafka" goto run
docker compose up -d kafka kafka-ui
echo Waiting 20 seconds for Kafka to become ready...
timeout /t 20 /nobreak >nul
set KAFKA_BOOTSTRAP=localhost:9094
echo Kafka UI: http://localhost:8090
:run
echo Demo sign-in: demo@example.com / Demo@12345 (customer), partner@example.com / Partner@12345 (partner)
echo Swagger UI: http://localhost:8080/swagger-ui.html
echo H2 console: http://localhost:8080/h2-console
if /I "%1"=="nokafka" (
  mvn spring-boot:run
) else (
  mvn spring-boot:run -Dspring-boot.run.profiles=local
)
