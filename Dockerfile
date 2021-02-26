FROM navikt/java:11
ENV JAVA_OPTS="-Dlogback.configurationFile=logback-remote.xml -Xms512M -Xmx1024M"
COPY build/libs/app*.jar app.jar