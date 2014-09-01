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

   $Id: SMProfile.jsp,v 1.4 2008/07/22 21:43:09 babysunil Exp $

--%>



	
<%@ page info="SMProfile" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<jato:useViewBean
    className="com.sun.identity.console.session.SMProfileViewBean"
    fireChildDisplayEvents="true" >

<cc:i18nbundle baseName="amConsole" id="amConsole"
    locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>

<cc:header name="hdrCommon" pageTitle="webconsole.title" bundleID="amConsole" copyrightYear="2004" fireDisplayEvents="true" onLoad="initView()">

<script language="javascript" src="../console/js/am.js">
</script>
<script language="javascript">
    function initView() {
	ccSetButtonDisabled('SMProfile.tblButtonInvalidate', 'SMProfile', true);
    }

    <jato:content name="toLogout">
        location.replace('<cc:text name="logoutUrl"/>');
    </jato:content>
</script>

<cc:form name="SMProfile" method="post" defaultCommandChild="/btnSearch">
<jato:hidden name="szCache" />

<%-- HEADER --------------------------------------------------------------- --%>
<script language="javascript">
    function confirmLogout() {
        return confirm("<cc:text name="txtLogout" defaultValue="masthead.logoutMessage" bundleID="amConsole"/>");
    }
</script>
<cc:primarymasthead name="mhCommon" bundleID="amConsole"  logoutOnClick="return confirmLogout();" locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>
<cc:tabs name="tabCommon" bundleID="amConsole" />

<table border="0" cellpadding="10" cellspacing="0" width="100%">
    <tr>
	<td>
	<cc:alertinline name="ialertCommon" bundleID="amConsole" />
	</td>
    </tr>
</table>
<table border="0" cellpadding="0" cellspacing="0">
<tr>
    <td width="97%" align="right">
         <cc:label name="lblServer" bundleID="amConsole" defaultValue="label.serverName" elementName="serverNameMenu"/>:
    &nbsp;
    </td>
    <td width="1%" >
        <cc:dropdownmenu name="serverNameMenu"
         bundleID="amConsole" commandChild="serverNameHref" type="jump" />
    </td>
    <td width="2%">&nbsp</td>
</tr>  
</table>

<%-- PAGE CONTENT --------------------------------------------------------- --%>
<cc:pagetitle name="pgtitle" bundleID="amConsole" pageTitleText="page.title.currentSessions" showPageTitleSeparator="true" viewMenuLabel="" pageTitleHelpMessage="" showPageButtonsTop="true" showPageButtonsBottom="false" />

<table border="0" cellpadding="0" cellspacing="0">
    <tr>
	<td><cc:spacer name="spacer" height="1" width="10" newline="false" /></td>
	<td><cc:label name ="lblFilter" elementName="tfFilter" /><cc:textfield name="tfFilter" defaultValue="*" /></td>
	<td><cc:spacer name="spacer" height="1" width="3" newline="false" /></td>
	<td><cc:button name="btnSearch" bundleID="amConsole" defaultValue="button.search" type="primary" onClick="document.forms['SMProfile'].submit();" /></td>
    </tr>
</table>

<cc:spacer name="spacer" height="10" newline="true" />

<cc:actiontable
    name="tblSessions"
    title="table.sessions.title.name"
    bundleID="amConsole"
    summary="table.sessions.summary"
    empty="table.sessions.empty.message"
    selectionType="multiple"
    showAdvancedSortingIcon="false"
    showLowerActions="false"
    showPaginationControls="true"
    showPaginationIcon="true"
    showSelectionIcons="true"
    selectionJavascript="toggleTblButtonState('SMProfile', 'SMProfile.tblSessions', 'tblSessions', 'SMProfile.tblButtonInvalidate', this)"
    showSelectionSortIcon="false"
    showSortingRow="false" />
</cc:form>

</cc:header>
</jato:useViewBean>
