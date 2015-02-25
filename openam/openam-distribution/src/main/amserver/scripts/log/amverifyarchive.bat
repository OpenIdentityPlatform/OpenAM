@echo off
:
: DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
:  
: Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
: $Id: amverifyarchive.bat,v 1.16 2009/07/06 22:01:16 hvijay Exp $
:

: Portions Copyrighted 2011-2014 ForgeRock AS.

set TOOLS_HOME="@TOOLS_HOME@"

setlocal
:WHILE
if x%1==x goto WEND
set PARAMS=%PARAMS% %1
shift
goto WHILE
:WEND

set CLASSPATH="@CONFIG_DIR@"
set CLASSPATH="%CLASSPATH%;lib/forgerock-util-${commons.forgerock-util.version}.jar"
set CLASSPATH="%CLASSPATH%;lib/opendj-server-${opendj.server.version}.jar"
set CLASSPATH="%CLASSPATH%;lib/mail-${mail.version}.jar"
set CLASSPATH="%CLASSPATH%;lib/servlet-api-${servlet-api.version}.jar"
set CLASSPATH="%CLASSPATH%;lib/webservices-api-${webservices-api.version}.jar"
set CLASSPATH="%CLASSPATH%;lib/webservices-rt-${webservices-rt.version}.jar"
set CLASSPATH="%CLASSPATH%;lib/json-${json.version}.jar"
set CLASSPATH="%CLASSPATH%;lib/xalan-${xalan.version}.jar"
set CLASSPATH="%CLASSPATH%;lib/xercesImpl-${xercesj.version}.jar"
set CLASSPATH="%CLASSPATH%;lib/xml-apis-${xercesj.version}.jar"
set CLASSPATH="%CLASSPATH%;lib/xmlsec-${santuario.xmlsec.version}.jar"
set CLASSPATH="%CLASSPATH%;lib/openam-core-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;lib/openam-shared-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;lib/openam-dtd-schema-${project.version}.jar"
set CLASSPATH="%CLASSPATH%;lib/openam-rest-${project.version}.jar"

"\@JAVA_HOME@/bin/java.exe" -Xms64m -Xmx256m -classpath %CLASSPATH% -D"bootstrap.dir=@CONFIG_DIR@" -D"java.version.current=java.vm.version" -D"java.version.expected=1.4+"  -D"am.version.current=com.iplanet.am.version" -D"am.version.expected=@AM_VERSION@" -D"amconfig=AMConfig" -D"max_conn_pool=10" -D"min_conn_pool=1" -D"s1is.java.util.logging.config.class=com.sun.identity.log.s1is.LogConfigReader" -D"com.iplanet.services.configpath=@CONFIG_DIR@" -D"com.sun.identity.configFilePath=@CONFIG_DIR@" -D"com.iplanet.coreservices.configpath=@CONFIG_DIR@"  -D"LOG_COMPATMODE=Off" -D"com.iplanet.am.logstatus=INACTIVE" -D"com.iplanet.am.serverMode=false" com.sun.identity.log.cli.ISArchiveVerify %PARAMS%
endlocal
:END

