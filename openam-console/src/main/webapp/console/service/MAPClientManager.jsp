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

   $Id: MAPClientManager.jsp,v 1.3 2008/07/22 21:42:48 babysunil Exp $

--%>




<%@ page info="SCCore" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>

<jato:useViewBean
    className="com.sun.identity.console.service.MAPClientManagerViewBean"
    fireChildDisplayEvents="true" >

<cc:i18nbundle baseName="amConsole" id="amConsole"
    locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>

<cc:header name="hdrCommon" 
    pageTitle="webconsole.title" 
    bundleID="amConsole" 
    copyrightYear="2004" 
    fireDisplayEvents="true">

<cc:form name="MAPClientManager" method="post" defaultCommandChild="/btnSearch">


<cc:secondarymasthead name="secMhCommon" />
<cc:tabs name="tabClientDetection" bundleID="amConsole" />

<%-- PAGE CONTENT --------------------------------------------------------- --%>
<table border="0" cellpadding="10" cellspacing="0" width="100%">
    <tr>
	<td>
	<cc:alertinline name="ialertCommon" bundleID="amConsole" />
	</td>
    </tr>
</table>

<cc:pagetitle name="pgtitle" bundleID="amConsole" pageTitleText="map.client.manager.window.title" showPageTitleSeparator="true" viewMenuLabel="" pageTitleHelpMessage="" showPageButtonsTop="true" showPageButtonsBottom="false" />

<cc:spacer name="spacer" height="10" newline="true" />

<table border="0" cellpadding="0" cellspacing="0" width="100%">
    <tr>
	<td width="97%" align="right">
	    <cc:label name="lblStyle" bundleID="amConsole" defaultValue="map.client.manager.style.label" elementName="singleChoiceStyle"/>:
	</td>
	<td width="1%"><cc:spacer name="spacer" height="1" width="5" newline="false" /></td>
	<td width="1%"><cc:dropdownmenu name="singleChoiceStyle" bundleID="amConsole" onChange="document.forms[0].submit()" type="jump" /></td>
	<td width="1%"><cc:spacer name="spacer" height="1" width="10" newline="false" /></td>
    </tr>
</table>

<table border="0" cellpadding="0" cellspacing="0">
    <tr>
	<td colspan="4"><cc:spacer name="spacer" height="10" width="1" newline="false" /></td>
    </tr>
    <tr>
	<td><cc:spacer name="spacer" height="1" width="10" newline="false" /></td>
	<td nowrap><cc:label name ="lblFilter" elementName="tfFilter" /><cc:textfield name="tfFilter" defaultValue="*" /></td>
	<td><cc:spacer name="spacer" height="1" width="3" newline="false" /></td>
	<td><cc:button name="btnSearch" bundleID="amConsole" defaultValue="button.search" type="primary" /></td>
    </tr>
</table>

<cc:spacer name="spacer" height="10" newline="true" />
<table border="0" cellpadding="0" cellspacing="0">
<tr>
<td width="99%" align="right"><cc:text name="lblCustomizable" defaultValue="clientDetection.customizable.label" bundleID="amConsole"/>&#160;<cc:text name="lblCustomizableLegend" defaultValue="clientDetection.customizable.legend" bundleID="amConsole" /></td>
<td width="1%"><cc:spacer name="spacer" width="10" height="1" newline="false" /></td>
</tr>
</table>
<cc:spacer name="spacer" height="10" newline="true" />

<cc:actiontable
    name="tblClients"
    title="table.clientDetection.client.title.name"
    bundleID="amConsole"
    summary="table.clientDetection.client.title.summary"
    empty="table.clientDetection.client.empty.message"
    showAdvancedSortingIcon="false"
    showLowerActions="false"
    showPaginationControls="true"
    showPaginationIcon="true"
    showSelectionIcons="false"
    showSelectionSortIcon="false"
    showSortingRow="false" />

<%-- PAGE CONTENT --------------------------------------------------------- --%>

</cc:form>
</cc:header>
</jato:useViewBean>
