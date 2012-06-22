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
 
  $Id: attrQuerytest.jsp,v 1.3 2009/02/10 22:10:07 vimal_67 Exp $
 
  Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 
--%>

<%@ page import="com.sun.identity.saml2.assertion.NameID" %>
<%@ page import="com.sun.identity.shared.debug.Debug" %>
<%@ page import="com.sun.identity.saml.common.SAMLUtils" %>
<%@ page import="com.sun.identity.saml2.assertion.AssertionFactory" %>
<%@ page import="com.sun.identity.saml2.assertion.Attribute" %>
<%@ page import="com.sun.identity.saml2.assertion.Assertion" %>
<%@ page import="com.sun.identity.saml2.assertion.AttributeStatement" %>
<%@ page import="com.sun.identity.saml2.assertion.Issuer" %>
<%@ page import="com.sun.identity.saml2.assertion.Subject" %>
<%@ page import="com.sun.identity.saml2.assertion.SubjectConfirmation" %>
<%@ page import="com.sun.identity.saml2.assertion.SubjectConfirmationData" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Constants" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Utils" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Exception" %>
<%@ page import="com.sun.identity.saml2.meta.SAML2MetaUtils" %>
<%@ page import="com.sun.identity.saml2.profile.AttributeQueryUtil" %>
<%@ page import="com.sun.identity.saml2.profile.SPCache" %>
<%@ page import="com.sun.identity.saml2.profile.SPSSOFederate" %>
<%@ page import="com.sun.identity.saml2.protocol.AttributeQuery" %>
<%@ page import="com.sun.identity.saml2.protocol.ProtocolFactory" %>
<%@ page import="com.sun.identity.saml2.protocol.Response" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.StringTokenizer" %>
<%@ page import="com.sun.identity.plugin.session.SessionManager" %>
<%@ page import="com.sun.identity.plugin.session.SessionProvider" %>
<%@ page import="com.sun.identity.plugin.session.SessionException" %>
<%@ page import="com.sun.identity.saml2.common.NameIDInfo" %>
<%@ page import="com.sun.identity.saml2.common.AccountUtils" %>



