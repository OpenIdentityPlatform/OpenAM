<%--
/**
 * ident "@(#)Masthead.jsp 1.18 04/08/23 SMI"
 * 
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * Use is subject to license terms.
 */
--%>
<%@ page language="java" %>
<%@page import="com.sun.web.ui.common.CCI18N" %>
<%@taglib uri="/WEB-INF/tld/com_iplanet_jato/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/tld/com_sun_web_ui/cc.tld" prefix="cc" %>

<%
    // Get query parameters.
    String windowTitle = (request.getParameter("windowTitle") != null)
	? request.getParameter("windowTitle") : "";
    String mastheadTitle = (request.getParameter("mastheadTitle") != null)
	? request.getParameter("mastheadTitle")	: "";
    String mastheadAlt = (request.getParameter("mastheadAlt") != null)
	? request.getParameter("mastheadAlt") : "";
    String pageTitle = (request.getParameter("pageTitle") != null)
	? request.getParameter("pageTitle") : "help.pageTitle";
    String helpLogoWidth = (request.getParameter("helpLogoWidth") != null)
	? request.getParameter("helpLogoWidth")	: "";
    String helpLogoHeight= (request.getParameter("helpLogoHeight") != null)
	? request.getParameter("helpLogoHeight") : "";	
    String showCloseButton = (request.getParameter("showCloseButton") != null)
	?  request.getParameter("showCloseButton") : "true";

    // Set default value for close button.
    if (!(showCloseButton.equalsIgnoreCase("false")))
	showCloseButton = "true";
%>

<jato:useViewBean className="com.sun.web.ui.servlet.help.MastheadViewBean">

<!-- Header -->
<cc:header name="Header"
 pageTitle="<%=windowTitle %>"
 copyrightYear="2004"
 baseName="com.sun.web.ui.resources.Resources"
 bundleID="helpBundle">


<form> 
<!-- Secondary Masthead -->
<div class="HlpMst">
<cc:secondarymasthead name="Masthead" src="<%=mastheadTitle %>" alt="<%=mastheadAlt %>" bundleID="helpBundle" width="<%=helpLogoWidth %>" height="<%=helpLogoHeight %>" />
</div>

<!-- Page Title -->
<div class="HlpTtl">
<cc:pagetitle name="PageTitle" bundleID="helpBundle"
 pageTitleText="<%=pageTitle %>"
 showPageTitleSeparator="true"
 showPageButtonsTop="<%=showCloseButton %>"
 showPageButtonsBottom="false" />
</div>

</form>

</cc:header>
</jato:useViewBean>
