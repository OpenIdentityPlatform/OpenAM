<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
  
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

   $Id: SA_IDP.jsp,v 1.10 2009/06/24 00:22:44 sean_brydon Exp $

   Portions Copyrighted 2013-2014 ForgeRock AS.
--%>

<%@ page language="java" 
import="java.util.*,
java.net.URL,
java.util.logging.Level,
com.sun.identity.plugin.session.SessionProvider,
com.sun.identity.plugin.session.SessionManager,
com.sun.identity.plugin.session.SessionException,
com.sun.identity.sae.api.SecureAttrs,
com.sun.identity.sae.api.Utils,
com.sun.identity.saml2.common.SAML2Constants,
com.sun.identity.saml2.common.SAML2Utils,
com.sun.identity.saml2.jaxb.metadata.AssertionConsumerServiceElement,
com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorElement,
com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement,
com.sun.identity.saml2.logging.LogUtil,
com.sun.identity.saml2.meta.SAML2MetaManager,
com.sun.identity.saml2.meta.SAML2MetaUtils,
com.sun.identity.saml2.meta.SAML2MetaException,
org.forgerock.openam.utils.ClientUtils,
org.owasp.esapi.ESAPI"
%>
<%@ page import="java.io.PrintWriter" %>

<%-- functions --%>
<%!
    private String getTrustedSP(
        SAML2MetaManager mm, 
        String realm, 
        String idpEntityId,
        String targetSPAppUrl) 
    {
        String firstspId = null;
        try {
            URL targetUrl = new URL(targetSPAppUrl);
            String targetHost = targetUrl.getHost();
            List spEntityIds = mm.getAllRemoteServiceProviderEntities(realm);
            Iterator iter = spEntityIds.iterator();
            while (iter.hasNext()) {
                String tempspId = (String) iter.next();
                if (mm.isTrustedProvider(realm, idpEntityId, tempspId)) {
                    if (targetHost == null) {
                        return tempspId;
                    }
                    if (firstspId == null) {
                        firstspId = tempspId;
                    }
                    // see if this sp's sso url contains targetSPAppUrl
                    SPSSODescriptorElement spDesc =
                        mm.getSPSSODescriptor(realm, tempspId);
                    Iterator acsIter = 
                        spDesc.getAssertionConsumerService().iterator();
                    while (acsIter.hasNext()) {
                        AssertionConsumerServiceElement acs =
                            (AssertionConsumerServiceElement) acsIter.next();
                        if (acs.getLocation().indexOf(targetHost) != -1) {
                            return tempspId;
                        }
                    }
                }
            }
        } catch (Exception e) {
            SAML2Utils.debug.error("SA_IDP.jsp: couldn't find trusted SP:",e);
        }
        return firstspId;
    }
%>

