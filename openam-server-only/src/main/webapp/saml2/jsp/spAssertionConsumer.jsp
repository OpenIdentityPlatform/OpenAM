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

   $Id: spAssertionConsumer.jsp,v 1.17 2010/01/23 00:07:06 exu Exp $

   Portions Copyrighted 2012-2016 ForgeRock AS.
--%>

<%@page
import="com.sun.identity.shared.encode.URLEncDec,
com.sun.identity.federation.common.FSUtils,
com.sun.identity.saml.common.SAMLUtils,
com.sun.identity.saml2.common.SAML2Constants,
com.sun.identity.saml2.common.SAML2Exception,
com.sun.identity.saml2.common.InvalidStatusCodeSaml2Exception,
com.sun.identity.saml2.common.SAML2Utils,
com.sun.identity.saml2.logging.LogUtil,
com.sun.identity.saml2.meta.SAML2MetaException,
com.sun.identity.saml2.meta.SAML2MetaManager,
com.sun.identity.saml2.meta.SAML2MetaUtils,
com.sun.identity.saml2.profile.ResponseInfo,
com.sun.identity.saml2.profile.SPACSUtils,
com.sun.identity.saml2.profile.IDPProxyUtil,
com.sun.identity.saml2.protocol.Response,
com.sun.identity.plugin.session.SessionManager,
com.sun.identity.plugin.session.SessionProvider,
com.sun.identity.plugin.session.SessionException,
java.util.logging.Level,
org.forgerock.guice.core.InjectorHolder,
org.forgerock.openam.audit.AuditEventPublisher,
org.forgerock.openam.saml2.audit.SAML2Auditor,
org.forgerock.openam.audit.AuditEventFactory,
java.io.PrintWriter
"
%>

<html>
<head>
    <title>SP Assertion Consumer Service</title>
</head>

<%!
    private String getLocalLoginUrl(
                                String orgName,
                                String hostEntityId,
                                SAML2MetaManager metaManager,
                                ResponseInfo respInfo,
                                String requestURL,
                                String relayState)
    {
        String localLoginUrl = SPACSUtils.prepareForLocalLogin(
                orgName, hostEntityId, metaManager, respInfo, requestURL);
        if (localLoginUrl.indexOf("?") == -1) {
            localLoginUrl += "?goto=";
        } else {
            localLoginUrl += "&goto=";
        }
        String gotoURL = requestURL + "?resID="
                        + URLEncDec.encode(respInfo.getResponse().getID());
        if (relayState != null && relayState.length() != 0) {
                gotoURL += "&RelayState=" + URLEncDec.encode(relayState);
        }
        localLoginUrl += URLEncDec.encode(gotoURL);
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("spAssertionConsumer.jsp: local login "
                        + "url=" + localLoginUrl);
        }
        return localLoginUrl;
    }
%>

