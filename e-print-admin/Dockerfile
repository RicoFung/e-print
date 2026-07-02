ARG REGISTRY
    FROM ${REGISTRY}/library/openjdk21:latest

ADD target/e-print-admin.jar /home/e-print-admin.jar

ENTRYPOINT ["sh","-c","java -server -XX:+UseG1GC -XX:+UseContainerSupport -XX:InitialRAMPercentage=75.0 -XX:MaxRAMPercentage=75.0 -XX:+UseGCOverheadLimit -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/home/public/dump-e-print-admin.hprof -jar /home/e-print-admin.jar"]
