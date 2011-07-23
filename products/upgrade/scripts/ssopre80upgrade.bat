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
: $Id: ssopre80upgrade.bat,v 1.3 2009/01/28 05:35:20 ww203982 Exp $
:

setlocal

echo ===================================================
echo OpenSSO 8.0 Pre-Upgrade
echo ===================================================
echo.

set /p UPGRADE_DIR=Enter the OpenSSO 8.0 base directory :
echo.
set /p STAGING_DIR=Enter the OpenSSO 8.0 staging directory :
echo.
set /p CONFIG_DIR=Enter the OpenSSO 8.0 configuration directory :

"%JAVA_HOME%/bin/java.exe" -Xms64m -Xmx256m -cp "%CONFIG_DIR%;%STAGING_DIR%\WEB-INF\lib\opensso-sharedlib.jar;%UPGRADE_DIR%\upgrade\lib\upgrade.jar;%STAGING_DIR%\WEB-INF\lib\amserver.jar" -D"basedir=%UPGRADE_DIR%" -D"stagingDir=%STAGING_DIR%" -D"configDir=%CONFIG_DIR%" com.sun.identity.upgrade.FAMPreUpgrade %*
endlocal
:END
echo.
echo ===================================================
