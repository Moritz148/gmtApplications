FROM openjdk:23

COPY target/gmtApplications-*.jar /demo.jar

CMD ["java", "-jar", "/demo.jar"]