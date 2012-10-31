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

   $Id: fedletSampleApp.jsp,v 1.15 2010/01/08 21:56:58 vimal_67 Exp $

--%>


<%@page
import="com.sun.identity.saml2.common.SAML2Exception,
com.sun.identity.saml2.common.SAML2Constants,
com.sun.identity.saml2.assertion.Assertion,
com.sun.identity.saml2.assertion.Subject,
com.sun.identity.saml2.profile.SPACSUtils,
com.sun.identity.saml2.protocol.Response,
com.sun.identity.saml2.assertion.NameID,
com.sun.identity.saml.common.SAMLUtils,
com.sun.identity.shared.encode.URLEncDec,
com.sun.identity.plugin.session.SessionException,
java.io.IOException,
java.util.Iterator,
java.util.List,
java.util.Map,
java.util.HashMap,
java.util.HashSet,
java.util.Set"
%>
<%@ include file="header.jspf" %>
<%
    String deployuri = request.getRequestURI();
    int slashLoc = deployuri.indexOf("/", 1);
    if (slashLoc != -1) {
        deployuri = deployuri.substring(0, slashLoc);
    }
%>
<html>
<head>
    <title>Fedlet Sample Application</title>
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
    // BEGIN : following code is a must for Fedlet (SP) side application
    Map map;
    try {
        // invoke the Fedlet processing logic. this will do all the
        // necessary processing conforming to SAMLv2 specifications,
        // such as XML signature validation, Audience and Recipient
        // validation etc.  
        map = SPACSUtils.processResponseForFedlet(request, response);
    } catch (SAML2Exception sme) {
        SAMLUtils.sendError(request, response,
            response.SC_INTERNAL_SERVER_ERROR, "failedToProcessSSOResponse",
            sme.getMessage());
        return;
    } catch (IOException ioe) {
        SAMLUtils.sendError(request, response,
            response.SC_INTERNAL_SERVER_ERROR, "failedToProcessSSOResponse",
            ioe.getMessage());
        return;
    } catch (SessionException se) {
        SAMLUtils.sendError(request, response, 
            response.SC_INTERNAL_SERVER_ERROR, "failedToProcessSSOResponse",
            se.getMessage());
        return;
    } catch (ServletException se) {
        SAMLUtils.sendError(request, response,
            response.SC_BAD_REQUEST, "failedToProcessSSOResponse",
            se.getMessage());
        return;
    }
    // END : code is a must for Fedlet (SP) side application
    
    String relayUrl = (String) map.get(SAML2Constants.RELAY_STATE);
    if ((relayUrl != null) && (relayUrl.length() != 0)) {
        // something special for validation to send redirect
        int stringPos  = relayUrl.indexOf("sendRedirectForValidationNow=true");
        if (stringPos != -1) {
            response.sendRedirect(relayUrl);
        }
    } 

    // Following are sample code to show how to retrieve information,
    // such as Reponse/Assertion/Attributes, from the returned map. 
    // You might not need them in your real application code. 
    Response samlResp = (Response) map.get(SAML2Constants.RESPONSE); 
    Assertion assertion = (Assertion) map.get(SAML2Constants.ASSERTION);
    Subject subject = (Subject) map.get(SAML2Constants.SUBJECT);
    String entityID = (String) map.get(SAML2Constants.IDPENTITYID);
    String spEntityID = (String) map.get(SAML2Constants.SPENTITYID);
    NameID nameId = (NameID) map.get(SAML2Constants.NAMEID);
    String value = nameId.getValue();
    String format = nameId.getFormat();
    out.println("<br><br><b>Single Sign-On successful with IDP " 
        + entityID + ".</b>");
    out.println("<br><br>");
    out.println("<table border=0>");
    if (format != null) {
        out.println("<tr>");
        out.println("<td valign=top><b>Name ID format: </b></td>");
        out.println("<td>" + format + "</td>");
        out.println("</tr>");
    }
    if (value != null) {
        out.println("<tr>");
        out.println("<td valign=top><b>Name ID value: </b></td>");
        out.println("<td>" + value + "</td>");
        out.println("</tr>");
    }    
    String sessionIndex = (String) map.get(SAML2Constants.SESSION_INDEX);
    if (sessionIndex != null) {
        out.println("<tr>");
        out.println("<td valign=top><b>SessionIndex: </b></td>");
        out.println("<td>" + sessionIndex + "</td>");
        out.println("</tr>");
    }    
    
    Map attrs = (Map) map.get(SAML2Constants.ATTRIBUTE_MAP);
    if (attrs != null) {
        out.println("<tr>");
        out.println("<td valign=top><b>Attributes: </b></td>");
        Iterator iter = attrs.keySet().iterator();
        out.println("<td>");
        while (iter.hasNext()) {
            String attrName = (String) iter.next();
            Set attrVals = (HashSet) attrs.get(attrName);
            if ((attrVals != null) && !attrVals.isEmpty()) {
                Iterator it = attrVals.iterator();
                while (it.hasNext()) {
                    out.println(attrName + "=" + it.next() + "<br>");
                }
            }
        }
        out.println("</td>");
        out.println("</tr>");
    }
    out.println("</table>");
    out.println("<br><br><b><a href=# onclick=toggleDisp('resinfo')>Click to view SAML2 Response XML</a></b><br>");
    out.println("<span style='display:none;' id=resinfo><textarea rows=40 cols=100>" + samlResp.toXMLString(true, true) + "</textarea></span>");

    out.println("<br><b><a href=# onclick=toggleDisp('assr')>Click to view Assertion XML</a></b><br>");
    out.println("<span style='display:none;' id=assr><br><textarea rows=40 cols=100>" + assertion.toXMLString(true, true) + "</textarea></span>");

    out.println("<br><b><a href=# onclick=toggleDisp('subj')>Click to view Subject XML</a></b><br>");
    out.println("<span style='display:none;' id=subj><br><textarea rows=10 cols=100>" + subject.toXMLString(true, true) + "</textarea></span>");

    if ((relayUrl != null) && (relayUrl.length() != 0)) {
        out.println("<br><br>Click <a href=\"" + relayUrl 
            + "\">here</a> to redirect to final destination.");
    }

    out.print("<p><p>");
    out.println("<br><b>Test Attribute Query:</b></br>");
    out.print("<p><p>");
    out.print("<b><a href="+deployuri+"/fedletAttrQuery.jsp?nameIDValue="+value+"&idpEntityID="+entityID+"&spEntityID="+spEntityID+">Fedlet Attribute Query </a></b>");
    out.print("<p><p>");

    out.println("<br><b>Test XACML Policy Decision Query:</b></br>");
    out.print("<p><p>");
    out.print("<b><a href="+deployuri+"/fedletXACMLQuery.jsp?nameIDValue="+value+"&idpEntityID="+entityID+"&spEntityID="+spEntityID+">Fedlet XACML Query </a></b>");
    out.print("<p><p>");

    Map idpMap = getIDPBaseUrlAndMetaAlias(entityID, deployuri);
    String idpBaseUrl = (String) idpMap.get("idpBaseUrl");
    String idpMetaAlias = (String) idpMap.get("idpMetaAlias");
    String fedletBaseUrl = getFedletBaseUrl(spEntityID, deployuri);
    out.println("<br><b>Test Single Logout:</b></br>");
    if (idpMetaAlias != null) {
        out.println("<br><b><a href=\"" + idpBaseUrl + "/IDPSloInit?metaAlias=" + idpMetaAlias + "&binding=urn:oasis:names:tc:SAML:2.0:bindings:SOAP&RelayState=" + fedletBaseUrl + "/index.jsp\">Run Identity Provider initiated Single Logout using SOAP binding</a></b></br>");
        out.println("<br><b><a href=\"" + idpBaseUrl + "/IDPSloInit?metaAlias=" + idpMetaAlias + "&binding=urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect&RelayState=" + fedletBaseUrl + "/index.jsp\">Run Identity Provider initiated Single Logout using HTTP Redirect binding</a></b></br>");
        out.println("<br><b><a href=\"" + idpBaseUrl + "/IDPSloInit?metaAlias=" + idpMetaAlias + "&binding=urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST&RelayState=" + fedletBaseUrl + "/index.jsp\">Run Identity Provider initiated Single Logout using HTTP POST binding</a></b></br>");
    }
    out.println("<br><b><a href=\"" + fedletBaseUrl + "/fedletSloInit?spEntityID=" + URLEncDec.encode(spEntityID) + "&idpEntityID=" + URLEncDec.encode(entityID) + "&NameIDValue=" + URLEncDec.encode(value) + "&SessionIndex=" + URLEncDec.encode(sessionIndex) + "&binding=urn:oasis:names:tc:SAML:2.0:bindings:SOAP&RelayState=" + URLEncDec.encode(fedletBaseUrl + "/index.jsp") + "\">Run Fedlet initiated Single Logout using SOAP binding</a></b></br>");
    out.println("<br><b><a href=\"" + fedletBaseUrl + "/fedletSloInit?spEntityID=" + URLEncDec.encode(spEntityID) + "&idpEntityID=" + URLEncDec.encode(entityID) + "&NameIDValue=" + URLEncDec.encode(value) + "&SessionIndex=" + URLEncDec.encode(sessionIndex) + "&binding=urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect&RelayState=" + URLEncDec.encode(fedletBaseUrl + "/index.jsp") + "\">Run Fedlet initiated Single Logout using HTTP Redirect binding</a></b></br>");
    out.println("<br><b><a href=\"" + fedletBaseUrl + "/fedletSloInit?spEntityID=" + URLEncDec.encode(spEntityID) + "&idpEntityID=" + URLEncDec.encode(entityID) + "&NameIDValue=" + URLEncDec.encode(value) + "&SessionIndex=" + URLEncDec.encode(sessionIndex) + "&binding=urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST&RelayState=" + URLEncDec.encode(fedletBaseUrl + "/index.jsp") + "\">Run Fedlet initiated Single Logout using HTTP POST binding</a></b></br>");
%>
<script>
function toggleDisp(id)
{
    var elem = document.getElementById(id);
    if (elem.style.display == 'none')
        elem.style.display = '';
    else
        elem.style.display = 'none';
}
</script>
</body>
</html>
