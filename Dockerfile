# First stage: complete build environment
FROM sbtscala/scala-sbt:graalvm-community-21.0.2_1.9.9_3.4.0 AS builder
# add pom.xml and source code
ADD ./build.sbt build.sbt
ADD ./client client
ADD ./common common
ADD ./files files
ADD ./project/build.properties project/build.properties
ADD ./project/plugins.sbt project/plugins.sbt
ADD ./server server
ADD ./.jvmopts .jvmopts
RUN --mount=type=cache,target=/root/.ivy2 sbt server/assembly


FROM ghcr.io/graalvm/jdk-community:21
COPY --from=builder /root/server/target/scala-3.3.1/server.jar server.jar
EXPOSE 8085
CMD ["java", "-jar", "server.jar"]
