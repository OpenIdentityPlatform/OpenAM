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

   $Id: ServerSite.jsp,v 1.3 2008/06/25 05:44:54 qcheng Exp $

--%>


<%@ page info="ServerSite" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<jato:useViewBean
    className="com.sun.identity.console.service.ServerSiteViewBean"
    fireChildDisplayEvents="true" >

<cc:i18nbundle baseName="amConsole" id="amConsole"
    locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>

<cc:header name="hdrCommon" pageTitle="webconsole.title" bundleID="amConsole" copyrightYear="2007" fireDisplayEvents="true">

<script language="javascript" src="../console/js/am.js"></script>

<cc:form name="ServerSite" method="post">
<jato:hidden name="szCache" />
<jato:hidden name="szCache1" />
<jato:hidden name="szCacheSite" />
<jato:hidden name="szCacheServer" />
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
<cc:pagetitle name="pgtitle" bundleID="amConsole" pageTitleText="page.title.serversite.config" showPageTitleSeparator="true" viewMenuLabel="" pageTitleHelpMessage="" showPageButtonsTop="false" showPageButtonsBottom="false" />

<cc:spacer name="spacer" height="10" newline="true" />
<table border=0 cellpadding=10 cellspacing=0>
<tr>
<td>
    <cc:button name="btnDefaultSettings" bundleID="amConsole"
        defaultValue="serverconfig.server.button.defaultserver"
        type="secondary" />
</td>
</tr>
</table>
<cc:actiontable
    name="tblServer"
    title="table.server.title.name"
    bundleID="amConsole"
    summary="table.server.summary"
    empty="table.server.empty.message"
    selectionType="multiple"
    showAdvancedSortingIcon="false"
    showLowerActions="false"
    showPaginationControls="true"
    showPaginationIcon="true"
    showSelectionIcons="true"
    selectionJavascript="toggleTblButtonState('ServerSite', 'ServerSite.tblServer', 'tblServerButton', 'ServerSite.tblServerButtonDelete', this); toggleTblButtonStateEx('ServerSite', 'ServerSite.tblServer', 'tblServerButtonClone', 'ServerSite.tblServerButtonClone', this, true);"
    showSelectionSortIcon="false"
    showSortingRow="false" />

<cc:spacer name="spacer" height="10" newline="true" />

<cc:actiontable
    name="tblSite"
    title="table.site.title.name"
    bundleID="amConsole"
    summary="table.site.summary"
    empty="table.site.empty.message"
    selectionType="multiple"
    showAdvancedSortingIcon="false"
    showLowerActions="false"
    showPaginationControls="true"
    showPaginationIcon="true"
    showSelectionIcons="true"
    selectionJavascript="toggleTblButtonState('ServerSite', 'ServerSite.tblSite', 'tblSiteButton', 'ServerSite.tblSiteButtonDelete', this)"
    showSelectionSortIcon="false"
    showSortingRow="false" />
</cc:form>

</cc:header>
</jato:useViewBean>
