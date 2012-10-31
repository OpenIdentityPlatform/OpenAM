<%--
/*
 * ident "@(#)Table.jsp 1.9 04/05/07 SMI"
 * 
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * Use is subject to license terms.
 */
--%>
<%@ page language="java" %>
<%@ page import="com.sun.web.ui.common.CCI18N" %>
<%@taglib uri="/WEB-INF/tld/com_iplanet_jato/jato.tld" prefix="jato"%> 
<%@taglib uri="/WEB-INF/tld/com_sun_web_ui/cc.tld" prefix="cc"%>

<%
    // Get query parameters.
    String windowTitle = (request.getParameter("windowTitle") != null)
	? request.getParameter("windowTitle") : "";
    String baseName = (request.getParameter("baseName") != null)
	? request.getParameter("baseName") : "";
%>

<jato:useViewBean className="com.sun.web.ui.servlet.table.TableViewBean">

<!-- Header -->
<cc:header name="Header"
 pageTitle="<%=windowTitle %>"
 copyrightYear="2003"
 bundleID="appBundle"
 baseName="<%=baseName %>"
 preserveFocus="true"
 preserveScroll="true"
 isPopup="true">

<!-- Masthead -->
<cc:secondarymasthead name="Masthead" bundleID="appBundle" />

<!-- Pagelet -->
<cc:includepagelet name="Pagelet" />

</cc:header>
</jato:useViewBean>
