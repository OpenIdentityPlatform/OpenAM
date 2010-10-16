@echo off

: DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
: Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
  
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
: $Id: ssoupgrade.bat,v 1.3 2009/01/28 05:35:20 ww203982 Exp $

:
setlocal

set TOOLS_HOME="STAGING_DIR"
set CONFIG_DIR="FAM_CONFIG_DIR"
set UPGRADE_DIR="FAM_UPGRADE_DIR"


"%JAVA_HOME%/bin/java.exe" -Xms64m -Xmx256m -cp  "%UPGRADE_DIR%/locale;%UPGRADE_DIR%/lib/amadm_setup.jar;%UPGRADE_DIR%/lib/upgrade.jar;%UPGRADE_DIR%/lib/legacy.jar;%CONFIG_DIR%;%TOOLS_HOME%/locale;%TOOLS_HOME%/lib/amadm_setup.jar;%TOOLS_HOME%/lib/OpenDS.jar;%TOOLS_HOME%/classes;%TOOLS_HOME%/lib/mail.jar;%TOOLS_HOME%/lib/j2ee.jar;%TOOLS_HOME%/lib/webservices-api.jar;%TOOLS_HOME%/lib/webservices-rt.jar;%TOOLS_HOME%/lib/webservices-tools.jar;%TOOLS_HOME%/lib/xsdlib.jar;%TOOLS_HOME%/lib/amserver.jar;%TOOLS_HOME%/lib/opensso-sharedlib.jar;%TOOLS_HOME%/lib/opensso.jar;%TOOLS_HOME%/lib/openfedlib.jar" -D"com.iplanet.am.serverMode=true" -D"bootstrap.dir=%CONFIG_DIR%" -D"definitionFiles=com.sun.identity.cli.AccessManager,com.sun.identity.federation.cli.FederationManager" -D"amconfig=AMConfig" -D"java.util.logging.manager=com.sun.identity.log.LogManager" -D"java.util.logging.config.class=com.sun.identity.log.s1is.LogConfigReader" -D"java.version.current=java.vm.version" -D"java.version.expected=1.4+" -D"am.version.current=com.iplanet.am.version" -D"am.version.expected=8.0" -D"configDir=%CONFIG_DIR%" com.sun.identity.upgrade.FAMUpgrade %*
endlocal
:END
