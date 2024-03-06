#!/bin/bash

JAVA_OPTS="$JAVA_OPTS -Xms64m -Xmx512m -Djava.net.preferIPv4Stack=true"
JAVA_OPTS="$JAVA_OPTS -Djava.security.egd=file:/dev/./urandom"

java -jar java-services.jar