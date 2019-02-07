FROM tomcat:8.5-jre8

MAINTAINER Open Identity Platform Community <open-identity-platform-openam@googlegroups.com>

ENV CATALINA_HOME /usr/local/tomcat

ENV PATH $CATALINA_HOME/bin:$PATH

WORKDIR $CATALINA_HOME

ENV OPENAM_USER="openam"

ENV OPENAM_DATA_DIR="/usr/openam/config"

ENV VERSION @project_version@

ENV CATALINA_OPTS="-Xmx2048m -server -Dcom.iplanet.services.configpath=$OPENAM_DATA_DIR -Dcom.sun.identity.configuration.directory=$OPENAM_DATA_DIR"

RUN apt-get install -y wget unzip

RUN wget --show-progress --progress=bar:force:noscroll --quiet https://github.com/OpenIdentityPlatform/OpenAM/releases/download/$VERSION/OpenAM-$VERSION.war

RUN mv *.war $CATALINA_HOME/webapps/openam.war

RUN mkdir /usr/openam

RUN wget --show-progress --progress=bar:force:noscroll --quiet --output-document=/usr/openam/ssoconfiguratortools.zip https://github.com/OpenIdentityPlatform/OpenAM/releases/download/$VERSION/SSOConfiguratorTools-$VERSION.zip \
 && unzip /usr/openam/ssoconfiguratortools.zip -d /usr/openam/ssoconfiguratortools \
 && rm /usr/openam/ssoconfiguratortools.zip

RUN wget --show-progress --progress=bar:force:noscroll --quiet --output-document=/usr/openam/ssoadmintools.zip https://github.com/OpenIdentityPlatform/OpenAM/releases/download/$VERSION/SSOAdminTools-$VERSION.zip \
 && unzip /usr/openam/ssoadmintools.zip -d /usr/openam/ssoadmintools \
 && rm /usr/openam/ssoadmintools.zip

RUN chgrp -R 0 /usr/openam/ && \
  chmod -R g=u /usr/openam/

RUN chgrp -R 0 /usr/local/tomcat && \
  chmod -R g=u /usr/local/tomcat

RUN useradd -m -r -u 1001 -g root $OPENAM_USER

USER $OPENAM_USER

CMD ["/usr/local/tomcat/bin/catalina.sh", "run"]
