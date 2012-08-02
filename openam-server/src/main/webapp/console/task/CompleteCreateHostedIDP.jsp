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

   $Id: CompleteCreateHostedIDP.jsp,v 1.6 2009/07/06 21:58:43 babysunil Exp $

--%>

<%@ page info="CompleteCreateHostedIDP" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<%
    request.setCharacterEncoding("UTF-8");
%>
<jato:useViewBean
    className="com.sun.identity.console.task.CompleteCreateHostedIDPViewBean"
    fireChildDisplayEvents="true" >

<cc:i18nbundle baseName="amConsole" id="amConsole"
    locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>

<cc:header name="hdrCommon" pageTitle="webconsole.title" bundleID="amConsole" copyrightYear="2008" fireDisplayEvents="true">

<link rel="stylesheet" type="text/css" href="../console/css/openam.css" />
<script language="javascript" src="../console/js/am.js"></script>

<cc:primarymasthead name="mhCommon" bundleID="amConsole"  logoutOnClick="return confirmLogout();" locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>

<script language="JavaScript">
    function createRemoteSP() {
        var frm = document.forms[0];
        var cot = frm.elements['CompleteCreateHostedIDP.tfcot'].value;
        var realm = frm.elements['CompleteCreateHostedIDP.tfrealm'].value;
        top.location.replace('../task/CreateRemoteSP?cot=' +
            escapeEx(cot) + '&realm=' + escapeEx(realm));
    }
    function createFedlet() {
        var frm = document.forms[0];
        var cot = frm.elements['CompleteCreateHostedIDP.tfcot'].value;
        var realm = frm.elements['CompleteCreateHostedIDP.tfrealm'].value;
        var entityId = frm.elements['CompleteCreateHostedIDP.tfentityId'].value;
        top.location.replace('../task/CreateFedlet?cot=' +
            escapeEx(cot) + '&realm=' + escapeEx(realm) + 
            '&entityId=' + escapeEx(entityId));
    }
    function configureGoogleApps() {
        var frm = document.forms[0];
        var cot = frm.elements['CompleteCreateHostedIDP.tfcot'].value;
        var realm = frm.elements['CompleteCreateHostedIDP.tfrealm'].value;
        var entityId = frm.elements['CompleteCreateHostedIDP.tfentityId'].value;
        top.location.replace('../task/ConfigureGoogleApps?cot=' +
            escapeEx(cot) + '&realm=' + escapeEx(realm) + 
            '&entityId=' + escapeEx(entityId));
    }
    
    function configureSalesForceApps() {
            var frm = document.forms[0];
            var cot = frm.elements['CompleteCreateHostedIDP.tfcot'].value;
            var realm = frm.elements['CompleteCreateHostedIDP.tfrealm'].value;
            var entityId = frm.elements['CompleteCreateHostedIDP.tfentityId'].value;
            top.location.replace('../task/ConfigureSalesForceApps?cot=' +
                escapeEx(cot) + '&realm=' + escapeEx(realm) +
                '&entityId=' + escapeEx(entityId));
    }
    
    function modifyIDP() {
        top.location.replace('../federation/Federation');
    }
</script>
<blockquote>
<blockquote>
<cc:form name="CompleteCreateHostedIDP" method="post">
<div class="PgTxtDiv">
<h1 class="PgTxt"><cc:text name="txtTitle" defaultValue="complete.create.host.idp.title" bundleID="amConsole" /></h1>
</div>
<div><font size="+1"><cc:text name="txtPrompt" defaultValue="complete.create.host.idp.prompt" bundleID="amConsole" /></font></div>
<p>&nbsp;</p>
<table border=0 cellpadding="0" width="100%">
<tr>
<td width="48%" valign="top">
<div class="ConFldSetLgdDiv"><cc:text name="txtRegSPTitle" defaultValue="complete.create.host.idp.reg.remote.sp.title" bundleID="amConsole" /></div>
<cc:text name="txtRegSPText" defaultValue="complete.create.host.idp.reg.remote.sp.text" escape="false" bundleID="amConsole" />
<p>
<div class="ConFldSetLgdDiv"><cc:text name="txtCreateFedletTitle" defaultValue="complete.create.host.idp.create.fedlet.title" bundleID="amConsole" /></div>
<cc:text name="txtCreateFedletText" defaultValue="complete.create.host.idp.create.fedlet.text" escape="false" bundleID="amConsole" />
</td>
<td width="4%" valign="top">
</td>
<td width="48%" valign="top">
<div class="ConFldSetLgdDiv"><cc:text name="txtModifyProfileTitle" defaultValue="complete.create.host.idp.modify.profile.title" bundleID="amConsole" /></div>
<cc:text name="txtModifyProfileText" defaultValue="complete.create.host.idp.modify.profile.text" escape="false" bundleID="amConsole" />
<p>
<div class="ConFldSetLgdDiv"><cc:text name="txtCreateFedletTitle" defaultValue="complete.create.host.idp.create.google.apps.title" bundleID="amConsole" /></div>
<cc:text name="txtCreateGoogleAppsText" defaultValue="complete.create.host.idp.create.google.apps.text" escape="false" bundleID="amConsole" />
<p>
<div class="ConFldSetLgdDiv"><cc:text name="txtCreateSalesForceTitle" defaultValue="complete.create.host.idp.create.salesforce.title" bundleID="amConsole" /></div>
<cc:text name="txtCreateSalesForceText" defaultValue="complete.create.host.idp.create.salesforce.text" escape="false" bundleID="amConsole" />
</td>
</tr>
</table>
<table border=0 cellpadding="0" width="100%">
<tr>
<td align="right">
<input name="btnFinish" type="button" class="Btn1" value="<cc:text name="txtFinishBtn" defaultValue="button.finish" bundleID="amConsole" escape="false" />" onClick="document.location.replace('../task/Home');return false;" />
</td>
</tr>
</table>

<cc:hidden name="tfcot" />
<cc:hidden name="tfrealm" />
<cc:hidden name="tfentityId" />
</cc:form>
</blockquote>
</blockquote>
</cc:header>
</jato:useViewBean>
