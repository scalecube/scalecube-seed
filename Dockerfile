FROM openjdk:8

LABEL maintainer="info@scalecube.io"

WORKDIR /opt/scalecube

# Cluster control port and communication port.
EXPOSE 4801 4802

# Copy jar
ARG EXECUTABLE_JAR
COPY target/lib lib
COPY target/${EXECUTABLE_JAR}.jar app.jar

ENTRYPOINT exec java $JAVA_OPTS -Dlog4j.configurationFile=log4j2.xml -jar app.jar
