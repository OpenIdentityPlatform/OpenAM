<!--
/*
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright 2012-2014 ForgeRock AS.
*
* The contents of this file are subject to the terms
* of the Common Development and Distribution License
* (the License). You may not use this file except in
* compliance with the License.
*
* You can obtain a copy of the License at
* http://forgerock.org/license/CDDLv1.0.html
* See the License for the specific language governing
* permission and limitations under the License.
*
* When distributing Covered Code, include this CDDL
* Header Notice in each file and include the License file
* at http://forgerock.org/license/CDDLv1.0.html
* If applicable, add the following below the CDDL Header,
* with the fields enclosed by brackets [] replaced by
* your own identifying information:
* "Portions Copyrighted [2012] [ForgeRock Inc]"
*/
-->
<%@ page info="ConfigureOAuth2" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<%
    request.setCharacterEncoding("UTF-8");
%>
<jato:useViewBean
        className="com.sun.identity.console.task.ConfigureOAuth2ViewBean"
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
 
   <cc:form name="ConfigureOAuth2" method="post">
       <jato:hidden name="szCache" />
       <script language="javascript">
           function confirmLogout() {
               return confirm("<cc:text name="txtLogout" defaultValue="masthead.logoutMessage" bundleID="amConsole"/>");
           }
 
           function cancelOp() {
               document.location.replace("../task/Home");
               return false;
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
       <cc:pagetitle name="pgtitle" bundleID="amConsole" pageTitleText="page.title.configure.oauth2" showPageTitleSeparator="true" viewMenuLabel="" pageTitleHelpMessage="" showPageButtonsTop="true" showPageButtonsBottom="false" />
 
       <table border="0" cellpadding="20" cellspacing="0">
           <tr><td>
               <cc:text name="txtDesc" defaultValue="page.desc.configure.oauth2" bundleID="amConsole" />
           </td></tr>
       </table>
 
 
       <cc:propertysheet name="propertyAttributes" bundleID="amConsole" showJumpLinks="false"/>
 
   </cc:form>
</cc:header>
</div>
<div id="dlg" class="dvs" style="width:600px; height: 225px; margin-left:-300px"></div>
 
<script language="javascript">
 
var msgCreating = "<p><img src=\"../console/images/processing.gif\" width=\"66\" height\"66\"/></p><cc:text name="txtConfiguring" defaultValue="configuring.oauth2.waiting" bundleID="amConsole" escape="false" />";
var ttlCreated = "<h3><cc:text name="txtTtlCreated" defaultValue="oauth2.configured.title" escape="false" bundleID="amConsole" /></h3>";
var msgCreated = "<p>&nbsp;</p><input name=\"btnOK\" type=\"submit\" class=\"Btn1\" value=\"<cc:text name="txtOKBtn" defaultValue="ajax.ok.button" bundleID="amConsole" />\" onClick=\"document.location.replace(\'../task/Home\');return false;\" /></div></p>";
var closeBtn = "<p>&nbsp;</p><p><div class=\"TtlBtnDiv\"><input name=\"btnClose\" type=\"submit\" class=\"Btn1\" value=\"<cc:text name="txtCloseBtn" defaultValue="ajax.close.button" bundleID="amConsole" />\" onClick=\"focusMain();return false;\" /></div></p>";
 
var frm = document.forms['ConfigureOAuth2'];
var btn1 = frm.elements['ConfigureOAuth2.button1'];
btn1.onclick = submitPage;
var btn2 = frm.elements['ConfigureOAuth2.button2'];
btn2.onclick = cancelOp;
var ajaxObj = getXmlHttpRequestObject();
var userLocale = "<%= viewBean.getUserLocale().toString() %>";
 
function getData(){
   var realm = frm.elements['ConfigureOAuth2.tfRealm'].value;
   var rtl = frm.elements['ConfigureOAuth2.choiceRefreshLifetime'].value;
   var acl = frm.elements['ConfigureOAuth2.choiceCodeLifetime'].value;
   var atl = frm.elements['ConfigureOAuth2.choiceTokenLifetime'].value;
   var irt = (frm.elements['ConfigureOAuth2.choiceRefreshToken'].checked == true) ? "true" : "false";
   var irtr = (frm.elements['ConfigureOAuth2.choiceRefreshTokenOnRefreshing'].checked == true) ? "true" : "false";
   var sic = frm.elements['ConfigureOAuth2.choiceScopeImpl'].value;
   return "&realm=" + escapeEx(realm) +
           "&rtl=" + escapeEx(rtl) +
           "&acl=" + escapeEx(acl) +
           "&atl=" + escapeEx(atl) +
           "&irt=" + escapeEx(irt) +
           "&irtr=" + escapeEx(irtr) +
           "&sic=" + escapeEx(sic);
}
 
function submitPage() {
   document.getElementById('dlg').style.top = '300px';
   fade();
   document.getElementById('dlg').innerHTML = '<center>' +
           msgCreating + '</center>';
   var url = "../console/ajax/AjaxProxy.jsp";
   var params = 'locale=' + userLocale +
           '&class=com.sun.identity.workflow.ConfigureOAuth2' + getData();
   ajaxPost(ajaxObj, url, params, configured);
   return false;
}
 
function configured() {
   if (ajaxObj.readyState == 4) {
       var result = hexToString(ajaxObj.responseText);
       var status = result.substring(0, result.indexOf('|'));
       var result = result.substring(result.indexOf('|') +1);
       var msg = '<center><p>' + result + '</p></center>';
       if (status == 0) {
           msg = '<center>' + ttlCreated + msg + msgCreated + '</center>';
       } else {
           msg = msg + '<center>' +  closeBtn + '</center>';
       }
       document.getElementById('dlg').innerHTML = msg;
   }
}
 
</script>
 
</jato:useViewBean>