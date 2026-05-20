# ─── Stage 1: Extract layers (Spring Boot layertools) ─────────────────────────
FROM eclipse-temurin:23-jre-alpine AS extractor

WORKDIR /extract
COPY target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
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
    -Xmx256m \
    -Xms256m \
    -XX:MaxMetaspaceSize=80m \
    -Xss256k \
    -XX:+UseSerialGC \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=prod"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]