FROM openjdk:17.0.2-jdk-slim
MAINTAINER maxauch maximilian.auch@hm.edu
ARG JAR_FILE=target/matilda-discovery.jar
ADD ${JAR_FILE} matilda-discovery.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "-Xmx1024m", "-Xss256m", "/matilda-discovery.jar"]
