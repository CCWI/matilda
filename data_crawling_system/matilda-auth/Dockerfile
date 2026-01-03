FROM openjdk:17.0.2-jdk-slim
MAINTAINER maxauch maximilian.auch@hm.edu
ARG JAR_FILE=target/matilda-auth.jar
ADD ${JAR_FILE} matilda-auth.jar
EXPOSE 8099
ENTRYPOINT ["java", "-jar", "-Xmx2048m", "-Xss512m", "/matilda-auth.jar"]
