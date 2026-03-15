FROM maven:3.8.4-openjdk-17

WORKDIR /app

COPY target/insurance-0.0.1-SNAPSHOT.jar app.jar
COPY entrypoint.sh entrypoint.sh
RUN chmod +x entrypoint.sh

ENTRYPOINT ["/app/entrypoint.sh"]