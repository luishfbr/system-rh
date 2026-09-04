# =============================================================================
# Build multi-stage: o estagio "build" tem o Maven e o JDK completo (~600MB);
# o estagio final leva apenas o JRE e o .jar. A imagem publicada nao carrega
# compilador nem codigo-fonte.
# =============================================================================

# ---------- Estagio 1: compilacao ----------
# Maven 3.9.16 + Temurin 26: mesma versao do ./mvnw e do <java.version> do pom.
FROM maven:3.9.16-eclipse-temurin-26 AS build
WORKDIR /build

# O pom e copiado sozinho, antes do src. Assim, enquanto as dependencias nao
# mudarem, o Docker reaproveita a camada de download em cada rebuild.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Estagio 2: execucao ----------
FROM eclipse-temurin:26-jre-noble AS runtime
WORKDIR /app

# Rodar como root dentro do container e desnecessario e amplia o estrago
# de uma eventual falha na aplicacao.
RUN groupadd --system sgc && useradd --system --gid sgc --create-home sgc

COPY --from=build /build/target/*.jar app.jar
RUN chown sgc:sgc app.jar
USER sgc

EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
