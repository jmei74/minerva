FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && \
    mvn package -DskipTests -q && \
    mv target/*.jar app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/app.jar .

# PCI-DSS: 不要 root 运行
RUN addgroup -S minerva && adduser -S minerva -G minerva
USER minerva

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]