<head>
<title>SAMLv2 Service Provider SSO</title>
</head>
<body bgcolor="#FFFFFF" text="#000000">
<%
    // Retreive the Request Query Parameters
    // metaAlias and idpEntiyID are the required query parameters
    // metaAlias - Service Provider Entity Id
    // idpEntityID - Identity Provider Identifier
    // Query parameters supported will be documented.
    SessionProvider sessionProvider = SessionManager.getProvider();
    String idpEntityID = null;
    String metaAlias= null;
    String idpSessionID = null;
    ProtocolFactory protocolFactory =  ProtocolFactory.getInstance();
    AssertionFactory assertionFactory =  AssertionFactory.getInstance();
    Subject subject = null;
    Issuer issuer = null;
    SubjectConfirmation sc = null;
    SubjectConfirmationData scd= null;
    Attribute attr = null;
    List content = new ArrayList();
    List attrs = new ArrayList();
    List scs = new ArrayList();
    Response samlResp =  null;
    HashMap paramsMap = new HashMap();
    String attributeProfile=SAML2Constants.BASIC_ATTRIBUTE_PROFILE;
    String userID=null;
    Object famsession = null;
    try {
	metaAlias = request.getParameter("metaAlias");
        if ((metaAlias ==  null) || (metaAlias.length() == 0)) {
            response.sendError(response.SC_BAD_REQUEST,
            SAML2Utils.bundle.getString("nullSPEntityID"));
            return;
        }
        String spEntityID =
            SAML2Utils.getSAML2MetaManager().getEntityByMetaAlias(metaAlias);
        String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);
        idpEntityID = request.getParameter("idpEntityID");

	if ((idpEntityID == null) || (idpEntityID.length() == 0)) {
            response.sendError(response.SC_BAD_REQUEST,
			   SAML2Utils.bundle.getString("nullIDPEntityID"));
	    return;
	}

        paramsMap.put("metaAlias", metaAlias);
        paramsMap.put("spEntityID", spEntityID);
        paramsMap.put("idpEntityID", idpEntityID);
        paramsMap.put(SAML2Constants.ROLE, SAML2Constants.SP_ROLE);

        famsession = SAML2Utils.checkSession(request, response, metaAlias, paramsMap);
	 if (session != null) {
            userID = sessionProvider.getPrincipalName(famsession);
 } else {
 %>
 No user session , please log in
 <%
 return;
}

	String requestType = request.getParameter("requestType");
        String requestAttrName = request.getParameter("attrName");
        String requestAttrValue = request.getParameter("attrValue");
        NameID nameID = AssertionFactory.getInstance().createNameID();
        NameIDInfo info = AccountUtils.getAccountFederation(userID,spEntityID,idpEntityID);
        nameID.setValue(info.getNameIDValue());
        nameID.setFormat(info.getFormat());
        nameID.setNameQualifier(idpEntityID);
        nameID.setSPNameQualifier(spEntityID);
	if (requestType != null && requestType.equalsIgnoreCase("noAttr")) {
            
        AttributeQuery attrQuery = protocolFactory.createAttributeQuery();
        issuer = assertionFactory.createIssuer();
        issuer.setValue(spEntityID);
        attrQuery.setIssuer(issuer);
        attrQuery.setID(SAML2Utils.generateID());
        attrQuery.setVersion(SAML2Constants.VERSION_2_0);
        attrQuery.setIssueInstant(new Date());
        subject = assertionFactory.createSubject();
	subject.setNameID(nameID);
        attrQuery.setSubject(subject);

        String attrQueryProfile = SAML2Constants.DEFAULT_ATTR_QUERY_PROFILE;
%>
Attribute Query =  <%= SAMLUtils.displayXML(attrQuery.toXMLString(true, true)) %>
<br>
<br>

<%

            samlResp = AttributeQueryUtil.sendAttributeQuery(attrQuery,
            idpEntityID, realm, attrQueryProfile, attributeProfile,SAML2Constants.SOAP);
%>
SAML Response = <%= SAMLUtils.displayXML(samlResp.toXMLString(true, true)) %> 

<%

%>

<%

}
       if (requestType != null & requestType.equalsIgnoreCase("attrNamed")) {
        AttributeQuery attrQuery = protocolFactory.createAttributeQuery();
        issuer = assertionFactory.createIssuer();
        issuer.setValue(spEntityID);
        attrQuery.setIssuer(issuer);
        attrQuery.setID(SAML2Utils.generateID());
        attrQuery.setVersion(SAML2Constants.VERSION_2_0);
        attrQuery.setIssueInstant(new Date());

        subject = assertionFactory.createSubject();
	%>
	NameID is <%= nameID %>
       <%
        subject.setNameID(nameID);
        attrQuery.setSubject(subject);
        attrs = new ArrayList();
        attr = assertionFactory.createAttribute();
        attr.setName(requestAttrName);
        attr.setNameFormat(SAML2Constants.BASIC_NAME_FORMAT);
        attrs.add(attr);
        attrQuery.setAttributes(attrs);
%>
<br>
<br>
<br>
<br>
Attribute Query with Attribute names =  <%= SAMLUtils.displayXML
        (attrQuery.toXMLString(true, true)) %>
<br>
<br>
idpEntity id is : <%= idpEntityID %>
attributeProfile id is : <%= attributeProfile %>
<%
            String attrQueryProfile = SAML2Constants.DEFAULT_ATTR_QUERY_PROFILE;
                samlResp = AttributeQueryUtil.sendAttributeQuery(attrQuery,
                        idpEntityID, realm, attrQueryProfile, attributeProfile,
                        SAML2Constants.SOAP);
%>
SAML Response = <%= SAMLUtils.displayXML(samlResp.toXMLString(true, true)) %>
<%
}
       if (requestType != null & requestType.equalsIgnoreCase("attrVal")) {
        AttributeQuery attrQuery = protocolFactory.createAttributeQuery();

        issuer = assertionFactory.createIssuer();
        issuer.setValue(spEntityID);

        attrQuery.setIssuer(issuer);
        attrQuery.setID(SAML2Utils.generateID());
        attrQuery.setVersion(SAML2Constants.VERSION_2_0);
        attrQuery.setIssueInstant(new Date());


        subject = assertionFactory.createSubject();

        subject.setNameID(nameID);
        attrQuery.setSubject(subject);

        attrs = new ArrayList();
        attr = assertionFactory.createAttribute();
        attr.setName(requestAttrName);
        attr.setNameFormat(SAML2Constants.BASIC_NAME_FORMAT);
        List values = new ArrayList();
        StringTokenizer st = new StringTokenizer(requestAttrValue, "|");
        while (st.hasMoreTokens()) {
            String attrValue = st.nextToken();
            values.add(attrValue);
        }        
        attr.setAttributeValueString(values);
        attrs.add(attr);
       

        attrQuery.setAttributes(attrs);
%>
<br>
<br>
<br>
<br>
Attribute Query with Attribute names and values =  <%= SAMLUtils.displayXML(attrQuery.toXMLString(true, true)) %>
<br>
<br>

<%

        String attrQueryProfile = SAML2Constants.DEFAULT_ATTR_QUERY_PROFILE;
         samlResp = AttributeQueryUtil.sendAttributeQuery(attrQuery,
                 idpEntityID, realm, attrQueryProfile, attributeProfile,
                 SAML2Constants.SOAP);
%>
SAML Response = <%= SAMLUtils.displayXML(samlResp.toXMLString(true, true)) %>
<%
}


    } catch (Exception ex) {
	SAML2Utils.debug.error("Error sending AttributeQuery " , ex);
%>
Got exception:  <%= ex %>
<%
    }
%>
</body>
</html>
