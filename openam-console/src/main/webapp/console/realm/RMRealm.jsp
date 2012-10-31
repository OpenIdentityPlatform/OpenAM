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

   $Id: RMRealm.jsp,v 1.5 2008/07/22 21:42:08 babysunil Exp $

--%>




<%@ page info="RMRealm" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<jato:useViewBean
    className="com.sun.identity.console.realm.RMRealmViewBean"
    fireChildDisplayEvents="true" >

<cc:i18nbundle baseName="amConsole" id="amConsole"
    locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>

<cc:header name="hdrCommon" pageTitle="webconsole.title" bundleID="amConsole" copyrightYear="2004" fireDisplayEvents="true">

<script language="javascript" src="../console/js/am.js"></script>

<cc:form name="RMRealm" method="post" defaultCommandChild="/btnSearch">
<jato:hidden name="szCache" />
<script language="javascript">
    function confirmLogout() {
        return confirm("<cc:text name="txtLogout" defaultValue="masthead.logoutMessage" bundleID="amConsole"/>");
    }
</script>
<cc:primarymasthead name="mhCommon" bundleID="amConsole"  logoutOnClick="return confirmLogout();" locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>
<cc:tabs name="tabCommon" bundleID="amConsole" />

<table border="0" cellpadding="10" cellspacing="0" width="100%">
    <tr>
	<td><cc:alertinline name="ialertCommon" bundleID="amConsole" /></td>
    </tr>
</table>

<%-- PAGE CONTENT --------------------------------------------------------- --%>
<cc:spacer name="spacer" height="5" newline="true" />
<table border="0" cellpadding="0" cellspacing="0">
    <tr>
        <td><cc:spacer name="spacer" height="1" width="10" newline="false" /></td>
        <td><cc:text name="tfText" defaultValue="accesscontrol.tab.text" bundleID="amConsole"/></td>
        <td><cc:spacer name="spacer" height="1" width="3" newline="false" /></td>
    </tr>
</table>
<cc:spacer name="spacer" height="5" newline="true" />

<cc:pagetitle name="pgtitle" bundleID="amConsole" pageTitleText="page.title.realms" showPageTitleSeparator="true" viewMenuLabel="" pageTitleHelpMessage="" showPageButtonsTop="false" showPageButtonsBottom="false" />

<cc:spacer name="spacer" height="10" newline="true" />

<table border="0" cellpadding="0" cellspacing="0">
    <tr>
	<td><cc:spacer name="spacer" height="1" width="10" newline="false" /></td>
	<td><cc:label name ="lblFilter" elementName="tfFilter" /><cc:textfield name="tfFilter" defaultValue="*" /></td>
	<td><cc:spacer name="spacer" height="1" width="3" newline="false" /></td>
	<td><cc:button name="btnSearch" bundleID="amConsole" defaultValue="button.search" type="primary" onClick="document.forms['RMRealm'].submit();" /></td>
    </tr>
</table>

<cc:spacer name="spacer" height="10" newline="true" />

<cc:actiontable
    name="tblSearch"
    title="table.realm.title.name"
    bundleID="amConsole"
    summary="table.realm.summary"
    empty="table.realm.empty.message"
    selectionType="multiple"
    showAdvancedSortingIcon="false"
    showLowerActions="false"
    showPaginationControls="true"
    showPaginationIcon="true"
    showSelectionIcons="true"
    selectionJavascript="toggleTblButtonState('RMRealm', 'RMRealm.tblSearch', 'tblButton', 'RMRealm.tblButtonDelete', this)"
    showSelectionSortIcon="false"
    showSortingRow="false" />
</cc:form>

</cc:header>
</jato:useViewBean>
