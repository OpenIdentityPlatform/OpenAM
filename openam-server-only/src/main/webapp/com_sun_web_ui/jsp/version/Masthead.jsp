<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

   Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved

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

   $Id: Masthead.jsp,v 1.1 2009/08/05 20:15:51 veiming Exp $

--%>

<%@ page language="java" %>
<%@page import="com.sun.identity.console.version.VersionViewBean" %>

<%@taglib uri="/WEB-INF/tld/com_iplanet_jato/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/tld/com_sun_web_ui/cc.tld" prefix="cc" %>

<%
    // obtain the versionFile and productSrc attrs from the request params 
    String versionFile = request.getParameter("versionFile") != null
	? request.getParameter("versionFile") : "";
    String productNameSrc = request.getParameter("productNameSrc") != null
        ? request.getParameter("productNameSrc") : "";
    String productNameHeight =
	request.getParameter("productNameHeight") != null
        ? request.getParameter("productNameHeight") : "";
    String productNameWidth = request.getParameter("productNameWidth") != null
        ? request.getParameter("productNameWidth") : "";

    versionFile = VersionViewBean.validateVersionFile(
        request, versionFile);
    productNameSrc = VersionViewBean.validateProductImage(
        request, productNameSrc);
%>

<jato:useViewBean className="com.sun.identity.console.version.MastheadViewBean">

<!-- Header -->
<cc:header name="Header"
 pageTitle=""
 styleClass="VrsMstBdy"
 baseName="com.sun.web.ui.resources.Resources"
 bundleID="testBundle">             
       
<cc:versionbanner versionFile="<%=versionFile%>" productNameSrc="<%=productNameSrc%>" 
 productNameHeight="<%=productNameHeight%>" productNameWidth="<%=productNameWidth%>" />

</cc:header>

</jato:useViewBean>
