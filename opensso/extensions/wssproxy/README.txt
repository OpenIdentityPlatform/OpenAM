#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
#
# The contents of this file are subject to the terms
# of the Common Development and Distribution License
# (the License). You may not use this file except in
# compliance with the License.
#
# You can obtain a copy of the License at
# https://opensso.dev.java.net/public/CDDLv1.0.html or
# opensso/legal/CDDLv1.0.txt
# See the License for the specific language governing
# permission and limitations under the License.
#
# When distributing Covered Code, include this CDDL
# Header Notice in each file and include the License file
# at opensso/legal/CDDLv1.0.txt.
# If applicable, add the following below the CDDL Header,
# with the fields enclosed by brackets [] replaced by
# your own identifying information:
# "Portions Copyrighted [year] [name of copyright owner]"
#
# $Id: README.txt,v 1.3 2008/11/04 06:04:44 veiming Exp $
#

TODO: Where to get openssowssproviders.jar
      Have to setup jks on client end

Web Service Security Proxy (WSS Proxy)
--------------------------------------

%%1. Overview
%%2. Build WSS Proxy WAR 
     %%2.1 Library dependencies
     %%2.2 Server Configuration
     %%2.3 Build Web ARchive, wssproxy.war
%%3. Deploy WAR
%%4. Simple Test

________________________________________________________________________________
%%1. Overview

WSS Proxy is a Java EE compliance Web ARchive (WAR) that encrypts and decrypts
web service message. This is the typical setup.

                                   secured
 +-------------+    +-----------+  messages   +-----------+    +-------------+
 |             |    |           |             |           |    |             |
 |             |--->|           |------------>|           |--->|             |
 | Web Service |    | WSS Proxy |             | WSS Proxy |    | Web Service |
 |   Client    |    |           |<------------|           |<---|  Provider   |
 |             |    |           |             |           |    |             |
 +-------------+    +-----------+             +-----------+    +-------------+
                    encrypts  |                | decrypts
                              |                |
                              |                |
                              |                |
                           +-------------------------+
                           |                         |
                           |    OpenSSO Server       |
                           |                         |
                           +-------------------------+

Each WSS Proxy is associated with a web service profile in OpenSSO Server.
In order to encrypt and decrypt web service messages, the proxies need to 
read their profiles from OpenSSO Server. The profile provides information on
security mechanism and end points.

________________________________________________________________________________
%%2. Build WSS Proxy WAR 

%%2.1 Library dependencies
Followings are the dependencies
    webservices-api.jar
    webservices-extra-api.jar
    webservices-extra.jar
    webservices-rt.jar
    webservices-tools.jar
    xalan.jar
    xercesImpl.jar
    j2ee.jar
    openssowssproviders.jar
    openssoclientsdk.jar

The following jars can be obtained by downloading opensso-sun-extlib.zip from
http://download.java.net/general/opensso/extlib/latest/opensso-sun-extlib.zip

    webservices-api.jar
    webservices-extra-api.jar
    webservices-extra.jar
    webservices-rt.jar
    webservices-tools.jar
    xalan.jar
    xercesImpl.jar

Look at opensso/products/README for instruction on how to obtain
    j2ee.jar

download the latest openssoclientsdk.jar from
https://opensso.dev.java.net/public/use/index.html

All the jars need to be placed in extlib sub directory.


%%2.2 Server Configuration
Enter server configuration information in resources/setupValues.properties.

%%2.3 Build Web ARchive, wssproxy.war
type ant build

________________________________________________________________________________
%%3. Deploy WAR

Deploy the wssproxy.war in any Java EE compliant web container. Make sure that 
its server is up and running.

________________________________________________________________________________
%%4. Simple Test

%%4.1 Deploy opensso.war and configure it (choose default option). Check that
      you are able to login to Administrator console. Choose Configuration ->
      Servers and Sites tab.
      Select the server instance in Servers tab. In the server instance
      profile page, select Security tab and note the value of Password
      Encryption Key.

%%4.2 cd <your workspace>/opensso/extensions/wssproxy.
      edit resources/setupValues.properties, these values have to be changed
      a. BASEDIR
      b. APPLICATION_PASSWORD
      c. SERVER_PROTOCOL
      d. SERVER_HOST
      e. SERVER_PORT
      f. DEPLOY_URI
      g. ENCRYPTION_KEY (you got the value in %%4.1)

%%4.3 cd <your workspace>/opensso/extensions/wssproxy
      ant clean; ant build

%%4.4 Use NetBean to create a Hello World Web Service Provider. And deploy the
      WAR. Let it is http://www.wspsample.com:8080/WSP. This web service can be
      as simple as printing "Hello World".

%%4.5 deploy <your workspace>/opensso/extensions/wssproxy/built/dist/wssproxy.war 
      in the same web container as the Hello World Web Service Provider. Let
      its deployment URI be /wspproxy. To check if it is deployed correct. 
      Visit http://www.wspsample.com:8080/wspproxy and you will see
      "Web Service Security Proxy" printed.

%%4.6 Login to OpenSSO server (as you have done in %%4.1). Choose Access
      Control tab. Select / (Top Level Realm) and then Agent tab -> Web
      Service Provider tab. Create a new agent for the wspproxy. Let its name
      be "wspproxy". Select UserNameToken as one of the supported Security
      Mechanisms. Enter http://www.wspsample.com:8080/WSP as the
      Web Service Security Proxy End Point.

Up to this point, we have setup the Web Service Provider ends.
                                   
             +-----------+    +-------------+
             |           |    |             |
             |           |    |             |
             | WSS Proxy |    | Web Service |
             | (wspproxy)|<---|  Provider   |
             |           |    |             |
             +-----------+    +-------------+
                    |
                    |
                    |
      +-------------------------+
      |                         |
      |    OpenSSO Server       |
      |                         |
      +-------------------------+

%%4.7 deploy <your workspace>/opensso/extensions/wssproxy/built/dist/wssproxy.war 
      in the same web container as the Web Service Client which is going to be
      deploy later (see %%4.9). Let its deployment URI be /wscproxy. To check
      if it is deployed correct.  Visit http://www.wscsample.com:8080/wscproxy
      and you will see "Web Service Security Proxy" printed.

%%4.8 Login to OpenSSO server (as you have done in %%4.1). Choose Access
      Control tab. Select / (Top Level Realm) and then Agent tab -> Web
      Service Client tab. Create a new agent for the wscproxy. Let its name
      be "wscproxy".  Select UserNameToken as the Security Mechanism.
      Enter http://www.wspsample.com:8080/wspproxy/SecurityProxy/wspproxy as
      the Web Service Security Proxy End Point.

%%4.9 Use NetBean to create a Web Service Client. Create a web service client.
      Enter http://www.wscproxy.com:8080/wscproxy/SecurityProxy/wscproxy to
      fetch WSDL. In the index.jsp, generate code to call the Web Service
      Operation. Deploy the WAR. let it is http://www.wscsample.com:8080/WSC

Point your browser to http://www.wscsample.com:8080/WSC and index.jsp will be
invoked and Hello world message will be printed accordingly.

