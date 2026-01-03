FROM openjdk:17.0.2-jdk-slim
MAINTAINER maxauch maximilian.auch@hm.edu
ARG JAR_FILE=target/matilda-gateway.jar
ADD ${JAR_FILE} matilda-gateway.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "-Xmx1024m", "-Xss256m", "/matilda-gateway.jar"]
