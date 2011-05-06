<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
  
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

   $Id: RegisterProduct.jsp,v 1.5 2009/07/20 23:03:23 asyhuang Exp $

--%>

<%@ page info="RegisterProduct" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<%
    request.setCharacterEncoding("UTF-8");
%>
<jato:useViewBean
    className="com.sun.identity.console.task.RegisterProductViewBean"
    fireChildDisplayEvents="true" >

<cc:i18nbundle baseName="amConsole" id="amConsole"
    locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>

<cc:header name="hdrCommon" pageTitle="webconsole.title" bundleID="amConsole" copyrightYear="2008" fireDisplayEvents="true">

<link rel="stylesheet" type="text/css" href="../console/css/opensso.css" />

<script language="javascript" src="../console/js/am.js"></script>
<script language="javascript" src="../com_sun_web_ui/js/dynamic.js"></script>

<div id="main" style="position: absolute; margin: 0; border: none; padding: 0; width:auto; height:101%;">
<cc:form name="RegisterProduct" method="post">
<jato:hidden name="szCache" />
<script language="javascript">
    function confirmLogout() {
        return confirm("<cc:text name="txtLogout" defaultValue="masthead.logoutMessage" bundleID="amConsole"/>");
    }

    function acctSelect(radio) {
        if (radio.value == 'acct') {
            document.getElementById('existAcct').style.display = '';
            document.getElementById('newAcct').style.display = 'none';
        } else {
            document.getElementById('existAcct').style.display = 'none';
            document.getElementById('newAcct').style.display = '';
        }
    }

    function cancelOp() {
        document.location.replace("../task/Home");
        return false;
    }
</script>

<cc:primarymasthead name="mhCommon" bundleID="amConsole"  logoutOnClick="return confirmLogout();"/>
<table border="0" cellpadding="10" cellspacing="0" width="100%">
    <tr>
        <td>
        <cc:alertinline name="ialertCommon" bundleID="amConsole" />
        </td>
    </tr>
</table>

<%-- PAGE CONTENT --------------------------------------------------------- --%>
<cc:pagetitle name="pgtitle" bundleID="amConsole" pageTitleText="page.title.register.product" showPageTitleSeparator="true" viewMenuLabel="" pageTitleHelpMessage="" showPageButtonsTop="true" showPageButtonsBottom="false" />

<cc:propertysheet name="propertyAttributes" bundleID="amConsole" showJumpLinks="false"/>

</cc:form>
</cc:header>
</div>
<div id="dlg" class="dvs"></div>

<script language="javascript">
    var msgConfiguring = "<cc:text name="txtRegistering" defaultValue="register.product.waiting" bundleID="amConsole" escape="false" />";

    var msgConfigured = '<p>&nbsp;</p><cc:text name="txtRegistered" defaultValue="register.product.done" bundleID="amConsole" /><p><div class="TtlBtnDiv"><input name="done" type="submit" class="Btn1" value="<cc:text name="txtOKBtn" defaultValue="ajax.ok.button" bundleID="amConsole" />" onClick="document.location.replace(\'../task/Home\');return false;" /></div></p>';

    var closeBtn = '<p>&nbsp;</p><p><div class="TtlBtnDiv"><input name="btnClose" type="submit" class="Btn1" value="<cc:text name="txtCloseBtn" defaultValue="ajax.close.button" bundleID="amConsole" />" onClick="focusMain();return false;" /></div></p>';

    var msgGetDomains = '<cc:text name="txtConfigured" defaultValue="register.product.get.domains" bundleID="amConsole" escape="false" /><p><div class="TtlBtnDiv"><input name="done" type="submit" class="Btn1" value="<cc:text name="txtOKBtn" defaultValue="ajax.ok.button" bundleID="amConsole" />" onClick="focusMain(); return false;" /></div></p>';

    var frm = document.forms['RegisterProduct'];
    var btn1 = frm.elements['RegisterProduct.button1'];
    btn1.onclick = submitPage;
    var btn2 = frm.elements['RegisterProduct.button2'];
    btn2.onclick = cancelOp;
    var ajaxObj = getXmlHttpRequestObject();
    var data = '';
    var userLocale = "<%= viewBean.getUserLocale().toString() %>";

    function submitPage() {
        fade();
        document.getElementById('dlg').innerHTML = msgConfiguring;
        var opt = getRadioVal(frm, 'RegisterProduct.radioAcctOption');
        var existing = (opt == 'acct');
        var url = "../console/ajax/AjaxProxy.jsp";
        var params = 'locale=' + userLocale +
            '&class=com.sun.identity.workflow.RegisterProduct' +
            getData(existing);
        ajaxPost(ajaxObj, url, params, configured);
        return false;
    }

    function getData(existing) {
        if (existing) {
            return "&newAccount=false" + getFieldValue('tfExistUserName') +
                getFieldValue('tfExistPswd') +
                getFieldValue('tfExistProxyHost') +
                getFieldValue('tfExistProxyPort') +
                getFieldValue('tfDomain');
        } else {
            return "&newAccount=true" + getFieldValue('tfUserName') +
                getFieldValue('tfEmailAddr') +
                getFieldValue('tfPswd') +
                getFieldValue('tfCfrmPswd') +
                getFieldValue('tfProxyHost') +
                getFieldValue('tfProxyPort') +
                getFieldValue('tfFirstName') +
                getFieldValue('tfLastName') +
                getFieldValue('tfCountry');
        }
    }

    function getFieldValue(name) {
        return "&" + name + "=" + escapeEx(
            frm.elements['RegisterProduct.' + name].value);
    }

    function setDomains(domains) {
        var menu = frm.elements['RegisterProduct.tfDomain'];
        for (var i = menu.options.length-1; i >= 0; --i) {
            menu.options[i] == null;
        }
        var counter = 0;
        var idx = domains.indexOf('|');
        while (idx != -1) {
            var s = domains.substring(0, idx);
            domains = domains.substring(idx +1);
             menu.options[counter++] = new Option(s, s);
            idx = domains.indexOf(',');
        }
        if (domains != '') {
            menu.options[counter] = new Option(domains, domains);
        }

        var radioOpt = frm.elements['RegisterProduct.radioAcctOption'];
        for (var i = 0; i < radioOpt.length; i++) {
            radioOpt[i].disabled = true;
        }
        frm.elements['RegisterProduct.tfExistUserName'].disabled = true;
        frm.elements['RegisterProduct.tfExistPswd'].disabled = true;
        frm.elements['RegisterProduct.tfExistProxyHost'].disabled = true;
        frm.elements['RegisterProduct.tfExistProxyPort'].disabled = true;
        document.getElementById('domainLabel').style.display = '';
        menu.style.display = '';
    }

    function configured() {
        if (ajaxObj.readyState == 4) {
            var result = hexToString(ajaxObj.responseText);
            var status = result.substring(0, result.indexOf('|'));
            var result = result.substring(result.indexOf('|') +1);
            var msg = '';
            if (status == 0) {
                if (result.indexOf('<selectdomain>') != -1) {
                    setDomains(result.substring(14));
                    msg = '<center>' +  msgGetDomains + '</center>';
                } else {
                    msg = '<center>' +  msgConfigured + '</center>';
                }
            } else {
                msg = '<center><p>' + result + '</p></center>';
                msg = msg + '<center>' +  closeBtn + '</center>';
                ajaxObj = getXmlHttpRequestObject();
            }
            document.getElementById('dlg').innerHTML = msg;
        }
    }

    document.getElementById('domainLabel').style.display = 'none';
    frm.elements['RegisterProduct.tfDomain'].style.display = 'none';

</script>

</jato:useViewBean>
