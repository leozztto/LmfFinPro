FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# A imagem roda em UTC. O fuso de São Paulo vale para os LocalDateTime.now() das datas de criação
# (ex.: envio de anexo, importação de extrato), que a tela mostra; as regras de "hoje" já usam o
# Clock de SchedulingConfig.
ENTRYPOINT ["java", "-Duser.timezone=America/Sao_Paulo", "-jar", "app.jar"]
