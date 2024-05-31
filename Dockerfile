# build
FROM maven:3-eclipse-temurin-11 AS build
WORKDIR /opt/powertac/server/build
COPY . .
RUN mvn clean install
RUN mvn -f docker-build.pom.xml package

# target
FROM eclipse-temurin:11
WORKDIR /powertac/server

COPY docker-entrypoint.sh .
RUN chmod +x docker-entrypoint.sh

ENV SERVER_JAR=powertac-server.jar
COPY --from=build /opt/powertac/server/build/target/${SERVER_JAR} ./${SERVER_JAR}

ENTRYPOINT ["/powertac/server/docker-entrypoint.sh"]