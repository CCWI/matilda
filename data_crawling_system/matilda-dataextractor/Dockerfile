FROM openjdk:17.0.2-jdk-slim
MAINTAINER maxauch maximilian.auch@hm.edu
ARG JAR_FILE=target/matilda-dataextractor.jar
ADD ${JAR_FILE} matilda-dataextractor.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "-Xmx10140m", "-Xss1024m", "/matilda-dataextractor.jar"]
# "-agentlib:jdwp=transport=dt_socket,address=*:8000,server=y,suspend=n",