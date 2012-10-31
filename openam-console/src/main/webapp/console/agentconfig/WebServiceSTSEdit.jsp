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

   $Id: WebServiceSTSEdit.jsp,v 1.6 2009/12/03 23:38:29 asyhuang Exp $

--%>

<%@ page info="WebServiceSTSEdit" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<jato:useViewBean
    className="com.sun.identity.console.agentconfig.WebServiceSTSEditViewBean"
    fireChildDisplayEvents="true" >

<cc:i18nbundle baseName="amConsole" id="amConsole"
    locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>

<cc:i18nbundle baseName="agentService" id="agentService"
    locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>
<cc:header name="hdrCommon" pageTitle="webconsole.title" bundleID="amConsole" copyrightYear="2007" fireDisplayEvents="true">

<script language="javascript" src="../console/js/am.js"></script>
<script language="javascript">
    function toggleKeyStoreComponent(radioComp) {
        frm = radioComp.form;
        disableCustomKeyStoreFields(frm, (radioComp.value == 'default'));
    }

    function disableCustomKeyStoreFields(frm, disable) {
        frm.elements['WebServiceSTSEdit.keystorelocation'].disabled =
            disable;
        frm.elements['WebServiceSTSEdit.keystorepassword'].disabled =
            disable;
        frm.elements['WebServiceSTSEdit.keypassword'].disabled = disable;
    }

    function disableSTSFields() {
        var frm = document.forms['WebServiceSTSEdit'];
        var securityMech = frm.elements['WebServiceSTSEdit.SecurityMech'];

        if (securityMech) {
            var disableSTS = true;
            var disableKerberos = true;

            for (var i = 0; (i < securityMech.length); i++) {
                if (securityMech[i].checked) {
                    var val = securityMech[i].value;
                    disableSTS = (val != "urn:sun:wss:sts:security");
                    disableKerberos =
                        (val != "urn:sun:wss:security:null:KerberosToken");
                    break;
                }
            }
            frm.elements['WebServiceSTSEdit.sts'].disabled = disableSTS;
            frm.elements['WebServiceSTSEdit.kerberosdomain'].disabled = disableKerberos;
            frm.elements['WebServiceSTSEdit.kerberosserviceprincipal'].disabled = disableKerberos;
            frm.elements['WebServiceSTSEdit.kerberosticketcachedir'].disabled = disableKerberos;
            frm.elements['WebServiceSTSEdit.kerberosdomainserver'].disabled = disableKerberos;
        }
    }
    
    function enableSigningElements()
    {
        var frm = document.forms['WebServiceSTSEdit'];
        if(frm.elements['WebServiceSTSEdit.isrequestsigned'].checked == true){
            frm.elements['WebServiceSTSEdit.Body'].disabled = false;
            frm.elements['WebServiceSTSEdit.SecurityToken'].disabled = false;
            frm.elements['WebServiceSTSEdit.Timestamp'].disabled = false;
            frm.elements['WebServiceSTSEdit.To'].disabled = false;
            frm.elements['WebServiceSTSEdit.From'].disabled = false;
            frm.elements['WebServiceSTSEdit.ReplyTo'].disabled = false;
            frm.elements['WebServiceSTSEdit.Action'].disabled = false;
            frm.elements['WebServiceSTSEdit.MessageID'].disabled = false;

            if((frm.elements['WebServiceSTSEdit.Body'].checked == false)
                && (frm.elements['WebServiceSTSEdit.SecurityToken'].checked == false)
                && (frm.elements['WebServiceSTSEdit.Timestamp'].checked == false)
                && (frm.elements['WebServiceSTSEdit.To'].checked == false)
                && (frm.elements['WebServiceSTSEdit.From'].checked == false)
                && (frm.elements['WebServiceSTSEdit.ReplyTo'].checked == false)
                && (frm.elements['WebServiceSTSEdit.Action'].checked == false)
                && (frm.elements['WebServiceSTSEdit.MessageID'].checked == false))
            {
                frm.elements['WebServiceSTSEdit.Body'].checked = true;
            }
            
        } else {
            frm.elements['WebServiceSTSEdit.Body'].disabled = true;
            frm.elements['WebServiceSTSEdit.SecurityToken'].disabled = true;
            frm.elements['WebServiceSTSEdit.Timestamp'].disabled = true;
            frm.elements['WebServiceSTSEdit.To'].disabled = true;
            frm.elements['WebServiceSTSEdit.From'].disabled = true;
            frm.elements['WebServiceSTSEdit.ReplyTo'].disabled = true;
            frm.elements['WebServiceSTSEdit.Action'].disabled = true;
            frm.elements['WebServiceSTSEdit.MessageID'].disabled = true;
        }
    }

    function enableRequestEncryptionOptions()
    {
        var frm = document.forms['WebServiceSTSEdit'];
        if(frm.elements['WebServiceSTSEdit.isRequestEncryptedEnabled'].checked == true){
            frm.elements['WebServiceSTSEdit.isrequestencrypted'].disabled = false;
            frm.elements['WebServiceSTSEdit.isRequestHeaderEncrypt'].disabled = false;
            if((frm.elements['WebServiceSTSEdit.isrequestencrypted'].checked==false)
                && (frm.elements['WebServiceSTSEdit.isRequestHeaderEncrypt'].checked==false))
            {
               frm.elements['WebServiceSTSEdit.isrequestencrypted'].checked=true;
            }
        } else {
            frm.elements['WebServiceSTSEdit.isrequestencrypted'].disabled = true;
            frm.elements['WebServiceSTSEdit.isRequestHeaderEncrypt'].disabled = true;
        }
    }

</script>

<cc:form name="WebServiceSTSEdit" method="post">
<script language="javascript">
    function confirmLogout() {
        return confirm("<cc:text name="txtLogout" defaultValue="masthead.logoutMessage" bundleID="amConsole"/>");
    }
</script>
<cc:primarymasthead name="mhCommon" bundleID="amConsole"  logoutOnClick="return confirmLogout();" locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>
<cc:tabs name="tabCommon" bundleID="amConsole" submitFormData="true" />

<table border="0" cellpadding="10" cellspacing="0" width="100%">
    <tr>
	<td>
	<cc:alertinline name="ialertCommon" bundleID="amConsole" />
	</td>
    </tr>
</table>

<%-- PAGE CONTENT --------------------------------------------------------- --%>
<cc:pagetitle name="pgtitleTwoBtns" bundleID="amConsole" pageTitleText="page.title.entities.create" showPageTitleSeparator="true" viewMenuLabel="" pageTitleHelpMessage="" showPageButtonsTop="true" showPageButtonsBottom="false" />

<table border="0" cellpadding="10" cellspacing="0">
    <tr>
        <td>
            <cc:button
                name="btnInherit"
                bundleID="amConsole"
                defaultValue="agentconfig.button.inherit"
                type="primary" />
        </td>
        <td>
            <cc:button
                name="btnDump"
                bundleID="amConsole"
                defaultValue="agentconfig.button.dump"
                type="primary" />
        </td>
    </tr>
</table>

<cc:propertysheet name="propertyAttributes" bundleID="agentService" showJumpLinks="true"/>

</cc:form>

<script language="javascript">
    var frm = document.forms['WebServiceSTSEdit'];
    var disabled = true;
    if (frm.elements['WebServiceSTSEdit.keystoreusage']) {
        disabled = frm.elements['WebServiceSTSEdit.keystoreusage'][0].checked;
    }
    disableCustomKeyStoreFields(frm, disabled);
    disableSTSFields();
    enableSigningElements();
    enableRequestEncryptionOptions();
</script>
</cc:header>
</jato:useViewBean>
