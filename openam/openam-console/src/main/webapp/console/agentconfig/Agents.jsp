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

   $Id: Agents.jsp,v 1.11 2009/01/26 22:50:01 babysunil Exp $

--%>

<%@page import="java.util.*" %>

<%@ page info="Agents" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<jato:useViewBean
    className="com.sun.identity.console.agentconfig.AgentsViewBean"
    fireChildDisplayEvents="true" >

<cc:i18nbundle baseName="amConsole" id="amConsole"
    locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>

<cc:header name="hdrCommon" pageTitle="webconsole.title" bundleID="amConsole" copyrightYear="2004" fireDisplayEvents="true">

<link rel="stylesheet" type="text/css" href="../console/css/openam.css" />

<script language="javascript" src="../console/js/am.js"></script>
<script language="javascript">
    function switchView(selectElmName) {
        var frm = document.forms[0];
        frm.elements['jato.defaultCommand'].value = "/btnShowMenu";
        frm.submit();
    }
</script>
<div id="main" style="position: absolute; margin: 0; border: none; padding: 0; width:100%; height:101%">

<cc:form name="Agents" method="post" defaultCommandChild="/btnSearch">
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
    pageTitleText="page.title.agents" 
    showPageTitleSeparator="true" 
    viewMenuLabel="" 
    pageTitleHelpMessage="" 
    showPageButtonsTop="true" 
    showPageButtonsBottom="false" />

<cc:spacer name="spacer" height="20" newline="true" />


<table border="0" cellpadding="0" cellspacing="0">
    <tr>
        <td><cc:spacer name="spacer" height="1" width="10" newline="false" /></td>
        <td nowrap><cc:label name="lblFilter" elementName="tfFilter" /><cc:textfield name="tfFilter" defaultValue="*" onKeyDown="if (event.keyCode == 13) {document.forms['Agents'].submit(); return false;}" /></td>
        <td><cc:spacer name="spacer" height="1" width="3" newline="false" /></td>
        <td><cc:button name="btnSearch" bundleID="amConsole" defaultValue="button.search" type="primary" onClick="document.forms['Agents'].submit();" /></td>
    </tr>
</table>

<cc:spacer name="spacer" height="10" newline="true" />
<cc:actiontable
    name="tblSearch"
    title="table.agents.title.name"
    bundleID="amConsole"
    summary="table.agents.summary"
    empty="table.agents.empty.message"
    selectionType="multiple"
    showAdvancedSortingIcon="false"
    showLowerActions="false"
    showPaginationControls="true"
    showPaginationIcon="true"
    showSelectionIcons="true"
    selectionJavascript="toggleTblButtonState('Agents', 'Agents.tblSearch', 'tblButton', 'Agents.tblButtonDelete', this)"
    showSelectionSortIcon="false"
    showSortingRow="false" />

<cc:spacer name="spacer" height="20" newline="true" />

<table border="0" cellpadding="0" cellspacing="0">
    <tr>
        <td><cc:spacer name="spacer" height="1" width="10" newline="false" /></td>
        <td nowrap><cc:label name="lblGroupFilter" elementName="tfGroupFilter" /><cc:textfield name="tfGroupFilter" defaultValue="*" onKeyDown="if (event.keyCode == 13) {document.forms['Agents'].submit(); return false;}" /></td>
        <td><cc:spacer name="spacer" height="1" width="3" newline="false" /></td>
        <td><cc:button name="btnGroupSearch" bundleID="amConsole" defaultValue="button.search" type="primary" onClick="document.forms['Agents'].submit();" /></td>
    </tr>
</table>

<cc:spacer name="spacer" height="10" newline="true" />
<cc:actiontable
    name="tblSearchGroup"
    title="table.agent.groups.title.name"
    bundleID="amConsole"
    summary="table.agent.groups.summary"
    empty="table.agent.groups.empty.message"
    selectionType="multiple"
    showAdvancedSortingIcon="false"
    showLowerActions="false"
    showPaginationControls="true"
    showPaginationIcon="true"
    showSelectionIcons="true"
    selectionJavascript="toggleTblButtonState('Agents', 'Agents.tblSearchGroup', 'tblButton', 'Agents.tblButtonGroupDelete', this)"
    showSelectionSortIcon="false"
    showSortingRow="false" />

