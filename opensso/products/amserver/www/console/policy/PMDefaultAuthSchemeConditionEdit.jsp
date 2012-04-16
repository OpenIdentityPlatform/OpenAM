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

   $Id: PMDefaultAuthSchemeConditionEdit.jsp,v 1.4 2010/01/27 10:44:32 si224302 Exp $

--%>




<%@ page info="PMDefaultAuthSchemeConditionEdit" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<jato:useViewBean
    className="com.sun.identity.console.policy.PMDefaultAuthSchemeConditionEditViewBean"
    fireChildDisplayEvents="true">

 <cc:i18nbundle baseName="amConsole" id="amConsole"
     locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>

<cc:header name="hdrCommon" pageTitle="webconsole.title" bundleID="amConsole" copyrightYear="2004" fireDisplayEvents="true" onUnload="closeWin()">

<script language="javascript">
    var selectWin = null;
    function openSelectWindow() {
	selectWin = window.open('../policy/SelectRealm','selectWindow',
            'height=500,width=650,top=' +
            ((screen.height-(screen.height/1.618))-(500/2)) +
            ',left=' +
            ((screen.width-650)/2) +
            ',scrollbars,resizable');
        selectWin.focus();
    }

    function selectAllElements(frm) {
        var sel = frm.elements['PMDefaultAuthSchemeConditionEdit.AuthScheme'];
	for (var i = 0; i < sel.options.length; i++) {
            sel.options[i].selected = true;
        }
    }

    function closeWin() {
        if (selectWin) {
            selectWin.close();
        }
    }

    function deleteSelected() {
	var frm = document.forms['PMDefaultAuthSchemeConditionEdit'];
        var sel = frm.elements['PMDefaultAuthSchemeConditionEdit.AuthScheme'];
        for (var i = sel.options.length-1; i >-1; --i) {
            if (sel.options[i].selected) {
                sel.options[i] = null;
            }
        }
    }
    
    function addOption(label, value) {
        var o = new Option(label, value);
        return o;
    }
</script>


<cc:form name="PMDefaultAuthSchemeConditionEdit" method="post" defaultCommandChild="/btnFilter" onSubmit="selectAllElements(this)">
<script language="javascript">
    function confirmLogout() {
        return confirm("<cc:text name="txtLogout" defaultValue="masthead.logoutMessage" bundleID="amConsole"/>");
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
<cc:pagetitle name="pgtitleTwoBtns" bundleID="amConsole" pageTitleText="page.title.policy.condition.edit" showPageTitleSeparator="true" viewMenuLabel="" pageTitleHelpMessage="" showPageButtonsTop="true" showPageButtonsBottom="false" />

<cc:propertysheet name="propertyAttributes" bundleID="amConsole" showJumpLinks="false"/>

</cc:form>

</cc:header>
</jato:useViewBean>
