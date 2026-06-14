# ═══════════════════════════════════════════════════════════════
# STAGE 1 — BUILD Maven (multi-module)
# ═══════════════════════════════════════════════════════════════
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace

# 1. POMs d'abord pour bénéficier du cache Docker
#    Tant que les pom.xml ne changent pas, la couche dépendances
#    est réutilisée → rebuilds rapides quand seul le code change.
COPY pom.xml ./pom.xml
COPY auth-service/pom.xml ./auth-service/pom.xml

# 2. Download des dépendances (offline mode)
#    -pl auth-service → ne builde que ce module
#    -am              → inclut le parent POM nécessaire
RUN mvn -B -f pom.xml -pl auth-service -am dependency:go-offline -DskipTests

# 3. Code source du module concerné uniquement
COPY auth-service/src ./auth-service/src

# 4. Build du JAR
RUN mvn -B -f pom.xml -pl auth-service -am clean package -DskipTests

# ═══════════════════════════════════════════════════════════════
# STAGE 2 — RUNTIME (JRE seul, image légère)
# ═══════════════════════════════════════════════════════════════
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# curl pour le HEALTHCHECK
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

# Utilisateur non-root (best practice sécurité)
RUN groupadd -r etrade && useradd -r -g etrade -d /app etrade

# Récupération du JAR depuis le stage build
COPY --from=build /workspace/auth-service/target/auth-service-*.jar /app/app.jar
RUN chown -R etrade:etrade /app

USER etrade

EXPOSE 8081

# JVM container-aware (respecte les limites mémoire du conteneur)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Healthcheck via Spring Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:8081/actuator/health | grep -q '"UP"' || exit 1

# exec → Java devient PID 1, reçoit SIGTERM proprement
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]