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

   $Id: ServerEditAdvanced.jsp,v 1.4 2008/09/16 23:43:27 asyhuang Exp $

--%>

<%@ page info="ServerEditAdvanced" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<jato:useViewBean
    className="com.sun.identity.console.service.ServerEditAdvancedViewBean"
    fireChildDisplayEvents="true" >

<cc:i18nbundle baseName="amConsole" id="amConsole"
    locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>

<cc:header name="hdrCommon" pageTitle="webconsole.title" bundleID="amConsole" copyrightYear="2007" fireDisplayEvents="true">

<script language="javascript" src="../console/js/am.js"></script>
<script language="javascript">
    function consolidateProperties() {
        var form = document.getElementsByName("ServerEditAdvanced")[0];
        var elm = form.elements['ServerEditAdvanced.properties'];
        var propertiesValues = '';

        var table = getActionTable();
        var rows = table.getElementsByTagName('tr');
        if (rows.length > 2) {
            for (i = 2; i < rows.length; i++) {
                var row = rows[i];
                var inputs = row.getElementsByTagName('input');
                propertiesValues += inputs[2].value + "=" + inputs[3].value + "\n";
            }
        }
        elm.value = propertiesValues;
    }

    function deletePropertyRow() {
        var table = getActionTable();
        var rows = table.getElementsByTagName('tr');
        for (i = rows.length-1; i >= 2; --i) {
            var row = rows[i];
            var inputs = row.getElementsByTagName('input');
            var cb = inputs[0];
            if (cb.checked) {
                table.deleteRow(i);
            }
        }

        rows = table.getElementsByTagName('tr');
        if (rows.length == 2) {
            addPropertyRow();
        }

        tblBtnCounter['tblButton'] = 0;
        ccSetButtonDisabled('ServerEditAdvanced.tblButtonDelete', 'ServerEditAdvanced', true);
        return false;
    }

    function addPropertyRow() {
        var table = getActionTable();
        var tBody = table.getElementsByTagName("TBODY").item(0);
        var row = document.createElement("TR");
        var cell1 = document.createElement("TD");
        cell1.setAttribute("align", "center");
        cell1.setAttribute("valign", "top");
        var cell2 = document.createElement("TD");
        var cell3 = document.createElement("TD");
        var cb = document.createElement("input");
        var hidden = document.createElement("input");
        var textnode1 = document.createElement("input");
        var textnode2 = document.createElement("input");
        hidden.setAttribute("type", "hidden");
        cb.setAttribute("type", "checkbox");
        cb.setAttribute("onclick", "toggleTblButtonState('ServerEditAdvanced', 'ServerEditAdvanced.tblAdvancedProperties', 'tblButton', 'ServerEditAdvanced.tblButtonDelete', this)");
        textnode1.setAttribute("size", "50");
        textnode2.setAttribute("size", "50");
        cell1.appendChild(cb);
        cell1.appendChild(hidden);
        cell2.appendChild(textnode1);
        cell3.appendChild(textnode2);
        row.appendChild(cell1);
        row.appendChild(cell2);
        row.appendChild(cell3);
        tBody.appendChild(row);

        scrollToObject(row);
        return false;
    }

    function scrollToObject(elt) {
        var posX = 0;
        var posY = 0;

        while (elt != null) {
            posX += elt.offsetLeft;
            posY += elt.offsetTop;
            elt = elt.offsetParent;
        }

        window.scrollTo(posX, posY);
    }

    function getActionTable() {
        var nodes = document.getElementsByTagName("table");
        var len = nodes.length;
        for (var i = 0; i < len; i++) {
            if (nodes[i].className == 'Tbl') {
                return nodes[i];
            }
        }
     } 
</script>

<cc:form name="ServerEditAdvanced" method="post" defaultCommandChild="/button1"
    onSubmit="consolidateProperties();">
<script language="javascript">
    function confirmLogout() {
        return confirm("<cc:text name="txtLogout" defaultValue="masthead.logoutMessage" bundleID="amConsole"/>");
    }
</script>
<cc:primarymasthead name="mhCommon" bundleID="amConsole"  logoutOnClick="return confirmLogout();" locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>
<cc:breadcrumbs name="breadCrumb" bundleID="amConsole" />
<cc:tabs name="tabCommon" bundleID="amConsole" />

<table border="0" cellpadding="10" cellspacing="0" width="100%">
    <tr>
        <td><cc:alertinline name="ialertCommon" bundleID="amConsole" /></td>
    </tr>
</table>

<%-- PAGE CONTENT --------------------------------------------------------- --%>
<cc:pagetitle name="pgtitleThreeBtns" bundleID="amConsole" pageTitleText="page.title.server.edit" showPageTitleSeparator="true" viewMenuLabel="" pageTitleHelpMessage="" showPageButtonsTop="true" showPageButtonsBottom="false" />
<jato:hidden name="szCache" />
<jato:hidden name="properties" />

<cc:spacer name="spacer" height="10" newline="true" />

<cc:actiontable
    name="tblAdvancedProperties"
    title="table.serverconfig.advanced.property.title.name"
    bundleID="amConsole"
    summary="table.serverconfig.advanced.property.summary"
    empty="table.serverconfig.advanced.property.empty.message"
    selectionType="multiple"
    showAdvancedSortingIcon="false"
    showLowerActions="false"
    showPaginationControls="false"
    showPaginationIcon="false"
    showSelectionIcons="false"    
    showSelectionSortIcon="false"
    selectionJavascript="toggleTblButtonState('ServerEditAdvanced', 'ServerEditAdvanced.tblAdvancedProperties', 'tblButton', 'ServerEditAdvanced.tblButtonDelete', this)"
    showSortingRow="false">
    <attribute name="extrahtml" value="name=\"propertyTable\"" />
</cc:actiontable>
</cc:form>

</cc:header>
</jato:useViewBean>
