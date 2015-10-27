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

   Portions Copyrighted 2012-2015 ForgeRock AS.
--%>

<%@page
        import="com.sun.identity.federation.common.FSUtils,
                com.sun.identity.plugin.session.SessionException,
                com.sun.identity.saml.common.SAMLUtils,
                com.sun.identity.saml2.assertion.Assertion,
                com.sun.identity.saml2.assertion.Subject,
                com.sun.identity.saml2.common.SAML2Constants,
                com.sun.identity.saml2.common.SAML2Exception,
                com.sun.identity.saml2.common.SAML2FailoverUtils,
                com.sun.identity.saml2.common.SAML2Utils,
                com.sun.identity.saml2.meta.SAML2MetaException,
                com.sun.identity.saml2.meta.SAML2MetaManager,
                com.sun.identity.saml2.meta.SAML2MetaUtils,
                com.sun.identity.saml2.profile.ResponseInfo,
                com.sun.identity.saml2.profile.SPACSUtils"
        %>
<%@ page import="com.sun.identity.saml2.profile.SPCache" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.UUID" %>
<%@ page import="org.forgerock.openam.authentication.modules.saml2.SAML2Proxy" %>
<%@ page import="org.forgerock.openam.authentication.modules.saml2.SAML2ResponseData" %>
<%@ page import="org.forgerock.openam.federation.saml2.SAML2TokenRepositoryException" %>
<%@ page import="org.forgerock.openam.saml2.SAML2Store" %>

<html>
<head>
    <title>SP Authenticator Assertion Consumer Service</title>

</head>

<%!
    private String generateKey() {
        return UUID.randomUUID().toString();
    }

    private String getForwardForm(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SessionException, SAML2Exception {

        if ((request == null) || (response == null)) {
            //logging?
            return SAML2Proxy.toPostWithErrorForm(request, SAML2Proxy.BAD_REQUEST);
        }

        try {
            SAMLUtils.checkHTTPContentLength(request);
        } catch (ServletException se) {
            //logging?
            return SAML2Proxy.toPostWithErrorForm(request, SAML2Proxy.BAD_REQUEST);
        }

        if (FSUtils.needSetLBCookieAndRedirect(request, response, false)) {
            //logging?
            return SAML2Proxy.toPostWithErrorForm(request, SAML2Proxy.MISSING_COOKIE);
        }

        // get entity id and orgName
        String requestURL = request.getRequestURL().toString();
        String metaAlias = SAML2MetaUtils.getMetaAliasByUri(requestURL);
        SAML2MetaManager metaManager = SAML2Utils.getSAML2MetaManager();
        String hostEntityId;

        if (metaManager == null) {
            // logging?
            return SAML2Proxy.toPostWithErrorForm(request, SAML2Proxy.MISSING_META_MANAGER);
        }

        try {
            hostEntityId = metaManager.getEntityByMetaAlias(metaAlias);
            if (hostEntityId == null ){
                throw new SAML2MetaException("Caught Instantly");
            }
        } catch (SAML2MetaException sme) {
            // logging?
            return SAML2Proxy.toPostWithErrorForm(request, SAML2Proxy.META_DATA_ERROR); // configuration problem ?
        }

        String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);

        if (realm == null || realm.length() == 0) {
            realm = "/";
        }

        ResponseInfo respInfo;
        try {
            respInfo = SPACSUtils.getResponse(request, response, realm, hostEntityId, metaManager);
        } catch (SAML2Exception se) {
            //logging?
            return SAML2Proxy.toPostWithErrorForm(request, SAML2Proxy.SAML_GET_RESPONSE_ERROR,
                    se.getL10NMessage(request.getLocale()));
        }

        Map smap;
        try {
            // check Response/Assertion and get back a Map of relevant data
            smap = SAML2Utils.verifyResponse(request, response, respInfo.getResponse(), realm, hostEntityId,
                    respInfo.getProfileBinding());
        } catch (SAML2Exception se) {
            //logging?
            return SAML2Proxy.toPostWithErrorForm(request, SAML2Proxy.SAML_VERIFY_RESPONSE_ERROR,
                    se.getL10NMessage(request.getLocale()));
        }
        String key = generateKey();

        //survival time is one hour

        SAML2ResponseData data = new SAML2ResponseData((String) smap.get(SAML2Constants.SESSION_INDEX),
                (Subject) smap.get(SAML2Constants.SUBJECT),
                (Assertion) smap.get(SAML2Constants.POST_ASSERTION));

        if (SAML2FailoverUtils.isSAML2FailoverEnabled()) {
            try {
                long sessionExpireTime = System.currentTimeMillis() / 1000 + SPCache.interval; //counted in seconds
                SAML2FailoverUtils.saveSAML2TokenWithoutSecondaryKey(key, data ,sessionExpireTime);
            } catch (SAML2TokenRepositoryException e) {
                //logging?
                return SAML2Proxy.toPostWithErrorForm(request, SAML2Proxy.SAML_FAILOVER_DISABLED_ERROR);
            }
        } else {
            SAML2Store.saveTokenWithKey(key, data);
        }

        return SAML2Proxy.toPostForm(request, key);

    }
%>

<body>
<%= getForwardForm(request, response)%>
</body>
</html>
