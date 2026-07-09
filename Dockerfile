FROM openjdk:25-rc-jdk-oracle
COPY build/libs/UltraQueue-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
