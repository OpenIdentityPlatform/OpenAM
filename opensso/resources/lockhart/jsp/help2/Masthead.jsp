<%--
/**
 * ident "@(#)Masthead.jsp 1.8 04/08/23 SMI"
 * 
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * Use is subject to license terms.
 */
--%>
<%@page language="java" %>
<%@page import="com.sun.web.ui.common.CCI18N" %>
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
%>

<jato:useViewBean className="com.sun.web.ui.servlet.help2.MastheadViewBean">

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
