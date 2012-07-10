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
: $Id: ssopatch.bat,v 1.2 2009/03/10 23:54:14 veiming Exp $
:

set TOOLS_HOME=%~dp$PATH:0
if '%TOOLS_HOME%'=='' set TOOLS_HOME=%~dps0

setlocal

"java.exe" -Xms256m -Xmx512m -cp "%TOOLS_HOME%/lib/ssopatch.jar;%TOOLS_HOME%/lib/ssomanifest.jar;%TOOLS_HOME%/lib/amadm_setup.jar;%TOOLS_HOME%/resources" com.sun.identity.tools.patch.Patch %*
endlocal
:END
