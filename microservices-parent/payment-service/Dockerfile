# Stage 1: build
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build

COPY pom.xml ./
COPY order-service/pom.xml order-service/
COPY inventory-service/pom.xml inventory-service/
COPY supplier-service/pom.xml supplier-service/
COPY payment-service/pom.xml payment-service/
COPY shipping-service/pom.xml shipping-service/
COPY notification-service/pom.xml notification-service/

RUN mvn dependency:go-offline -pl payment-service -am -q

COPY payment-service/src/ payment-service/src/
RUN mvn package -pl payment-service -am -DskipTests -q

# Stage 2: runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=builder /build/payment-service/target/*.jar app.jar

EXPOSE 8089

ENTRYPOINT ["java", "-jar", "app.jar"]
