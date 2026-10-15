FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src
COPY frontend frontend
RUN mvn -f backend/pom.xml -B package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system campusos && useradd --system --gid campusos campusos
COPY --from=build /build/backend/target/campus-os-1.0.0.jar app.jar
COPY frontend frontend
RUN chown -R campusos:campusos /app
USER campusos
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
