# ── Stage 1: Build ──────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copiar Maven Wrapper y pom.xml primero (cache de capas)
COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY pom.xml .

# Descargar dependencias (esta capa se cachea si pom.xml no cambia)
RUN ./mvnw dependency:go-offline -B

# Copiar código fuente y compilar
COPY src src
RUN ./mvnw package -DskipTests -B

# ── Stage 2: Runtime ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Usuario no-root para seguridad
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Copiar el JAR del stage anterior
COPY --from=builder /app/target/*.jar app.jar

# Puerto expuesto (Railway/Render usan la variable PORT)
EXPOSE 8080

# Health check para plataformas de despliegue
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
