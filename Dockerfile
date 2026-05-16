# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-23 AS builder

WORKDIR /build

COPY pom.xml .
COPY src ./src

# A flag -B (batch-mode) ajuda a formatar os logs no Docker sem travar
RUN mvn clean package -DskipTests -B

# ─── Stage 2: Extract layers (Spring Boot layertools) ─────────────────────────
FROM eclipse-temurin:23-jre-alpine AS extractor

WORKDIR /extract
COPY --from=builder /build/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# ─── Stage 3: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:23-jre

RUN groupadd -r app && useradd -r -g app app

WORKDIR /app

COPY --from=extractor /extract/dependencies/          ./
COPY --from=extractor /extract/spring-boot-loader/    ./
COPY --from=extractor /extract/snapshot-dependencies/ ./
COPY --from=extractor /extract/application/           ./

USER app

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=prod"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]