FROM azul/zulu-openjdk-alpine:11 
COPY ./k-synthetic-test-0.0.7.jar /
WORKDIR /
EXPOSE 8080/tcp
RUN adduser -D -g '' k-synthetic-test
USER k-synthetic-test
HEALTHCHECK --interval=5m --timeout=10s CMD curl -f http://localhost:8080/ || exit 1
CMD java -jar k-synthetic-test-0.0.7.jar k-synthetic-test.properties
