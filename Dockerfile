FROM adoptopenjdk/openjdk11:alpine-jre
ENV ALLOWED_ORIGIN=""
COPY target/ecommerce-project-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]