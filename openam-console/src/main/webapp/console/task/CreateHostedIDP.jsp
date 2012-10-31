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

   $Id: CreateHostedIDP.jsp,v 1.15 2009/07/20 23:03:23 asyhuang Exp $

--%>

<%@ page info="CreateHostedIDP" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<%
    request.setCharacterEncoding("UTF-8");
%>
<jato:useViewBean
    className="com.sun.identity.console.task.CreateHostedIDPViewBean"
    fireChildDisplayEvents="true" >

<cc:i18nbundle baseName="amConsole" id="amConsole"
    locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>

<cc:header name="hdrCommon" pageTitle="webconsole.title" bundleID="amConsole" copyrightYear="2008" fireDisplayEvents="true">

<link rel="stylesheet" type="text/css" href="../console/css/openam.css" />

<script language="javascript" src="../console/js/am.js"></script>
<script language="javascript" src="../console/js/tasksPage.js"></script>
<script language="javascript" src="../com_sun_web_ui/js/dynamic.js"></script>

<div id="main" style="position: absolute; margin: 0; border: none; padding: 0; width:auto; height:1000">
<div id="divhelp" style="display: none; position:absolute; margin: 0; border: 1px solid #AABCC8; padding: 0; width:400px; height:200px; background:#FCFCFC">
<table border=0 cellpadding=2 cellspacing=0 width="100%">
<tr><td width=99%><span id="divHelpmsg" /></td>
<td width="1%" valign="top">
<img src="../console/images/tasks/close.gif" width="16" height="16" onClick="hideHelp()" />
</td>
</tr>
</table>
</div>


<cc:form name="CreateHostedIDP" method="post">
<jato:hidden name="szCache" />

<cc:primarymasthead name="mhCommon" bundleID="amConsole"  logoutOnClick="return confirmLogout();" locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>
<table border="0" cellpadding="10" cellspacing="0" width="100%">
    <tr>
        <td>
        <cc:alertinline name="ialertCommon" bundleID="amConsole" />
        </td>
    </tr>
</table>

<%-- PAGE CONTENT --------------------------------------------------------- --%>
<cc:pagetitle name="pgtitle" bundleID="amConsole" pageTitleText="page.title.configure.hosted.idp" showPageTitleSeparator="true" viewMenuLabel="" pageTitleHelpMessage="" showPageButtonsTop="true" showPageButtonsBottom="false" />

<table border="0" cellpadding="20" cellspacing="0">
<tr><td>
    <cc:text name="txtDesc" defaultValue="page.desc.configure.hosted.idp" bundleID="amConsole" />
</td></tr>
</table>

<cc:propertysheet name="propertyAttributes" bundleID="amConsole" showJumpLinks="false"/>

</cc:form>
</div>
<div id="dlg" class="dvs" style="height:200px"></div>

