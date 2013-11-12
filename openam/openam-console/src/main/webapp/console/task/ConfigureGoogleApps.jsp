<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
  
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

   $Id: ConfigureGoogleApps.jsp,v 1.8 2009/08/14 18:32:46 asyhuang Exp $

--%>

<%--
   Portions Copyrighted 2013 ForgeRock AS
--%>

<%@ page import="org.owasp.esapi.ESAPI"%>
<%@ page info="CreateGoogleApps" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<%
    request.setCharacterEncoding("UTF-8");
%>

<jato:useViewBean
    className="com.sun.identity.console.task.ConfigureGoogleAppsViewBean"
    fireChildDisplayEvents="true" >

<cc:i18nbundle baseName="amConsole" id="amConsole"
    locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>

<cc:header name="hdrCommon" pageTitle="webconsole.title" bundleID="amConsole" copyrightYear="2009" fireDisplayEvents="true">

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

<cc:form name="ConfigureGoogleApps" method="post">
<jato:hidden name="szCache" />
<script language="javascript">
    function confirmLogout() {
        return confirm("<cc:text name="txtLogout" defaultValue="masthead.logoutMessage" bundleID="amConsole"/>");
    }

    function cancelOp() {
        document.location.replace("../task/Home");
        return false;
    }

    function realmSelect(radio) {
        hideCOTObjs();
        hideIDPObjs();
        getCircleOfTrust(radio.value);
        var choiceCOT = frm.elements['ConfigureGoogleApps.choiceCOT'];
        if(choiceCOT.length>0) {
            cotSelect(choiceCOT.options[0]);
        }
    }

    function cotSelect(radio) {
        getIDP(radio.value);
        var idpobj = frm.elements['ConfigureGoogleApps.choiceIDP'];
        if (idpobj.options.length > 1) {
            showIDPObjs();
        }
    }

    function hideRealm() {
        var frm = document.forms['ConfigureGoogleApps'];
        var realmobj = frm.elements['ConfigureGoogleApps.tfRealm'];
        if (realmobj.options.length < 2) {
            hideRealmObjs();
        }
        var cotobj = frm.elements['ConfigureGoogleApps.choiceCOT'];
        if (cotobj.options.length < 2) {
            document.getElementById('cotfld').style.display = 'none';
            document.getElementById('cottxt').innerHTML = cotobj.value;
        }
        var idpobj = frm.elements['ConfigureGoogleApps.choiceIDP'];
        if (idpobj.options.length < 2) {
            document.getElementById('idpfld').style.display = 'none';
            document.getElementById('idptxt').innerHTML = idpobj.value;
        }
    }

    function hideRealmObjs() {
        document.getElementById('realmlbl').style.display = 'none';
        document.getElementById('realmfld').style.display = 'none';
    }

    function showCOTObjs() {
        document.getElementById('cotlbl').style.display = '';
        document.getElementById('cotfld').style.display = '';
    }

    function hideCOTObjs() {
        document.getElementById('cotlbl').style.display = 'none';
        document.getElementById('cotfld').style.display = 'none';
    }

    function showIDPObjs() {
        document.getElementById('idplbl').style.display = '';
        document.getElementById('idpfld').style.display = '';
    }

    function hideIDPObjs() {
        document.getElementById('idplbl').style.display = 'none';
        document.getElementById('idpfld').style.display = 'none';
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
<cc:pagetitle name="pgtitle" bundleID="amConsole" pageTitleText="page.title.configure.google.apps" showPageTitleSeparator="true" viewMenuLabel="" pageTitleHelpMessage="" showPageButtonsTop="true" showPageButtonsBottom="false" />

<table border="0" cellpadding="20" cellspacing="0">
<tr><td>
    <cc:text name="txtDesc" defaultValue="page.desc.configure.google.apps" bundleID="amConsole" />
</td></tr>
</table>


<cc:propertysheet name="propertyAttributes" bundleID="amConsole" showJumpLinks="false"/>
</cc:form>
</cc:header>
</div>
<div id="dlg" class="dvs" style="width:600px; height: 225px; margin-left:-300px"></div>

<script language="javascript">    
    var frm = document.forms['ConfigureGoogleApps'];
    var idpNew = frm.elements['ConfigureGoogleApps.choiceIDP'].value;
    var realmNew = frm.elements['ConfigureGoogleApps.tfRealm'].value;
    var domainIdNew ;
    
    var msgCreating = "<p><img src=\"../console/images/processing.gif\" width=\"66\" height\"66\"/></p><cc:text name="txtConfiguring" defaultValue="configuring.google.apps.waiting" bundleID="amConsole" escape="false" />";
    var msgCreated = "<p>&nbsp;</p><input name=\"btnOK\" type=\"submit\" class=\"Btn1\" value=\"<cc:text name="txtOKBtn" defaultValue="ajax.ok.button" bundleID="amConsole" />\" onClick=\"document.location.replace(\'../task/ConfigureGoogleAppsComplete?idp=\' + idpNew + \'&realm=\' + realmNew + \'&domainId=\' + domainIdNew);return false;\" /></div></p>";
    var ttlCreated = "<h3><cc:text name="txtTtlCreated" defaultValue="google.apps.configured.msg" escape="false" bundleID="amConsole" /></h3>";
    var closeBtn = "<p>&nbsp;</p><p><div class=\"TtlBtnDiv\"><input name=\"btnClose\" type=\"submit\" class=\"Btn1\" value=\"<cc:text name="txtCloseBtn" defaultValue="ajax.close.button" bundleID="amConsole" />\" onClick=\"focusMain();return false;\" /></div></p>";
    
    var btn1 = frm.elements['ConfigureGoogleApps.button1'];
    btn1.onclick = submitPage;
    var btn2 = frm.elements['ConfigureGoogleApps.button2'];
    btn2.onclick = cancelOp;
    var ajaxObj = getXmlHttpRequestObject();
    var selectOptionCache;
    var userLocale = "<%= viewBean.getUserLocale().toString() %>";

    function submitPage() {
        document.getElementById('dlg').style.top = '300px';
        fade();
        document.getElementById('dlg').innerHTML = '<center>' +
            msgCreating + '</center>';
        var url = "../console/ajax/AjaxProxy.jsp";
        var params = 'locale=' + userLocale +
            '&class=com.sun.identity.workflow.ConfigureGoogleApps' + getData();
        frm = document.forms['ConfigureGoogleApps'];
        idpNew = frm.elements['ConfigureGoogleApps.choiceIDP'].value;
        realmNew = frm.elements['ConfigureGoogleApps.tfRealm'].value;
        var size = frm.elements['ConfigureGoogleApps.tfDomainId.listbox'].length ;
        i=0;
        domainIdNew="";
        for (i=0;i<size-1;i++) 
        { 
        domainIdNew += frm.elements['ConfigureGoogleApps.tfDomainId.listbox'].options[i].value + "," ;
        }  
        ajaxPost(ajaxObj, url, params, configured);
        return false;
    }

    function getData() {
        var cot = frm.elements['ConfigureGoogleApps.choiceCOT'].value;
        var idp = frm.elements['ConfigureGoogleApps.choiceIDP'].value;
        var realm = frm.elements['ConfigureGoogleApps.tfRealm'].value;
        var size = frm.elements['ConfigureGoogleApps.tfDomainId.listbox'].length ;
        i=0;
        domainIdNew="";
        for (i=0;i<size-1;i++)
        {
            domainIdNew += frm.elements['ConfigureGoogleApps.tfDomainId.listbox'].options[i].value + "," ;
        }

        return "&realm=" + escapeEx(realm) +
            "&cot=" + escapeEx(cot) +
            "&idp=" + escapeEx(idp) +
            "&domainId=" + escapeEx(domainIdNew);
    }

    function configured() {
        if (ajaxObj.readyState == 4) {
            var result = hexToString(ajaxObj.responseText);
            var status = result.substring(0, result.indexOf('|'));
            var result = result.substring(result.indexOf('|') +1);
            var msg = '<center><p>' + result + '</p></center>';
            if (status == 0) {               
                msg = '<center>' + ttlCreated + msgCreated + '</center>';               
            } else {
                msg = msg + '<center>' +  closeBtn + '</center>';
            }            
            frm = document.forms['ConfigureGoogleApps'];
            idpNew = frm.elements['ConfigureGoogleApps.choiceIDP'].value;
            realmNew = frm.elements['ConfigureGoogleApps.tfRealm'].value;
            var size = frm.elements['ConfigureGoogleApps.tfDomainId.listbox'].length ;
            i=0; domainIdNew="";
            for (i=0;i<size-1;i++)
            {
                domainIdNew += frm.elements['ConfigureGoogleApps.tfDomainId.listbox'].options[i].value + "," ;
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

    function getIDP(cot) {
        clearOptions(frm, 'ConfigureGoogleApps.choiceIDP');
        var url = "../console/ajax/AjaxProxy.jsp";
        var realm = frm.elements['ConfigureGoogleApps.tfRealm'].value;
        var params = 'locale=' + userLocale +
            '&class=com.sun.identity.workflow.GetHostedIDPs' +
            '&realm=' + escapeEx(realm) +
            '&cot=' + escapeEx(cot);
        ajaxPost(ajaxObj, url, params, gotIDPs);
    }

    function circleOfTrust() {
        if (ajaxObj.readyState == 4) {
            var result = hexToString(ajaxObj.responseText);
            var status = result.substring(0, result.indexOf('|'));
            var result = result.substring(result.indexOf('|') +1);
            var msg = '';
            if (status == 0) {
                result = result.replace(/^\s+/, '');
                result = result.replace(/\s+$/, '');
                if (result.length == 0) {
                } else {
                    var cots = result.split('|');
                    var choiceCOT = frm.elements['ConfigureGoogleApps.choiceCOT'];
                    for (var i = choiceCOT.length - 1; i>=0; i--) {
                       choiceCOT.remove(i);
                    }
                    for (var i = 0; i < cots.length; i++) {
                        choiceCOT.options[i] = new Option(cots[i], cots[i]);
                    }
                    showCOTObjs();
                }
                if (presetcot) {
                    selectOption(frm, 'ConfigureGoogleApps.choiceCOT', presetcot);
                    presetcot = null;
                }
            } else {
                msg = '<center><p>' + result + '</p></center>';
                msg = msg + '<center>' +  closeBtn + '</center>';
                document.getElementById('dlg').innerHTML = msg;
                ajaxObj = getXmlHttpRequestObject();
            }
        }
    }

    function gotIDPs() {
        if (ajaxObj.readyState == 4) {
            var result = hexToString(ajaxObj.responseText);
            var status = result.substring(0, result.indexOf('|'));
            var result = result.substring(result.indexOf('|') +1);
            var msg = '';
            if (status == 0) {
                result = result.replace(/^\s+/, '');
                result = result.replace(/\s+$/, '');
                if (result.length == 0) {
                } else {
                    var idps = result.split('|');
                    var choiceIDP = frm.elements['ConfigureGoogleApps.choiceIDP'];
                    for (var i = 0; i < idps.length; i++) {
                        choiceIDP.options[i] = new Option(idps[i], idps[i]);
                    }
                    showIDPObjs();
                }
            } else {
                msg = '<center><p>' + result + '</p></center>';
                msg = msg + '<center>' +  closeBtn + '</center>';
                document.getElementById('dlg').innerHTML = msg;
                ajaxObj = getXmlHttpRequestObject();
            }
        }
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

    var presetcot = null;

<%
    String cot = request.getParameter("cot");
    if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + cot , cot,
        "HTTPParameterValue", 2000, true)) {
            cot = null;
    }
    String idp = request.getParameter("entityId");
    if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + idp, idp ,
        "HTTPParameterValue", 2000, false)) {
        idp = "";
    }
    if ((cot != null) && (cot.trim().length() > 0)) {
        out.println("hideRealmObjs();");
        out.println("hideIDPObjs();");       
        out.println("presetcot = \"" + ESAPI.encoder().encodeForHTML(cot) + "\";");
        out.println("document.getElementById('cotfld').style.display = 'none';");
        out.println("document.getElementById('cottxt').innerHTML = \"" + ESAPI.encoder().encodeForHTML(cot) + "\";");
        out.println("document.getElementById('idpfld').style.display = 'none';");
        out.println("document.getElementById('idplbl').style.display = '';");
        out.println("document.getElementById('idptxt').innerHTML = \"" + ESAPI.encoder().encodeForHTML(idp) + "\";");
    } else {
        out.println("hideRealm();");
    }

%>

    function unescapeQuote(str) {
        str = str.replace(/&quot;/g, '"');
        str = str.replace(/&lt;/g, '<');
        str = str.replace(/&gt;/g, '>');
        return str;
    }

    var infoRealm = unescapeQuote("<cc:text name="txtInfoRealm" defaultValue="configure.google.apps.help.realm" bundleID="amConsole" />");
    var infoEntityId = unescapeQuote("<cc:text name="txtInfoEntityId" defaultValue="configure.google.apps.help.entity.id" bundleID="amConsole" />");

</script>

</jato:useViewBean>
