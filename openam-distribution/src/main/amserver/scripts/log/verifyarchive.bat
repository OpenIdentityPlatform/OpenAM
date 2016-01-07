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

: Portions Copyrighted 2010-2016 ForgeRock AS.

setlocal enabledelayedexpansion

IF NOT DEFINED JAVA_HOME (
	set JAVA_HOME="\@JAVA_HOME@"
)

set TOOLS_HOME=@TOOLS_HOME@
set ORIG_CLASSPATH=%CLASSPATH%
set CLASSPATH=@CONFIG_DIR@
set CLASSPATH=%CLASSPATH%;%TOOLS_HOME%/classes;%TOOLS_HOME%/resources;%TOOLS_HOME%/lib/*

IF DEFINED ORIG_CLASSPATH (
	set CLASSPATH=%ORIG_CLASSPATH%;%CLASSPATH%
)

"%JAVA_HOME%/bin/java.exe" -Xms64m -Xmx256m -cp %CLASSPATH% org.forgerock.audit.handlers.csv.CsvSecureArchiveVerifierCli %*
endlocal
:END

