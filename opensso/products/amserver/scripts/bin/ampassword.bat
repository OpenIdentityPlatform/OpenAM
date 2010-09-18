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

set TOOLS_HOME="@TOOLS_HOME@"

setlocal
:WHILE
if x%1==x goto WEND
set PARAMS=%PARAMS% %1
shift
goto WHILE
:WEND

set TOOLS_CLASSPATH="@CONFIG_DIR@";%TOOLS_HOME%/lib/amadm_setup.jar;%TOOLS_HOME%/lib/OpenDS.jar;%TOOLS_HOME%/lib/activation.jar;%TOOLS_HOME%/lib/db.jar;%TOOLS_HOME%/lib/j2ee.jar;%TOOLS_HOME%/lib/webservices-api.jar;%TOOLS_HOME%/lib/webservices-rt.jar;%TOOLS_HOME%/lib/webservices-tools.jar;%TOOLS_HOME%/lib/jaxrpc-spi.jar;%TOOLS_HOME%/lib/mail.jar;%TOOLS_HOME%/lib/amserver.jar;%TOOLS_HOME%/lib/opensso-sharedlib.jar;%TOOLS_HOME%/resources;%TOOLS_HOME%/config


"@JAVA_HOME@/bin/java.exe" -Xms64m -Xmx256m -classpath %TOOLS_CLASSPATH% -D"bootstrap.dir=@CONFIG_DIR@" -D"java.version.current=java.vm.version" -D"java.version.expected=1.4+"  -D"am.version.current=com.iplanet.am.version" -D"am.version.expected=@AM_VERSION@" com.iplanet.services.ldap.ServerConfigMgr %PARAMS%
endlocal
:END
