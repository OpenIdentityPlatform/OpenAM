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
 
 $Id: aIDReqTest.jsp,v 1.1 2008/08/26 22:33:23 sridharev Exp $
 
 Copyright 2007 Sun Microsystems Inc. All Rights Reserved
--%>




<%@ page import="com.sun.identity.shared.debug.Debug" %>
<%@ page import="com.sun.identity.saml.common.SAMLUtils" %>
<%@ page import="com.sun.identity.saml2.assertion.Assertion" %>
<%@ page import="com.sun.identity.saml2.assertion.AssertionFactory" %>
<%@ page import="com.sun.identity.saml2.assertion.AssertionIDRef" %>
<%@ page import="com.sun.identity.saml2.assertion.Issuer" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Constants" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Utils" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Exception" %>
<%@ page import="com.sun.identity.saml2.meta.SAML2MetaUtils" %>
<%@ page import="com.sun.identity.saml2.profile.AssertionIDRequestUtil" %>
<%@ page import="com.sun.identity.saml2.protocol.AssertionIDRequest" %>
<%@ page import="com.sun.identity.saml2.protocol.ProtocolFactory" %>
<%@ page import="com.sun.identity.saml2.protocol.Response" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>

<head>
<title>SAMLv2 Service Provider SSO</title>
</head>
<body bgcolor="#FFFFFF" text="#000000">
<%
    String idpEntityID = null;
    String spMetaAlias = null;
    String tassertionID = null;

    try {
	spMetaAlias = request.getParameter("spMetaAlias");
        tassertionID = request.getParameter("aID");
        if ((spMetaAlias ==  null) || (spMetaAlias.length() == 0)) {
            response.sendError(response.SC_BAD_REQUEST,
            SAML2Utils.bundle.getString("nullSPEntityID"));
            return;
        }
        String spEntityID =
            SAML2Utils.getSAML2MetaManager().getEntityByMetaAlias(spMetaAlias);
        String realm = SAML2MetaUtils.getRealmByMetaAlias(spMetaAlias);
        idpEntityID = request.getParameter("idpEntityID");

	if ((idpEntityID == null) ||
            (idpEntityID.length() == 0)) {
            response.sendError(response.SC_BAD_REQUEST,
		SAML2Utils.bundle.getString("nullIDPEntityID"));
	    return;
	}


%>
spEntityID = <%= spEntityID %>
<br>
idpEntityID = <%= idpEntityID %>
<br>

<%

        ProtocolFactory protocolFactory = ProtocolFactory.getInstance();
        AssertionFactory assertionFactory = AssertionFactory.getInstance();

        List aIDRefs = new ArrayList();
        AssertionIDRef aIDRef = assertionFactory.createAssertionIDRef();
        aIDRef.setValue(tassertionID);
        aIDRefs.add(aIDRef);

        AssertionIDRequest aIDReq = protocolFactory.createAssertionIDRequest();
        aIDReq.setAssertionIDRefs(aIDRefs);

        Issuer issuer = assertionFactory.createIssuer();
        issuer.setValue(spEntityID);

        aIDReq.setIssuer(issuer);
        aIDReq.setID(SAML2Utils.generateID());
        aIDReq.setVersion(SAML2Constants.VERSION_2_0);
        aIDReq.setIssueInstant(new Date());


%>
AssertionIDRequest =  <%= SAMLUtils.displayXML(aIDReq.toXMLString(true, true)) %>
<br>
<br>

<%

        Response samlResp = AssertionIDRequestUtil.sendAssertionIDRequest(
            aIDReq, idpEntityID, SAML2Constants.IDP_ROLE, realm,
            SAML2Constants.SOAP);

        List assertions = samlResp.getAssertion();
        if ((assertions == null) || (assertions.isEmpty())) {
%>
No assertion found
<%
        } else {
            for(Iterator iter = assertions.iterator(); iter.hasNext(); ) {
                Assertion assertion = (Assertion)iter.next();
 
%>
Assertion = <%= SAMLUtils.displayXML(assertion.toXMLString(true, true)) %>
<br>
<br>
<br>

<%
            }
        }
%>

<%
    } catch (Exception ex) {
	SAML2Utils.debug.error("Error sending AssertionIDRequest " , ex);
%>
Got exception: <%= ex.getMessage() %>
<%
    }
%>
</body>
</html>
