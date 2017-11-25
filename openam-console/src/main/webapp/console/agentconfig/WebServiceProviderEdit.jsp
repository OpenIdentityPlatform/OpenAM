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

   $Id: WebServiceProviderEdit.jsp,v 1.10 2009/12/19 00:04:51 asyhuang Exp $

--%>

<%@ page info="WebServiceProviderEdit" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<jato:useViewBean
    className="com.sun.identity.console.agentconfig.WebServiceProviderEditViewBean"
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
        frm.elements['WebServiceProviderEdit.keystorelocation'].disabled =
            disable;
        frm.elements['WebServiceProviderEdit.keystorepassword'].disabled =
            disable;
        frm.elements['WebServiceProviderEdit.keypassword'].disabled = disable;
    }

    function showSAMLConfig() {
        var frm = document.forms['WebServiceProviderEdit'];
        var visible = 'none';
        var kerberos = false;
        for (var i = 0; i <frm.elements.length && (visible == 'none'); i++) {
            var elt = frm.elements[i];
            if (elt.name && elt.name.indexOf('WebServiceProviderEdit.securitymech') == 0) {
                if (elt.name.indexOf('jato_boolean') == -1) {
                    if ((elt.name.indexOf('SAMLToken-') != -1) ||
                        (elt.name.indexOf('SAML2Token-') != -1)
                    ) {
                        if (elt.checked) {
                            visible = '';
                        }
                    } else if (elt.name.indexOf('KerberosToken') != -1) {
                        kerberos = (elt.checked);
                    }
                }
            }
        }
        document.getElementById('samlconf').style.display = visible;

        if (visible == '') {
            frm.elements['WebServiceProviderEdit.tokenconversiontype'].disabled = (frm.elements['WebServiceProviderEdit.authenticationchain'].value == '[Empty]');
        } else {
            frm.elements['WebServiceProviderEdit.tokenconversiontype'].disabled = true;
        }
        frm.elements['WebServiceProviderEdit.kerberosdomainserver'].disabled = !kerberos;
        frm.elements['WebServiceProviderEdit.kerberosdomain'].disabled = !kerberos;
        frm.elements['WebServiceProviderEdit.kerberosserviceprincipal'].disabled = !kerberos;
        frm.elements['WebServiceProviderEdit.kerberoskeytabfile'].disabled = !kerberos;
        frm.elements['WebServiceProviderEdit.isverifykrbsignature'].disabled = !kerberos;
        return true;
    }

    function enableSigningElements()
    {
        var frm = document.forms['WebServiceProviderEdit'];
        if(frm.elements['WebServiceProviderEdit.isresponsesigned'].checked == true){
            frm.elements['WebServiceProviderEdit.Body'].disabled = false;
            frm.elements['WebServiceProviderEdit.SecurityToken'].disabled = false;
            frm.elements['WebServiceProviderEdit.Timestamp'].disabled = false;
            frm.elements['WebServiceProviderEdit.To'].disabled = false;
            frm.elements['WebServiceProviderEdit.From'].disabled = false;
            frm.elements['WebServiceProviderEdit.ReplyTo'].disabled = false;
            frm.elements['WebServiceProviderEdit.Action'].disabled = false;
            frm.elements['WebServiceProviderEdit.MessageID'].disabled = false;

            if((frm.elements['WebServiceProviderEdit.Body'].checked == false)
                && (frm.elements['WebServiceProviderEdit.SecurityToken'].checked == false)
                && (frm.elements['WebServiceProviderEdit.Timestamp'].checked == false)
                && (frm.elements['WebServiceProviderEdit.To'].checked == false)
                && (frm.elements['WebServiceProviderEdit.From'].checked == false)
                && (frm.elements['WebServiceProviderEdit.ReplyTo'].checked == false)
                && (frm.elements['WebServiceProviderEdit.Action'].checked == false)
                && (frm.elements['WebServiceProviderEdit.MessageID'].checked == false))
            {
                frm.elements['WebServiceProviderEdit.Body'].checked = true;
            }

        } else {
            frm.elements['WebServiceProviderEdit.Body'].disabled = true;
            frm.elements['WebServiceProviderEdit.SecurityToken'].disabled = true;
            frm.elements['WebServiceProviderEdit.Timestamp'].disabled = true;
            frm.elements['WebServiceProviderEdit.To'].disabled = true;
            frm.elements['WebServiceProviderEdit.From'].disabled = true;
            frm.elements['WebServiceProviderEdit.ReplyTo'].disabled = true;
            frm.elements['WebServiceProviderEdit.Action'].disabled = true;
            frm.elements['WebServiceProviderEdit.MessageID'].disabled = true;
        }
    }

    function enableRequestEncryptionOptions()
    {
        var frm = document.forms['WebServiceProviderEdit'];
        if(frm.elements['WebServiceProviderEdit.isRequestEncryptedEnabled'].checked == true){
            frm.elements['WebServiceProviderEdit.isrequestencrypted'].disabled = false;
            frm.elements['WebServiceProviderEdit.isRequestHeaderEncrypt'].disabled = false;
            if((frm.elements['WebServiceProviderEdit.isrequestencrypted'].checked==false)
                && (frm.elements['WebServiceProviderEdit.isRequestHeaderEncrypt'].checked==false))
            {
               frm.elements['WebServiceProviderEdit.isrequestencrypted'].checked=true;
            }
        } else {
            frm.elements['WebServiceProviderEdit.isrequestencrypted'].disabled = true;
            frm.elements['WebServiceProviderEdit.isRequestHeaderEncrypt'].disabled = true;
        }
    }
</script>

<cc:form name="WebServiceProviderEdit" method="post">
<jato:hidden name="szCache" />

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
        <td>
            <cc:button
                name="btnExportPolicy"
                bundleID="amConsole"
                defaultValue="agentconfig.button.export.policy"
                type="primary" />
        </td>
    </tr>
</table>

<cc:propertysheet name="propertyAttributes" bundleID="agentService" showJumpLinks="true"/>

</cc:form>

<script language="javascript">
    var frm = document.forms['WebServiceProviderEdit'];
    var disabled = true;
    if (frm.elements['WebServiceProviderEdit.keystoreusage']) {
        disabled = frm.elements['WebServiceProviderEdit.keystoreusage'][0].checked;
    }
    disableCustomKeyStoreFields(frm, disabled);
    showSAMLConfig();
    enableSigningElements();
    enableRequestEncryptionOptions();
</script>

</cc:header>
</jato:useViewBean>
