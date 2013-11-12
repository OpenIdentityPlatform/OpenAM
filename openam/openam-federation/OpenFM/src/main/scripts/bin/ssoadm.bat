@echo off
:
: DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
:  
: Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
:  
: The contents of this file are subject to the terms
: of the Common Development and Distribution License
: (the License). You may not use this file except in
: compliance with the License.
:
: You can obtain a copy of the License at
: https://opensso.dev.java.net/public/CDDLv1.0.html or
: opensso/legal/CDDLv1.0.txt
: See the License for the specific language governing
: permission and limitations under the License.
:
: When distributing Covered Code, include this CDDL
: Header Notice in each file and include the License file
: at opensso/legal/CDDLv1.0.txt.
: If applicable, add the following below the CDDL Header,
: with the fields enclosed by brackets [] replaced by
: your own identifying information:
: "Portions Copyrighted [year] [name of copyright owner]"
:
: $Id: ssoadm.bat,v 1.19 2010/01/28 00:49:05 bigfatrat Exp $
:

: Portions Copyrighted 2010-2013 ForgeRock AS.

setlocal

IF NOT DEFINED JAVA_HOME (
	set JAVA_HOME=\@JAVA_HOME@
)
set TOOLS_HOME=@TOOLS_HOME@

set ORIG_CLASSPATH=%CLASSPATH%

set CLASSPATH="@CONFIG_DIR@"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/classes;%TOOLS_HOME%/resources"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/forgerock-util-${commons.forgerock-util.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-distribution-amadmsetup-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/opendj-server-${opendj.server.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/mail-1.4.5.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/servlet-api-2.5.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/webservices-api-2009-14-01.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/webservices-rt-2009-29-07.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/webservices-tools-2.1-b16.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/json-20090211.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/webservices-extra-2008-03-12.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/webservices-extra-api-2003-09-04.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/xalan-2.7.1.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/xercesImpl-2.10.0.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/xml-apis-1.4.01.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/xml-serializer-2.11.0.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/xmlsec-1.3.0.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-cli-definitions-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-cli-impl-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-core-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-shared-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-entitlements-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-auth-ad-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-auth-adaptive-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-auth-anonymous-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-auth-application-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-auth-cert-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-auth-datastore-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-auth-hotp-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-auth-httpbasic-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-auth-jdbc-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-auth-ldap-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-auth-membership-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-auth-msisdn-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-auth-nt-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-auth-oath-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-auth-oauth2-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-auth-radius-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-auth-safeword-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-auth-securid-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-auth-windowsdesktopsso-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-coretoken-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-dtd-schema-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-entitlements-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-federation-library-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-idsvcs-schema-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-jaxrpc-schema-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-liberty-schema-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-mib-schema-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-oauth-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-oauth2-core-token-service-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-oauth2-main-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-oauth2-openam-extension-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-oauth2-restlet-extension-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-rest-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-saml2-schema-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-locale-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-wsfederation-schema-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/openam-xacml3-schema-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;%TOOLS_HOME%/lib/OpenFM-${project.version}.jar"

IF DEFINED ORIG_CLASSPATH (
	set CLASSPATH=%ORIG_CLASSPATH%;%CLASSPATH%
)

"%JAVA_HOME%/bin/java.exe" -Xms256m -Xmx512m -cp %CLASSPATH% -D"sun.net.client.defaultConnectTimeout=3000" -D"openam.naming.sitemonitor.disabled=true" -D"com.iplanet.am.serverMode=false" -D"com.sun.identity.sm.notification.enabled=false" -D"bootstrap.dir=@CONFIG_DIR@" -D"com.iplanet.services.debug.directory=@DEBUG_DIR@" -D"com.sun.identity.log.dir=@LOG_DIR@" -D"definitionFiles=com.sun.identity.cli.AccessManager,com.sun.identity.federation.cli.FederationManager" -D"commandName=ssoadm" -D"amconfig=AMConfig" -D"java.version.current=java.vm.version" -D"java.version.expected=1.4+" -D"am.version.current=com.iplanet.am.version" -D"am.version.expected=@AM_VERSION@" -D"com.iplanet.am.sdk.package=com.iplanet.am.sdk.remote" -D"com.sun.identity.idm.remote.notification.enabled=false" com.sun.identity.cli.CommandManager %*
endlocal
:END

