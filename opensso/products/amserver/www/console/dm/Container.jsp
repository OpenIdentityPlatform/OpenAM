<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
  
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

   $Id: Container.jsp,v 1.5 2008/07/22 21:38:17 babysunil Exp $

--%>

<%@ page info="Container" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<jato:useViewBean
    className="com.sun.identity.console.dm.ContainerViewBean"
    fireChildDisplayEvents="true" >

<cc:i18nbundle baseName="amConsole" id="amConsole"
    locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>

<cc:header 
    name="hdrCommon" 
    pageTitle="webconsole.title" 
    bundleID="amConsole" 
    copyrightYear="2004" 
    fireDisplayEvents="true">

<script language="javascript" src="../console/js/am.js"></script>

<cc:form name="Container" method="post" defaultCommandChild="/btnSearch">
<jato:hidden name="szCache" />

<%-- HEADER --%>
<script language="javascript">
    function confirmLogout() {
        return confirm("<cc:text name="txtLogout" defaultValue="masthead.logoutMessage" bundleID="amConsole"/>");
    }
</script>
<cc:primarymasthead name="mhCommon" bundleID="amConsole"  logoutOnClick="return confirmLogout();" 
    locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>
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
        <td align="left">
            <cc:breadcrumbs name="parentagepath" bundleID="amConsole" />
            <div class="BcmWhtDiv">
                <cc:text name="txtRoot" bundleID="amConsole" />
            </div>
        </td>
    </tr>
</table>

<%-- PAGE CONTENT --%>
<cc:pagetitle name="pgtitle" 
    bundleID="amConsole" 
    pageTitleText="page.title.organizational.unit" 
    showPageTitleSeparator="true" 
    viewMenuLabel="" 
    pageTitleHelpMessage="" 
    showPageButtonsTop="false" 
    showPageButtonsBottom="false" />

<cc:spacer name="spacer" height="10" newline="true" />

<table border="0" cellpadding="0" cellspacing="0">
    <tr>
        <td><cc:spacer name="spacer" height="1" width="10" newline="false" /></td>
        <td><cc:label name ="lblFilter" elementName="tfFilter" /><cc:textfield name="tfFilter" defaultValue="*" /></td>
        <td><cc:spacer name="spacer" height="1" width="3" newline="false" /></td>
        <td><cc:button name="btnSearch" bundleID="amConsole" defaultValue="button.search" type="primary" onClick="document.forms['Container'].submit();" /></td>
    </tr>
</table>

<cc:spacer name="spacer" height="10" newline="true" />

<cc:actiontable
    name="tblSearch"
    title="table.organizational.unit.title.name"
    bundleID="amConsole"
    summary="table.organizational.unit.summary"
    empty="table.organizational.unit.empty.message"
    selectionType="multiple"
    showAdvancedSortingIcon="false"
    showLowerActions="false"
    showPaginationControls="true"
    showPaginationIcon="true"
    showSelectionIcons="true"
    selectionJavascript="toggleTblButtonState('Container', 'Container.tblSearch', 'tblButton', 'Container.tblButtonDelete', this)"
    showSelectionSortIcon="false"
    showSortingRow="false" />

</cc:form>
</cc:header>
</jato:useViewBean>
