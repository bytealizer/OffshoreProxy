# Use Alpine as base image
FROM alpine:3.20

# Install necessary packages and Java 21 (OpenJDK)
RUN apk update && \
    apk add --no-cache \
    openjdk21 \
    curl \
    unzip \
    bash

# Set environment variables
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk \
    GRADLE_VERSION=8.7 \
    PATH=$PATH:/opt/gradle/bin

# Install Gradle
RUN curl -sLo gradle.zip https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip && \
    unzip gradle.zip -d /opt && \
    ln -s /opt/gradle-${GRADLE_VERSION} /opt/gradle && \
    rm gradle.zip

# Set working directory
WORKDIR /app

# Copy project files
COPY . .

# Build the project
RUN gradle build --no-daemon

# Expose application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "build/libs/OffShoreProxy-1.0-SNAPSHOT.jar"]
