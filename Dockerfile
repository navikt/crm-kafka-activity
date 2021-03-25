FROM navikt/java:8
ENV JAVA_OPTS="-Dlogback.configurationFile=logback-remote.xml -Dhttps.protocols=SSLv3,TLSv1,TLSv1.1,TLSv1.2 -Xms512M -Xmx1024M"
COPY build/libs/app*.jar app.jar