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

   $Id: post.jsp,v 1.3 2008/06/25 05:48:38 qcheng Exp $

--%>
<%--
   Portions Copyrighted 2014 ForgeRock AS
--%>
<%@page contentType="text/html; charset=UTF-8"
    import="com.sun.identity.wsfederation.common.WSFederationConstants"
%>
<%
    String targetURL = 
        (String)request.getAttribute(WSFederationConstants.POST_ACTION);
    String wa = 
        (String)request.getAttribute(WSFederationConstants.POST_WA);
    String wctx = 
        (String)request.getAttribute(WSFederationConstants.POST_WCTX);
    String wresult = 
        (String)request.getAttribute(WSFederationConstants.POST_WRESULT);
%>
<html xmlns="https://www.w3.org/1999/xhtml">
  <head>
      <title>Access rights validated</title>
  </head>
  <body onLoad="document.forms[0].submit()">
    <form method="POST" action="<%=targetURL%>">
      <input type="hidden" name="wa" value="<%=wa%>">
    <%
    if (wctx != null && wctx.length() != 0) {
    %>
      <input type="hidden" name="wctx" value="<%=wctx%>">
    <%
    }
    %>
      <input type="hidden" name="wresult" value="<%=wresult%>">
    </form>
  </body>
