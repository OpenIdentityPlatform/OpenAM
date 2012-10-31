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

   $Id: spSingleLogoutInit.jsp,v 1.13 2009/10/15 00:01:11 exu Exp $

--%>




<%@ page import="com.sun.identity.shared.debug.Debug" %>
<%@ page import="com.sun.identity.plugin.session.SessionManager" %>
<%@ page import="com.sun.identity.plugin.session.SessionException" %>
<%@ page import="com.sun.identity.saml.common.SAMLUtils" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Utils" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Constants" %>
<%@ page import="com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement" %>
<%@ page import="com.sun.identity.saml2.meta.SAML2MetaManager" %>
<%@ page import="com.sun.identity.saml2.meta.SAML2MetaUtils" %>
<%@ page import="com.sun.identity.saml2.common.SAML2Exception" %>
<%@ page import="com.sun.identity.saml2.profile.LogoutUtil" %>
<%@ page import="com.sun.identity.saml2.profile.SPCache" %>
<%@ page import="com.sun.identity.saml2.profile.SPSingleLogout" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.List" %>

<%--
    spSingleLogoutInit.jsp
    - initiates the LogoutRequest at the Service Provider.

    Required parameters to this jsp are :
    "binding" - binding used for this request
    "NameIDValue" - NameID value for the user. Required in fedlet case.
    "SessionIndex" - Session that has this sessionIndex is to be single logout.
                     Required in fedlet case.
    "idpEntityID" - Identifier for identity provider. Required for fedlet case.
                    If binding is not set, this parameter is used to find the
                    default binding.

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
    "spEntityID" - Fedlet's entity ID. Used in fedlet case. When it is missing,
                   first sp from metadata is used.
    Check the SAML2 Documentation for supported parameters.
--%>

