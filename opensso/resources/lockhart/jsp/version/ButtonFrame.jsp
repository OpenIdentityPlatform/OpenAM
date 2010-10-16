<%--
/**
 * ident "@(#)ButtonFrame.jsp 1.5 04/08/24 SMI"
 * 
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * Use is subject to license terms.
 */
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

<jato:useViewBean className="com.sun.web.ui.servlet.version.ButtonFrameViewBean">

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
