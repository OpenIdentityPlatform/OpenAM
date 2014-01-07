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

   $Id: idpMNIRequestInit.jsp,v 1.10 2009/10/15 00:00:40 exu Exp $

--%>

<%--
   Portions Copyrighted 2013 ForgeRock AS
--%>

<%@ page import="com.sun.identity.federation.common.FSUtils" %>
<%@ page import="com.sun.identity.saml.common.SAMLUtils" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Constants" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Utils" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Exception" %>
<%@ page import="com.sun.identity.saml2.meta.SAML2MetaUtils" %>
<%@ page import="com.sun.identity.saml2.profile.DoManageNameID" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="org.owasp.esapi.ESAPI" %>

<%--
    idpMNIRequestInit.jsp initiates the ManageNameIDRequest at
    the Identity Provider.
    Required parameters to this jsp are :
    - metaAlias - identifier for Identity Provider
    - spEntityID - identifier for Service Provider
    - requestType - the request type of ManageNameIDRequest (Terminate / NewID)

    Somce of the other optional parameters are :
    - relayState - the target URL on successful complete of the Request

    Check the SAML2 Documentation for supported parameters.

--%>

<%
    // Retreive the Request Query Parameters
    // metaAlias, spEntiyID and RequestType are the required query parameters
    // metaAlias - Hosted Entity Id
    // spEntityID - Service Provider Identifier
    // requestType - the request type of ManageNameIDRequest (Terminate / NewID)
    // affiliationID - affiliation entity ID
    // Query parameters supported will be documented.

    if (FSUtils.needSetLBCookieAndRedirect(request, response, true)) {
        return;
    }

    try {
        String metaAlias = request.getParameter("metaAlias");
        if ((metaAlias ==  null) || (metaAlias.length() == 0)) {
            SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                "nullIDPEntityID", 
                SAML2Utils.bundle.getString("nullIDPEntityID"));
            return;
        }

        String idpEntityID =
            SAML2Utils.getSAML2MetaManager().getEntityByMetaAlias(metaAlias);
        String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);
        String spEntityID = request.getParameter("spEntityID");

        if ((spEntityID == null) || (spEntityID.length() == 0)) {
            SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                "nullSPEntityID", 
                SAML2Utils.bundle.getString("nullSPEntityID"));
            return;
        }

        String binding = DoManageNameID.getMNIBindingInfo(request, metaAlias,
                                        SAML2Constants.IDP_ROLE, spEntityID);
        if (!SAML2Utils.isIDPProfileBindingSupported(
            realm, idpEntityID, SAML2Constants.MNI_SERVICE, binding))
        {
            SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                "unsupportedBinding",
                SAML2Utils.bundle.getString("unsupportedBinding"));
            return;
        }

        String requestType = request.getParameter("requestType");

        if ((requestType == null) || (requestType.length() == 0)) {
            SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                "nullRequestType", 
                SAML2Utils.bundle.getString("nullRequestType"));
            return;
        }

        String relayState = request.getParameter(SAML2Constants.RELAY_STATE);
        if (!ESAPI.validator().isValidInput("HTTP Query String: " + relayState, relayState, "HTTPQueryString", 2000, true)) {
            relayState = null;
        }
        String affiliationID =
            request.getParameter(SAML2Constants.AFFILIATION_ID);

        HashMap paramsMap = new HashMap();
        paramsMap.put("metaAlias", metaAlias);
        paramsMap.put("spEntityID", spEntityID);
        paramsMap.put("requestType", requestType);
        paramsMap.put(SAML2Constants.ROLE, SAML2Constants.IDP_ROLE);
        paramsMap.put(SAML2Constants.BINDING, binding);

        if (relayState != null) {
            paramsMap.put(SAML2Constants.RELAY_STATE, relayState);
        }

        if (affiliationID != null) {
            paramsMap.put(SAML2Constants.AFFILIATION_ID, affiliationID);
        }

        Object sess = SAML2Utils.checkSession(request,response,
                                          metaAlias, paramsMap);
        if (sess == null) {
            return;
        }

        DoManageNameID.initiateManageNameIDRequest(request,response,
                                          metaAlias, spEntityID, paramsMap);

        if (binding.equalsIgnoreCase(SAML2Constants.SOAP)) {
            if (relayState != null && SAML2Utils.isRelayStateURLValid(request, relayState, SAML2Constants.IDP_ROLE) &&
                ESAPI.validator().isValidInput("HTTP URL Value: " + relayState, relayState, "URL", 2000, true)) {
                response.sendRedirect(relayState);
            } else {
                %>
                <jsp:forward page="/saml2/jsp/default.jsp?message=mniSuccess" />
                <%
            }
        }
    } catch (SAML2Exception e) {
        SAML2Utils.debug.error("Error processing ManageNameID Request ",e);
        SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
            "requestProcessingMNIError",
            SAML2Utils.bundle.getString("requestProcessingMNIError"));
        return;
    }
%>
