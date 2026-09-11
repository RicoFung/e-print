ARG REGISTRY
FROM ${REGISTRY}/library/openjdk21:latest

COPY target/e-print-server.jar /home/e-print-server.jar

ENTRYPOINT ["sh", "-c", "exec java -server -XX:+UseG1GC -XX:+UseContainerSupport -XX:InitialRAMPercentage=75.0 -XX:MaxRAMPercentage=75.0 -XX:+UseGCOverheadLimit -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/home/public/dump-e-print-server.hprof ${JAVA_OPTS} -jar /home/e-print-server.jar"]
