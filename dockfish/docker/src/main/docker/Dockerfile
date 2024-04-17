FROM eclipse-temurin:21.0.2_13-jdk-jammy AS jre-build
RUN jlink  \
  --add-modules java.base\
,java.datatransfer\
,java.instrument\
,java.logging\
,java.management\
,java.naming\
,java.security.sasl\
,java.sql\
,java.xml\
,jdk.management\
,jdk.unsupported \
  --strip-java-debug-attributes \
  --no-man-pages \
  --no-header-files \
  --compress=2 \
  --bind-services \
  --output /jre
WORKDIR /jre/bin
RUN rm -f jar jarsigner javac javadoc javap \
    jcmd jconsole jdb jdeprscan jdeps jfr jhsdb jimage \
    jinfo jlink jmap jmod jpackage jps jrunscript jstack jstat \
    rmiregistry serialver

FROM ubuntu:jammy
RUN apt-get update && apt-get upgrade -y \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends wget curl \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/opt/java/openjdk
ENV PATH "${JAVA_HOME}/bin:${PATH}"
COPY --from=jre-build /jre $JAVA_HOME
RUN java -version

EXPOSE 8080/tcp
ENTRYPOINT exec java -Xms32m -Xmx64m -XX:+ExitOnOutOfMemoryError -jar dockfish-web-runner.jar
COPY maven /
RUN chmod +x engines/dragon engines/stockfish engines/stockfish16
RUN echo uci | engines/dragon
RUN echo uci | engines/stockfish
RUN echo uci | engines/stockfish16
