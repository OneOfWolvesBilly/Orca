FROM eclipse-temurin:21-jdk

WORKDIR /workspace/orca_backend

COPY orca_backend/.mvn .mvn
COPY orca_backend/mvnw ./mvnw
COPY orca_backend/pom.xml ./pom.xml
COPY orca_backend/src ./src

RUN chmod +x ./mvnw

EXPOSE 8080

CMD ["./mvnw", "spring-boot:run", "-Dspring-boot.run.profiles=local"]
