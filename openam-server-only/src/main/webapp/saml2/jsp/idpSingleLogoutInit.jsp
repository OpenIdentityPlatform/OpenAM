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

   $Id: idpSingleLogoutInit.jsp,v 1.9 2009/10/15 00:00:41 exu Exp $

   Portions Copyrighted 2010-2014 ForgeRock AS
--%>


<%@ page import="com.iplanet.am.util.SystemProperties" %>
<%@ page import="com.sun.identity.plugin.session.SessionException" %>
<%@ page import="com.sun.identity.plugin.session.SessionManager" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Utils" %>
<%@ page import="com.sun.identity.saml.common.SAMLUtils" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Constants" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Exception" %>
<%@ page import="com.sun.identity.saml2.meta.SAML2MetaUtils" %>
<%@ page import="com.sun.identity.saml2.profile.IDPSingleLogout" %>
<%@ page import="com.sun.identity.saml2.profile.LogoutUtil" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="org.owasp.esapi.ESAPI" %>
<%@ page import="java.io.PrintWriter" %>

<%--
    idpSingleLogoutInit.jsp
    - initiates the LogoutRequest at the Identity Provider.

    Required parameters to this jsp are :
    - binding - binding used for this request

    Some of the other optional parameters are :
    "RelayState" - the target URL on successful Single Logout
    "goto" - the target URL on successful Single Logout.
             "RelayState" takes precedence to "goto" parameter.
    "Destination" - A URI Reference indicating the address to
                    which the request has been sent.
    "Consent" - Specifies a URI a SAML defined identifier
                known as Consent Identifiers.
    "Extension" - Specifies a list of Extensions as list of
                 String objects.
    "logoutAll" - Specifies IDP sends slo request to SP without any session
                 index.

    Check the SAML2 Documentation for supported parameters.

--%>

<%
    // Retrieve the Request Query Parameters
    // binding are the required query parameters
    // binding - binding used for this request

    try {
        String relayState = request.getParameter(SAML2Constants.RELAY_STATE);
        if ((relayState == null) || (relayState.length() == 0)) {
            relayState = request.getParameter(SAML2Constants.GOTO);
        }
        if (!ESAPI.validator().isValidInput("HTTP Query String: " + relayState, relayState, "HTTPQueryString", 2000, true)) {
            relayState = null;
        }
        Object ssoToken = null;
        try {
              ssoToken = SessionManager.getProvider().getSession(request);
        } catch (SessionException e) {
            String intermmediatePage = SystemProperties.get(
                    "openam.idpsloinit.nosession.intermmediate.page", "");

            if ( intermmediatePage.length() != 0 ) {
               if (relayState != null) {
                   intermmediatePage = intermmediatePage + "?RelayState=" + relayState;
               } 
               response.sendRedirect(intermmediatePage);
            } else {
                if (relayState != null && SAML2Utils.isRelayStateURLValid(request, relayState, SAML2Constants.IDP_ROLE) &&
                    ESAPI.validator().isValidInput("RelayState", relayState, "URL", 2000, true)) {
                   response.sendRedirect(relayState);
               } else {
                   %>
                     <jsp:forward
                        page="/saml2/jsp/default.jsp?message=idpSloSuccess" />
                   <%
               }
            }
            return;
        }
        if (ssoToken == null) {
            SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                "nullSSOToken", SAML2Utils.bundle.getString("nullSSOToken"));
            return;
        }
        String[] values = SessionManager.getProvider().
            getProperty(ssoToken, SAML2Constants.IDP_META_ALIAS);
        String metaAlias = null;
        if (values != null && values.length > 0) {
            metaAlias = values[0];
        }
        if (metaAlias == null) {
            SessionManager.getProvider().invalidateSession(ssoToken, request, response);
            if (relayState != null && SAML2Utils.isRelayStateURLValid(request, relayState, SAML2Constants.IDP_ROLE)
                    && ESAPI.validator().isValidInput("RelayState", relayState, "URL", 2000, true)) {
                response.sendRedirect(relayState);
            } else {
                %>
                <jsp:forward
                    page="/saml2/jsp/default.jsp?message=idpSloSuccess" />
                <%
            }
            return;
        }

        String idpEntityID = 
            SAML2Utils.getSAML2MetaManager().getEntityByMetaAlias(metaAlias);
        String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);

        String binding = LogoutUtil.getSLOBindingInfo(request, metaAlias,
                                        SAML2Constants.SP_ROLE, idpEntityID);
        if (!SAML2Utils.isIDPProfileBindingSupported(
            realm, idpEntityID, SAML2Constants.SLO_SERVICE, binding))
        {
            SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                "unsupportedBinding", 
                SAML2Utils.bundle.getString("unsupportedBinding"));
            return;
        }

        String logoutAll = request.getParameter(SAML2Constants.LOGOUT_ALL);
        /**
        * Parses the request parameters and builds the Logout
        * Request to be sent to the SP.
        *
        * @param request the HttpServletRequest.
        * @param response the HttpServletResponse.
        * @param binding binding used for this request.
        * @param paramsMap Map of all other parameters.
        *       Following parameters names with their respective
        *       String values are allowed in this paramsMap.
        *       "RelayState" - the target URL on successful Single Logout
        *       "Destination" - A URI Reference indicating the address to
        *                       which the request has been sent.
        *       "Consent" - Specifies a URI a SAML defined identifier
        *                   known as Consent Identifiers.
        *       "Extension" - Specifies a list of Extensions as list of
        *                   String objects.
        * @throws SAML2Exception if error initiating request to SP.
        */
        HashMap paramsMap = new HashMap();
        paramsMap.put("metaAlias", metaAlias);
        paramsMap.put(SAML2Constants.ROLE, SAML2Constants.IDP_ROLE);
        paramsMap.put(SAML2Constants.BINDING, binding);
        paramsMap.put("Destination", request.getParameter("Destination"));
        paramsMap.put("Consent", request.getParameter("Consent"));
        paramsMap.put("Extension", request.getParameter("Extension"));
        if (relayState != null) {
            paramsMap.put(SAML2Constants.RELAY_STATE, relayState);
        }

        if (logoutAll != null) {
            paramsMap.put(SAML2Constants.LOGOUT_ALL, logoutAll);
        }

        IDPSingleLogout.initiateLogoutRequest(request,response, new PrintWriter(out, true),
            binding,paramsMap);
        if (!response.isCommitted()) {
            if (relayState != null && SAML2Utils.isRelayStateURLValid(metaAlias, relayState, SAML2Constants.IDP_ROLE)
                    && ESAPI.validator().isValidInput("RelayState", relayState, "URL", 2000, true)) {
                response.sendRedirect(relayState);
            } else {
                %>
                <jsp:forward
                    page="/saml2/jsp/default.jsp?message=idpSloSuccess" />
                <%
            }
        }
    } catch (SAML2Exception sse) {
        SAML2Utils.debug.error("Error sending Logout Request " , sse);
        SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
            "LogoutRequestProcessingError",
            SAML2Utils.bundle.getString("LogoutRequestProcessingError") + " " +
            sse.getMessage());
        return;
    } catch (Exception e) {
        SAML2Utils.debug.error("Error processing Request ",e);
        SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
            "LogoutRequestProcessingError",
            SAML2Utils.bundle.getString("LogoutRequestProcessingError") + " " +
            e.getMessage());
        return;
    }
%>
