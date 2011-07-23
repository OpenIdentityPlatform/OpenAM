<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
  
   The contents of this file are subject to the terms
   of the Common Development and Distribution License
   (the License). You may not use this file except in
   compliance with the License.
                                                                                                                                  
   You can obtain a copy of the License at
   https://opensso.dev.java.net/public/CDDLv1.0.html or
   opensso/legal/CDDLv1.0.txt
   See the License for the specific language governing
   permission and limitations under the License.
                                                                                                                                  
   When distributing Covered Code, include this CDDL
   Header Notice in each file and include the License file
   at opensso/legal/CDDLv1.0.txt.
   If applicable, add the following below the CDDL Header,
   with the fields enclosed by brackets [] replaced by
   your own identifying information:
   "Portions Copyrighted [year] [name of copyright owner]"

   $Id: cli.jsp,v 1.4 2008/09/09 21:31:43 qcheng Exp $

--%>

<%@ page
import="com.sun.identity.cli.StringOutputWriter,
        com.sun.identity.cli.CLIConstants,
        com.sun.identity.cli.CLIRequest,
        com.sun.identity.cli.CommandManager,
        com.iplanet.sso.SSOException,
        com.iplanet.sso.SSOToken,
        com.iplanet.sso.SSOTokenManager,
        java.util.HashMap,
        java.util.Map"
%>

<%
    StringOutputWriter outputWriter = new StringOutputWriter();
    Map env = new HashMap();
    env.put(CLIConstants.SYS_PROPERTY_OUTPUT_WRITER, outputWriter);
    env.put(CLIConstants.ARGUMENT_LOCALE, request.getLocale());
    env.put(CLIConstants.SYS_PROPERTY_DEFINITION_FILES,
        "com.sun.identity.federation.cli.FederationManager");
    env.put(CLIConstants.SYS_PROPERTY_COMMAND_NAME, "ssoadm");
    CommandManager cmdManager = new CommandManager(env);

    SSOTokenManager manager = SSOTokenManager.getInstance();
    SSOToken ssoToken = null;
    try {
        ssoToken = manager.createSSOToken(request);
    } catch (SSOException se) {
        // do nothing
    }

%>