<%
    // Retrieves the Request Query Parameters
    // Binding are the required query parameters
    // binding - binding used for this request

    try {
        String RelayState = request.getParameter(SAML2Constants.RELAY_STATE);
        if ((RelayState == null) || (RelayState.length() == 0)) {
            RelayState = request.getParameter(SAML2Constants.GOTO);
        }

        String metaAlias = null;

        Object ssoToken = null;
        try {
            ssoToken = SessionManager.getProvider().getSession(request);
        } catch (SessionException se) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("No session.");
            }
            ssoToken = null;
        }

        String spEntityID = null;
        SAML2MetaManager manager = new SAML2MetaManager();
        if (!SPCache.isFedlet) {
            if (ssoToken == null) {
                SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                    "nullSSOToken",SAML2Utils.bundle.getString("nullSSOToken"));
                return;
            }
            String[] values = SessionManager.getProvider().
                getProperty(ssoToken, SAML2Constants.SP_METAALIAS);
            if (values != null && values.length > 0) {
                metaAlias = values[0];
            }
        } else {
            spEntityID = request.getParameter("spEntityID");
            if ((spEntityID == null) || (spEntityID.length() == 0)) {
                List spMetaAliases =
                    manager.getAllHostedServiceProviderMetaAliases("/");
                if ((spMetaAliases != null) && !spMetaAliases.isEmpty()) {
                    // get first one
                    metaAlias = (String) spMetaAliases.get(0);
                }
            } else {
                SPSSOConfigElement spConfig = 
                    manager.getSPSSOConfig("/", spEntityID);
                if (spConfig != null) {
                    metaAlias = spConfig.getMetaAlias();
                }
            } 
        }
        if (metaAlias == null) {
            try {
                SessionManager.getProvider().invalidateSession(
                    ssoToken, request, response);
            } catch (SessionException se) {
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message("No session.");
                }
            }
            if (RelayState != null) {
                response.sendRedirect(RelayState);
            } else {
                %>
                <jsp:forward page="/saml2/jsp/default.jsp?message=spSloSuccess"/>
                <%
            }
            return;
        }

        String idpEntityID = request.getParameter("idpEntityID");
        String binding = LogoutUtil.getSLOBindingInfo(request, metaAlias,
                                        SAML2Constants.SP_ROLE, idpEntityID);
        if (spEntityID == null) {
            spEntityID = manager.getEntityByMetaAlias(metaAlias);
        }
        String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);
        if (!SAML2Utils.isSPProfileBindingSupported(
            realm, spEntityID, SAML2Constants.SLO_SERVICE, binding))
        {
            SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                "unsupportedBinding",
                SAML2Utils.bundle.getString("unsupportedBinding"));
            return;
        }
 
        /**
        * Parses the request parameters and builds the Logout
        * Request to be sent to the IDP.
        *
        * @param request the HttpServletRequest.
        * @param response the HttpServletResponse.
        * @param metaAlias metaAlias of Service Provider. The format of
        *               this parameter is /realm_name/SP_name.
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
        * @throws SAML2Exception if error initiating request to IDP.
        */
        HashMap paramsMap = new HashMap();
        if (SPCache.isFedlet) {
            String sessionIndex = request.getParameter("SessionIndex");
            if ((sessionIndex == null) || (sessionIndex.length() == 0)) {
                SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                    "nullSessionIndex",
                    SAML2Utils.bundle.getString("nullSessionIndex"));
                return;
            } else {
                paramsMap.put("SessionIndex", sessionIndex);
            }
            String nameID = request.getParameter("NameIDValue");
            if ((nameID == null) || (nameID.length() == 0)) {
                SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                    "nullNameID",
                    SAML2Utils.bundle.getString("nullNameID"));
                return;
            } else {
                if (spEntityID == null) {
                    if (manager == null) {
                        manager = new SAML2MetaManager();    
                    }
                    spEntityID = manager.getEntityByMetaAlias(metaAlias);
                }
                if (idpEntityID == null) {
                    SAMLUtils.sendError(request, response, 
                        response.SC_BAD_REQUEST,
                        "nullIDPEntityID",
                        SAML2Utils.bundle.getString("nullIDPEntityID"));
                    return;
                }
                paramsMap.put(
                    "infoKey", spEntityID+ "|" + idpEntityID + "|" + nameID);
            }
           
        }
        paramsMap.put("metaAlias", metaAlias);
        paramsMap.put("idpEntityID", idpEntityID);
        paramsMap.put(SAML2Constants.ROLE, SAML2Constants.SP_ROLE);
        paramsMap.put(SAML2Constants.BINDING, binding);
        paramsMap.put("Destination", request.getParameter("Destination"));
        paramsMap.put("Consent", request.getParameter("Consent"));
        paramsMap.put("Extension", request.getParameter("Extension"));
        if ((RelayState == null) || (RelayState.equals(""))) {
            RelayState = SAML2Utils.getAttributeValueFromSSOConfig(
                realm, spEntityID, SAML2Constants.SP_ROLE,
                SAML2Constants.DEFAULT_RELAY_STATE);
        }
        if (RelayState != null) {
            paramsMap.put(SAML2Constants.RELAY_STATE, RelayState);
        }

        String sessionIndex = request.getParameter("sessionIndex");
        SPSingleLogout.initiateLogoutRequest( request,response,
            binding,paramsMap);
        
        if (binding.equalsIgnoreCase(SAML2Constants.SOAP)) {
            if (RelayState != null && (!RelayState.equals(""))) {
                response.sendRedirect(RelayState);
            } else {
                %>
                <jsp:forward page="/saml2/jsp/default.jsp?message=spSloSuccess"/>
                <%
            }
        }
    } catch (SAML2Exception sse) {
        SAML2Utils.debug.error("Error sending Logout Request " , sse);
        SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
            "LogoutRequestCreationError",
            SAML2Utils.bundle.getString("LogoutRequestCreationError") + " " +
            sse.getMessage());
        return;
    } catch (Exception e) {
        SAML2Utils.debug.error("Error initializing Request ",e);
        SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
            "LogoutRequestCreationError",
            SAML2Utils.bundle.getString("LogoutRequestCreationError") + " " +
            e.getMessage());
        return;
    }
%>
