# BUILDER
FROM maven:3.8.7-openjdk-18-slim AS builder
WORKDIR /builder
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -f pom.xml clean package -DskipTests=true

# APP
FROM openjdk:17-slim

RUN apt-get update -y 
RUN apt-get install -y curl iputils-ping telnet -y
RUN apt-get clean

WORKDIR /app

COPY --from=builder /builder/target/java-services-0.0.1-SNAPSHOT.jar java-services.jar
COPY --from=builder /builder/docker/startup.sh .

RUN chmod +x ./startup.sh

EXPOSE 8080

# HEALTHCHECK --interval=20s --timeout=10s --retries=200 CMD curl -fail http://localhost:8080/api/health || exit 1

ENTRYPOINT ["/bin/bash", "startup.sh"]
