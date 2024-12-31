FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY build/libs/syslogSender-0.0.1-SNAPSHOT.jar syslogSender.jar

# Download the OpenTelemetry Java agent
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.2.0/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

EXPOSE 8081

# ENTRYPOINT ["java", "-jar", "syslogSender.jar"]
ENTRYPOINT ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "-jar", "syslogSender.jar"]

