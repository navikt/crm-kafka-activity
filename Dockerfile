FROM ghcr.io/navikt/baseimages/temurin:17
ENV JAVA_OPTS="-Dlogback.configurationFile=logback-remote.xml -Dhttps.protocols=TLSv1,TLSv1.1,TLSv1.2 -Djdk.tls.client.protocols=TLSv1,TLSv1.1,TLSv1.2 -Dsoapui.https.protocols=TLSv1,TLSv1.1,TLSv1.2"
COPY build/libs/app*.jar app.jar