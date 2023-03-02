FROM azul/zulu-openjdk-alpine:11 
COPY ./k-synthetic-test-0.0.7.jar /
WORKDIR /
EXPOSE 80/tcp
CMD java -jar k-synthetic-test-0.0.7.jar k-synthetic-test.properties
