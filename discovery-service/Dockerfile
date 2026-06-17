# Runtime-only : le JAR est buildé par Jenkins (stage Maven Build).
# Contexte de build = RACINE du repo.
FROM eclipse-temurin:17-jre-alpine

# curl : HEALTHCHECK + debug réseau inter-conteneurs
RUN apk add --no-cache curl

# Utilisateur non-root
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Fat-jar repackagé par spring-boot-maven-plugin (un seul .jar matche)
COPY discovery-service/target/discovery-service-*.jar /app/app.jar
RUN chown -R spring:spring /app

USER spring

EXPOSE 8761

# /actuator/health en permitAll → pas besoin de creds ici
HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
  CMD curl -f http://localhost:8761/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]