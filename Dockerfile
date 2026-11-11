FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/scalys-0.0.1-SNAPSHOT.jar ./app.jar
EXPOSE 10000
USER 10001:10001
ENTRYPOINT ["java", "-Xms64m", "-Xmx300m", "-jar", "/app/app.jar"]
