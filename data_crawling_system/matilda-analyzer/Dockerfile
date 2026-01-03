FROM openjdk:17.0.2-jdk-slim
MAINTAINER maxauch maximilian.auch@hm.edu
ARG JAR_FILE=target/matilda-analyzer.jar
ADD ${JAR_FILE} matilda-analyzer.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "-Xmx2048m", "-Xss512m", "/matilda-analyzer.jar"]
