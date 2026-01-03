FROM openjdk:17.0.2-jdk-slim
MAINTAINER maxauch maximilian.auch@hm.edu
ARG JAR_FILE=target/matilda-state.jar
ADD ${JAR_FILE} matilda-state.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "-Xmx2048m", "-Xss1024m", "/matilda-state.jar"]