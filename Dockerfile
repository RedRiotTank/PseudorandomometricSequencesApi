FROM eclipse-temurin:21-jre-jammy

COPY *.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app.jar"]