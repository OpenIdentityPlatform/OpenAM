FROM tomcat:8.5-jdk8

MAINTAINER Open Identity Platform Community <open-identity-platform-openam@googlegroups.com>

ENV CATALINA_HOME=/usr/local/tomcat \
    OPENAM_USER="openam" \
    OPENAM_DATA_DIR="/usr/openam/config"    
ENV PATH=$CATALINA_HOME/bin:$PATH \
    CATALINA_OPTS="-Xmx2048m -server -Dcom.iplanet.services.configpath=$OPENAM_DATA_DIR -Dcom.sun.identity.configuration.directory=$OPENAM_DATA_DIR"

WORKDIR $CATALINA_HOME

ARG VERSION @project_version@

RUN apt-get update && apt-get install -y \
    wget \
    unzip \
 && rm -rf /var/lib/apt/lists/* \
 && wget --show-progress --progress=bar:force:noscroll --quiet --output-document=$CATALINA_HOME/webapps/openam.war https://github.com/OpenIdentityPlatform/OpenAM/releases/download/$VERSION/OpenAM-$VERSION.war \
 && mkdir /usr/openam \
 && wget --show-progress --progress=bar:force:noscroll --quiet --output-document=/usr/openam/ssoconfiguratortools.zip https://github.com/OpenIdentityPlatform/OpenAM/releases/download/$VERSION/SSOConfiguratorTools-$VERSION.zip \
 && unzip /usr/openam/ssoconfiguratortools.zip -d /usr/openam/ssoconfiguratortools \
 && rm /usr/openam/ssoconfiguratortools.zip \
 && wget --show-progress --progress=bar:force:noscroll --quiet --output-document=/usr/openam/ssoadmintools.zip https://github.com/OpenIdentityPlatform/OpenAM/releases/download/$VERSION/SSOAdminTools-$VERSION.zip \
 && unzip /usr/openam/ssoadmintools.zip -d /usr/openam/ssoadmintools \
 && rm /usr/openam/ssoadmintools.zip \
 && chgrp -R 0 /usr/openam/ \
 && chmod -R g=u /usr/openam/ \
 && chgrp -R 0 /usr/local/tomcat \
 && chmod -R g=u /usr/local/tomcat \
 && useradd -m -r -u 1001 -g root $OPENAM_USER \
 && install -d -o $OPENAM_USER $OPENAM_DATA_DIR

USER $OPENAM_USER

CMD ["/usr/local/tomcat/bin/catalina.sh", "run"]
