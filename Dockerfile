FROM eclipse-temurin:21-jre-jammy

# Java2D (used by the sequence plot endpoint) needs a font configuration on Linux.
RUN apt-get update \
    && apt-get install -y --no-install-recommends fontconfig fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

ARG JAR_VERSION

COPY target/PseudorandomometricSequencesApi-*${JAR_VERSION}.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app.jar"]