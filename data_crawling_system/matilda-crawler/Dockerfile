FROM openjdk:17.0.2-jdk-slim
MAINTAINER maxauch maximilian.auch@hm.edu
ARG JAR_FILE=target/matilda-crawler.jar
ADD ${JAR_FILE} matilda-crawler.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "-Xmx1024m", "-Xss256m", "/matilda-crawler.jar"]


#----------------------------------------------------
#FROM maven:alpine
#WORKDIR .
#RUN mvn -v
#RUN mvn clean install
#LABEL maintainer="HM-Auch<maximilian.auch@hm.edu>"
#ADD matilda-crawler/target/matilda-crawler.jar /matilda-crawler.jar
#VOLUME /tmp
#WORKDIR /home/max/dev/matilda-system
#RUN bash -c 'touch /matilda-crawler.jar'
#ADD /matilda-crawler/target/matilda-crawler.jar /matilda-crawler.jar
#ARG JAR_FILE=target/*.jar
#ENV JAVA_HOME /usr/lib/jvm/java-1.8.0-openjdk
#ENV QSQL_HOME /usr/local/qsql-0.7.0
#ENV SPARK_HOME /usr/local/spark-2.4.4-bin-hadoop2.7
#ENV PATH $JAVA_HOME/bin:$SPARK_HOME/bin:$SPARK_HOME/sbin:$QSQL_HOME/bin:$PATH
#RUN export PATH QSQL_HOME SPARK_HOME
#CMD ["/usr/sbin/init"]
#----------------------------------------------------
