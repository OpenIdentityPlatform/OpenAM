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

  $Id: fedletAttrResp.jsp,v 1.2 2009/07/24 22:53:20 madan_ranganath Exp $

  Copyright 2009 Sun Microsystems Inc. All Rights Reserved
--%>


<%--
  fedletAttrResp.jsp
  This JSP used by the Fedlet performs the following:
   1) Retrieves the list of attributes from fedletAttrQuery.jsp
   2) Invokes the method to retrieve the attribute values 
   3) Displays the attribute value pairs.
--%>

<%@ page import="com.sun.identity.shared.debug.Debug" %>
<%@ page import="com.sun.identity.saml.common.SAMLUtils" %>
<%@ page import="com.sun.identity.saml2.assertion.Assertion" %>
<%@ page import="com.sun.identity.saml2.assertion.AssertionFactory" %>
<%@ page import="com.sun.identity.saml2.assertion.Attribute" %>
<%@ page import="com.sun.identity.saml2.assertion.NameID" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Constants" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Utils" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Exception" %>
<%@ page import="com.sun.identity.saml2.profile.AttributeQueryUtil" %>
<%@ page import="com.sun.identity.saml2.protocol.Response" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>

<%
    String deployuri = request.getRequestURI();
    int slashLoc = deployuri.indexOf("/", 1);
    if (slashLoc != -1) {
        deployuri = deployuri.substring(0, slashLoc);
    }
%>

<html>
<head>
    <title>Sample Fedlet Attribute Query Application</title>
    <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
    <link rel="stylesheet" type="text/css" href="<%= deployuri %>/com_sun_web_ui/css/css_ns6up.css" />
</head>

<body>
<div class="MstDiv"><table width="100%" border="0" cellpadding="0" cellspacing="0" class="MstTblTop" title="">
<tbody><tr>
<td nowrap="nowrap">&nbsp;</td>
<td nowrap="nowrap">&nbsp;</td>
</tr></tbody></table>

<table width="100%" border="0" cellpadding="0" cellspacing="0" class="MstTblBot" title="">
<tbody><tr>
<td class="MstTdTtl" width="99%">
<div class="MstDivTtl"><img name="ProdName" src="<%= deployuri %>/console/images/PrimaryProductName.png" alt="" /></div></td><td class="MstTdLogo" width="1%"><img name="RMRealm.mhCommon.BrandLogo" src="<%= deployuri %>/com_sun_web_ui/images/other/javalogo.gif" alt="Java(TM) Logo" border="0" height="55" width="31" /></td></tr></tbody></table>
<table class="MstTblEnd" border="0" cellpadding="0" cellspacing="0" width="100%"><tbody><tr><td><img name="RMRealm.mhCommon.EndorserLogo" src="<%= deployuri %>/com_sun_web_ui/images/masthead/masthead-sunname.gif" alt="Sun(TM) Microsystems,
Inc." align="right" border="0" height="10" width="108" /></td></tr></tbody></table></div><div class="SkpMedGry1"><a name="SkipAnchor2089" id="SkipAnchor2089"></a></div>
<div class="SkpMedGry1"><a href="#SkipAnchor4928"><img src="<%= deployuri %>/com_sun_web_ui/images/other/dot.gif" alt="Jump Over Tab Navigation Area. Current Selection is: Access Control" border="0" height="1" width="1" /></a></div>


<%
    try {
        String idpEntityID = request.getParameter("idpEntityID");
	if ((idpEntityID == null) || 
            (idpEntityID.length() == 0)) {
           response.sendError(response.SC_BAD_REQUEST,
			   SAML2Utils.bundle.getString("nullIDPEntityID"));
	    return;
	}

        String spEntityID = request.getParameter("spEntityID");
	if ((spEntityID == null) || 
            (spEntityID.length() == 0)) {
           response.sendError(response.SC_BAD_REQUEST,
			   SAML2Utils.bundle.getString("nullSPEntityID"));
	    return;
	}

        String nameIDValue = request.getParameter("nameidvalue");
        String newNameIDValue = nameIDValue.replace("%2F","/");
        AssertionFactory assertionFactory = AssertionFactory.getInstance();
        List attrsList = new ArrayList();

        try {
            if (request.getParameter("attr1").length() > 1 | 
                request.getParameter("attr2").length() > 1 |
                request.getParameter("attr3").length() > 1) {
                if (request.getParameter("attr1").length() > 1) {
                    attrsList.add(request.getParameter("attr1"));
                }
                if (request.getParameter("attr2").length() > 1) {
                    attrsList.add(request.getParameter("attr2"));
                }
                if (request.getParameter("attr3").length()>1) {
                    attrsList.add(request.getParameter("attr3"));
                }
            } 
        } catch (Exception e){
            out.print("Requesting all available IDP attributes");	
	}

        String attrQueryProfile = request.getParameter("attrQueryProfile");

        String subjectDN = request.getParameter("subjectDN");

        Map attrMap = AttributeQueryUtil.getAttributeMapForFedlet(
                                        spEntityID,
                                        idpEntityID,
                                        newNameIDValue,
                                        attrsList,
                                        attrQueryProfile,
                                        subjectDN);
                                         
%>
<h2> Fedlet Attribute Query Response </h2>
<table border="2" cellspacing="0" cellpadding="7">
<tr>
<th>Attribute Query</th>
<th>Attribute Response</th>
</tr>
<% 
   Iterator itr = attrMap.entrySet().iterator();
   while (itr.hasNext()) {
       Map.Entry pairs = (Map.Entry)itr.next(); 
       out.println("<tr>");
       out.println("<td>"); 
       String attrKey = (String)pairs.getKey();
       out.println(attrKey + "<br>");
       out.println("</td>"); 
       out.println("<td>"); 
       String attrValue = (String)pairs.getValue();
       out.println(attrValue + "<br>");
       out.println("</td>"); 
       out.println("</tr>"); 
   } 
%>
</table>
<%
   } catch (Exception ex) {
   SAML2Utils.debug.error("Error sending AttributeQuery " , ex);
   }
%>
</body>
</html>
