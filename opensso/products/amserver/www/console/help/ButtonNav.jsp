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

   $Id: ButtonNav.jsp,v 1.1 2009/08/04 21:54:49 veiming Exp $

--%>

<%@page language="java" %>
<%@page import="com.sun.web.ui.common.CCI18N" %>
<%@page import="com.sun.web.ui.common.CCImage" %>
<%@taglib uri="/WEB-INF/tld/com_iplanet_jato/jato.tld" prefix="jato"%>
<%@taglib uri="/WEB-INF/tld/com_sun_web_ui/cc.tld" prefix="cc"%>

<%
    // Get query parameters.
    String windowTitle = (request.getParameter("windowTitle") != null)
	? request.getParameter("windowTitle") : "";

    // Get the image src values.
    String backSrc = CCImage.HELP_BACK;
    String forwardSrc = CCImage.HELP_FORWARD;
    String printSrc = CCImage.HELP_PRINT;
%>

<jato:useViewBean className="com.sun.identity.console.help.ButtonNavViewBean">

<!-- Header -->
<cc:header name="Header"
 pageTitle="<%=windowTitle %>"
 styleClass="HlpBtnNavBdy"
 baseName="com.sun.web.ui.resources.Resources"
 bundleID="help2Bundle">

<cc:form name="buttonNavForm" method="post">
  <table border="0" cellspacing="0" cellpadding="0" width="100%">
    <tr>
      <td nowrap="nowrap">
        <div class="HlpBtnDiv"><cc:button name="BackButton" bundleID="help2Bundle" alt="help2.backButtonTitle" src="<%=backSrc %>" title="help2.backButtonTitle" type="icon" onClick="javascript:window.parent.contentFrame.focus(); window.parent.contentFrame.history.back(); return false;" /><cc:spacer name="Spacer1" width="5" height="1" /><cc:button name="ForwardButton" bundleID="help2Bundle" alt="help2.forwardButtonTitle" src="<%=forwardSrc %>" title="help2.forwardButtonTitle" type="icon" onClick="javascript:window.parent.contentFrame.focus(); window.parent.contentFrame.history.forward(); return false;" /><cc:spacer name="Spacer2" width="10" height="1" /><cc:button name="PrintButton" bundleID="help2Bundle" alt="help2.printButtonTitle" src="<%=printSrc %>" title="help2.printButtonTitle" type="icon" onClick="window.parent.contentFrame.focus(); window.parent.contentFrame.print()" /></div>
      </td>
    </tr>
  </table>

</cc:form>
</cc:header>
</jato:useViewBean>
