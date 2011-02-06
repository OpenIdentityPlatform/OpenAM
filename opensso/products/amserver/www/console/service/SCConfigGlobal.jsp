<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
  
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

   $Id: SCConfigGlobal.jsp,v 1.3 2008/06/25 05:44:53 qcheng Exp $

--%>

<%@ page info="SCConfigGlobal" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<jato:useViewBean
    className="com.sun.identity.console.service.SCConfigGlobalViewBean"
    fireChildDisplayEvents="true" >

<cc:i18nbundle baseName="amConsole" id="amConsole"
    locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>


<cc:header name="hdrCommon" 
    pageTitle="webconsole.title" 
    bundleID="amConsole" 
    copyrightYear="2007" 
    fireDisplayEvents="true">

<cc:form name="SCConfigGlobal" method="post">

<%-- HEADER --------------------------------------------------------------- --%>
<script language="javascript">
    function confirmLogout() {
        return confirm("<cc:text name="txtLogout" defaultValue="masthead.logoutMessage" bundleID="amConsole"/>");
    }
</script>
<cc:primarymasthead name="mhCommon" bundleID="amConsole"  logoutOnClick="return confirmLogout();" locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>
<cc:tabs name="tabCommon" bundleID="amConsole" />
<cc:breadcrumbs name="breadCrumb" bundleID="amConsole" />

<%-- PAGE CONTENT --------------------------------------------------------- --%>
<cc:pagetitle name="pgtitle" 
    bundleID="amConsole" 
    pageTitleText="page.title.config" 
    showPageTitleSeparator="true" 
    viewMenuLabel="" 
    pageTitleHelpMessage="" 
    showPageButtonsTop="false" 
    showPageButtonsBottom="false">

    <cc:propertysheet
        name="psSections"
        bundleID="amConsole"
        showJumpLinks="true" />

</cc:pagetitle>

</cc:form>

</cc:header>
</jato:useViewBean>
