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

   $Id: authnQuery.jsp,v 1.1 2008/06/23 13:52:22 docteger Exp $

   Copyright 2006 Sun Microsystems Inc. All Rights Reserved
--%>




<%@ page import="com.sun.identity.shared.debug.Debug" %>
<%@ page import="com.sun.identity.plugin.session.SessionManager" %>
<%@ page import="com.sun.identity.plugin.session.SessionProvider" %>
<%@ page import="com.sun.identity.saml.common.SAMLUtils" %>
<%@ page import="com.sun.identity.saml.xmlsig.KeyProvider" %>
<%@ page import="com.sun.identity.saml2.assertion.Assertion" %>
<%@ page import="com.sun.identity.saml2.assertion.AssertionFactory" %>
<%@ page import="com.sun.identity.saml2.assertion.Attribute" %>
<%@ page import="com.sun.identity.saml2.assertion.AuthnStatement" %>
<%@ page import="com.sun.identity.saml2.assertion.EncryptedAssertion" %>
<%@ page import="com.sun.identity.saml2.assertion.EncryptedID" %>
<%@ page import="com.sun.identity.saml2.assertion.Issuer" %>
<%@ page import="com.sun.identity.saml2.assertion.NameID" %>
<%@ page import="com.sun.identity.saml2.assertion.Subject" %>
<%@ page import="com.sun.identity.saml2.assertion.SubjectConfirmation" %>
<%@ page import="com.sun.identity.saml2.assertion.SubjectConfirmationData" %>
<%@ page import="com.sun.identity.saml2.common.AccountUtils" %>
<%@ page import="com.sun.identity.saml2.common.NameIDInfo" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Constants" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Utils" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Exception" %>
<%@ page import="com.sun.identity.saml2.jaxb.metadata.AttributeAuthorityDescriptorElement" %>
<%@ page import="com.sun.identity.saml2.key.EncInfo" %>
<%@ page import="com.sun.identity.saml2.key.KeyUtil" %>
<%@ page import="com.sun.identity.saml2.meta.SAML2MetaUtils" %>
<%@ page import="com.sun.identity.saml2.profile.AuthnQueryUtil" %>
<%@ page import="com.sun.identity.saml2.profile.SPCache" %>
<%@ page import="com.sun.identity.saml2.profile.SPSSOFederate" %>
<%@ page import="com.sun.identity.saml2.protocol.AuthnQuery" %>
<%@ page import="com.sun.identity.saml2.protocol.ProtocolFactory" %>
<%@ page import="com.sun.identity.saml2.protocol.RequestedAuthnContext" %>
<%@ page import="com.sun.identity.saml2.protocol.Response" %>
<%@ page import="java.security.PrivateKey" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.StringTokenizer" %>

<head>
<title>SAMLv2 Service Provider SSO</title>
</head>
<body bgcolor="#FFFFFF" text="#000000">
<%
    String authnAuthorityEntityID = null;
    String spMetaAlias= null;

    try {
	spMetaAlias = request.getParameter("spMetaAlias");
        if ((spMetaAlias ==  null) || (spMetaAlias.length() == 0)) {
            response.sendError(response.SC_BAD_REQUEST,
            SAML2Utils.bundle.getString("nullSPEntityID"));
            return;
        }
        String spEntityID =
            SAML2Utils.getSAML2MetaManager().getEntityByMetaAlias(spMetaAlias);
        String realm = SAML2MetaUtils.getRealmByMetaAlias(spMetaAlias);
        authnAuthorityEntityID = request.getParameter("authnAuthorityEntityID");

	if ((authnAuthorityEntityID == null) ||
            (authnAuthorityEntityID.length() == 0)) {
            response.sendError(response.SC_BAD_REQUEST,
		SAML2Utils.bundle.getString("nullIDPEntityID"));
	    return;
	}

        SessionProvider sessionProvider = SessionManager.getProvider();
        Object sessionObj = sessionProvider.getSession(request);
        String userID = sessionProvider.getPrincipalName(sessionObj);

%>
spEntityID = <%= spEntityID %>
<br>
authnAuthorityEntityID = <%= authnAuthorityEntityID %>
<br>
userID = <%= userID %>
<br>

<%

        NameIDInfo nameIDInfo = AccountUtils.getAccountFederation(userID,
            spEntityID, authnAuthorityEntityID);

        if (nameIDInfo == null) {
            response.sendError(response.SC_BAD_REQUEST, "NameIDInfo is null");
            return;
        }

        NameID nameID = nameIDInfo.getNameID();
%>
NameID = <%= SAMLUtils.displayXML(nameID.toXMLString(true, true)) %>
<br>
<br>

<%

        ProtocolFactory protocolFactory = ProtocolFactory.getInstance();
        AssertionFactory assertionFactory = AssertionFactory.getInstance();
        AuthnQuery authnQuery = protocolFactory.createAuthnQuery();

        Issuer issuer = assertionFactory.createIssuer();
        issuer.setValue(spEntityID);

        authnQuery.setIssuer(issuer);
        authnQuery.setID(SAML2Utils.generateID());
        authnQuery.setVersion(SAML2Constants.VERSION_2_0);
        authnQuery.setIssueInstant(new Date());

        Subject subject = assertionFactory.createSubject();
        subject.setNameID(nameID);
        authnQuery.setSubject(subject);

// customization starts here
//        authnQuery.setSessionIndex("s26621c635d0d395dcbec44a6eeb69ff5164244a01");
        RequestedAuthnContext reqAC =
            protocolFactory.createRequestedAuthnContext();
        List acClassRefs = new ArrayList();
/*
        acClassRefs.add("urn:oasis:names:tc:SAML:2.0:ac:classes:Level1");
        acClassRefs.add("urn:oasis:names:tc:SAML:2.0:ac:classes:Level3");
*/

        acClassRefs.add("urn:oasis:names:tc:SAML:2.0:ac:classes:Level5");
/*
        acClassRefs.add("urn:oasis:names:tc:SAML:2.0:ac:classes:Level7");
        acClassRefs.add("urn:oasis:names:tc:SAML:2.0:ac:classes:Level9");
*/
        reqAC.setAuthnContextClassRef(acClassRefs);
/*
        reqAC.setComparison("exact");
        reqAC.setComparison("better");
        reqAC.setComparison("minimum");
*/
        reqAC.setComparison("maximum");
// customization ends here


        authnQuery.setRequestedAuthnContext(reqAC);

%>
AuthnQuery =  <%= SAMLUtils.displayXML(authnQuery.toXMLString(true, true)) %>
<br>
<br>

<%

        Response samlResp = AuthnQueryUtil.sendAuthnQuery(authnQuery,
            authnAuthorityEntityID, realm, SAML2Constants.SOAP);

        List assertions = samlResp.getAssertion();
        if ((assertions == null) || (assertions.isEmpty())) {
%>
No assertion found
<%
        } else {
            for(Iterator iter = assertions.iterator(); iter.hasNext(); ) {
                Assertion assertion = (Assertion)iter.next();
                List authnStmts = assertion.getAuthnStatements();
 
%>
Assertion = <%= SAMLUtils.displayXML(((AuthnStatement)authnStmts.get(0)).toXMLString(true, true)) %>
<br>
<br>
<%
            }
        }
    } catch (Exception ex) {
	SAML2Utils.debug.error("Error sending AttributeQuery " , ex);
%>
Got exception: <%= ex.getMessage() %>
<%
    }
%>
</body>
</html>
