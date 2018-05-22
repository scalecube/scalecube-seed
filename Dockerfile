ARG JDKVERSION=8-slim
FROM openjdk:$JDKVERSION

MAINTAINER info@scalecube.io

# copy package information.
ADD target/maven-archiver/pom.properties                   /usr/src/myapp/package.properties

# Cluster control port and communication port.
EXPOSE 4801 4802

###################################################################
# Environment Variables:
# -----------------------------------------------------------------
# SC_HOME:         the root folder for a scalecube node.
# SC_SEED_ADDRESS: the list of seed nodes to join a cluster
###################################################################

ENV SC_HOME="/usr/src/myapp/" \
    SC_SEED_ADDRESS="172.17.0.2:4802, 172.17.0.3:4802, 172.17.0.4:4802"

ARG JARFILE
ARG MAINCLASS

# Add the service itself
COPY target/${JARFILE} /usr/src/myapp/${JARFILE}
ENV FILE ${JARFILE}
WORKDIR /usr/src/myapp
ENV JAVA_OPTS=
CMD java $JAVA_OPTS -jar $FILE -Xmn2g
