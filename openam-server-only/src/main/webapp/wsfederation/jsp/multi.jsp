<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
  
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

   $Id: multi.jsp,v 1.1 2009/07/02 22:00:17 exu Exp $

--%>

<%--
 Portions Copyrighted 2013 ForgeRock AS
 Portions Copyright 2026 3A Systems, LLC.
--%>

<%@page
    import="com.sun.identity.wsfederation.common.WSFederationConstants"
    import="com.sun.identity.plugin.session.SessionManager"
    import="com.sun.identity.wsfederation.common.WSFederationUtils"
    import="com.sun.identity.wsfederation.common.WReplyValidator"
    import="org.owasp.esapi.ESAPI"
%><%
    // handle multi-federation protocol case
    Object uSession = null;
    try {
        uSession = SessionManager.getProvider().getSession(request);
    } catch (Exception e) {
    }

    String realm = request.getParameter(
        WSFederationConstants.REALM_PARAM);
    if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + realm,
        realm, "HTTPParameterValue", 2000, true)){
            realm = null;
    }
    // Not HTTPParameterValue: that pattern has no ":" in its character class, so it rejects every realistic
    // WS-Federation entity ID - they are URIs, "urn:federation:..." or "https://...". Nulling it here would
    // leave the multi-protocol logout below without the entity whose signout it is completing. What that
    // pattern did buy is kept by hand: a length bound, and no control characters, since the value reaches
    // SingleLogoutManager and the debug log from an unauthenticated request. It is never written into the page.
    String entityID = request.getParameter(
        WSFederationConstants.ENTITYID_PARAM);
    if (entityID != null) {
        if (entityID.length() > 2000) {
            entityID = null;
        } else {
            for (int i = 0; i < entityID.length(); i++) {
                if (Character.isISOControl(entityID.charAt(i))) {
                    entityID = null;
                    break;
                }
            }
        }
    }

    // The wreply parameter ends up in a redirect, and this endpoint has nothing trustworthy to validate it
    // against: it carries no metaAlias - logout.jsp names the entity in the query string - and it answers an
    // unauthenticated request, so realm and entityID are whatever the caller sent. Letting them select the
    // wreplyList would let any hosted entity's list authorise a redirect issued for another entity, in another
    // realm, so no whitelist is read here and only this deployment's own URLs are accepted. A wreply on
    // another host was never reachable on any shipped build either: the ESAPI "URL" check this replaces
    // rejected every value, so multi.jsp redirected nowhere at all.
    String wreply = request.getParameter(WSFederationConstants.LOGOUT_WREPLY);
    if (!WReplyValidator.pointsBackAtThisServer(request, wreply)) {
            if (wreply != null && wreply.length() != 0) {
                // Without this an operator has nothing to correlate: the request below then answers 200 with
                // no body and no redirect.
                WSFederationUtils.debug.warning("multi.jsp: the wreply URL does not point back at this "
                    + "server and is ignored");
            }
            wreply = null;
    }

    if ((uSession == null) || !SessionManager.getProvider().isValid(uSession)) {
        if ((wreply != null) && (wreply.length() != 0)) {
            response.sendRedirect(wreply);
        }
    } else {
        request.setAttribute(WSFederationConstants.LOGOUT_WREPLY, wreply);
        request.setAttribute(WSFederationConstants.REALM_PARAM, realm);
        request.setAttribute(WSFederationConstants.ENTITYID_PARAM, entityID);

        WSFederationUtils.processMultiProtocolLogout(request, response, uSession);
    }
%>
