<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

   Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved

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

   $Id: testSessionPropChange.jsp,v 1.2 2009/02/27 22:54:02 srivenigan Exp $

--%>

<%-- 
This jsp is used by session property change notification test. It 
gets session id from http POST request, gets sso token and add/modifies
proptected properties on that token. The name of protected property
being added is specified in the SessionProperty resource bundle
under session module in resources directory.
--%>

<%@ page
import="com.iplanet.sso.SSOToken,
        com.iplanet.sso.SSOTokenManager"
%>
<%
        try {
            String method = request.getMethod();
            if ("POST".equals(request.getMethod()) != true) {
                response.sendError(405);
                return;
            }
            String tokid = request.getParameter("IDToken");
            out.println("Token id: " + tokid);

            SSOTokenManager stMgr = SSOTokenManager.getInstance();
            SSOToken stok = stMgr.createSSOToken(tokid);

            stok.setProperty("testprotectedproperty", "pro-0");
            String oldServerid = stok.getProperty("testprotectedproperty");
            out.println(" old serverid = " + oldServerid);

            stok.setProperty("testprotectedproperty", "pro-1");
            String newServerid = stok.getProperty("testprotectedproperty");
            out.println(" new serverid = " + newServerid);

            stok.setProperty("testprotectedproperty", "pro-2");
            newServerid = stok.getProperty("testprotectedproperty");
            out.println(" new serverid = " + newServerid);

            out.flush();
       } catch (Exception e) {
           out.println(e.getMessage());
           e.printStackTrace();
       }
%>
