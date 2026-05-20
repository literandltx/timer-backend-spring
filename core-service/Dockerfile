FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY . .

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw clean package -pl core-service -am -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/core-service/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
