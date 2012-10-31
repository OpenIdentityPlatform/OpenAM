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

   $Id: ValidateSAML2Setup.jsp,v 1.9 2009/07/20 23:03:23 asyhuang Exp $

--%>

<%@ page info="ValidateSAML2Setup" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<%
    request.setCharacterEncoding("UTF-8");
    response.setContentType("text/html; charset=utf-8");
%>
<jato:useViewBean
    className="com.sun.identity.console.task.ValidateSAML2SetupViewBean"
    fireChildDisplayEvents="true" >

<cc:i18nbundle baseName="amConsole" id="amConsole"
    locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>

<cc:header name="hdrCommon" pageTitle="webconsole.title" bundleID="amConsole" copyrightYear="2008" fireDisplayEvents="true" onLoad="onload()">

<link rel="stylesheet" type="text/css" href="../console/css/openam.css" />

<script language="javascript" src="../console/js/am.js"></script>
<script language="javascript" src="../console/js/tasksPage.js"></script>
<script language="javascript" src="../com_sun_web_ui/js/dynamic.js"></script>

<div id="main" style="position: absolute; margin: 0; border: none; padding: 0; width:auto; height:101%;">

<div id="divhelp" style="display: none; position:absolute; margin: 0; border: 1px solid #AABCC8; padding: 0; width:400px; height:200px; background:#FCFCFC">
<table border=0 cellpadding=2 cellspacing=0 width="100%">
<tr><td width=99%><span id="divHelpmsg" /></td>
<td width="1%" valign="top">
<img src="../console/images/tasks/close.gif" width="16" height="16" onClick="hideHelp()" />
</td>
</tr>
</table>
</div>


<cc:form name="ValidateSAML2Setup" method="post">
<jato:hidden name="szCache" />
<script language="javascript">
    function onload() {
    }
    function confirmLogout() {
        return confirm("<cc:text name="txtLogout" defaultValue="masthead.logoutMessage" bundleID="amConsole"/>");
    }

    function cancelOp() {
        document.location.replace("../task/Home");
        return false;
    }

    function realmSelect(radio) {
        document.location.replace("../task/ValidateSAML2Setup?realm=" + escapeEx(radio.value));
    }
</script>

<cc:primarymasthead name="mhCommon" bundleID="amConsole"  logoutOnClick="return confirmLogout();" locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>
<table border="0" cellpadding="10" cellspacing="0" width="100%">
    <tr>
        <td>
        <cc:alertinline name="ialertCommon" bundleID="amConsole" />
        </td>
    </tr>
</table>

<%-- PAGE CONTENT --------------------------------------------------------- --%>
<cc:pagetitle name="pgtitle" bundleID="amConsole" pageTitleText="page.title.validate.fed" showPageTitleSeparator="true" viewMenuLabel="" pageTitleHelpMessage="" showPageButtonsTop="true" showPageButtonsBottom="false" />

<table border="0" cellpadding="20" cellspacing="0">
<tr><td>
    <cc:text name="txtDesc" defaultValue="page.desc.validate" bundleID="amConsole" />
</td></tr>
</table>

<cc:propertysheet name="propertyAttributes" bundleID="amConsole" showJumpLinks="false"/>

</cc:form>
<form name="hidden" action="../validatorMain.jsp" method="POST">
<input type="hidden" name="realm" />
<input type="hidden" name="cot" />
<input type="hidden" name="idp" />
<input type="hidden" name="sp" />
<input type="hidden" name="locale" />
</form>

<div id="cannotValidateDiv" class="bubble"><cc:text name="txtCannotValidateDiv" defaultValue="validate.cannot.validate.div" bundleID="amConsole" escape="false" /></div>
</div>
<iframe id="hiddenframe" style="display:none"></iframe>
<div id="dlg" class="dvs"></div>

