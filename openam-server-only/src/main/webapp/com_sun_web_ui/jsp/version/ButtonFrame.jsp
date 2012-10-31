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

   $Id: ButtonFrame.jsp,v 1.1 2009/08/05 20:15:51 veiming Exp $

--%>

<%@ page language="java" %>
<%@taglib uri="/WEB-INF/tld/com_iplanet_jato/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/tld/com_sun_web_ui/cc.tld" prefix="cc" %>

<%@ page import="com.sun.web.ui.common.CCI18N" %>
<%@ page import="java.net.URLDecoder" %>
<%@ page import="com.sun.web.ui.common.CCDebug" %>


<%
    // obtain the versionFile and productSrc attrs from the request params
    String versionNumber = request.getParameter("versionNumber") != null
        ? request.getParameter("versionNumber") : "";
    versionNumber = URLDecoder.decode(versionNumber, CCI18N.UTF8_ENCODING);
    CCDebug.trace3("DEC VERSION TXT: " + versionNumber);
%>

<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
 <script>

    window.parent.frames[1].onload = window.parent.frames[2].checkVersionTxt;

    function checkVersionTxt() {

    	var loc = window.location.href;
    	var strArray = loc.split("?");
    	var versionParm = strArray[1];
    	var versionArray = versionParm.split("=");
	var versionNumber;
	if (window.parent.frames[2].document.buttonFrameForm) {
	  versionNumber = 
	    window.parent.frames[2].document.buttonFrameForm.elements[1].value;
	} else { 
	   versionNumber = unescape(versionArray[1]);
	}

        var divList;
          divList =
              window.parent.frames[1].document.getElementsByTagName("div");
          if (divList.length > 0) {
            for (var i=0; i<divList.length; i++) {
                if (divList[i].className == "VrsHdrTxt") {
                    divList[i].childNodes[0].nodeValue = versionNumber;
                    break;
                }
            }
          }
    }
</script>
</head>

<jato:useViewBean className="com.sun.identity.console.version.ButtonFrameViewBean">

<!-- Header -->
<cc:header name="Header"
 pageTitle=""
 styleClass="VrsBtnBdy"
 baseName="com.sun.web.ui.resources.Resources"
 bundleID="testBundle"
 onLoad="document.buttonFrameForm.elements[0].focus();">

<cc:form name="buttonFrameForm" method="post">
<div class="VrsBtnAryDiv">
  <cc:button name="Close" defaultValue="help.close" type="Default" 
   bundleID="testBundle"
   onClick="javascript: parent.close(); return false;" />

  <cc:hidden elementId="com_sun_web_ui_vtxt" 
	name="com_sun_web_ui_vtxt" defaultValue="<%= versionNumber %>" />
</div>
</cc:form>

</cc:header>

</jato:useViewBean>