</cc:form>

</cc:header>
</div>
<div id="dlg" class="dvs"></div>
<script language="javascript">
    <%
        if (viewBean.combinedType) {
            out.println("var frm = document.forms['Agents'];");
            out.println("var btnGrp = frm.elements['Agents.tblButtonGroupAdd'];");
            out.println("var btn= frm.elements['Agents.tblButtonAdd'];");
            out.println("btn.onclick = chooseAgentType;");
            out.println("btnGrp.onclick = chooseAgentGroupType;");

        }
    %>
    
    var txtSelectAgentType = "<cc:text name="txtSelectAgentType" defaultValue="agenttype.select.agent.type" bundleID="amConsole" />";
    var closeBtn = '<p><div class="TtlBtnDiv"><input name="btnClose" type="submit" class="Btn1" value="<cc:text name="txtCloseBtn" defaultValue="ajax.close.button" bundleID="amConsole" />" onClick="focusMain();return false;" /></div></p>';

    function getScrollY() {
        var scrOfY = 0;
        if (typeof( window.pageYOffset ) == 'number') {
            //Netscape compliant
            scrOfY = window.pageYOffset;
        } else if( document.body && ( document.body.scrollLeft || document.body.scrollTop ) ) {
            //DOM compliant
            scrOfY = document.body.scrollTop;
        } else if( document.documentElement && ( document.documentElement.scrollLeft || document.documentElement.scrollTop ) ) {
            //IE6 standards compliant mode
            scrOfY = document.documentElement.scrollTop;
        }
        return scrOfY;
}

    function chooseAgentType() {
        fade();
        var str = '<form name="dummy" action="#" onSubmit="return false;">' +
            '<b>' + "<cc:text name="txtSelectAgentType" defaultValue="agenttype.select.type" bundleID="amConsole" />" + '</b><p><div style="text-align:left">';
<%
        for (Iterator i = viewBean.supportedTypes.iterator(); i.hasNext(); ) {
            String type = (String)i.next();
            out.print("str += '<input type=\"radio\" name=\"agenttype\" value=\"");
            out.print(type);
            out.print("\" onClick=\"newAgent(this);\">");
            out.print(viewBean.getModel().getLocalizedString("agenttype." + type));
            out.println("<br />';");
        }
%>
        str += '</div>' + closeBtn + '</p>' + '</form>';
        var eltDlg = document.getElementById('dlg');
        eltDlg.style.top = getWindowHeight()/2 + getScrollY()  + 'px';
        eltDlg.innerHTML = '<center>' + str + '</center>';
        return false;
    }

    function chooseAgentGroupType() {
        fade();
        var str = '<form name="dummy" action="#" onSubmit="return false;">' +
            '<b>' + "<cc:text name="txtSelectAgentType" defaultValue="agenttype.select.type" bundleID="amConsole" />" + '</b><p><div style="text-align:left">';
<%
        for (Iterator i = viewBean.supportedTypes.iterator(); i.hasNext(); ) {
            String type = (String)i.next();
            out.print("str += '<input type=\"radio\" name=\"agenttype\" value=\"");
            out.print(type);
            out.print("\" onClick=\"newAgentGroup(this);\">");
            out.print(viewBean.getModel().getLocalizedString("agenttype." + type));
            out.println("<br />';");
        }
%>
        str += '</div>' + closeBtn + '</p>' + '</form>';
        var eltDlg = document.getElementById('dlg');
        eltDlg.style.top = getWindowHeight()/2 + getScrollY()  + 'px';
        eltDlg.innerHTML = '<center>' + str + '</center>';
        return false;
    }

    function newAgent(radio) {
        var frm = document.forms['Agents'];
        frm.action += '?agenttype=' + radio.value + '&Agents.tblButtonAdd=';
       frm.submit();
    }

    function newAgentGroup(radio) {
        var frm = document.forms['Agents'];
        frm.action += '?agenttype=' + radio.value + '&Agents.tblButtonGroupAdd=';
       frm.submit();
    }
</script>
</jato:useViewBean>
