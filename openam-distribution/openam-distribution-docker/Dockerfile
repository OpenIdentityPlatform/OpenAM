# The contents of this file are subject to the terms of the Common Development and
# Distribution License (the License). You may not use this file except in compliance with the
# License.
#
# You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
# specific language governing permission and limitations under the License.
#
# When distributing Covered Software, include this CDDL Header Notice in each file and include
# the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
# Header, with the fields enclosed by brackets [] replaced by your own identifying
# information: "Portions copyright [year] [name of copyright owner]".
#
# Portions Copyright 2026 3A Systems, LLC.

FROM tomcat:11-jre25

LABEL org.opencontainers.image.authors="Open Identity Platform Community"

ENV CATALINA_HOME=/usr/local/tomcat
ENV OPENAM_PATH="openam"
ENV OPENAM_USER="openam"
ENV OPENAM_DATA_DIR="/usr/openam/config"
ENV PATH="$CATALINA_HOME/bin:$PATH"
ENV JAVA_OPTS="--add-exports java.base/sun.security.util=ALL-UNNAMED --add-exports java.security.jgss/sun.security.krb5=ALL-UNNAMED --add-exports java.base/sun.security.x509=ALL-UNNAMED --add-exports java.base/sun.security.tools.keytool=ALL-UNNAMED --add-exports java.xml/com.sun.org.apache.xerces.internal.dom=ALL-UNNAMED"
ENV MEMORY="-server"
ENV CATALINA_OPTS="$MEMORY -Dcom.iplanet.services.configpath=$OPENAM_DATA_DIR -Dcom.sun.identity.configuration.directory=$OPENAM_DATA_DIR"

ARG VERSION
RUN mkdir /usr/openam \
    && rm -rf $CATALINA_HOME/webapps/*

#COPY openam-server/target/OpenAM-*.war $CATALINA_HOME/webapps/$OPENAM_PATH.war
#COPY openam-distribution/openam-distribution-ssoadmintools/target/*.zip /usr/openam/ssoadmintools.zip
#COPY openam-distribution/openam-distribution-ssoconfiguratortools/target/*.zip /usr/openam/ssoconfiguratortools.zip

RUN if [ ! -f "$CATALINA_HOME/webapps/$OPENAM_PATH.war" ]; then echo file exists; fi

RUN apt-get update \
 && DEBIAN_FRONTEND=noninteractive apt-get upgrade -y \
 && apt-get install -y --no-install-recommends curl unzip \
 && if [ -z "$VERSION" ] ; then VERSION="$(curl -i -o - --silent https://api.github.com/repos/OpenIdentityPlatform/OpenAM/releases/latest | grep -m1 "\"name\"" | cut -d\" -f4)"; fi \
 && if [ ! -f "$CATALINA_HOME/webapps/$OPENAM_PATH.war" ]; then echo file exists && curl -L https://github.com/OpenIdentityPlatform/OpenAM/releases/download/$VERSION/OpenAM-$VERSION.war --output $CATALINA_HOME/webapps/$OPENAM_PATH.war; fi \
 && if [ ! -f "/usr/openam/ssoconfiguratortools.zip" ]; then curl -L https://github.com/OpenIdentityPlatform/OpenAM/releases/download/$VERSION/SSOConfiguratorTools-$VERSION.zip --output /usr/openam/ssoconfiguratortools.zip; fi \
 && mkdir /usr/openam/ssoconfiguratortools && unzip /usr/openam/ssoconfiguratortools.zip -d /usr/openam/ssoconfiguratortools  && rm /usr/openam/ssoconfiguratortools.zip \
 && if [ ! -f "/usr/openam/ssoadmintools.zip" ]; then curl -L https://github.com/OpenIdentityPlatform/OpenAM/releases/download/$VERSION/SSOAdminTools-$VERSION.zip --output /usr/openam/ssoadmintools.zip; fi \
 && mkdir /usr/openam/ssoadmintools && unzip /usr/openam/ssoadmintools.zip -d /usr/openam/ssoadmintools && rm /usr/openam/ssoadmintools.zip \
 && chgrp -R 0 /usr/openam/ \
 && chmod -R g=u /usr/openam/ \
 && chgrp -R 0 $CATALINA_HOME \
 && chmod -R g=u $CATALINA_HOME \
 && useradd -m -r -u 1001 -g root $OPENAM_USER \
 && install -d -o $OPENAM_USER $OPENAM_DATA_DIR \
 && chown -R $OPENAM_USER:root $CATALINA_HOME \
 && apt-get remove -y --purge unzip \
 && rm -rf /var/lib/apt/lists/* \
 && sed -i '/<\/Host>/i \ \ \ \ <Valve className=\"org.apache.catalina.valves.RemoteIpValve\" httpServerPort=\"8080\" httpsServerPort=\"443\" />' $CATALINA_HOME/conf/server.xml

USER $OPENAM_USER

EXPOSE 8080/tcp

HEALTHCHECK --interval=30s --timeout=30s --start-period=1s --retries=3 CMD curl -v -L --fail http://localhost:8080/$OPENAM_PATH/isAlive.jsp || exit 1

CMD ["/usr/local/tomcat/bin/catalina.sh", "run"]
