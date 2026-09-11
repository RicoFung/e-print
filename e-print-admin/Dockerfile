ARG REGISTRY
FROM ${REGISTRY}/library/openjdk21:latest

COPY target/e-print-admin.jar /home/e-print-admin.jar

ENTRYPOINT ["sh", "-c", "exec java -server -XX:+UseG1GC -XX:+UseContainerSupport -XX:InitialRAMPercentage=75.0 -XX:MaxRAMPercentage=75.0 -XX:+UseGCOverheadLimit -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/home/public/dump-e-print-admin.hprof ${JAVA_OPTS} -jar /home/e-print-admin.jar"]
