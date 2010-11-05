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

   $Id: Masthead.jsp,v 1.1 2009/08/04 21:54:49 veiming Exp $

--%>

<%@page language="java" %>
<%@page import="com.sun.web.ui.common.CCI18N" %>
<%@page import="com.sun.identity.console.help.Help2ViewBean" %>

<%@taglib uri="/WEB-INF/tld/com_iplanet_jato/jato.tld" prefix="jato"%>
<%@taglib uri="/WEB-INF/tld/com_sun_web_ui/cc.tld" prefix="cc"%>

<%
    // Get query parameters.
    String windowTitle = (request.getParameter("windowTitle") != null)
	? request.getParameter("windowTitle") : "";
    String mastheadTitle = (request.getParameter("mastheadTitle") != null)
	? request.getParameter("mastheadTitle") : "";
    String mastheadAlt = (request.getParameter("mastheadAlt") != null)
	? request.getParameter("mastheadAlt") : "";
    String pageTitle = (request.getParameter("pageTitle") != null)
	? request.getParameter("pageTitle") : "help.pageTitle";
    String helpLogoWidth = (request.getParameter("helpLogoWidth") != null)
        ? request.getParameter("helpLogoWidth") : "";
    String helpLogoHeight= (request.getParameter("helpLogoHeight") != null)
        ? request.getParameter("helpLogoHeight") : "";

    windowTitle = Help2ViewBean.escapeHTML(windowTitle);
%>

<jato:useViewBean className="com.sun.identity.console.help.MastheadViewBean">

<!-- Header -->
<cc:header
 name="Header"
 pageTitle="<%=windowTitle %>"
 styleClass="HlpMstTtlBdy"
 baseName="com.sun.web.ui.resources.Resources"
 bundleID="help2Bundle">

<cc:form name="mastheadForm" method="post">

<!-- Secondary Masthead -->
<cc:secondarymasthead
 name="Masthead"
 src="<%=mastheadTitle %>"
 alt="<%=mastheadAlt %>"
 bundleID="help2Bundle"
 width="<%=helpLogoWidth %>"
 height="<%=helpLogoHeight %>" />

<!-- Page Title -->
<cc:pagetitle name="PageTitle" bundleID="help2Bundle"
 pageTitleText="<%=pageTitle %>"
 showPageTitleSeparator="true"
 showPageButtonsTop="true"
 showPageButtonsBottom="false" />

</cc:form>
</cc:header>
</jato:useViewBean>
