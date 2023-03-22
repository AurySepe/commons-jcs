# syntax=docker/dockerfile:1

FROM maven:3.9.0-eclipse-temurin-17 as base
COPY commons-jcs-core /home/app/commons-jcs-core
COPY commons-jcs-dist /home/app/commons-jcs-dist
COPY commons-jcs-jcache /home/app/commons-jcs-jcache
COPY commons-jcs-jcache-extras /home/app/commons-jcs-jcache-extras
COPY commons-jcs-jcache-openjpa /home/app/commons-jcs-jcache-openjpa
COPY commons-jcs-sandbox /home/app/commons-jcs-sandbox
COPY commons-jcs-tck-tests /home/app/commons-jcs-tck-tests
COPY commons-jcs-webapp /home/app/commons-jcs-webapp
COPY pom.xml /home/app

#
# Package stage
#


#FROM base as development
#CMD ["./mvnw", "spring-boot:run", "-Dspring-boot.run.profiles=mysql", "-Dspring-boot.run.jvmArguments='-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8000'"]

FROM base as test
COPY --from=base /home/app /home/app
WORKDIR /home/app
CMD ["mvn", "test"]

FROM base as build
RUN mvn -f /home/app/pom.xml clean package

FROM eclipse-temurin:17-jre-jammy as production
COPY --from=build /home/app/commons-jcs-webapp/target/*.jar /webapp.jar
CMD ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/webapp.jar"]