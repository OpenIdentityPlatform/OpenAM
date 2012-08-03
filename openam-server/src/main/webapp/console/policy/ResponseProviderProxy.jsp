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

   $Id: ResponseProviderProxy.jsp,v 1.2 2008/06/25 05:44:44 qcheng Exp $

--%>
<%--
   Portions Copyrighted 2012 ForgeRock Inc
   Portions Copyrighted 2012 Open Source Solution Technology Corporation
--%>

<%@ page info="ResponseProviderProxy" language="java" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<jato:useViewBean
    className="com.sun.identity.console.policy.ResponseProviderProxyViewBean"
    fireChildDisplayEvents="true" >

<cc:i18nbundle baseName="amConsole" id="amConsole"
    locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>

<html>
<script language="javascript">
    function forward() {
	var frm = document.forms[0];
	var jatoFrm = document.forms[1];
	frm.elements['providerType'].value =
	    jatoFrm.elements['ResponseProviderProxy.providerType'].value;
	frm.elements['providerName'].value =
	    jatoFrm.elements['ResponseProviderProxy.providerName'].value;
	frm.elements['locDN'].value =
	    jatoFrm.elements['ResponseProviderProxy.locDN'].value;
	frm.elements['cachedID'].value =
	    jatoFrm.elements['ResponseProviderProxy.cachedID'].value;
	frm.elements['tfOp'].value =
	    jatoFrm.elements['ResponseProviderProxy.tfOp'].value;
	frm.elements['jato.pageSession'].value =
	    jatoFrm.elements['jato.pageSession'].value;
	frm.submit();
    }
</script>

<body onload="forward();">

<form action="<jato:text name="tfURL" />" method="post">
<input type="hidden" name="jato.pageSession">
<input type="hidden" name="providerType">
<input type="hidden" name="providerName">
<input type="hidden" name="locDN">
<input type="hidden" name="cachedID">
<input type="hidden" name="tfOp">
</form>

<jato:form name="ResponseProviderProxy" method="post">
<jato:hidden name="providerType" />
<jato:hidden name="providerName" />
<jato:hidden name="locDN" />
<jato:hidden name="cachedID" />
<jato:hidden name="tfOp" />
</jato:form>

</body>
</html>

</jato:useViewBean>