<script language="javascript">
    metadiv = document.getElementById("meta");
    metadivDisplay = metadiv.style.display;

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

    function metadataOptionSelect(radio) {
        var infodiv = document.getElementById("info");
        hasMetaData = radio.value;
        if (radio.value == 'yes') {
            infodiv.style.display = 'none';
            metadiv.style.display = '';
            document.getElementById('cotsection').style.display = 'none';
            document.getElementById('cotq').style.display = 'none';
            document.getElementById('cottf').style.display = 'none';
            document.getElementById('cotchoice').style.display = 'none';
        } else {
            infodiv.style.display = '';
            metadiv.style.display = 'none';
            document.getElementById('cotsection').style.display = '';
            document.getElementById('cotq').style.display = '';
            var realm = frm.elements['CreateHostedIDP.tfRealm'].value;
            getCircleOfTrust(realm);
        }
    }

    function metaOptionSelect(radio) {
        if (radio.value == 'url') {
            frm.elements['CreateHostedIDP.tfMetadataFileURL'].style.display = '';
            frm.elements['CreateHostedIDP.btnMetadata'].style.display = 'none';
            document.getElementById('metadatafilename').style.display = 'none';
        } else {
            frm.elements['CreateHostedIDP.tfMetadataFileURL'].style.display = 'none';
            frm.elements['CreateHostedIDP.btnMetadata'].style.display = '';
            document.getElementById('metadatafilename').style.display = '';
        }
    }

    function extendedOptionSelect(radio) {
        if (radio.value == 'url') {
            frm.elements['CreateHostedIDP.tfExtendedFileURL'].style.display = '';
            frm.elements['CreateHostedIDP.btnExtendedFile'].style.display = 'none';
            document.getElementById('extendedfilename').style.display = 'none';
        } else {
            frm.elements['CreateHostedIDP.tfExtendedFileURL'].style.display = 'none';
            frm.elements['CreateHostedIDP.btnExtendedFile'].style.display = '';
            document.getElementById('extendedfilename').style.display = '';
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
            document.getElementById('cotchoice').style.display = '';
            document.getElementById('cottf').style.display = 'none';
            frm.elements['CreateHostedIDP.tfCOT'].value = '';
        } else {
            document.getElementById('cotchoice').style.display = 'none';
            document.getElementById('cottf').style.display = '';
        }
    }

    function getExtendedData() {
        var extRadio = getRadioVal(frm, 'CreateHostedIDP.radioExtendedData');
        var extended = (extRadio == 'url') ?
            frm.elements['CreateHostedIDP.tfExtendedFileURL'].value :
            frm.elements['CreateHostedIDP.tfExtendedFile'].value;
        extended = extended.replace(/^\s+/, "");
        extended = extended.replace(/\s+$/, "");
        return extended;
    }

    function getCircleOfTrustFromExt() {
        var extended = getExtendedData();
        if (extended.length == 0) {
            return;
        }

        document.getElementById('dlg').style.top = '400px';
        fade();
        document.getElementById('dlg').innerHTML = '<center>' + 
            msgGetCOTs + '</center>';
        var url = "../console/ajax/AjaxProxy.jsp";
        var params = 'locale=' + userLocale +
            '&class=com.sun.identity.workflow.GetCircleOfTrusts' + 
            '&extendeddata=' + escapeEx(extended);
        ajaxPost(ajaxObj, url, params, circleOfTrust);
    }

    function hideRealm() {
        var frm = document.forms['CreateHostedIDP'];
        var realmobj = frm.elements['CreateHostedIDP.tfRealm'];
        if (realmobj.options.length < 2) {
            document.getElementById('realmlbl').style.display = 'none';
            document.getElementById('realmfld').style.display = 'none';
        }
    }
    hideRealm();

    var msgConfiguring = "<cc:text name="txtConfiguring" defaultValue="configure.provider.waiting" bundleID="amConsole" escape="false" />";

    var closeBtn = "<p>&nbsp;</p><p><div class=\"TtlBtnDiv\"><input name=\"btnClose\" type=\"submit\" class=\"Btn1\" value=\"<cc:text name="txtCloseBtn" defaultValue="ajax.close.button" bundleID="amConsole" escape="false" />\" onClick=\"focusMain();return false;\" /></div></p>";

    var msgGetCOTs = "<cc:text name="txtConfigured" defaultValue="configure.provider.get.cots" bundleID="amConsole" escape="false" />";

    var msgMissingAttrMappingValues = "<cc:text name="txtMissingAttrValues" defaultValue="configure.provider.missing.attribute.mapping.values" bundleID="amConsole" escape="false" />" + "<p>" + closeBtn + "</p>";

    var hasMetaData = 'no';
    var frm = document.forms['CreateHostedIDP'];
    var btn1 = frm.elements['CreateHostedIDP.button1'];
    btn1.onclick = submitPage;
    var btn2 = frm.elements['CreateHostedIDP.button2'];
    btn2.onclick = cancelOp;
    var ajaxObj = getXmlHttpRequestObject();
    var data = '';
    var selectOptionCache;
    var userLocale = "<%= viewBean.getUserLocale().toString() %>";

    function submitPage() {
        document.getElementById('dlg').style.top = '300px';
        fade();
        if (document.getElementById('cotsection').style.display != '') {
            var extended = getExtendedData();
            if (extended.length > 0) {
                if (hasMetaData) {
                    getCircleOfTrustFromExt();
                } else {
                    var realm = frm.elements['CreateHostedIDP.tfRealm'].value;
                    getCircleOfTrust(realm);
                }
                focusMain();
                return false;
            }
        }
        document.getElementById('dlg').innerHTML = '<center>' + 
        msgConfiguring + '</center>';
        var url = "../console/ajax/AjaxProxy.jsp";
        var params = 'locale=' + userLocale +
            '&class=com.sun.identity.workflow.CreateHostedIDP' + getData();
        ajaxPost(ajaxObj, url, params, configured);
        return false;
    }

    function getData() {
        var cot;
        var cotRadio = getRadioVal(frm, 'CreateHostedIDP.radioCOT');
        if (cotRadio == "yes") {
            cot = frm.elements['CreateHostedIDP.choiceCOT'].value;
        } else {
            cot = frm.elements['CreateHostedIDP.tfCOT'].value;
        }
        if (hasMetaData == "yes") {
            var metaRadio = getRadioVal(frm, 'CreateHostedIDP.radioMeta');
            var meta = (metaRadio == 'url') ?
                frm.elements['CreateHostedIDP.tfMetadataFileURL'].value :
                frm.elements['CreateHostedIDP.tfMetadataFile'].value;
            var extRadio = getRadioVal(frm, 'CreateHostedIDP.radioExtendedData');
            var extended = (extRadio == 'url') ?
                frm.elements['CreateHostedIDP.tfExtendedFileURL'].value :
                frm.elements['CreateHostedIDP.tfExtendedFile'].value;

            return "&metadata=" + escapeEx(meta) +
                "&extendeddata=" + escapeEx(extended) +
                "&cot=" + escapeEx(cot) +
                "&attributemappings=" + escapeEx(getNameAttributeMapping());
        } else {
            var realm = frm.elements['CreateHostedIDP.tfRealm'].value;
            return "&entityId=" +
            escapeEx(frm.elements['CreateHostedIDP.tfEntityId'].value) +
            "&realm=" + escapeEx(realm) +
            "&idpscert=" +
            escapeEx(frm.elements['CreateHostedIDP.tfSigningKey'].value) +
            "&cot=" + escapeEx(cot) +
            "&attributemappings=" + escapeEx(getNameAttributeMapping());
        }
    }

    function getNameAttributeMapping() {
        var attrMappings = '';
        var table = getActionTable();
        var rows = table.getElementsByTagName('TR');
        for (var i = rows.length-1; i >=3; --i) {
            var inputs = rows[i].getElementsByTagName('input');
            var cb = inputs[0];
            attrMappings += cb.getAttribute("value") + '|';
        }
        return attrMappings;
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
                document.getElementById('cotsection').style.display = '';
                result = result.replace(/^\s+/, '');
                result = result.replace(/\s+$/, '');
                if (result.length == 0) {
                    document.getElementById('cotq').style.display = 'none';
                    document.getElementById('cotchoice').style.display = 'none';
                    document.getElementById('cottf').style.display = '';
                    chooseRadio(frm, 'CreateHostedIDP.radioCOT', 'no');
                } else {
                    var cots = result.split('|');
                    var choiceCOT = frm.elements['CreateHostedIDP.choiceCOT'];
                    for (var i = 0; i < cots.length; i++) {
                        var decodedCOT = decodeURIComponent(cots[i]);
                        choiceCOT.options[i] = new Option(decodedCOT,
                            decodedCOT);
                    }
                    document.getElementById('cotq').style.display = '';
                    document.getElementById('cotchoice').style.display = '';
                    document.getElementById('cottf').style.display = 'none';
                    chooseRadio(frm, 'CreateHostedIDP.radioCOT', 'yes');
                }
                focusMain();
            } else {
                msg = '<center><p>' + result + '</p></center>';
	        msg = msg + '<center>' +  closeBtn + '</center>';
                document.getElementById('dlg').innerHTML = msg;
                document.getElementById('cotsection').style.display = 'none';
                ajaxObj = getXmlHttpRequestObject();
            }
        }
    }

    function configured() {
        if (ajaxObj.readyState == 4) {
            var result = hexToString(ajaxObj.responseText);
            var status = result.substring(0, result.indexOf('|'));
            var result = result.substring(result.indexOf('|') +1);
            var msg = '';
            if (status == 0) {
                var idx = result.indexOf('|||');
                data = result.substring(idx +3);
        var cot;
        var cotRadio = getRadioVal(frm, 'CreateHostedIDP.radioCOT');
        if (cotRadio == "yes") {
            cot = frm.elements['CreateHostedIDP.choiceCOT'].value;
        } else {
            cot = frm.elements['CreateHostedIDP.tfCOT'].value;
        }
        document.location.replace('CompleteCreateHostedIDP?cot=' + cot + '&' + data);
            } else {
                msg = '<center><p>' + result + '</p></center>';
		msg = msg + '<center>' +  closeBtn + '</center>';
                ajaxObj = getXmlHttpRequestObject();
            }
            document.getElementById('dlg').innerHTML = msg;
        }
    }

    function addAttrMapping() {
        var name = frm.elements['CreateHostedIDP.tfAttrMappingName'].value;
        var assertn = frm.elements['CreateHostedIDP.tfAttrMappingAssertion'].value;
        name = name.replace(/^\s+/, '');
        name = name.replace(/\s+$/, '');
        assertn = assertn.replace(/^\s+/, '');
        assertn = assertn.replace(/\s+$/, '');
        if ((name == '') || (assertn == '')) {
            document.getElementById('dlg').style.top = '600px';
            fade();
            document.getElementById('dlg').innerHTML = '<center>' + 
                msgMissingAttrMappingValues  + '</center>';
        } else {
            addPropertyRow(name, assertn);
            frm.elements['CreateHostedIDP.tfAttrMappingName'].value = '';
            frm.elements['CreateHostedIDP.tfAttrMappingAssertion'].value = '';
        }
    }

    var runningNumber = 0;

    function addPropertyRow(name, assertn) {
        var table = getActionTable();
        var tBody = table.getElementsByTagName("TBODY").item(0);
        var row = document.createElement("TR");
        var cell1 = document.createElement("TD");
        var cell2 = document.createElement("TD");
        var cell3 = document.createElement("TD");

        cell1.setAttribute("align", "center");
        cell1.setAttribute("valign", "top");

        var cb = document.createElement("input");
        var textnode1 = document.createTextNode(assertn);
        var textnode2 = document.createTextNode(name);
        cb.setAttribute("type", "checkbox");
        cb.setAttribute("value", assertn + "=" + name);
        cell1.appendChild(cb);
        cell2.appendChild(textnode1);
        cell3.appendChild(textnode2);
        cb.onclick = function() {toggleTblButtonState('CreateHostedIDP', 'CreateHostedIDP.tblattrmapping', 'tblButton', 'CreateHostedIDP.deleteAttrMappingBtn', this);};
        row.appendChild(cell1);
        row.appendChild(cell2);
        row.appendChild(cell3);
        tBody.appendChild(row);

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

    function deletePropertyRow() {
        var table = getActionTable();
        var rows = table.getElementsByTagName('TR');
        for (var i = rows.length-1; i >=3; --i) {
            var inputs = rows[i].getElementsByTagName('input');
            var cb = inputs[0];
            if (cb.checked) {
                table.deleteRow(i-1);
            }
        }
        tblBtnCounter['tblButton'] = 0;
        ccSetButtonDisabled('CreateHostedIDP.deleteAttrMappingBtn', 'CreateHostedIDP', true);
        return false;
    }

    function signKeySelect(menu) {
        if (menu.value == 'test') {
            document.getElementById('signTest').style.display = '';
        } else {
            document.getElementById('signTest').style.display = 'none';
        }
    }

    function userAttrSelect(menu) {
        frm.elements['CreateHostedIDP.tfAttrMappingName'].value = menu.value;
    }

    frm.elements['CreateHostedIDP.tfMetadataFileURL'].style.display = 'none';
    frm.elements['CreateHostedIDP.tfExtendedFileURL'].style.display = 'none';
    getCircleOfTrust('/');
    getActionTable().deleteRow(2);

    function unescapeQuote(str) {
        return str.replace(/&quot;/g, '"');
    }

    var infoHasMetaData = unescapeQuote("<cc:text name="txtInfoHasMetaData" defaultValue="configure.provider.help.hasMetaData" bundleID="amConsole" />");
    var infoRadioMeta = unescapeQuote("<cc:text name="txtInfoRadioMeta" defaultValue="configure.provider.help.metadata" bundleID="amConsole" />");
    var infoExtendedDataRadio = unescapeQuote("<cc:text name="txtInfoHasMetaData" defaultValue="configure.provider.help.extendeddata" bundleID="amConsole" />");
    var infoRealm = unescapeQuote("<cc:text name="txtInfoRealm" defaultValue="configure.provider.help.realm" bundleID="amConsole" />");
    var infoEntityId = unescapeQuote("<cc:text name="txtInfoEntityId" defaultValue="configure.provider.help.entityId" bundleID="amConsole" />");
    var infoSigningKey = unescapeQuote("<cc:text name="txtInfoSigningKey" defaultValue="configure.provider.help.signing.key" bundleID="amConsole" />");
    var infoMenuUserAttributes = unescapeQuote("<cc:text name="txtInfoMenuUserAttributes" defaultValue="configure.provider.help.user.attributes.choices" bundleID="amConsole" />");
</script>

</cc:header>
</jato:useViewBean>