<%
    // Setup POST or GET for SAE interactions on IDP end
    String action = "POST";
    // Determine metadata alias of hosted IDP this endpoint represents.
    String idpMetaAlias = 
               SAML2MetaUtils.getMetaAliasByUri(request.getRequestURI()) ;

    String realm = null;
    String idpEntityId = null;
    SAML2MetaManager mm = null;
    String servletPath = request.getServletPath();
    String gotoUrl = request.getRequestURL().toString();
    int pathIndex = gotoUrl.lastIndexOf(servletPath);
    String appBase = gotoUrl.substring(0, pathIndex+1);
    gotoUrl = gotoUrl.substring(pathIndex, gotoUrl.length());
    String errorUrl = appBase+"saml2/jsp/saeerror.jsp";
    String ipaddr = ClientUtils.getClientIPAddress(request);
    String userid = null;
    Object token = null;
    SessionProvider provider = null;

    if (idpMetaAlias == null) {
	String errStr = 
            errorUrl+"?errorcode=IDP_9&errorstring=Null_metaAlias";
	SAML2Utils.debug.error(errStr);
        String[] data = {errStr};
        SAML2Utils.logError(Level.INFO, LogUtil.SAE_IDP_ERROR, 
                   data, token, ipaddr, userid, realm, "SAE", null);
        response.sendRedirect(errStr);
        return;
    }

    // Check if a user is already authenticated
    boolean loggedIn = false;
    boolean forceAuth = false;
    String loggedinPrincipal = null;
    String loggedinAuthLevel = null;
    try {
        provider = SessionManager.getProvider();
        token = provider.getSession(request);
        if((token != null) && (provider.isValid(token))) {
            loggedIn = true;
            loggedinPrincipal = provider.getPrincipalName(token);
            String[] levelStr = 
                    provider.getProperty(token, SessionProvider.AUTH_LEVEL);
            if ((levelStr != null) && (levelStr.length > 0)) {
                loggedinAuthLevel = levelStr[0];
            }
        } else {
            token  = null; // for logging
        }
    } catch (UnsupportedOperationException ue) {
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(
                "SA_IDP:cannot get auth level from session:",ue);
        }
    } catch (SessionException e) {
	SAML2Utils.debug.message("SA_IDP:sessionvalidation exc:ignored" , e);
        token  = null; // for logging
        // Assumed not logged in
    }

    // retrieve sun.data parameter first.
    String sunData = request.getParameter(SecureAttrs.SAE_PARAM_DATA);
    if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + sunData, sunData,
        "HTTPParameterValue", 2000, true)){
            sunData = null;
    }

    if (sunData == null) {
        String errStr = errorUrl+"?errorcode=IDP_1&errorstring=Null_sun.data";
	    SAML2Utils.debug.error(errStr);
        String[] data = {errStr};
        SAML2Utils.logError(Level.INFO, LogUtil.SAE_IDP_ERROR_NODATA, 
                   data, token, ipaddr, userid, realm, "SAE", null);
        response.sendRedirect(errStr);
        return;
    }

    String idpAppUrl =  request.getParameter(SecureAttrs.SAE_PARAM_IDPAPPURL);
    if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + idpAppUrl, idpAppUrl,
        "HTTPParameterValue", 2000, true)) {
        idpAppUrl = null;
    }

    boolean metaError = false;
    try {
        realm = SAML2MetaUtils.getRealmByMetaAlias(idpMetaAlias);
        mm = SAML2Utils.getSAML2MetaManager();
        if ((idpEntityId = mm.getEntityByMetaAlias(idpMetaAlias)) == null) {
            metaError = true;
        }
    } catch (SAML2MetaException se) {
        // error
        metaError = true;
    }
    if (metaError) {
	String errStr = errorUrl+"?errorcode=IDP_2&errorstring=Error_idp_metadata:"
                                +idpMetaAlias;
	SAML2Utils.debug.error(errStr);
        String[] data = {errStr};
        SAML2Utils.logError(Level.INFO, LogUtil.SAE_IDP_ERROR, 
                   data, token, ipaddr, userid, realm, "SAE", null);
        response.sendRedirect(errStr);
        return;
    }

    Map hp = SAML2Utils.getSAEAttrs(
        realm, idpEntityId, SAML2Constants.IDP_ROLE, idpAppUrl);

    if (hp == null) {
        String errStr = errorUrl
                       +"?errorcode=IDP_3&errorstring=No_App_Attrs:"
                       +idpAppUrl;
	SAML2Utils.debug.error(errStr);
        String[] data = {errStr};
        SAML2Utils.logError(Level.INFO, LogUtil.SAE_IDP_ERROR, 
                   data, token, ipaddr, userid, realm, "SAE", null);
        response.sendRedirect(errStr);
        return;
    }
    String cryptoType = (String) hp.get(SecureAttrs.SAE_CRYPTO_TYPE);
    String secret = null;
    String encryptionSecret = null;
    String encAlg = (String)hp.get(SecureAttrs.SAE_CONFIG_DATA_ENCRYPTION_ALG);
    String encStrength = (String)hp.get(
                 SecureAttrs.SAE_CONFIG_ENCRYPTION_KEY_STRENGTH);
    if (SecureAttrs.SAE_CRYPTO_TYPE_SYM.equals(cryptoType)) {
        // Shared secret between FM-IDP and IDPApp
        secret = (String) hp.get(SecureAttrs.SAE_CONFIG_SHARED_SECRET );
        encryptionSecret = secret;
    }
    else {
        // IDPApp's public key
        secret = (String) hp.get(SecureAttrs.SAE_CONFIG_PUBLIC_KEY_ALIAS);
        encryptionSecret =  (String) hp.get(
                      SecureAttrs.SAE_CONFIG_PRIVATE_KEY_ALIAS);
    }

    if (secret == null || secret.length() == 0) {
        String errStr = errorUrl
                  +"?errorcode=IDP_4&errorstring=Error_invalid_secret_or_key:"
                  +idpAppUrl;
	SAML2Utils.debug.error(errStr);
        String[] data = {errStr};
        SAML2Utils.logError(Level.INFO, LogUtil.SAE_IDP_ERROR, 
                   data, token, ipaddr, userid, realm, "SAE", null);
        response.sendRedirect(errStr);
        return;
    }

    // This time we nead the real crypto
    String saInstanceName = cryptoType + "_" + encAlg + "_" + encStrength;
    SecureAttrs sa = SecureAttrs.getInstance(saInstanceName);
    if (sa == null) {
        Properties prop = new Properties();
        prop.setProperty(SecureAttrs.SAE_CONFIG_CERT_CLASS,
            "com.sun.identity.sae.api.FMCerts");
        if (encAlg != null) {
            prop.setProperty(
                SecureAttrs.SAE_CONFIG_DATA_ENCRYPTION_ALG, encAlg);
        }

        if (encStrength != null) {
            prop.setProperty(
                SecureAttrs.SAE_CONFIG_ENCRYPTION_KEY_STRENGTH, encStrength);
        }
        SecureAttrs.init(saInstanceName, cryptoType, prop);
        sa = SecureAttrs.getInstance(saInstanceName);
    }

    // check for sa
    if (sa == null) {
        String errStr = errorUrl
              +"?errorcode=IDP_10&errorstring=Error_null_secure_attrs_instance:"
              +idpAppUrl;
	SAML2Utils.debug.error(errStr);
        String[] data = {errStr};
        SAML2Utils.logError(Level.INFO, LogUtil.SAE_IDP_ERROR, 
                   data, token, ipaddr, userid, realm, "SAE", null);
        response.sendRedirect(errStr);
        return;
    }

    Map rawattrs = sa.getRawAttributesFromEncodedData(sunData,
                   encryptionSecret);

    // Process Logout
    if (SecureAttrs.SAE_CMD_LOGOUT.equals(
            rawattrs.get(SecureAttrs.SAE_PARAM_CMD))) {
        if (!loggedIn) {
            String errStr = 
               errorUrl+"?errorcode=IDP_5&errorstring=Logout_NoSession";
	    SAML2Utils.debug.error(errStr);
            String[] data = {errStr, rawattrs.toString()};
            SAML2Utils.logError(Level.INFO, LogUtil.SAE_IDP_ERROR, 
                   data, token, ipaddr, userid, realm, "SAE", null);
            response.sendRedirect(errStr);
            return;
        } else {
            Map decodeMap = sa.verifyEncodedString(sunData, secret,
                    encryptionSecret);
            if (decodeMap == null) {
                String errStr = 
                 errorUrl+"?errorcode=IDP_6&errorstring=Logout_VerifyFailed";
	        SAML2Utils.debug.error(errStr);
                String[] data = {errStr, rawattrs.toString()};
                SAML2Utils.logError(Level.INFO, LogUtil.SAE_IDP_ERROR, 
                   data, token, ipaddr, userid, realm, "SAE", null);
                response.sendRedirect(errStr);
                return;
            }
            String sloBinding = SAML2Constants.HTTP_REDIRECT;
                
            String relayState = (String) decodeMap.get(
                                    SecureAttrs.SAE_PARAM_APPSLORETURNURL);
            String sloUrl = appBase+
                             "IDPSloInit?binding="
                             +sloBinding+"&RelayState="+relayState;
	    SAML2Utils.debug.message("SA_IDP.jsp :Success:redirect to SLO");
            String[] data = {rawattrs.toString()};
            SAML2Utils.logAccess(Level.INFO, LogUtil.SAE_IDP_SUCCESS, 
                   data, token, ipaddr, userid, realm, "SAE", null);
            // Comment this  redirect and uncomment the href for debugging.
            // The href below will take effect.
            response.sendRedirect(sloUrl);
            return;
/*
%>
           <a href=<%=sloUrl%>>Redirect me : SLO initiation URL</a>
<%
*/
        }
    }
   
    

    // Check if user is switched : will destroy session and treat like new login
    // Alternate scenarios : (i) ignore userid (ii) treat as error
    if (loggedIn) {
        String requestedUserid = 
           (String) rawattrs.get(SecureAttrs.SAE_PARAM_USERID);
        if (requestedUserid != null) {
            // TODO
            requestedUserid = "id=" + requestedUserid.toLowerCase() + ",";
            loggedinPrincipal = loggedinPrincipal.toLowerCase();
            if (!loggedinPrincipal.startsWith(requestedUserid)) {
                Map verifiedattrs = null;
                if ((verifiedattrs = sa.verifyEncodedString(
                        sunData, secret, encryptionSecret)) 
                     == null) {
                    String errStr = 
                      errorUrl+"SA_IDP:errcode=4,verifyEncodedString failed.";
	            SAML2Utils.debug.error(errStr);
                    String[] data = {errStr, rawattrs.toString()};
                    SAML2Utils.logError(Level.INFO, LogUtil.SAE_IDP_ERROR, 
                       data, token, ipaddr, userid, realm, "SAE", null);
                    response.sendRedirect(errStr);
                    return;
                } else {
	            SAML2Utils.debug.message(
                        "SA_IDP:Destroy ssotoken to switch user.");
                    provider.invalidateSession(token, request, response);
                    loggedIn = false;
                }
                rawattrs = verifiedattrs;
            } else {
                String requestedAuthLevel = 
                    (String) rawattrs.get(SecureAttrs.SAE_PARAM_AUTHLEVEL);
                if ((requestedAuthLevel != null) &&
                    (loggedinAuthLevel != null) &&
                    (Integer.parseInt(requestedAuthLevel) >
                        Integer.parseInt(loggedinAuthLevel)))
                {
                    forceAuth = true;
                }
            }
        }
    }
    
    // Process fresh login/1st time FM access/switched user
    if(!loggedIn || forceAuth) {
        if (SAML2Utils.debug.messageEnabled()) {
	    SAML2Utils.debug.message(
                "SA_IDP:Fresh session needs to be created or the existing " +
                "session needs to be updated.");
        }
        String qs = Utils.queryStringFromRequest(request); 
        if (qs != null && qs.length() > 0) {
            gotoUrl = gotoUrl + "?" + qs;
        } 

        String redirectUrl;
        HashMap postParams = new HashMap();
        redirectUrl = appBase + "UI/Login";
        postParams.put("module", "SAE");
        postParams.put("forward", "true");
        postParams.put("goto", gotoUrl);
        postParams.put(SecureAttrs.SAE_PARAM_DATA, sunData);
        postParams.put(SAML2Constants.SAE_REALM, realm);
        postParams.put(SAML2Constants.SAE_IDP_ENTITYID ,
            idpEntityId);
        postParams.put(SAML2Constants.SAE_IDPAPP_URL,
            idpAppUrl);
        if (forceAuth) {
            postParams.put("ForceAuth", "true");
        }
        
        String[] data = {rawattrs.toString()};
        SAML2Utils.logAccess(Level.INFO, LogUtil.SAE_IDP_AUTH, 
                   data, token, ipaddr, userid, realm, "SAE", null);
        try {
            Utils.redirect(response, new PrintWriter(out, true), redirectUrl, postParams, action);
        } catch (Exception ex) {
            String errStr = 
              errorUrl+"SA_IDP:errcode=5,redirect to Login failed:"+ex;
	    SAML2Utils.debug.error(errStr);
            String[] data1 = {errStr, rawattrs.toString()};
            SAML2Utils.logError(Level.INFO, LogUtil.SAE_IDP_ERROR, 
               data1, token, ipaddr, userid, realm, "SAE", null);
            response.sendRedirect(errStr);
        }
        return;
    }


    // Valid FM session - simply verify sun.data and add attributes to session
    if ((rawattrs = sa.verifyEncodedString(sunData, secret, encryptionSecret))
            == null) 
    {
        
	if (SAML2Utils.debug.messageEnabled()) {
	    SAML2Utils.debug.message(
               "SA_IDP:errcode=3,could not verify:"+sunData);
        }
        String errStr = errorUrl+"?errorcode=IDP_7&errorstring=Invalid_sun.data";
	SAML2Utils.debug.error(errStr+" attrs :");
        String[] data = {errStr, sunData};
        SAML2Utils.logError(Level.INFO, LogUtil.SAE_IDP_ERROR, 
                   data, token, ipaddr, userid, realm, "SAE", null);
        response.sendRedirect(errStr);
            
        return;
    }

    // Process fresh login/1st time FM access/switched user
    // sun.data verified okay - update session
    Iterator iter = rawattrs.entrySet().iterator();
    while(iter.hasNext()) {
        Map.Entry entry = (Map.Entry)iter.next();
        String key = (String)entry.getKey();
        String value = (String)entry.getValue();
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("SA_IDP:Session Properties set: " 
                 + key + "= " + value);
        }
        if(key.equals(SecureAttrs.SAE_PARAM_USERID)) {
            continue;
        }
        String[] values = { value };
        provider.setProperty(token, key, values);
    }
  
    String spAppUrl = (String) rawattrs.get(SecureAttrs.SAE_PARAM_SPAPPURL);
    String spEntityID = null;
    String spFMUrl = null;
    try {
        spEntityID = getTrustedSP(mm, realm, idpEntityId, spAppUrl);
        if (spEntityID == null) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(
                    "SA_IDP.jsp: errorcode=IDP_8:no matching SP is found.");
            }
            String errStr =
                errorUrl+"?errorcode=IDP_8&errorstring=Error_SP_metadata";
            SAML2Utils.debug.error(errStr);
            response.sendRedirect(errStr);
            return;
        }
        SPSSOConfigElement spConfig = mm.getSPSSOConfig(realm, spEntityID); 
        spFMUrl = SAML2Utils.getAttributeValueFromSPSSOConfig(
            spConfig, SAML2Constants.SAE_SP_URL);
    } catch (SAML2MetaException se) {
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(
                "SA_IDP.jsp: errorcode=IDP_8:couldn't obtain SP's metadata", se);
        }
    }
    String spUrl = null;
    if (spFMUrl == null || spFMUrl.length() == 0) {
/* 
        String errStr = errorUrl+"?errorcode=IDP_8&errorstring=Error_SP_metadata";
        SAML2Utils.debug.error(errStr);
        response.sendRedirect(errStr);
        return;
*/
        spUrl = (String) rawattrs.get(SecureAttrs.SAE_PARAM_SPAPPURL);
    } else {
    if (spFMUrl.indexOf("?") != -1) {
        spUrl = spFMUrl+"&"+
                    SecureAttrs.SAE_PARAM_SPAPPURL+"="+
                    rawattrs.get(SecureAttrs.SAE_PARAM_SPAPPURL);
    } else {
        spUrl = spFMUrl+"?"+
                    SecureAttrs.SAE_PARAM_SPAPPURL+"="+
                    rawattrs.get(SecureAttrs.SAE_PARAM_SPAPPURL);
    }
    }
    String ssoUrl = appBase + "saml2/jsp/idpSSOInit.jsp?metaAlias="
        + idpMetaAlias + "&spEntityID=" + spEntityID 
        + "&" + SAML2Constants.BINDING + "=" + SAML2Constants.HTTP_ARTIFACT
        + "&NameIDFormat=transient"
        + "&RelayState=" + java.net.URLEncoder.encode(spUrl);

    if (SAML2Utils.debug.messageEnabled()) {
        SAML2Utils.debug.message("SA_IDP:INITIATE SSO:"+spUrl );
    }

    SAML2Utils.debug.message("SA_IDP.jsp :Success:redirect to SSO");
    String[] data = {rawattrs.toString()};
    SAML2Utils.logAccess(Level.INFO, LogUtil.SAE_IDP_SUCCESS, 
                   data, token, ipaddr, userid, realm, "SAE", null);
    // Comment the sendRedirect below and uncomment the below br blocks to debug
    // The href at the bottom will take effect.
    response.sendRedirect(ssoUrl);

/*
%>
    <br> DEBUG : We are in SAE handler deployed on FM in IDP role.
    <br> Click <a href=<%=ssoUrl%>>here </a> to continue SSO with SP.
    <br> Edit saml2/jsp/SA_IDP.jsp and uncomment the "send.Redirect(..)" line to execute a redirect automatically so that the user doesnt see this debug page.
<%
*/
%>
