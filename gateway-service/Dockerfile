# =========================================================
# gateway-service — image runtime-only
# Le JAR est déjà construit par Jenkins (Maven) AVANT le docker build.
# =========================================================
FROM eclipse-temurin:17-jre-alpine

# curl : utilisé par le HEALTHCHECK + debug réseau inter-conteneurs
RUN apk add --no-cache curl

# Utilisateur non-root
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# JAR repackagé par spring-boot-maven-plugin (un seul .jar matche le glob)
COPY gateway-service/target/gateway-service-*.jar /app/app.jar
RUN chown -R spring:spring /app

USER spring

EXPOSE 8080

# Le gateway expose /actuator/health (include: health,gateway,info)
HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]