FROM ghcr.io/jqlang/jq:latest AS jq-stage

FROM eclipse-temurin:21-jdk AS build
COPY --from=jq-stage /jq /usr/bin/jq

# HOME stays /root, so Maven and Vaadin use the cache mounts below
WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn/ .mvn/
COPY src/ src/

# If the build needs a Vaadin license key, pass a Pro key as a secret with id "proKey", or an
# offline key with id "offlineKey":
#
#   $ docker build --secret id=proKey,src=$HOME/.vaadin/proKey .
#   $ docker build --secret id=offlineKey,src=$HOME/.vaadin/offlineKey .

# package builds the production frontend bundle; the project has no production profile
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=cache,target=/root/.vaadin \
    --mount=type=secret,id=proKey \
    --mount=type=secret,id=offlineKey \
    sh -c 'PRO_KEY=$(jq -r ".proKey // empty" /run/secrets/proKey 2>/dev/null || echo "") && \
    OFFLINE_KEY=$(cat /run/secrets/offlineKey 2>/dev/null || echo "") && \
    ./mvnw clean package -DskipTests -Dvaadin.proKey=${PRO_KEY} -Dvaadin.offlineKey=${OFFLINE_KEY}'

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# The builder reads and writes the views' Java sources at src/main/java, relative to the working directory, so they
# ship with the image. Saving changes them inside the container; mount a volume on /app/src to keep them.
COPY --from=build /app/src/main/java/ src/main/java/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
