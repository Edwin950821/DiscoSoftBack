FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
ENV JAVA_TOOL_OPTIONS="-Xmx384m"
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.workers.max=1"
COPY gradle gradle
COPY gradlew .
COPY build.gradle.kts .
COPY settings.gradle.kts .
RUN chmod +x gradlew
COPY src src
RUN ./gradlew bootJar --no-daemon -x test --no-build-cache

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/app.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