<body>
<%
    // set up audit logger and attach initial information
    AuditEventPublisher aep = InjectorHolder.getInstance(AuditEventPublisher.class);
    AuditEventFactory aef = InjectorHolder.getInstance(AuditEventFactory.class);
    SAML2Auditor saml2Auditor = new SAML2Auditor(aep, aef, request);
    saml2Auditor.setMethod("spAssertionConsumer");
    saml2Auditor.setSessionTrackingId(session.getId());
    saml2Auditor.auditAccessAttempt();

    // check request, response, content length
    if ((request == null) || (response == null)) {
        SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
            "nullInput", SAML2Utils.bundle.getString("nullInput"));
        saml2Auditor.auditAccessFailure(String.valueOf(response.SC_BAD_REQUEST),
                SAML2Utils.bundle.getString("nullInput"));
        return;
    }
    // to avoid dos attack
    // or use SAML2Utils?
    try {
        SAMLUtils.checkHTTPContentLength(request);
    } catch (ServletException se) {
        SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST, 
            "largeContentLength", se.getMessage());
        saml2Auditor.auditAccessFailure(String.valueOf(response.SC_BAD_REQUEST),
                se.getMessage());
        return;
    }

    if (FSUtils.needSetLBCookieAndRedirect(request, response, false)) {
        saml2Auditor.auditForwardToProxy();
        return;
    }

    String requestURL = request.getRequestURL().toString();
    // get entity id and realm
    String metaAlias = SAML2MetaUtils.getMetaAliasByUri(requestURL);
    String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);
    if (realm == null || realm.length() == 0) {
        realm = "/";
    }
    saml2Auditor.setRealm(realm);

    SAML2MetaManager metaManager = SAML2Utils.getSAML2MetaManager();
    if (metaManager == null) {
        // logging?
        SAMLUtils.sendError(request, response, 
            response.SC_INTERNAL_SERVER_ERROR, "errorMetaManager",
            SAML2Utils.bundle.getString("errorMetaManager"));
        saml2Auditor.auditAccessFailure(String.valueOf(response.SC_BAD_REQUEST),
                SAML2Utils.bundle.getString("errorMetaManager"));
        return;
    }
    String hostEntityId = null;
    try {
        hostEntityId = metaManager.getEntityByMetaAlias(metaAlias);
    } catch (SAML2MetaException sme) {
        // logging?
        SAMLUtils.sendError(request, response, 
            response.SC_INTERNAL_SERVER_ERROR, "metaDataError", 
            SAML2Utils.bundle.getString("metaDataError"));
        saml2Auditor.auditAccessFailure(String.valueOf(response.SC_INTERNAL_SERVER_ERROR),
                SAML2Utils.bundle.getString("metaDataError"));
        return;
    }
    if (hostEntityId == null) {
        // logging?
        SAMLUtils.sendError(request, response, 
            response.SC_INTERNAL_SERVER_ERROR, "metaDataError",
            SAML2Utils.bundle.getString("metaDataError"));
        saml2Auditor.auditAccessFailure(String.valueOf(response.SC_INTERNAL_SERVER_ERROR),
                SAML2Utils.bundle.getString("metaDataError"));
        return;
    }
    String relayState = request.getParameter(SAML2Constants.RELAY_STATE);

    // federate flag
    String federate = request.getParameter(SAML2Constants.FEDERATE);
    SessionProvider sessionProvider = null;
    ResponseInfo respInfo = null; 
    try {
        sessionProvider = SessionManager.getProvider();
    } catch (SessionException se) {
        SAMLUtils.sendError(request, response, 
            response.SC_INTERNAL_SERVER_ERROR, "nullSessionProvider",
            se.getMessage());
        saml2Auditor.auditAccessFailure(se.getErrorCode(), se.getLocalizedMessage());
        return;
    }
    try {
        respInfo = SPACSUtils.getResponse(
            request, response, realm, hostEntityId, metaManager);
        saml2Auditor.setRequestId(respInfo.getResponse().getInResponseTo());
    } catch (SAML2Exception se) {
        // Only do a sendError if one hasn't already been called.
        if (!response.isCommitted()) {
            SAMLUtils.sendError(request, response,
                response.SC_INTERNAL_SERVER_ERROR, "getResponseError",
                se.getMessage());
        }
        saml2Auditor.auditAccessFailure(se.getErrorCode(), se.getLocalizedMessage());
        return;
    }

    String ecpRelayState = respInfo.getRelayState();
    if ((ecpRelayState != null) && (ecpRelayState.length() > 0)) {
        relayState = ecpRelayState;
    }

    Object token = null;
    try {
        token = sessionProvider.getSession(request);
        saml2Auditor.setAuthTokenId(token);

    } catch (SessionException se) {
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(
                "spAssertionConsumer.jsp: Token is null." +
                se.getMessage());
        }
        token = null;
    }
    if (federate != null && federate.trim().equals("true") &&
        token == null) {
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("spAssertionConsumer.jsp: federate "
                + "is true, and token is null. do local login first.");
        }
        FSUtils.forwardRequest(request, response,
                getLocalLoginUrl(realm, hostEntityId, metaManager, respInfo, requestURL, relayState));
        saml2Auditor.auditForwardToLocalUserLogin();
        return;
    }
    Object newSession = null;
    Response saml2Resp = respInfo.getResponse();

    String requestID = saml2Resp.getInResponseTo();
    boolean isProxyOn = IDPProxyUtil.isIDPProxyEnabled(requestID);
    try {
        newSession = SPACSUtils.processResponse( request, response, new PrintWriter(out, true), metaAlias, token,
                respInfo, realm, hostEntityId, metaManager, saml2Auditor);
        saml2Auditor.setUserId(sessionProvider.getPrincipalName(newSession));
        saml2Auditor.setSSOTokenId(newSession);

    } catch (SAML2Exception se) {
        String[] data = {hostEntityId, se.getMessage(), ""};
        if (LogUtil.isErrorLoggable(Level.FINE)) {
            data[2] = saml2Resp.toXMLString(true, true);
        }
        LogUtil.error(Level.INFO, LogUtil.SP_SSO_FAILED, data, null);
        if (se instanceof InvalidStatusCodeSaml2Exception) {
            if (isProxyOn) {
                SAML2Utils.debug.error("spAssertionConsumer.jsp: Non-Success status code in response");
                String firstlevelStatusCodeValue = ((InvalidStatusCodeSaml2Exception) se).getFirstlevelStatuscode();
                String secondlevelStatusCodeValue = ((InvalidStatusCodeSaml2Exception) se).getSecondlevelStatuscode();
                try {
                    IDPProxyUtil.sendResponseWithStatus(request, response, new PrintWriter(out, true),
                            requestID, metaAlias, hostEntityId, realm, firstlevelStatusCodeValue,
                            secondlevelStatusCodeValue);
                } catch (SAML2Exception samle) {
                    SAML2Utils.debug.error("Failed to send response with status ", samle);
                }
                return;
            }
        } else {
            SAML2Utils.debug.error("spAssertionConsumer.jsp: SSO failed.", se);
            if (se.isRedirectionDone()) {
                saml2Auditor.auditAccessSuccess();
                return;
            }
            if (se.getMessage().equals(SAML2Utils.bundle.getString("noUserMapping"))) {
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message("spAssertionConsumer.jsp:need local login!!");
                }
                FSUtils.forwardRequest(request, response,
                        getLocalLoginUrl(realm, hostEntityId, metaManager, respInfo, requestURL, relayState));
                saml2Auditor.auditForwardToLocalUserLogin();
                return;
            }
            saml2Auditor.auditAccessFailure(String.valueOf(response.SC_INTERNAL_SERVER_ERROR),
                    SAML2Utils.bundle.getString("SSOFailed"));
            SAMLUtils.sendError(request, response, response.SC_INTERNAL_SERVER_ERROR, "SSOFailed",
                    SAML2Utils.bundle.getString("SSOFailed"));
            return;
        }
    }

    if (newSession == null) {
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("Session is null.");
            SAML2Utils.debug.message("spAssertionConsumer.jsp:Login has failed!!");
        }
        SAMLUtils.sendError(request, response, response.SC_INTERNAL_SERVER_ERROR, "SSOFailed",
                SAML2Utils.bundle.getString("SSOFailed"));
        saml2Auditor.auditAccessFailure(String.valueOf(
                response.SC_INTERNAL_SERVER_ERROR), SAML2Utils.bundle.getString("SSOFailed"));
        return;
    }
    SAML2Utils.debug.message("SSO SUCCESS");
    String[] redirected = sessionProvider.getProperty(newSession,
        SAML2Constants.RESPONSE_REDIRECTED);
    if ((redirected != null) && (redirected.length != 0) &&
        redirected[0].equals("true")) {
        SAML2Utils.debug.message("Redirection already done in SPAdapter.");
        // response redirected already in SPAdapter

        saml2Auditor.auditForwardToProxy();
        return;
    }
    if (isProxyOn) { 
        try {
            IDPProxyUtil.generateProxyResponse(request, response, new PrintWriter(out, true), metaAlias, respInfo,
                    newSession, saml2Auditor);
            saml2Auditor.auditForwardToProxy();
        } catch (SAML2Exception se) {
            SAML2Utils.debug.error("Failed sending proxy response", se);
            saml2Auditor.auditAccessFailure(se.getErrorCode(), se.getLocalizedMessage());
        }
        return;  
    } 
    // redirect to relay state
    String finalUrl = SPACSUtils.getRelayState(relayState, realm, hostEntityId, metaManager);

    String realFinalUrl = finalUrl;
    if (finalUrl != null && finalUrl.length() != 0) {
        try {
            realFinalUrl = sessionProvider.rewriteURL(newSession, finalUrl);
        } catch (SessionException se) {
            SAML2Utils.debug.message(
                 "spAssertionConsumer.jsp: URL rewriting failed.", se);
                 realFinalUrl = finalUrl;
        }
    }
    String redirectUrl = SPACSUtils.getIntermediateURL(realm, hostEntityId, metaManager);
    String realRedirectUrl = null;
    if (redirectUrl != null && redirectUrl.length() != 0) {
        if (realFinalUrl != null && realFinalUrl.length() != 0) {
            if (redirectUrl.indexOf("?") != -1) {
                redirectUrl += "&goto=";
            } else {
                redirectUrl += "?goto=";
            }
            redirectUrl += URLEncDec.encode(realFinalUrl);
            try {
                realRedirectUrl = sessionProvider.rewriteURL(newSession, redirectUrl);
            } catch (SessionException se) {
                SAML2Utils.debug.message("spAssertionConsumer.jsp: URL rewriting failed.", se);
                realRedirectUrl = redirectUrl;
            }
        } else {
            realRedirectUrl = redirectUrl;
        }
    } else {
        realRedirectUrl = finalUrl;
    }
    if (realRedirectUrl == null || (realRedirectUrl.trim().length() == 0)) {
        if (isProxyOn) {
            saml2Auditor.auditForwardToProxy();
            return;
        } else {
            saml2Auditor.auditAccessSuccess();
           %>
            <jsp:forward page="/saml2/jsp/default.jsp?message=ssoSuccess" />
          <% 
        }  
    } else {
        // log it
	    try {
	        SAML2Utils.validateRelayStateURL(realm, hostEntityId, realRedirectUrl, SAML2Constants.SP_ROLE);
        } catch (SAML2Exception se) {
	        SAMLUtils.sendError(request, response,
                response.SC_BAD_REQUEST, "requestProcessingError",
	            SAML2Utils.bundle.getString("requestProcessingError") + " " + se.getMessage());
            saml2Auditor.auditAccessFailure(se.getErrorCode(), se.getLocalizedMessage());
            return;
        }

        saml2Auditor.auditAccessSuccess();
        response.sendRedirect(realRedirectUrl);
    }
%>
</body>
</html>
