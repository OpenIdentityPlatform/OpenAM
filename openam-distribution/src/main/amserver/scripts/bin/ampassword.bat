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
: $Id: ampassword.bat,v 1.18 2009/01/28 05:34:45 ww203982 Exp $
:

: Portions Copyrighted 2010-2011 ForgeRock AS

set TOOLS_HOME="@TOOLS_HOME@"

setlocal
:WHILE
if x%1==x goto WEND
set PARAMS=%PARAMS% %1
shift
goto WHILE
:WEND

set CLASSPATH="@CONFIG_DIR@"
set CLASSPATH="%CLASSPATH%;lib/openam-distribution-amadmsetup-10.1.0-SNAPSHOT.jar"
set CLASSPATH="%CLASSPATH%;lib/opendj-server-2.4.6.jar"
set CLASSPATH="%CLASSPATH%;lib/mail-1.4.5.jar"
set CLASSPATH="%CLASSPATH%;lib/j2ee-2007-18-10.jar"
set CLASSPATH="%CLASSPATH%;lib/webservices-api-2009-14-01.jar"
set CLASSPATH="%CLASSPATH%;lib/webservices-rt-2009-29-07.jar"
set CLASSPATH="%CLASSPATH%;lib/wwebservices-tools-2.1-b16.jar"
set CLASSPATH="%CLASSPATH%;lib/json-20090211.jar"
set CLASSPATH="%CLASSPATH%;lib/xalan-2.7.1.jar"
set CLASSPATH="%CLASSPATH%;lib/xercesImpl-2.10.0.jar"
set CLASSPATH="%CLASSPATH%;lib/xml-apis-1.4.01.jar"
set CLASSPATH="%CLASSPATH%;lib/xmlsec-1.3.0.jar"
set CLASSPATH="%CLASSPATH%;lib/openam-core-10.1.0-SNAPSHOT.jar"
set CLASSPATH="%CLASSPATH%;lib/openam-shared-10.1.0-SNAPSHOT.jar"
set CLASSPATH="%CLASSPATH%;lib/openam-dtd-schema-10.1.0-SNAPSHOT.jar"
set CLASSPATH="%CLASSPATH%;lib/openam-rest-10.1.0-SNAPSHOT.jar"

"@JAVA_HOME@/bin/java.exe" -Xms64m -Xmx256m -classpath %CLASSPATH% -D"bootstrap.dir=@CONFIG_DIR@" -D"java.version.current=java.vm.version" -D"java.version.expected=1.4+"  -D"am.version.current=com.iplanet.am.version" -D"am.version.expected=@AM_VERSION@" com.iplanet.services.ldap.ServerConfigMgr %PARAMS%
endlocal
:END
