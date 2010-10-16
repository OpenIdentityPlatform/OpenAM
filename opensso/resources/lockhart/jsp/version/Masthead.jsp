<%--
/**
 * ident "@(#)Masthead.jsp 1.3 04/08/13 SMI"
 * 
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * Use is subject to license terms.
 */
--%>
<%@ page language="java" %>
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
%>

<jato:useViewBean className="com.sun.web.ui.servlet.version.MastheadViewBean">

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
