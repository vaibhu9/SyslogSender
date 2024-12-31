
# Open Telemetry and Syslog Client Integration

## Overview
This Spring Boot application demonstrates how to send Syslog messages using the **CloudBees Syslog Java Client library**. It also integrates with the **OpenTelemetry Collector** as a Syslog receiver to collect and process Syslog messages for observability and monitoring.

---

# Table of Contents

   [Overview](#overview)
1. [What is Syslog?](#1-what-is-syslog)
2. [Syslog Message Types](#2-syslog-message-types)
   - [Facility](#i-facility)
   - [Severity](#ii-severity)
3. [Syslog Message Formats](#3-syslog-message-formats)
   - [Modern Format (RFC 5424)](#31-modern-format-rfc-5424)
   - [Legacy Format (RFC 3164)](#32-legacy-format-rfc-3164)
   - [Comparing RFC 3164 vs RFC 5424](#comparing-rfc-3164-vs-rfc-5424)
4. [OpenTelemetry Collector as a Syslog Receiver](#4-opentelemetry-collector-as-a-syslog-receiver)
5. [Syslog Client Implementation](#5-syslog-client-implementation)
   - [Dependencies and Setup](#1-dependencies-and-setup)
   - [Define Syslog Host and Port](#2-define-syslog-host-and-port)
   - [Sending Syslog Messages](#3-sending-syslog-messages)
   - [Testing the Syslog Message](#4-testing-the-syslog-message)
6. [Docker Integration](#6-docker-integration)
   - [Dockerfile](#dockerfile)
   - [Docker Compose](#docker-compose)
7. [Running the Application](#7-running-the-application)
   - [Build the Application](#1-build-the-application)
   - [Start with Docker Compose](#2-start-with-docker-compose)
   - [Test Sending Syslog Messages](#3-test-sending-syslog-messages)
8. [Sending Syslog with Different Severities](#8-sending-syslog-with-different-severities)
9. [Notes](#notes)
10. [References](#10-references)

---

## 1. What is Syslog?
**Syslog** (System Logging Protocol) is a standard protocol used for message logging. It allows devices and applications to send logs or events to a central server for monitoring and analysis. Syslog is commonly used for:

- System diagnostics and troubleshooting.
- Centralized log management.
- Application performance monitoring.

---

## 2. Syslog Message Types

Syslog messages are categorized using **Facility** and **Severity** values:  

### i) Facility  
The **Facility** value identifies the type of system or process generating the log message. It ranges from **0** to **23**, with predefined categories for **0–15** and locally defined categories for **16–23**.

#### Common Facilities:
| **Facility** | **Code** | **Description**                     |
|--------------|----------|-------------------------------------|
| `kern`       | 0        | Kernel messages                    |
| `user`       | 1        | User-level messages                |
| `mail`       | 2        | Mail system messages               |
| `daemon`     | 3        | System daemons                     |
| `auth`       | 4        | Security/authentication messages   |
| `syslog`     | 5        | Syslog internal messages           |

---

### ii) Severity  
The **Severity** value indicates the importance or urgency of the message. It is represented as a numerical code ranging from **0** (most critical) to **7** (least critical).  

#### Severity Levels:
| **Severity**     | **Code** | **Description**                           |
|-------------------|----------|------------------------------------------|
| **Emergency**     | 0        | System is unusable                      |
| **Alert**         | 1        | Action must be taken immediately        |
| **Critical**      | 2        | Critical conditions                     |
| **Error**         | 3        | Error conditions                        |
| **Warning**       | 4        | Warning conditions                      |
| **Notice**        | 5        | Normal but significant conditions       |
| **Informational** | 6        | Informational messages                  |
| **Debug**         | 7        | Debug-level messages                    |

---

## 3. Syslog Message Formats

Syslog supports two main message formats: RFC 5424 (newer) and RFC 3164 (legacy/BSD format).

### 3.1 Modern Format (RFC 5424):

- The newer RFC 5424 format provides more structured data and better internationalization support.

### Header Structure
```plaintext
<PRI>VERSION TIMESTAMP HOSTNAME APP-NAME PROCID MSGID
```
- `PRI`: Priority value (Facility * 8 + Severity)
- `VERSION`: Protocol version (1)
- `TIMESTAMP`: ISO 8601 formatted timestamp
- `HOSTNAME`: Machine name
- `APP-NAME`: Application name
- `PROCID`: Process identifier
- `MSGID`: Message identifier

### Structured Data
- Contains structured information in key-value pairs
- Format: [SD-ID param-name="param-value"]
- Multiple structured data elements can be present

### Message Body
- UTF-8 encoded message content
- Can contain any valid Unicode characters

### For example:
```plaintext
<14>1 2024-12-12T12:34:56Z myhostname syslogSender 1234 - - Test message
```

In this message:
- `<14>`: Priority (Facility `1` for **user** and Severity `6` for **Informational**).  
- `1`: Version of the Syslog protocol.  
- `2024-12-12T12:34:56Z`: Timestamp.  
- `myhostname`: Hostname of the system generating the message.  
- `syslogSender`: Application name.  
- `1234`: Process ID (optional).  
- `Test message`: Actual message content.


### 3.2 Legacy Format (RFC 3164):

  The older BSD Syslog format is simpler but less structured.

### Header Structure
   ```plaintext
  <PRI>TIMESTAMP HOSTNAME TAG: MESSAGE
  ```
- `PRI`: Priority value (same calculation as RFC 5424)
- `TIMESTAMP`: Format is "MMM DD HH:MM:SS" (e.g., "Dec 12 12:34:56")
- `HOSTNAME`: Name of the machine
- `TAG`: Program name and optional process ID (up to 32 characters)
- `MESSAGE`: Free-form message text

### For example:
```plaintext
<14>Dec 12 12:34:56 myhostname syslogSender[1234]: Test message
```

In this message:
- `<14>`: Priority (same as RFC 5424)
- `Dec 12 12:34:56`: Timestamp in BSD format
- `myhostname`: Hostname
- `syslogSender[1234]`: Program name with PID
- `Test message`: Message content

#### Comparing RFC 3164 vs RFC 5424:

Feature | RFC 3164 (Legacy) | RFC 5424 (Modern)
--------|------------------|------------------
Protocol Version | Not included | Mandatory version field (1)
Timestamp Format | Basic format (Dec 12 12:34:56) | ISO 8601 with timezone and subsecond precision
Message Structure | Simple text format | Supports structured data with key-value pairs
Program Identification | Combined TAG field with program name and PID | Separate APP-NAME and PROCID fields
Character Encoding | ASCII recommended | Full UTF-8 support
Time Precision | Limited (no year, timezone) | Full precision with timezone support

The modern RFC 5424 format offers greater flexibility and precision, while RFC 3164 remains simpler but more limited in its capabilities.

## 4. OpenTelemetry Collector as a Syslog Receiver

### OpenTelemetry Collector Configuration
The OpenTelemetry Collector is configured to receive and process Syslog messages in **RFC 5424** format.

Key configuration in `otel-collector-config.yaml`:
```yaml
receivers:
  otlp:
    protocols:
      http:
        endpoint: 0.0.0.0:4318
      grpc:
        endpoint: 0.0.0.0:4317
  syslog:
    tcp:
      listen_address: "0.0.0.0:514"  # Listening on TCP port 514 for Syslog
    protocol: rfc5424

exporters:
  debug:
    verbosity: detailed

service:
  pipelines:
    logs:
      receivers: [syslog, otlp]   # Collect Syslog logs
      exporters: [debug]    # Send to debug exporter
```

---

## 5. Syslog Client Implementation

#### 1. Dependencies and Setup

To send Syslog messages, this application uses the CloudBees Syslog Java Client library. Add the following dependency to your `build.gradle` file:

```
implementation 'com.cloudbees:syslog-java-client:1.1.7'
```

#### 2. Define Syslog Host and Port
In `application.properties`, the Syslog server configuration is defined:
```properties
syslog.server.hostname=${SYSLOG_SERVER_HOSTNAME:myhostname}
syslog.server.port=${SYSLOG_SERVER_PORT:514}
```
This allows flexibility, where you can pass the server configuration via environment variables or use default values like `myhostname` and port `514`.

#### 3. Sending Syslog Messages
The service class `SyslogSenderServiceImpl` uses **CloudBees Syslog Java Client** to send Syslog messages:
```java
import com.cloudbees.syslog.Facility;
import com.cloudbees.syslog.MessageFormat;
import com.cloudbees.syslog.Severity;
import com.cloudbees.syslog.sender.TcpSyslogMessageSender;

...

// Initialise sender
 
      TcpSyslogMessageSender messageSender = new TcpSyslogMessageSender();

        messageSender.setDefaultAppName(appName);
        messageSender.setSyslogServerHostname(syslogServerHostname);
        messageSender.setSyslogServerPort(syslogServerPort);
        messageSender.setDefaultFacility(Facility.USER);
        messageSender.setDefaultSeverity(Severity.INFORMATIONAL);
        messageSender.setMessageFormat(MessageFormat.RFC_5424);
        messageSender.setSsl(false);

// send a Syslog message
messageSender.sendMessage("This is a test message");
```

Example method to send messages:
```java
public void sendSyslogMessage(String message, Severity severity) {
    try {
        messageSender.setDefaultSeverity(severity);
        messageSender.sendMessage(message);
        logger.info("Syslog message sent: {}", message);
    } catch (Exception e) {
        logger.error("Failed to send syslog message: {}", e.getMessage(), e);
    }
}
```
This method accepts a `message` and `severity` (e.g., INFORMATIONAL, ERROR, DEBUG), sets the severity for the message, and sends it. If the message is successfully sent, a log is generated, otherwise, an error message is logged.

#### 4. Testing the Syslog Message
Use the endpoint `/send-syslog` to test:
```bash
curl -X POST "http://localhost:8081/send-syslog?message=TestMessage&severity=INFORMATIONAL"
```

---

## 6. Docker Integration

### Dockerfile
The Dockerfile includes the OpenTelemetry Java Agent for tracing:
```dockerfile
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY build/libs/syslogSender-0.0.1-SNAPSHOT.jar syslogSender.jar
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.2.0/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar
ENTRYPOINT ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "-jar", "syslogSender.jar"]
```

### Docker Compose
The application is run with the OpenTelemetry Collector via Docker Compose:
```yaml
services:
  syslog-sender:
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "8081:8081"
    depends_on:
      - otel-collector
    environment:
      OTEL_EXPORTER_OTLP_ENDPOINT: http://otel-collector:4318
      OTEL_SERVICE_NAME: syslog-sender
      OTEL_METRICS_EXPORTER: none # disable export metrics
      OTEL_TRACES_EXPORTER: none  # disable trace export

      # Syslog Configuration
      SYSLOG_SERVER_HOSTNAME: myhostname
      SYSLOG_SERVER_PORT: 514
    networks:
      - otel-network

  otel-collector:
    image: otel/opentelemetry-collector-contrib:latest
    command: ["--config=/etc/docker/collector/otel-collector-config.yaml"]  # set the path of otel-collector-config.yaml file
    hostname: myhostname
    volumes:
      - ./docker/collector/otel-collector-config.yaml:/etc/docker/collector/otel-collector-config.yaml
    ports:
      - "4317:4317"   # OTLP gRPC receiver
      - "4318:4318"   # OTLP HTTP receiver
      - "514:514" # Syslog TCP
    networks:
      - otel-network

networks:
  otel-network:
    driver: bridge
```

---

## 7. Running the Application
1. Build the application:
   ```bash
   ./gradlew build
   ```
2. Start the application with Docker Compose:
   ```bash
   docker-compose up --build
   ```
3. Test sending Syslog messages:
   ```bash
   curl -X POST "http://localhost:8081/send-syslog?message=HelloSyslog&severity=INFORMATIONAL"
   ```


### Output Message after Sending Syslog

When you hit the test Syslog message URL with the given `curl` command, the application will log the following messages in the **application logs**:

#### Example Log Output:
```plaintext
INFO  2024-12-12 14:32:45 [SyslogSenderServiceImpl]: Syslog message sent: HelloSyslog
```

At the **Syslog receiver (OpenTelemetry Collector)**, you will observe an output similar to this:
```plaintext
<14>1 2024-12-12T14:32:45Z myhostname syslogSender 1234 - - HelloSyslog
```

Here:
- `<14>`: The calculated priority value (Facility `USER` and Severity `INFORMATIONAL`).
- `1`: Protocol version.
- `2024-12-12T14:32:45Z`: Timestamp.
- `myhostname`: The host sending the message.
- `syslogSender`: Application name.
- `1234`: Process ID (can be a placeholder).
- `HelloSyslog`: The actual message.

---

## 8. Sending Syslog with Different Severities

   - You can modify the severity by passing it as a query parameter when testing the Syslog message.

#### Example 1: Sending a DEBUG Message
```bash
curl -X POST "http://localhost:8081/send-syslog?message=DebugMessage&severity=DEBUG"
```

**Output:**
```plaintext
INFO  2024-12-12 14:35:12 [SyslogSenderServiceImpl]: Syslog message sent: DebugMessage
<7>1 2024-12-12T14:35:12Z myhostname syslogSender 1234 - - DebugMessage
```
Here, `<7>` indicates the **DEBUG** severity level.

#### Example 2: Sending an ERROR Message
```bash
curl -X POST "http://localhost:8081/send-syslog?message=ErrorMessage&severity=ERROR"
```

**Output:**
```plaintext
INFO  2024-12-12 14:36:28 [SyslogSenderServiceImpl]: Syslog message sent: ErrorMessage
<3>1 2024-12-12T14:36:28Z myhostname syslogSender 1234 - - ErrorMessage
```
Here, `<3>` indicates the **ERROR** severity level.

---

## 9. Notes
- You can replace the OpenTelemetry Collector configuration or use a different exporter (e.g., Elasticsearch or Splunk) based on your requirements.
- Ensure the Syslog server is reachable at the hostname and port specified.

---

## 10. References 
- https://www.paessler.com/it-explained/syslog
- https://github.com/open-telemetry/opentelemetry-collector-contrib/blob/main/receiver/syslogreceiver/README.md
- https://github.com/jenkinsci/syslog-java-client

