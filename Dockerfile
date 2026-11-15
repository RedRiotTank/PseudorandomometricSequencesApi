FROM eclipse-temurin:21-jre-jammy

# Java2D (used by the sequence plot endpoint) needs a font configuration on Linux.
RUN apt-get update \
    && apt-get install -y --no-install-recommends fontconfig fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

ARG JAR_VERSION

COPY target/PseudorandomometricSequencesApi-*${JAR_VERSION}.jar app.jar

EXPOSE 8080

# Keep the heap bounded so a burst of large generation requests cannot exhaust
# the host. Operators can override JAVA_OPTS from docker-compose/.env.
ENV JAVA_OPTS="-Xms128m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+ExitOnOutOfMemoryError"

# The service never needs root: it only reads /app.jar and writes to stdout/tmp.
RUN groupadd --system app && useradd --system --gid app --home-dir /home/app --shell /usr/sbin/nologin app \
    && mkdir -p /home/app && chown app:app /home/app

USER app

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app.jar"]
