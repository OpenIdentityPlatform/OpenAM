@echo off

REM DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
REM Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
  
REM The contents of this file are subject to the terms
REM of the Common Development and Distribution License
REM (the License). You may not use this file except in
REM compliance with the License.
REM
REM You can obtain a copy of the License at
REM https://opensso.dev.java.net/public/CDDLv1.0.html or
REM opensso/legal/CDDLv1.0.txt
REM See the License for the specific language governing
REM permission and limitations under the License.
REM
REM When distributing Covered Code, include this CDDL
REM Header Notice in each file and include the License file
REM at opensso/legal/CDDLv1.0.txt.
REM If applicable, add the following below the CDDL Header,
REM with the fields enclosed by brackets [] replaced by
REM your own identifying information:
REM "Portions Copyrighted [year] [name of copyright owner]"
REM
REM $Id: wssagentadmin.bat,v 1.1 2009/06/12 22:19:28 huacui Exp $
REM
REM

setlocal
if "%AGENT_HOME%"=="" set AGENT_HOME=%~dp0..

if not "%JAVA_HOME%"=="" set JAVA_VM=%JAVA_HOME%\bin\java.exe
if "%JAVA_VM%"=="" set JAVA_VM=java.exe

set AGENT_CLASSPATH=%AGENT_HOME%\lib\opensso-installtools-launcher.jar;%AGENT_HOME%\config

"%JAVA_VM%" -classpath "%AGENT_CLASSPATH%" com.sun.identity.install.tools.launch.AdminToolLauncher %*

set JAVA_VM=
set AGENT_CLASSPATH=
set AGENT_HOME=

endlocal
