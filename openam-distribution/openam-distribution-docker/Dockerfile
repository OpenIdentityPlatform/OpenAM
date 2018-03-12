FROM tomcat:7.0-jre8

MAINTAINER Open Identity Platform Community <open-identity-platform-openam@googlegroups.com>

ENV CATALINA_HOME /usr/local/tomcat

ENV PATH $CATALINA_HOME/bin:$PATH

WORKDIR $CATALINA_HOME

ENV VERSION @project_version@

ENV CATALINA_OPTS="-Xmx2048m -server"

RUN apt-get install -y wget unzip

RUN wget --quiet https://github.com/OpenIdentityPlatform/OpenAM/releases/download/$VERSION/OpenAM-$VERSION.war

RUN mv *.war $CATALINA_HOME/webapps/openam.war

CMD ["/usr/local/tomcat/bin/catalina.sh", "run"]

