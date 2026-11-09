FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY biblioteca-comum ./biblioteca-comum
COPY eureka-server ./eureka-server
COPY gateway-service ./gateway-service
COPY traffic-service ./traffic-service
RUN mvn clean package -pl eureka-server -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/eureka-server/target/eureka-server-0.0.1-SNAPSHOT.jar /app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "/app.jar"]
