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

   $Id: SecurityTokenService.jsp,v 1.3 2009/12/19 00:10:28 asyhuang Exp $

--%>

<%@ page info="SecurityTokenService" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<jato:useViewBean
    className="com.sun.identity.console.service.SecurityTokenServiceViewBean"
    fireChildDisplayEvents="true" >

<cc:i18nbundle baseName="amConsole" id="amConsole"
    locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>

<cc:header name="hdrCommon" pageTitle="webconsole.title" bundleID="amConsole" copyrightYear="2007" fireDisplayEvents="true">

<script language="javascript" src="../console/js/am.js"></script>

<script language="javascript">
    function enableSigningElements()
    {
        var frm = document.forms['SecurityTokenService'];
        if(frm.elements['SecurityTokenService.isresponsesigned'].checked == true){
            frm.elements['SecurityTokenService.Body'].disabled = false;
            frm.elements['SecurityTokenService.SecurityToken'].disabled = false;
            frm.elements['SecurityTokenService.Timestamp'].disabled = false;
            frm.elements['SecurityTokenService.To'].disabled = false;
            frm.elements['SecurityTokenService.From'].disabled = false;
            frm.elements['SecurityTokenService.ReplyTo'].disabled = false;
            frm.elements['SecurityTokenService.Action'].disabled = false;
            frm.elements['SecurityTokenService.MessageID'].disabled = false;

            if((frm.elements['SecurityTokenService.Body'].checked == false)
                && (frm.elements['SecurityTokenService.SecurityToken'].checked == false)
                && (frm.elements['SecurityTokenService.Timestamp'].checked == false)
                && (frm.elements['SecurityTokenService.To'].checked == false)
                && (frm.elements['SecurityTokenService.From'].checked == false)
                && (frm.elements['SecurityTokenService.ReplyTo'].checked == false)
                && (frm.elements['SecurityTokenService.Action'].checked == false)
                && (frm.elements['SecurityTokenService.MessageID'].checked == false))
            {
                frm.elements['SecurityTokenService.Body'].checked = true;
            }
            
        } else {
            frm.elements['SecurityTokenService.Body'].disabled = true;
            frm.elements['SecurityTokenService.SecurityToken'].disabled = true;
            frm.elements['SecurityTokenService.Timestamp'].disabled = true;
            frm.elements['SecurityTokenService.To'].disabled = true;
            frm.elements['SecurityTokenService.From'].disabled = true;
            frm.elements['SecurityTokenService.ReplyTo'].disabled = true;
            frm.elements['SecurityTokenService.Action'].disabled = true;
            frm.elements['SecurityTokenService.MessageID'].disabled = true;
        }
    }

    function enableRequestEncryptionOptions()
    {
        var frm = document.forms['SecurityTokenService'];
        if(frm.elements['SecurityTokenService.isRequestEncryptedEnabled'].checked == true){
            frm.elements['SecurityTokenService.isRequestEncrypt'].disabled = false;
            frm.elements['SecurityTokenService.isRequestHeaderEncrypt'].disabled = false;
            if((frm.elements['SecurityTokenService.isRequestEncrypt'].checked==false)
                && (frm.elements['SecurityTokenService.isRequestHeaderEncrypt'].checked==false))
            {
               frm.elements['SecurityTokenService.isRequestEncrypt'].checked=true;
            }
        } else {
            frm.elements['SecurityTokenService.isRequestEncrypt'].disabled = true;
            frm.elements['SecurityTokenService.isRequestHeaderEncrypt'].disabled = true;
        }
    }
</script>
<cc:form name="SecurityTokenService" method="post">
<cc:hidden name="tfPageModified" />

<script language="javascript">
    function confirmLogout() {
        return confirm("<cc:text name="txtLogout" defaultValue="masthead.logoutMessage" bundleID="amConsole"/>");
    }
</script>
<cc:primarymasthead name="mhCommon" bundleID="amConsole"  logoutOnClick="return confirmLogout();" 
    locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>

<table border="0" cellpadding="10" cellspacing="0" width="100%">
    <tr>
	<td>
	<cc:alertinline name="ialertCommon" bundleID="amConsole" />
	</td>
    </tr>
</table>

<%-- PAGE CONTENT --------------------------------------------------------- --%>
<cc:pagetitle name="pgtitle" bundleID="amConsole" pageTitleText="" showPageTitleSeparator="true" viewMenuLabel="" pageTitleHelpMessage="" showPageButtonsTop="true" showPageButtonsBottom="false" />


<table border="0" cellpadding="10" cellspacing="0">
    <tr>
        <td>
            <cc:button
                name="btnSTSExportPolicy"
                bundleID="amConsole"
                defaultValue="sts.button.export.policy"
                type="primary" />
        </td>
    </tr>
</table>

<cc:propertysheet name="propertyAttributes" bundleID="amConsole" showJumpLinks="true" />

</cc:form>

<script language="javascript">
    enableSigningElements();
    enableRequestEncryptionOptions();
</script>

</cc:header>
</jato:useViewBean>
