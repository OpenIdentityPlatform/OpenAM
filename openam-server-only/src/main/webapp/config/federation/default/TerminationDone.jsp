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

   $Id: TerminationDone.jsp,v 1.4 2008/06/25 05:48:34 qcheng Exp $

--%>

<%@ page language="java"
import="com.sun.liberty.LibertyManager"
%>
<%@ include file="Header.jsp"%>
<center>

<%
    // Alias processing
    String providerAlias =
        request.getParameter(LibertyManager.getMetaAliasKey());
    if (providerAlias == null || providerAlias.length() < 1) {
        response.sendError(response.SC_INTERNAL_SERVER_ERROR,
            "Provider Alias not found");
        return;
    }
    String realm = LibertyManager.getRealmByMetaAlias(providerAlias);
    String providerId = LibertyManager.getEntityID(providerAlias);
    String providerRole = LibertyManager.getProviderRole(providerAlias);
    String HOME_URI = "";
    if (providerId != null) {
        HOME_URI = LibertyManager.getHomeURL(realm, providerId, providerRole);
    }

    if (LibertyManager.isTerminationSuccess(request)) {
%>
        <p><b>The user has successfully terminated federation with the provider.</b></p>

<% } else if (LibertyManager.isTerminationCancelled(request)) { %>

       <p><b>The user has cancelled federation termination.</b></p>

<%  } else { %>

       <p><b>Unable to terminate federation with the provider.</b></p>

<%  } %>

<p>
<% if (HOME_URI == null){ %>
    <a href="http://www.sun.com">Continue</a>
<%}else {%>
    <a href="<%=HOME_URI%>">Continue</a>
<% } %>
</p>
</center>

<%@ include file="Footer.jsp"%>

