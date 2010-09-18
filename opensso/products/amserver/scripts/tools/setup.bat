@echo off
:
: DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
:  
: Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
: $Id: setup.bat,v 1.13 2009/01/28 05:34:46 ww203982 Exp $
:

if not "%JAVA_HOME%" == "" goto checkJavaHome
echo Please define JAVA_HOME environment variable before running this program
echo setup program will use the JVM defined in JAVA_HOME for all the CLI tools
goto exit

:checkJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto invalidJavaHome
goto validJavaHome

:invalidJavaHome
echo The defined JAVA_HOME environment variable is not correct
echo setup program will use the JVM defined in JAVA_HOME for all the CLI tools
goto exit

:validJavaHome
SETLOCAL
CALL %JAVA_HOME%\bin\java.exe -version 2>&1|more > java_version.txt
SET /P java_version=< java_version.txt
DEL java_version.txt
CALL :GET_VERSION_NUM %java_version:"1.= %
CALL :GET_MID_VERSION_NUM %java_version:.= %
IF "%java_version%" == "0" goto invalidJavaVersion
IF "%java_version%" == "1" goto invalidJavaVersion
IF "%java_version%" == "2" goto invalidJavaVersion
IF "%java_version%" == "3" goto invalidJavaVersion
goto runSetup

:invalidJavaVersion
echo This program is designed to work with 1.4 or newer JRE.
goto exit

:GET_VERSION_NUM
SET java_version=%3
goto exit

:GET_MID_VERSION_NUM
SET java_version=%1
goto exit

:runSetup
IF "%1" == "-h" SET help_print=yes
IF "%1" == "--help" SET help_print=yes
IF "%1" == "-l" SET path_log=%~2
IF "%1" == "--log" SET path_log=%~2
IF "%1" == "-d" SET path_debug=%~2
IF "%1" == "--debug" SET path_debug=%~2
IF "%1" == "-p" SET path_AMConfig=%~2
IF "%1" == "--path" SET path_AMConfig=%~2
IF "%3" == "-l" SET path_log=%~4
IF "%3" == "--log" SET path_log=%~4
IF "%3" == "-d" SET path_debug=%~4
IF "%3" == "--debug" SET path_debug=%~4
IF "%3" == "-p" SET path_AMConfig=%~4
IF "%3" == "--path" SET path_AMConfig=%~4
IF "%5" == "-l" SET path_log=%~6
IF "%5" == "--log" SET path_log=%~6
IF "%5" == "-d" SET path_debug=%~6
IF "%5" == "--debug" SET path_debug=%~6
IF "%5" == "-p" SET path_AMConfig=%~6
IF "%5" == "--path" SET path_AMConfig=%~6

"%JAVA_HOME%/bin/java.exe" -D"load.config=yes" -D"help.print=%help_print%" -D"path.AMConfig=%path_AMConfig%" -D"path.log=%path_log%" -D"path.debug=%path_debug%" -cp "lib/amserver.jar;lib/amadm_setup.jar;lib/opensso-sharedlib.jar;lib/OpenDS.jar;resources" com.sun.identity.tools.bundles.Main
ENDLOCAL

:exit
exit /b 1
