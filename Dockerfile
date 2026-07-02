FROM eclipse-temurin:21-jre-jammy

ARG JAR_VERSION

COPY target/PseudorandomometricSequencesApi-*${JAR_VERSION}.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app.jar"]