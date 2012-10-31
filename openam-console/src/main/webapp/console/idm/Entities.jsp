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

   $Id: Entities.jsp,v 1.5 2008/09/04 00:03:53 asyhuang Exp $

--%>




<%@ page info="Entities" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<jato:useViewBean
    className="com.sun.identity.console.idm.EntitiesViewBean"
    fireChildDisplayEvents="true" >

<cc:i18nbundle baseName="amConsole" id="amConsole"
    locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>

<cc:header name="hdrCommon" pageTitle="webconsole.title" bundleID="amConsole" copyrightYear="2004" fireDisplayEvents="true">

<script language="javascript" src="../console/js/am.js"></script>
<script language="javascript">
    function switchView(selectElmName) {
        var frm = document.forms[0];
        frm.elements['jato.defaultCommand'].value = "/btnShowMenu";
        frm.submit();
    }
</script>

<cc:form name="Entities" method="post" defaultCommandChild="/btnSearch">
<jato:hidden name="szCache" />
<script language="javascript">
    function confirmLogout() {
        return confirm("<cc:text name="txtLogout" defaultValue="masthead.logoutMessage" bundleID="amConsole"/>");
    }
</script>
<cc:primarymasthead name="mhCommon" bundleID="amConsole"  logoutOnClick="return confirmLogout();" locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>
<cc:breadcrumbs name="breadCrumb" bundleID="amConsole" />
<cc:tabs name="tabCommon" bundleID="amConsole" submitFormData="true" />

<table border="0" cellpadding="10" cellspacing="0" width="100%">
    <tr>
	<td>
	<cc:alertinline name="ialertCommon" bundleID="amConsole" />
	</td>
    </tr>
</table>

<table border="0" cellpadding="0" cellspacing="0" width="100%">
    <tr>
	<td>
	    <cc:breadcrumbs name="parentagepath" bundleID="amConsole" />
	    <div class="BcmWhtDiv"><cc:text name="txtRoot" bundleID="amConsole" /></div>
	</td>
    </tr>
</table>

<%-- PAGE CONTENT --%>
<cc:pagetitle name="pgtitle" 
    bundleID="amConsole" 
    pageTitleText="page.title.entities" 
    showPageTitleSeparator="true" 
    viewMenuLabel="" 
    pageTitleHelpMessage="" 
    showPageButtonsTop="true" 
    showPageButtonsBottom="false" />

<cc:spacer name="spacer" height="10" newline="true" />

<table border="0" cellpadding="0" cellspacing="0">
    <tr>
	<td><cc:spacer name="spacer" height="1" width="10" newline="false" /></td>
	<td nowrap><cc:label name ="lblFilter" elementName="tfFilter" /><cc:textfield name="tfFilter" defaultValue="*" onKeyDown="if (event.keyCode == 13) { document.forms['Entities'].submit(); return false;}" /></td>
	<td><cc:spacer name="spacer" height="1" width="3" newline="false" /></td>
	<td><cc:button name="btnSearch" bundleID="amConsole" defaultValue="button.search" type="primary" onClick="document.forms['Entities'].submit();" /></td>
    </tr>
</table>

<cc:spacer name="spacer" height="10" newline="true" />

<cc:actiontable
    name="tblSearch"
    title="table.entities.title.name"
    bundleID="amConsole"
    summary="table.entities.summary"
    empty="table.entities.empty.message"
    selectionType="multiple"
    showAdvancedSortingIcon="false"
    showLowerActions="false"
    showPaginationControls="true"
    showPaginationIcon="true"
    showSelectionIcons="true"
    selectionJavascript="toggleTblButtonState('Entities', 'Entities.tblSearch', 'tblButton', 'Entities.tblButtonDelete', this)"
    showSelectionSortIcon="false"
    showSortingRow="false" />
</cc:form>

</cc:header>
</jato:useViewBean>
