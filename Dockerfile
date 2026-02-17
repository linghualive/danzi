# ============================================================
# Stage 1: Build all modules
# ============================================================
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /build

# Copy Maven wrapper and POM files first for dependency caching
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./
COPY common/pom.xml common/
COPY gateway/pom.xml gateway/
COPY base-service/pom.xml base-service/
COPY mall-service/pom.xml mall-service/

RUN chmod +x mvnw && ./mvnw dependency:go-offline -B -q || true

# Copy source code
COPY common/src common/src
COPY gateway/src gateway/src
COPY base-service/src base-service/src
COPY mall-service/src mall-service/src

# Build all modules
RUN ./mvnw clean package -DskipTests -B -q

# ============================================================
# Stage 2: base-service runtime
# ============================================================
FROM eclipse-temurin:17-jre AS base-service

WORKDIR /app
COPY --from=builder /build/base-service/target/base-service-*.jar app.jar

EXPOSE 9001

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -sf http://localhost:9001/v3/api-docs > /dev/null || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]

# ============================================================
# Stage 3: mall-service runtime
# ============================================================
FROM eclipse-temurin:17-jre AS mall-service

WORKDIR /app
COPY --from=builder /build/mall-service/target/mall-service-*.jar app.jar

EXPOSE 9002

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -sf http://localhost:9002/v3/api-docs > /dev/null || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]

# ============================================================
# Stage 4: gateway runtime
# ============================================================
FROM eclipse-temurin:17-jre AS gateway

WORKDIR /app
COPY --from=builder /build/gateway/target/gateway-*.jar app.jar

EXPOSE 9000

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -sf http://localhost:9000/actuator/health > /dev/null 2>&1 || curl -sf http://localhost:9000/ > /dev/null 2>&1 || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