<script language="javascript">
    var msgGetEntities = "<cc:text name="txtGetEntities" defaultValue="validate.entities.get.entities" bundleID="amConsole" escape="false" />";
    var closeBtn = "<p>&nbsp;</p><p><div class=\"TtlBtnDiv\"><input name=\"btnClose\" type=\"submit\" class=\"Btn1\" value=\"<cc:text name="txtCloseBtn" defaultValue="ajax.close.button" bundleID="amConsole" />\" onClick=\"focusMain();return false;\" /></div></p>";
    var okBtn = "<p>&nbsp;</p><p><div class=\"TtlBtnDiv\"><input name=\"btnOk\" type=\"submit\" class=\"Btn1\" value=\"<cc:text name="txtOkBtn" defaultValue="button.ok" bundleID="amConsole" />\" onClick=\"validate();return false;\" /></div></p>";
    var readyValidate = "<cc:text name="txtReadyForTest" defaultValue="validate.ready.for.test" bundleID="amConsole" escape="false" />";
    var loggingout = "<p><center><img src=\"../console/images/processing.gif\" /></center></p><cc:text name="txtloggingout" defaultValue="validate.logout" bundleID="amConsole" escape="false" />";

    var userLocale = "<%= viewBean.getUserLocale().toString() %>";
    var ajaxObj = getXmlHttpRequestObject();
    var frm = document.forms['ValidateSAML2Setup'];
    var btn1 = frm.elements['ValidateSAML2Setup.button1'];
    btn1.onclick = validateNow;
    var btn2 = frm.elements['ValidateSAML2Setup.button2'];
    btn2.onclick = cancelOp;

    function showCannotValidateDiv(e) {
        var vdiv = document.getElementById('cannotValidateDiv');
        if (window.event) {
            vdiv.style.left = (window.event.clientX +10) + 'px';
            vdiv.style.top = (window.event.clientY +10) + 'px';
        } else {
            vdiv.style.left = (e.pageX +10) + 'px';
            vdiv.style.top = (e.pageY +10) + 'px';
        }
        vdiv.style.display = 'block';
    }

    function hideCannotValidateDiv() {
        var vdiv = document.getElementById('cannotValidateDiv');
        vdiv.style.display = 'none';
    }

    function cancelOp() {
        document.location.replace("../task/Home");
        return false;
    }

    function validate() {
        document.getElementById('hiddenframe').src = '../UI/Logout';
        var msg = "<center>" + loggingout + "</center>";
        document.getElementById('dlg').innerHTML = msg;
        setTimeout('validateEx()', 4000);
    }

    function validateEx() {
        var realm = frm.elements['ValidateSAML2Setup.tfRealm'].value;
        var cotName = frm.elements['ValidateSAML2Setup.tfCOT'].value;
        var idp = frm.elements['ValidateSAML2Setup.tfIDP'].value;
        var sp = frm.elements['ValidateSAML2Setup.tfSP'].value;

        var hiddenFrm = document.forms['hidden'];
        hiddenFrm.elements['realm'].value = realm;
        hiddenFrm.elements['cot'].value = cotName;
        hiddenFrm.elements['idp'].value = idp;
        hiddenFrm.elements['sp'].value = sp;
        hiddenFrm.elements['locale'].value = '<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>';
        hiddenFrm.submit();
    }

    function validateNow() {
        fade();
        var msg = "<center>" + readyValidate + okBtn + "</center>";
        document.getElementById('dlg').innerHTML = msg;
        return false;
    }

    function selectCOT(radio) {
        var cotName = cots[radio.value];
        selectCOTWithValue(cotName);
    }

    function selectCOTWithValue(cotName) {
        document.getElementById('txtCot').innerHTML = cotName;
        frm.elements['ValidateSAML2Setup.tfCOT'].value = cotName;
        document.getElementById('divCOTLabel').style.display = '';
        document.getElementById('divCOT').style.display = '';
        hideCOTTable();
        document.getElementById('linkShowCOTTable').style.display = '';
        document.getElementById('divEntities').style.display = '';

        fade();
        document.getElementById('dlg').innerHTML = '<center>' +
            msgGetEntities + '<center>';
        var realm = frm.elements['ValidateSAML2Setup.tfRealm'].value;
        var url = "../console/ajax/AjaxProxy.jsp";
        var params = 'locale=' + userLocale +
            '&class=com.sun.identity.workflow.GetIDPSPPairingInCOT' +
            '&cot=' + escapeEx(cotName) + "&realm=" + escapeEx(realm) ;
        ajaxPost(ajaxObj, url, params, gotEntities);
    }

    function gotEntities() {
        if (ajaxObj.readyState == 4) {
            var result = hexToString(ajaxObj.responseText);
            var status = result.substring(0, result.indexOf('|'));
            var result = result.substring(result.indexOf('|') +1);
            var msg = '';
            if (status == 0) {
                constructArray(result);

                if (hostedidp.length > 0) {
                    for (var i = 0; i < hostedidp.length; i++) {
                        addOption(frm, 'ValidateSAML2Setup.tfIDP', 
                            decodeURIComponent(hostedidp[i]));
                    }
                    for (var i = 0; i < remotesp.length; i++) {
                        addOption(frm, 'ValidateSAML2Setup.tfSP',
                            decodeURIComponent(remotesp[i]));
                    }
                } else {
                    for (var i = 0; i < hostedsp.length; i++) {
                        addOption(frm, 'ValidateSAML2Setup.tfSP',
                            decodeURIComponent(hostedsp[i]));
                    }
                }

                for (var i = 0; i < remoteidp.length; i++) {
                    addOption(frm, 'ValidateSAML2Setup.tfIDP',
                        decodeURIComponent(remoteidp[i]));
                }
                    
                focusMain();
                disableButton(frm, 'ValidateSAML2Setup.button1', false);
            } else {
                msg = '<center><p>' + result + '</p></center>';
                msg = msg + '<center>' +  closeBtn + '</center>';
                document.getElementById('dlg').innerHTML = msg;
                ajaxObj = getXmlHttpRequestObject();
            }
        }
    }


    function hideCOTTable() {
        document.getElementById('linkShowCOTTable').style.display = '';
        document.getElementById('linkHideCOTTable').style.display = 'none';
        document.getElementById('divCOTTable').style.display = 'none';
    }

    function showCOTTable() {
        document.getElementById('linkShowCOTTable').style.display = 'none';
        document.getElementById('linkHideCOTTable').style.display = '';
        document.getElementById('divCOTTable').style.display = '';
    }

    function idpSelect(menu) {
        var idp = menu.value;
        clearOptions(frm, 'ValidateSAML2Setup.tfSP');
        if (idp.indexOf("(") != -1) {
            for (var i = 0; i < remotesp.length; i++) {
                addOption(frm, 'ValidateSAML2Setup.tfSP',
                    decodeURIComponent(remotesp[i]));
            }
        } else {
            for (var i = 0; i < hostedsp.length; i++) {
                addOption(frm, 'ValidateSAML2Setup.tfSP', hostedsp[i]);
            }
        }
    }

    function unescapeQuote(str) {
        str = str.replace(/&quot;/g, '"');
        str = str.replace(/&lt;/g, '<');
        str = str.replace(/&gt;/g, '>');
        return str;
    }
 
    var infoRealm = unescapeQuote("<cc:text name="txtInfoRealm" defaultValue="validate.entities.help.realm" bundleID="amConsole" />");
    var infoIDP = unescapeQuote("<cc:text name="txtInfoIDP" defaultValue="validate.entities.help.idp" bundleID="amConsole" />");
    var infoSP = unescapeQuote("<cc:text name="txtInfoSP" defaultValue="validate.entities.help.sp" bundleID="amConsole" />");

</script>
</cc:header>

</jato:useViewBean>
