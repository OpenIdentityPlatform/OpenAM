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
: $Id: ssoadm.bat,v 1.4 2009/08/07 16:26:31 veiming Exp $
:

set TOOLS_HOME="@TOOLS_HOME@"

setlocal

"@JAVA_HOME@/bin/java.exe" -Xms256m -Xmx512m -cp "@CONFIG_DIR@;%TOOLS_HOME%/classes;%TOOLS_HOME%/resources;%TOOLS_HOME%/lib/amadm_setup.jar;%TOOLS_HOME%/lib/OpenDJ.jar;%TOOLS_HOME%/classes;%TOOLS_HOME%/lib/mail.jar;%TOOLS_HOME%/lib/j2ee.jar;%TOOLS_HOME%/lib/webservices-api.jar;%TOOLS_HOME%/lib/webservices-rt.jar;%TOOLS_HOME%/lib/webservices-tools.jar;%TOOLS_HOME%/lib/xsdlib.jar;%TOOLS_HOME%/lib/amserver.jar;%TOOLS_HOME%/lib/opensso-sharedlib.jar;%TOOLS_HOME%/lib/opensso.jar;%TOOLS_HOME%/lib/openfedlib.jar;${TOOLS_HOME}/lib/jdmkrt.jar;${TOOLS_HOME}/lib/jdmktk.jar;${TOOLS_HOME}/lib/json.jar" -D"sun.net.client.defaultConnectTimeout=3000" -D"ssoadm=true" -D"com.iplanet.am.serverMode=false" -D"com.sun.identity.sm.notification.enabled=false" -D"bootstrap.dir=@CONFIG_DIR@" -D"com.iplanet.services.debug.directory=@DEBUG_DIR@" -D"com.sun.identity.log.dir=@LOG_DIR@" -D"definitionFiles=com.sun.identity.cli.AccessManager,com.sun.identity.federation.cli.FederationManager,com.sun.identity.entitlement.opensso.cli.EntitlementManager" -D"commandName=ssoadm" -D"amconfig=AMConfig" -D"java.util.logging.manager=com.sun.identity.log.LogManager" -D"java.util.logging.config.class=com.sun.identity.log.s1is.LogConfigReader" -D"java.version.current=java.vm.version" -D"java.version.expected=1.4+" -D"am.version.current=com.iplanet.am.version" -D"am.version.expected=@AM_VERSION@" -D"com.iplanet.am.sdk.package=com.iplanet.am.sdk.remote" -D"com.sun.identity.idm.remote.notification.enabled=false" -D"java.util.logging.config.class=com.sun.identity.log.s1is.LogConfigReader" com.sun.identity.cli.CommandManager %*
endlocal
:END
