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

   $Id: AgentAdd.jsp,v 1.6 2008/09/20 06:38:47 veiming Exp $

--%>




<%@ page info="AgentAdd" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<jato:useViewBean
    className="com.sun.identity.console.agentconfig.AgentAddViewBean"
    fireChildDisplayEvents="true" >

<cc:i18nbundle baseName="amConsole" id="amConsole"
    locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>

<cc:header name="hdrCommon" pageTitle="webconsole.title" bundleID="amConsole" copyrightYear="2007" fireDisplayEvents="true" onLoad="toggleTextBox()">

<cc:form name="AgentAdd" method="post" defaultCommandChild="/button1">
<script language="javascript">
    function toggleTextBox() {
        var formElement = document.forms["AgentAdd"];
        var nodeList = formElement.elements['AgentAdd.radioChoice'];
        for (var i = 0; i < nodeList.length; i++) {
            if (nodeList.item(i).checked) {
                SelectLocal(nodeList.item(i));
            }
        }
    }

    function confirmLogout() {
        return confirm("<cc:text name="txtLogout" defaultValue="masthead.logoutMessage" bundleID="amConsole"/>");
    }
    function SelectLocal(radio) {
       var formElement = document.forms["AgentAdd"];
       if(radio.value=="local") {
           formElement.elements['AgentAdd.tfServerURL'].disabled = true;
       } else if (radio.value=="centralized") {
           formElement.elements['AgentAdd.tfServerURL'].disabled = false;
       } 
    } 
</script>
<cc:primarymasthead name="mhCommon" bundleID="amConsole"  logoutOnClick="return confirmLogout();" locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>
<cc:breadcrumbs name="breadCrumb" bundleID="amConsole" />

<table border="0" cellpadding="10" cellspacing="0" width="100%">
    <tr>
	<td>
	<cc:alertinline name="ialertCommon" bundleID="amConsole" />
	</td>
    </tr>
</table>

<%-- PAGE CONTENT --------------------------------------------------------- --%>
<cc:pagetitle name="pgtitleTwoBtns" bundleID="amConsole" pageTitleText="page.title.create.agent" showPageTitleSeparator="true" viewMenuLabel="" pageTitleHelpMessage="" showPageButtonsTop="true" showPageButtonsBottom="false" />

<cc:propertysheet name="propertyAttributes" bundleID="amConsole" showJumpLinks="true"/>

</cc:form>

</cc:header>
</jato:useViewBean>
