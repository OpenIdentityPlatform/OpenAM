FROM tomcat:9-jre21

MAINTAINER Open Identity Platform Community <open-identity-platform-openam@googlegroups.com>

ENV CATALINA_HOME=/usr/local/tomcat \
    OPENAM_PATH="openam" \
    OPENAM_USER="openam" \
    OPENAM_DATA_DIR="/usr/openam/config" 
ENV PATH=$CATALINA_HOME/bin:$PATH \
 	JAVA_OPTS="--add-exports java.base/sun.security.util=ALL-UNNAMED --add-exports java.security.jgss/sun.security.krb5=ALL-UNNAMED --add-exports java.base/sun.security.x509=ALL-UNNAMED --add-exports java.base/sun.security.tools.keytool=ALL-UNNAMED --add-exports java.xml/com.sun.org.apache.xerces.internal.dom=ALL-UNNAMED" \
 	MEMORY="-server -XX:+UseG1GC -XX:+UseContainerSupport" \
    CATALINA_OPTS="$MEMORY -Dcom.iplanet.services.configpath=$OPENAM_DATA_DIR -Dcom.sun.identity.configuration.directory=$OPENAM_DATA_DIR"

WORKDIR $CATALINA_HOME

ARG VERSION @project_version@

RUN apt-get update && apt-get install -y curl unzip 

RUN rm -rf  $CATALINA_HOME/webapps/* \
 && curl -L https://github.com/OpenIdentityPlatform/OpenAM/releases/download/$VERSION/OpenAM-$VERSION.war --output $CATALINA_HOME/webapps/$OPENAM_PATH.war  \
 && mkdir /usr/openam \
 && curl -L https://github.com/OpenIdentityPlatform/OpenAM/releases/download/$VERSION/SSOConfiguratorTools-$VERSION.zip --output /usr/openam/ssoconfiguratortools.zip  \
 && mkdir /usr/openam/ssoconfiguratortools && unzip /usr/openam/ssoconfiguratortools.zip -d /usr/openam/ssoconfiguratortools  && rm /usr/openam/ssoconfiguratortools.zip \
 && curl -L https://github.com/OpenIdentityPlatform/OpenAM/releases/download/$VERSION/SSOAdminTools-$VERSION.zip --output /usr/openam/ssoadmintools.zip  \
 && mkdir /usr/openam/ssoadmintools && unzip /usr/openam/ssoadmintools.zip -d /usr/openam/ssoadmintools && rm /usr/openam/ssoadmintools.zip \
 && chgrp -R 0 /usr/openam/ \
 && chmod -R g=u /usr/openam/ \
 && useradd -m -r -u 1001 -g root $OPENAM_USER \
 && install -d -o $OPENAM_USER $OPENAM_DATA_DIR \
 && chown -R $OPENAM_USER:root $CATALINA_HOME \
 && sed -i '/<\/Host>/i \ \ \ \ <Valve className=\"org.apache.catalina.valves.RemoteIpValve\" httpServerPort=\"8080\" httpsServerPort=\"443\" />' $CATALINA_HOME/conf/server.xml

RUN  apt-get remove -y --purge unzip \
 && rm -rf /var/lib/apt/lists/*
 
USER $OPENAM_USER

EXPOSE 8080 

HEALTHCHECK --interval=30s --timeout=30s --start-period=1s --retries=3 CMD curl -v -L --fail http://localhost:8080/$OPENAM_PATH/isAlive.jsp || exit 1

CMD ["/usr/local/tomcat/bin/catalina.sh", "run"]
