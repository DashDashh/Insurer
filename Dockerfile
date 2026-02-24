FROM maven:3.8.4-openjdk-17

WORKDIR /app

COPY target/insurance-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]