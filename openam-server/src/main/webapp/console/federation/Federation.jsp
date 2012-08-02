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

   $Id: Federation.jsp,v 1.4 2008/06/25 05:50:22 qcheng Exp $

--%>

<%@ page info="Federation" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<jato:useViewBean
    className="com.sun.identity.console.federation.FederationViewBean"
    fireChildDisplayEvents="true" >

<cc:i18nbundle baseName="amConsole" id="amConsole"
    locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>

<cc:header name="hdrCommon" pageTitle="webconsole.title" bundleID="amConsole" copyrightYear="2007" fireDisplayEvents="true">

<link rel="stylesheet" type="text/css" href="../console/css/openam.css" />

<script language="javascript">
    <%--
        swichView() is called when the entity provider dropdown menu is 
        selected. This is called to submit the page with the selected value
        in the dropdown submitted as the value in the form.
    --%>
    function switchView(selectElmName) {
        var frm = document.forms[0];
        frm.action += "?Federation.btnSearch=1";
        frm.submit();
    }

    var txtSelectProtocol = "<cc:text name="txtSelectProtocol" defaultValue="federation.entity.select.protocol" bundleID="amConsole" />";
    var optionSAML2 =  "<cc:text name="txtProtocolSAMLv2" defaultValue="federation.entity.protocol.samlv2" bundleID="amConsole" />";
    var optionIDFF = "<cc:text name="txtProtocolIDFF" defaultValue="federation.entity.protocol.idff" bundleID="amConsole" />";
    var optionWSFed =  "<cc:text name="txtProtocolWSFed" defaultValue="federation.entity.protocol.wsfed" bundleID="amConsole" />";

    var closeBtn = '<p><div class="TtlBtnDiv"><input name="btnClose" type="submit" class="Btn1" value="<cc:text name="txtCloseBtn" defaultValue="ajax.close.button" bundleID="amConsole" />" onClick="focusMain();return false;" /></div></p>';

    function gotoProtocolPage(radio) {
        top.location = '../federation/CreateSAML2MetaData.jsp?p=' + radio.value;
    }

    function selectProviderType() {
        document.getElementById('dlg').style.height = '175px';
        fade();
        var str = '<form name="dummy" action="#" onSubmit="return false;">' +
            '<b>' + txtSelectProtocol + '</b><p><div style="text-align:left">' +
            '<input type="radio" name="protocoltype" value="samlv2"' +
                ' onClick="gotoProtocolPage(this);">' +
            optionSAML2 + '<br />' +
            '<input type="radio" name="protocoltype" value="idff"' +
                ' onClick="gotoProtocolPage(this);">' +
            optionIDFF + '<br />' +
            '<input type="radio" name="protocoltype" value="wsfed"' +
                ' onClick="gotoProtocolPage(this);">' +
            optionWSFed + '</div>' +
            closeBtn + '</p>' +
            '</form>';
        document.getElementById('dlg').innerHTML = '<center>' +
            str + '</center>';
        return false;
    }
</script>

<script language="javascript" src="../console/js/am.js"></script>
<div id="main" style="position: absolute; margin: 0; border: none; padding: 0; width:auto; height:1000">

<cc:form name="Federation" method="post" defaultCommandChild="/btnSearch">
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

<%-- PAGE CONTENT --%>

<cc:propertysheet name="propertyAttributes" bundleID="amConsole" showJumpLinks="true" />


<%-- END CONTENT --%>
</cc:form>
</div>
<div id="dlg" class="dvs"></div>

</cc:header>
</jato:useViewBean>
