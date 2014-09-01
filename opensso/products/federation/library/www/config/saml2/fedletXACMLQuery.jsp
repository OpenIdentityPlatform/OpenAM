<%--
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

   Copyright 2009 Sun Microsystems Inc. All Rights Reserved

--%>

<%--
  fedletXACMLQuery.jsp
  This JSP used by the Fedlet to get the Resource URL. Fedlet uses XACML
  to determine whether right policy has been defined for the Resource URL
--%>

<%@ page import="com.sun.identity.saml2.common.SAML2Exception" %>
<%@ page import="com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorElement" %>
<%@ page import="com.sun.identity.saml2.jaxb.metadata.SingleSignOnServiceElement" %>
<%@ page import="com.sun.identity.saml2.meta.SAML2MetaException" %>
<%@ page import="com.sun.identity.saml2.meta.SAML2MetaManager" %>
<%@ page import="com.sun.identity.shared.encode.URLEncDec" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.io.File" %>
<%@ page import="java.io.InputStream" %>
<%@ page import="java.io.FileOutputStream" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>

<%@ page import="com.sun.identity.cot.CircleOfTrustManager" %>
<%@ page import="com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement" %>

<script>
function checkEmptyResource() {
    if (document.fedletXACMLQuery.resource.value == "")  {
        alert("Resource URL cannot be empty");
        return false;
    }        
    return true;
}
</script>
<%
    String deployuri = request.getRequestURI();
    int slashLoc = deployuri.indexOf("/", 1);
    if (slashLoc != -1) {
        deployuri = deployuri.substring(0, slashLoc);
    }
    String fedletHomeDir = System.getProperty("com.sun.identity.fedlet.home");
    if ((fedletHomeDir == null) || (fedletHomeDir.trim().length() == 0)) {
        if (System.getProperty("user.home").equals(File.separator)) {
            fedletHomeDir = File.separator + "fedlet";
        } else {
            fedletHomeDir = System.getProperty("user.home") +
                File.separator + "fedlet";
        }
    }

%>
<html>
<head>
    <title>XACML Query</title>
    <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
    <link rel="stylesheet" type="text/css" href="<%= deployuri %>/com_sun_web_ui/css/css_ns6up.css" />
</head>

<body>
<div class="MstDiv"><table width="100%" border="0" cellpadding="0" cellspacing="0" class="MstTblTop" title="">
<tbody><tr>
<td nowrap="nowrap">&nbsp;</td>
<td nowrap="nowrap">&nbsp;</td>
</tr></tbody></table>


<%
    try {		
        CircleOfTrustManager cotManager = new CircleOfTrustManager();
        Set members = cotManager.getAllCirclesOfTrust("/");
            
        if ((members == null) || members.isEmpty()) {
            out.print("Misconfiguration - No circle of trust for root realm.");
        } else {
            out.print(members.toArray()[0]);
        }
    
    } catch (Exception e) {
        out.print(e.toString()); 
    }
%>		

<table width="100%" border="0" cellpadding="0" cellspacing="0" class="MstTblBot" title="">
<tbody><tr>
<td class="MstTdTtl" width="99%">
<div class="MstDivTtl"><img name="ProdName" src="<%= deployuri %>/console/images/PrimaryProductName.png" alt="" /></div></td><td class="MstTdLogo" width="1%"><img name="RMRealm.mhCommon.BrandLogo" src="<%= deployuri %>/com_sun_web_ui/images/other/javalogo.gif" alt="Java(TM) Logo" border="0" height="55" width="31" /></td></tr></tbody></table>
<table class="MstTblEnd" border="0" cellpadding="0" cellspacing="0" width="100%"><tbody><tr><td><img name="RMRealm.mhCommon.EndorserLogo" src="<%= deployuri %>/com_sun_web_ui/images/masthead/masthead-sunname.gif" alt="Sun(TM) Microsystems, Inc." align="right" border="0" height="10" width="108" /></td></tr></tbody></table></div><div class="SkpMedGry1"><a name="SkipAnchor2089" id="SkipAnchor2089"></a></div>
<div class="SkpMedGry1"><a href="#SkipAnchor4928"><img src="<%= deployuri %>/com_sun_web_ui/images/other/dot.gif" alt="Jump Over Tab Navigation Area. Current Selection is: Access Control" border="0" height="1" width="1" /></a></div>

    <p><br>
    <table border="0" width="700">
	
    <tr>
      <td colspan="2"> </td>
    </tr>
    <tr>
      <td colspan="2"> </td>
    </tr>
	
    <tr>
        <td colspan="2">
        <hr>
        <form method=get name="fedletXACMLQuery" action=fedletXACMLResp.jsp onsubmit="return checkEmptyResource();">
	<h1> XACML Query </h1>
	<%
            String idpEntityID = request.getParameter("idpEntityID");
            String spEntityID = request.getParameter("spEntityID");
            String nameIDValue = request.getParameter("nameIDValue");
            String newNameIDValue = URLEncDec.encode(nameIDValue);
    	%>
	<p>
            <input type=hidden name=idpEntityID value="<%=idpEntityID%>">
            <input type=hidden name=spEntityID value="<%=spEntityID%>">
            <input type=hidden name=nameIDValue value="<%=newNameIDValue%>">
             <h3>Resource URL</h3>
	     <input type=text name=resource value=<%=request.getRequestURL()%> size=120> <br>
	     <p> <p>
	    <h3>Action</h3>
            <input type="radio" name="action" value="GET" checked/> GET <br> 
	    <input type="radio" name="action" value="POST"/> POST <br>
            <input type=submit>
	</form>
	<hr>
        </td>
	</tr>	

        <tr>
          <td colspan="2"> </td>
        </tr>
    </table>
</body>
</html>
