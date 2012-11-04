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

   $Id: CreateRemoteIDP.jsp,v 1.9 2009/07/20 23:03:23 asyhuang Exp $

--%>

<%--
   Portions Copyrighted 2012 ForgeRock Inc
--%>

<%@ page info="CreateRemoteIDP" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<%
    request.setCharacterEncoding("UTF-8");
%>
<jato:useViewBean
    className="com.sun.identity.console.task.CreateRemoteIDPViewBean"
    fireChildDisplayEvents="true" >

<cc:i18nbundle baseName="amConsole" id="amConsole"
    locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>

<cc:header name="hdrCommon" pageTitle="webconsole.title" bundleID="amConsole" copyrightYear="2008" fireDisplayEvents="true">

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

<cc:form name="CreateRemoteIDP" method="post">
<jato:hidden name="szCache" />
<script language="javascript">
    function confirmLogout() {
        return confirm("<cc:text name="txtLogout" defaultValue="masthead.logoutMessage" bundleID="amConsole"/>");
    }

    function openWindow(fieldName) {
        selectWin = window.open('../federation/FileUploader', fieldName,
            'height=300,width=650,top=' +
            ((screen.height-(screen.height/2))-(500/2)) +
            ',left=' + ((screen.width-650)/2));
        selectWin.focus();
    }

    function metaOptionSelect(radio) {
        if (radio.value == 'url') {
            frm.elements['CreateRemoteIDP.tfMetadataFileURL'].style.display = '';
            frm.elements['CreateRemoteIDP.btnMetadata'].style.display = 'none';
        } else {
            frm.elements['CreateRemoteIDP.tfMetadataFileURL'].style.display = 'none';
            frm.elements['CreateRemoteIDP.btnMetadata'].style.display = '';
        }
    }

    function cancelOp() {
        document.location.replace("../task/Home");
        return false;
    }

    function realmSelect(radio) {
        getCircleOfTrust(radio.value);
    }

    function cotOptionSelect(radio) {
        var ans = radio.value;
        if (ans == 'yes') {
            document.getElementById('cotchoice').style.display = 'block';
            document.getElementById('cottf').style.display = 'none';
            frm.elements['CreateRemoteIDP.tfCOT'].value = '';
        } else {
            document.getElementById('cotchoice').style.display = 'none';
            document.getElementById('cottf').style.display = 'block';
        }
    }

    function hideRealm() {
        var frm = document.forms['CreateRemoteIDP'];
        var realmobj = frm.elements['CreateRemoteIDP.tfRealm'];
        if (realmobj.options.length < 2) {
            hideRealmObjs();
        }
    }

    function hideRealmObjs() {
        document.getElementById('realmlbl').style.display = 'none';
        document.getElementById('realmfld').style.display = 'none';
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
<cc:pagetitle name="pgtitle" bundleID="amConsole" pageTitleText="page.title.configure.remote.idp" showPageTitleSeparator="true" viewMenuLabel="" pageTitleHelpMessage="" showPageButtonsTop="true" showPageButtonsBottom="false" />

<table border="0" cellpadding="20" cellspacing="0">
<tr><td>
    <cc:text name="txtDesc" defaultValue="page.desc.configure.remote.idp" bundleID="amConsole" />
</td></tr>
</table>


<cc:propertysheet name="propertyAttributes" bundleID="amConsole" showJumpLinks="false"/>

</cc:form>
</cc:header>
</div>
<div id="dlg" class="dvs"></div>

<script language="javascript">
    hideRealm();

    var msgConfiguring = "<cc:text name="txtConfiguring" defaultValue="configure.provider.waiting" bundleID="amConsole" escape="false" />";

    var msgConfigured = "<p>&nbsp;</p><input name=\"btnOK\" type=\"submit\" class=\"Btn1\" value=\"<cc:text name="txtOKBtn" defaultValue="ajax.ok.button" bundleID="amConsole" />\" onClick=\"document.location.replace(\'../task/Home\');return false;\" /></div></p>";
    var closeBtn = "<p>&nbsp;</p><p><div class=\"TtlBtnDiv\"><input name=\"btnClose\" type=\"submit\" class=\"Btn1\" value=\"<cc:text name="txtCloseBtn" defaultValue="ajax.close.button" bundleID="amConsole" />\" onClick=\"focusMain();return false;\" /></div></p>";

    var frm = document.forms['CreateRemoteIDP'];
    var btn1 = frm.elements['CreateRemoteIDP.button1'];
    btn1.onclick = submitPage;
    var btn2 = frm.elements['CreateRemoteIDP.button2'];
    btn2.onclick = cancelOp;
    var ajaxObj = getXmlHttpRequestObject();
    var userLocale = "<%= viewBean.getUserLocale().toString() %>";

    function submitPage() {
        fade();
        document.getElementById('dlg').innerHTML = '<center>' + 
            msgConfiguring + '</center>';
        var url = "../console/ajax/AjaxProxy.jsp";
        var params = 'locale=' + userLocale +
            '&class=com.sun.identity.workflow.CreateRemoteIDP' + getData();
        ajaxPost(ajaxObj, url, params, configured);
        return false;
    }

    function getData() {
        var cot;
        var cotRadio = getRadioVal(frm, 'CreateRemoteIDP.radioCOT');
        if (cotRadio == "yes") {
            cot = frm.elements['CreateRemoteIDP.choiceCOT'].value;
        } else {
            cot = frm.elements['CreateRemoteIDP.tfCOT'].value;
        }

        var realm = frm.elements['CreateRemoteIDP.tfRealm'].value;
        var metaRadio = getRadioVal(frm, 'CreateRemoteIDP.radioMeta');
        var meta = (metaRadio == 'url') ?
            frm.elements['CreateRemoteIDP.tfMetadataFileURL'].value :
            frm.elements['CreateRemoteIDP.tfMetadataFile'].value;

        return "&metadata=" + escapeEx(meta) +
            "&realm=" + escapeEx(realm) +
            "&cot=" + escapeEx(cot);
    }

    function configured() {
        if (ajaxObj.readyState == 4) {
            var result = hexToString(ajaxObj.responseText);
            var status = result.substring(0, result.indexOf('|'));
            var result = result.substring(result.indexOf('|') +1);
            var msg = '<center><p>' + result + '</p></center>';
            if (status == 0) {
                msg = msg + '<center>' +  msgConfigured + '</center>';
            } else {
                msg = msg + '<center>' +  closeBtn + '</center>';
            }
            document.getElementById('dlg').innerHTML = msg;
        }
    }

    function getCircleOfTrust(realm) {
        var url = "../console/ajax/AjaxProxy.jsp";
        var params = 'locale=' + userLocale +
            '&class=com.sun.identity.workflow.GetCircleOfTrusts' +
            '&realm=' + escapeEx(realm);
        ajaxPost(ajaxObj, url, params, circleOfTrust);
    }

    function circleOfTrust() {
        if (ajaxObj.readyState == 4) {
            var result = hexToString(ajaxObj.responseText);
            var status = result.substring(0, result.indexOf('|'));
            var result = result.substring(result.indexOf('|') +1);
            var msg = '';
            if (status == 0) {
                document.getElementById('cotsection').style.display = 'block';
                result = result.replace(/^\s+/, '');
                result = result.replace(/\s+$/, '');
                if (result.length == 0) {
                    document.getElementById('cotq').style.display = 'none';
                    document.getElementById('cotchoice').style.display = 'none';
                    document.getElementById('cottf').style.display = 'block';
                    chooseRadio(frm, 'CreateRemoteIDP.radioCOT', 'no');
                } else {
                    var cots = result.split('|');
                    var choiceCOT = frm.elements['CreateRemoteIDP.choiceCOT'];
                    for (var i = 0; i < cots.length; i++) {
                        var decodedCOT = decodeURIComponent(cots[i]);
                        choiceCOT.options[i] = new Option(decodedCOT,
                            decodedCOT);
                    }
                    document.getElementById('cotq').style.display = 'block';
                    document.getElementById('cotchoice').style.display = 'block';
                    document.getElementById('cottf').style.display = 'none';
                    chooseRadio(frm, 'CreateRemoteIDP.radioCOT', 'yes');
                }
                focusMain();
                if (presetcot) {
                    selectOption(frm, 'CreateRemoteIDP.choiceCOT', presetcot);
                    presetcot = null;
                }
            } else {
                msg = '<center><p>' + result + '</p></center>';
                msg = msg + '<center>' +  closeBtn + '</center>';
                document.getElementById('dlg').innerHTML = msg;
                document.getElementById('cotsection').style.display = 'none';
                ajaxObj = getXmlHttpRequestObject();
            }
        }
    }

    frm.elements['CreateRemoteIDP.btnMetadata'].style.display = 'none';
    var presetcot = frm.elements['CreateRemoteIDP.tfCOT'].value;

<%
    String cot = request.getParameter("cot");
    if ((cot != null) && (cot.trim().length() > 0)) {
        out.println("hideRealmObjs();");
        out.println("document.getElementById('cotsection').style.display = 'none';");
    } else {
        out.println("getCircleOfTrust('/');");
    }
    
%>

    function unescapeQuote(str) {
        str = str.replace(/&quot;/g, '"');
        str = str.replace(/&lt;/g, '<');
        str = str.replace(/&gt;/g, '>');
        return str;
    }
 
    var infoRealm = unescapeQuote("<cc:text name="txtInfoRealm" defaultValue="configure.provider.help.realm" bundleID="amConsole" />");
    var infoMetadataFile = unescapeQuote("<cc:text name="txtInfoMetadataFile" defaultValue="configure.provider.help.metadata" bundleID="amConsole" />");
    var infoMetadataFileURL = unescapeQuote("<cc:text name="txtInfoMetadataFileURL" defaultValue="configure.provider.help.metadataurl" bundleID="amConsole" />");
</script>

</jato:useViewBean>
