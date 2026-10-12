# GraalVM native image: compiles the Spring AOT-processed app into a standalone Linux binary.
FROM ghcr.io/graalvm/native-image-community:25 AS build
RUN microdnf install -y unzip findutils && microdnf clean all
WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN sh mvnw -B -q -Pnative dependency:go-offline || true

COPY src src
RUN sh mvnw -B -Pnative -DskipTests native:compile

# The binary links glibc and zlib dynamically, so the runtime image must provide both.
FROM debian:bookworm-slim
COPY --from=build /workspace/target/lms /app/lms
USER nobody
EXPOSE 8080
ENTRYPOINT ["/app/lms"]
