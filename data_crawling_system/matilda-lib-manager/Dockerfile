FROM openjdk:17.0.2-jdk-slim
MAINTAINER maxauch maximilian.auch@hm.edu
ARG JAR_FILE=target/matilda-lib-manager.jar
ADD ${JAR_FILE} matilda-lib-manager.jar
EXPOSE 8097
ENTRYPOINT ["java", "-jar", "-Xmx2048m", "-Xss1024m", "/matilda-lib-manager.jar"]