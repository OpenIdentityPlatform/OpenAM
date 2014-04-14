/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2014 ForgeRock AS. All Rights Reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package com.sun.identity.saml2.plugins;

import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.cot.CircleOfTrustDescriptor;
import com.sun.identity.cot.COTException;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.jaxb.assertion.AttributeElement;
import com.sun.identity.saml2.jaxb.assertion.AttributeValueElement;
import com.sun.identity.saml2.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.ExtensionsType;
import com.sun.identity.saml2.jaxb.metadataattr.EntityAttributesElement;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorElement;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.profile.IDPSSOUtil;
import com.sun.identity.saml2.profile.SPSSOFederate;
import com.sun.identity.saml2.profile.SPCache;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.RequestedAuthnContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang.StringUtils;

/**
 * This class <code>SAML2IDPProxyFRImpl</code> is used to find a preferred Identity
 * Authenticating provider to proxy the authentication request. It might use an external
 * JSP page to interact with the user agent
 */
public class SAML2IDPProxyFRImpl implements SAML2IDPFinder {

    public static String IDP_FINDER_ENABLED_IN_SP = "idpFinderEnabled";

    public static String SESSION_ATTR_NAME_IDP_LIST = "_IDPLIST_";
    public static String SESSION_ATTR_NAME_RELAYSTATE = "_RELAYSTATE_";
    public static String SESSION_ATTR_NAME_SPREQUESTER = "_SPREQUESTER_";
    public static String SESSION_ATTR_NAME_REQAUTHNCONTEXT = "_REQAUTHNCONTEXT_";

    SPSSODescriptorElement spSSODescriptor = null;
    String relayState = "";
    String binding = "";

    /*
     * Constructor.
     */
    public SAML2IDPProxyFRImpl() {
    }

    public static String className = "SAML2IDPProxyFRImpl.";

    /**
     * Returns a list of preferred IDP providerIDs.
     * @param authnRequest original authnrequest
     * @param hostProviderID ProxyIDP providerID.
     * @param realm Realm
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @return a list of providerID's of the authenticating providers to be
     *     proxied or <code>null</code> to disable the proxying and continue
     *     for the localauthenticating provider.
     * @exception SAML2Exception if error occurs.
     */
    public List getPreferredIDP(
            AuthnRequest authnRequest,
            String hostProviderID,
            String realm,
            HttpServletRequest request,
            HttpServletResponse response) throws SAML2Exception {

        // Entering the class and method
        String methodName = "getPreferredIDP";
        String classMethod = className + methodName + ":";

        debugMessage(methodName, "Entering.");

        Boolean isIdpFinderForAllSPsEnabled = isIDPFinderForAllSPs(realm, hostProviderID);

        // Start the logic to obtain the list of preferred IdPs
        try {
            // Inititate the metadata manager
            SAML2MetaManager sm = new SAML2MetaManager();
            if (sm == null) {
                throw new SAML2Exception(
                        SAML2Utils.bundle.getString("errorMetaManager"));
            }

            // Obtain the SP configuration
            try {
                spSSODescriptor = IDPSSOUtil.metaManager.getSPSSODescriptor(realm, authnRequest.getIssuer().getValue().toString());
            } catch (SAML2MetaException sme) {
                SAML2Utils.debug.error(classMethod, sme);
                spSSODescriptor = null;
            }

            // Get the relay state from the request, if exists
            relayState = request.getParameter(SAML2Constants.RELAY_STATE);
            binding = SAML2Constants.HTTP_REDIRECT;
            if (request.getMethod().equals("POST")) {
                binding = SAML2Constants.HTTP_POST;
            }

            // Read the local metadata of the SP that made the request
            SPSSOConfigElement spEntityCfg =
                    sm.getSPSSOConfig(realm, authnRequest.getIssuer().getValue());
            Map spConfigAttrsMap = null;
            if (spEntityCfg != null) {
                spConfigAttrsMap = SAML2MetaUtils.getAttributes(spEntityCfg);
            }

            // Check if the local configuration of the remote SP wants to use
            // the Introduction Cookie
            Boolean isIntroductionForProxyingEnabled = false;
            String useIntroductionForProxying =
                    SPSSOFederate.getParameter(spConfigAttrsMap,
                    SAML2Constants.USE_INTRODUCTION_FOR_IDP_PROXY);
            if (useIntroductionForProxying != null)
                isIntroductionForProxyingEnabled = 
                        useIntroductionForProxying.equalsIgnoreCase("true");

            // Check if the local configuration of the remote SP wants to use
            // the IDP Finder
            Boolean isIdPFinderEnabled = false;
            String idpFinderEnabled = SPSSOFederate.getParameter(spConfigAttrsMap,
                    IDP_FINDER_ENABLED_IN_SP);
            if (idpFinderEnabled != null)
                isIdPFinderEnabled = idpFinderEnabled.equalsIgnoreCase("true");

            String idpFinderJSP = getIDPFinderJSP(realm, hostProviderID);

            // providerIDs will contain the list of IdPs to return from this method
            List providerIDs = new ArrayList();

            // If the SP doesn't want to use the Introduction cookie and does not
            // want to use the IdP Finder. i.e. just use the manual list in the
            // extended metadata
            if (!isIntroductionForProxyingEnabled && !isIdPFinderEnabled
                    && !isIdpFinderForAllSPsEnabled) {
                debugMessage(methodName, " idpFinder wil use the static list of the SP");
                List<String> proxyIDPs = null;
                if (spConfigAttrsMap != null && !spConfigAttrsMap.isEmpty()) {
                    proxyIDPs = (List<String>) spConfigAttrsMap.get(SAML2Constants.IDP_PROXY_LIST);
                }
                
                debugMessage(methodName, " List from the configuration: " + proxyIDPs);

                if (proxyIDPs == null || proxyIDPs.isEmpty()) {
                    SAML2Utils.debug.error("SAML2IDPProxyImpl.getPrefferedIDP:"
                            + "Preferred IDPs are null.");
                    return null;
                }


                // If there are several IdPs listed in the SP configuration,
                // give the user the chance to select one interactively
                if (proxyIDPs.size() > 1) {
                    String idpListSt = selectIDPBasedOnLOA(proxyIDPs, realm, authnRequest);
                    // Construct the IDPFinder URL to redirect to
                    String idpFinder = getRedirect(request, idpFinderJSP);
                    // Generate the requestID
                    String requestID = SAML2Utils.generateID();
                    // Store the important stuff and the session parameters so the
                    // idpFinderImplemenatation can read them and process them
                    storeSessionParamsAndCache(request, idpListSt, authnRequest,
                            hostProviderID, realm, requestID);

                    debugMessage(methodName, ": Redirect url = " + idpFinder);
                    response.sendRedirect(idpFinder);
                    
                    // return something different than null
                    providerIDs.add(requestID);
                    debugMessage(methodName, " Redirected successfully");
                    return providerIDs;
                }
                
                providerIDs.add(proxyIDPs.iterator().next());
                return providerIDs;
            }

            // If the SP wants to use the IdPFinder or it is globally enabled 
            // and it does not want to use the introduction cookie
            if (!isIntroductionForProxyingEnabled && (isIdPFinderEnabled
                    || isIdpFinderForAllSPsEnabled)) {
                debugMessage(methodName, "SP wants to use IdP Finder");
                String idpListSt = idpList(authnRequest, realm);
                if (!idpListSt.trim().isEmpty()) {
                    // Construct the IDPFinder URL to redirect to
                    String idpFinder = getRedirect(request, idpFinderJSP);

                    // Generate the requestID
                    String requestID = SAML2Utils.generateID();
                    // Store the important stuff and the session parameters so the
                    // idpFinderImplemenatation can read them and process them
                    storeSessionParamsAndCache(request, idpListSt, authnRequest,
                            hostProviderID, realm, requestID);

                    debugMessage(methodName, ": Redirect url = " + idpFinder);
                    response.sendRedirect(idpFinder);
                    // return something different than null
                    providerIDs.add(requestID);
                    debugMessage(methodName, " Redirected successfully");
                    return providerIDs;
                } else {
                    return null;
                }
            } else {
                // IDP Proxy with introduction cookie
                List cotList = (List) spConfigAttrsMap.get("cotlist");
                String cotListStr = (String) cotList.iterator().next();
                CircleOfTrustManager cotManager = new CircleOfTrustManager();
                CircleOfTrustDescriptor cotDesc =
                        cotManager.getCircleOfTrust(realm, cotListStr);
                String readerURL = cotDesc.getSAML2ReaderServiceURL();
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(classMethod + "SAMLv2 idp"
                            + "discovery reader URL = " + readerURL);
                }
                if (readerURL != null && (!readerURL.equals(""))) {
                    String rID = SAML2Utils.generateID();
                    String redirectURL =
                            SAML2Utils.getRedirectURL(readerURL, rID, request);
                    if (SAML2Utils.debug.messageEnabled()) {
                        SAML2Utils.debug.error(classMethod
                                + "Redirect url = " + redirectURL);
                    }
                    if (redirectURL != null) {
                        response.sendRedirect(redirectURL);
                        Map aMap = new HashMap();
                        SPCache.reqParamHash.put(rID, aMap);
                        providerIDs.add(rID);
                        return providerIDs;
                    }
                }
            }
            return null;
        } catch (SAML2MetaException ex) {
            SAML2Utils.debug.error(classMethod
                    + "meta Exception in retrieving the preferred IDP", ex);
            return null;
        } catch (COTException sme) {
            SAML2Utils.debug.error(classMethod
                    + "Error retreiving COT ", sme);
            return null;
        } catch (Exception e) {
            SAML2Utils.debug.error(classMethod
                    + "Exception in retrieving the preferred IDP", e);
            return null;
        }
    }


    private void debugMessage(String methodName, String message) {

        String classMethod = "SAML2IDPPRoxyFRImpl." + methodName + ":";

        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(classMethod + message);
        }
    }

    private String idpList(
            AuthnRequest authnRequest,
            String realm)
    {
        String classMethod = "idpList";
        
        try {            
            List<String> idpList = SAML2Utils.getSAML2MetaManager().getAllRemoteIdentityProviderEntities(realm);
            return selectIDPBasedOnLOA(idpList, realm, authnRequest);
        } catch (SAML2MetaException me) {
            debugMessage(classMethod, "SOmething went wrong: " + me);
            return null;
        }
    }

    private String selectIDPBasedOnLOA(List<String> idpList, String realm, AuthnRequest authnRequest) {

        String classMethod = "selectIdPBasedOnLOA";
        EntityDescriptorElement idpDesc = null;
        Set authnRequestContextSet = null;
        String idps = "";

        try {
            RequestedAuthnContext requestedAuthnContext = authnRequest.getRequestedAuthnContext();
            if (requestedAuthnContext == null) {
                //Handle the special case when the original request did not contain any Requested AuthnContext:
                //In this case we just simply return all the IdPs as each one should support a default AuthnContext.
                return StringUtils.join(idpList, " ");
            }
            List listOfAuthnContexts = requestedAuthnContext.getAuthnContextClassRef();
            debugMessage(classMethod, "listofAuthnContexts: " + listOfAuthnContexts);

            try {
                authnRequestContextSet = new HashSet(listOfAuthnContexts);
            } catch (Exception ex1) {
                authnRequestContextSet = new HashSet();
            }

            if ((idpList != null) && (!idpList.isEmpty())) {
                Iterator idpI = idpList.iterator();
                while (idpI.hasNext()) {
                    String idp = (String) idpI.next();
                    debugMessage(classMethod, "IDP is: " + idp);
                    idpDesc = SAML2Utils.getSAML2MetaManager().getEntityDescriptor(realm, idp);
                    if (idpDesc != null) {
                        ExtensionsType et = idpDesc.getExtensions();
                        if (et != null) {
                            debugMessage(classMethod, "Extensions found for idp: " + idp);
                            List idpExtensions = et.getAny();
                            if (idpExtensions != null || !idpExtensions.isEmpty()) {
                                debugMessage(classMethod, "Extensions content found for idp: " + idp);
                                Iterator idpExtensionsI = idpExtensions.iterator();
                                while (idpExtensionsI.hasNext()) {
                                    EntityAttributesElement eael = (EntityAttributesElement) idpExtensionsI.next();
                                    if (eael != null) {
                                        debugMessage(classMethod, "Entity Attributes found for idp: " + idp);
                                        List attribL = eael.getAttributeOrAssertion();
                                        if (attribL != null || !attribL.isEmpty()) {
                                            Iterator attrI = attribL.iterator();
                                            while (attrI.hasNext()) {
                                                AttributeElement ae = (AttributeElement) attrI.next();
                                                // TODO: Verify what type of element this is (Attribute or assertion)
                                                // For validation purposes
                                                List av = ae.getAttributeValue();
                                                if (av != null || !av.isEmpty()) {
                                                    debugMessage(classMethod, "Attribute Values found for idp: " + idp);
                                                    Iterator avI = av.iterator();
                                                    while (avI.hasNext()) {
                                                        AttributeValueElement ave = (AttributeValueElement) avI.next();
                                                        if (ave != null) {
                                                            List contentL = ave.getContent();
                                                            debugMessage(classMethod, "Attribute Value Elements found for idp: " + idp
                                                                    + "-->" + contentL);
                                                            if (contentL != null || !contentL.isEmpty()) {
                                                                Set idpContextSet = trimmedListToSet(contentL);
                                                                debugMessage(classMethod, "idpContextSet = " + idpContextSet);
                                                                idpContextSet.retainAll(authnRequestContextSet);
                                                                if (idpContextSet != null && !idpContextSet.isEmpty()) {
                                                                    idps = idp + " " + idps;
                                                                    debugMessage(classMethod, "Extension Values found for idp " + idp
                                                                            + ": " + idpContextSet);
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            debugMessage(classMethod, " No extensions found for IdP " + idp);
                        }
                    } else {
                        debugMessage(classMethod, "Configuration for the idp " + idp + " was not found in this system");
                    }
                }
            }
        } catch (SAML2MetaException me) {
            debugMessage(classMethod, "SOmething went wrong: " + me);
        }

        debugMessage(classMethod, " IDPList returns: " + idps);
        return idps.trim();

    }

    private String selectIDPBasedOnAuthContext(List idpList, String realm, AuthnRequest authnRequest) {

        String classMethod = "selectIdPBasedOnLOA";
        EntityDescriptorElement idpDesc = null;
        Set authnRequestContextSet = null;
        String idps = "";

        try {

            List listOfAuthnContexts = authnRequest.getRequestedAuthnContext().getAuthnContextClassRef();
            debugMessage(classMethod, "listofAuthnContexts: " + listOfAuthnContexts);

            try {
                authnRequestContextSet = new HashSet(listOfAuthnContexts);
            } catch (Exception ex1) {
                authnRequestContextSet = new HashSet();
            }

            if ((idpList != null) && (!idpList.isEmpty())) {
                Iterator idpI = idpList.iterator();
                while (idpI.hasNext()) {
                    String idp = (String) idpI.next();
                    debugMessage(classMethod, "IDP is: " + idp);
                    List supportedAuthnContextsbyIDP =
                            getSupportedAuthnContextsByIDP(realm, idp);
                    if (supportedAuthnContextsbyIDP != null) {
                        debugMessage(classMethod, "Standard Authn Contexts found for idp: " + idp);
                        Set idpContextSet = trimmedListToSet(supportedAuthnContextsbyIDP);
                        debugMessage(classMethod, "idpContextSet = " + idpContextSet);
                        idpContextSet.retainAll(authnRequestContextSet);
                        if (idpContextSet != null && !idpContextSet.isEmpty()) {
                            idps = idp + " " + idps;
                            debugMessage(classMethod, "Standard Authn Contexts found for idp " + idp
                                    + ": " + idpContextSet);
                        }

                    } else {
                        debugMessage(classMethod, "The IdP" + idp + " has no standard authentication"
                                + " contexts configured");
                    }
                }
            }
        } catch (Exception me) {
            SAML2Utils.debug.error(classMethod + "Error when trying to get the idp's by standard Authn Context: " + me);
        }

        debugMessage(classMethod, " IDPList returns: " + idps);
        return idps.trim();

    }

    private Set trimmedListToSet(List list) {
        Set trimmedSet= new HashSet();
        String classMethod = "trimmedListToSet";
        Iterator I = list.iterator();

        while (I.hasNext()) {
          trimmedSet.add(I.next().toString().trim());
          debugMessage(classMethod, " element added to Set : ");
        }
        return trimmedSet;
    }

    private String buildReturnURL(String requestID,
            HttpServletRequest request) {

        String methodName = "buildReturnURL";
        StringBuffer sb = new StringBuffer();
        String baseURL = request.getScheme() + "://" +
                request.getHeader("host") + 
                request.getRequestURI();
        String qs = request.getQueryString();
        if (qs != null && !qs.isEmpty()) {
            baseURL = baseURL + "?" + qs;
        }
        StringBuffer retURL = new StringBuffer().append(baseURL);
        if (retURL.toString().indexOf("?") == -1) {
            retURL.append("?");
        } else {
            retURL.append("&");
        }
        retURL.append("requestID=").append(requestID);
        sb.append(retURL);
        String returnURL = sb.toString();
        debugMessage(methodName, " ReturnURL is: " + returnURL);
        return returnURL;
    }

    private void storeSessionParamsAndCache(
            HttpServletRequest request,
            String idpListSt,
            AuthnRequest authnRequest,
            String hostProviderID,
            String realm,
            String requestID) {

        String methodName = "storeSessionParamsAndCache";
        HttpSession hts = request.getSession();

        hts.setAttribute(SESSION_ATTR_NAME_IDP_LIST, idpListSt);
        debugMessage(methodName, " Setting " + SESSION_ATTR_NAME_IDP_LIST + " = " + idpListSt);
        hts.setAttribute(SESSION_ATTR_NAME_RELAYSTATE, buildReturnURL(requestID, request));
        debugMessage(methodName, " Setting " + SESSION_ATTR_NAME_RELAYSTATE);
        hts.setAttribute(SESSION_ATTR_NAME_SPREQUESTER, authnRequest.getIssuer().getValue().toString());
        debugMessage(methodName, " Setting " + SESSION_ATTR_NAME_SPREQUESTER);
        RequestedAuthnContext requestedAuthnContext = authnRequest.getRequestedAuthnContext();
        hts.setAttribute(SESSION_ATTR_NAME_REQAUTHNCONTEXT,
                requestedAuthnContext == null ? null : requestedAuthnContext.getAuthnContextClassRef());
        debugMessage(methodName, " Setting " + SESSION_ATTR_NAME_REQAUTHNCONTEXT);

        // Save the important param in the reqParamHash so we can
        // locate them when we return to the IDPSSOFederate.

        Map paramsMap = new HashMap();
        paramsMap.put("authnReq", authnRequest);
        paramsMap.put("spSSODescriptor", spSSODescriptor);
        paramsMap.put("idpEntityID", hostProviderID);
        paramsMap.put("realm", realm);
        paramsMap.put("relayState", relayState);
        paramsMap.put("binding", binding);
        SPCache.reqParamHash.put(requestID, paramsMap);

    }

        private String getRedirect(
            HttpServletRequest request,
            String idpFinderImplementation) {

        String methodName = "getRedirect";

        // Get the base URL and construct the IdP Finder URL
        String baseURL = request.getScheme() + "://"
                + request.getHeader("host")
                + request.getContextPath();
        String idpFinder = baseURL + "/" + idpFinderImplementation;

        debugMessage(methodName, ": Redirect url = " + idpFinder);
        return idpFinder;

    }

     /**
     * Returns  <code>true</code> or <code>false</code>
     * depending if the flag isIDPFinderForAllSPs is set in the
     * IDP Extended metadata
     *
     * @param realm the realm name
     * @param idpEntityID the entity id of the identity provider
     *
     * @return the <code>true/false</code>
     * @exception SAML2Exception if the operation is not successful
     */
    private Boolean isIDPFinderForAllSPs(
                                 String realm, String idpEntityID)
        throws SAML2Exception {
        String methodName = "isIDPFinderForAllSPs";
       
        Boolean isIdpFinderForAllSPsEnabled = false;

        try {
            String idpFinderForAllSPs = IDPSSOUtil.getAttributeValueFromIDPSSOConfig(
                realm, idpEntityID, SAML2Constants.ENABLE_PROXY_IDP_FINDER_FOR_ALL_SPS);
            if (idpFinderForAllSPs != null && !idpFinderForAllSPs.isEmpty()) {
                debugMessage(methodName, "idpFinderForAllSPs is: " +  idpFinderForAllSPs);
                isIdpFinderForAllSPsEnabled = idpFinderForAllSPs.equalsIgnoreCase("true");
            } else isIdpFinderForAllSPsEnabled  = false;
        } catch (Exception ex) {
            SAML2Utils.debug.error(methodName +
                "Unable to get IDP Proxy Finder.", ex);
            throw new SAML2Exception(ex);
        }

        return isIdpFinderForAllSPsEnabled;
    }


    /**
     * Returns the IDP Finder JSP configured in the extended metadata
     *
     * @param realm the realm name
     * @param idpEntityID the entity id of the identity provider
     *
     * @return the IDP Finder JSP
     * @exception SAML2Exception if the operation is not successful
     */
    private String getIDPFinderJSP(
                                 String realm, String idpEntityID)
        throws SAML2Exception {
        String methodName = "getIDPFinderJSP";


        String idpFinderJSP = SAML2Constants.DEFAULT_PROXY_IDP_FINDER;

        try {
            idpFinderJSP = IDPSSOUtil.getAttributeValueFromIDPSSOConfig(
                realm, idpEntityID, SAML2Constants.PROXY_IDP_FINDER_JSP);
            if (idpFinderJSP != null && !idpFinderJSP.isEmpty()) {
                debugMessage(methodName, "idpFinderForAllSPs is: " +  idpFinderJSP);
            }
        } catch (Exception ex) {
            SAML2Utils.debug.error(methodName +
                "Unable to get IDP Proxy Finder.", ex);
            throw new SAML2Exception(ex);
        }

        return idpFinderJSP;
    }



     public List getAttributeListValueFromIDPSSOConfig(
                             String realm,
                             String hostEntityId,
                             String attrName)
    {
        String classMethod = "IDPSSOUtil.getAttributeValueFromIDPSSOConfig: ";
        List result = null;
        try {

            IDPSSOConfigElement config = SAML2Utils.getSAML2MetaManager().getIDPSSOConfig(
                                          realm, hostEntityId);
            Map attrs = SAML2MetaUtils.getAttributes(config);
            List value = (List) attrs.get(attrName);
            if (value != null && value.size() != 0) {
                result = value;
            }
        } catch (SAML2MetaException sme) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(classMethod +
                   "get IDPSSOConfig failed:", sme);
            }
            result = null;
        }
        return result;
    }

     
     public List getSupportedAuthnContextsByIDP(String realm, String hostEntityId)
     {
        List authnContextList = null;
        List supportedClassRef = null;

        supportedClassRef =
                getAttributeListValueFromIDPSSOConfig(
                realm,
                hostEntityId,
                SAML2Constants.IDP_AUTHNCONTEXT_CLASSREF_MAPPING);

        if (supportedClassRef != null && !supportedClassRef.isEmpty()) {
            Iterator it = supportedClassRef.iterator();
            while (it.hasNext()) {
                StringTokenizer tokenizer =
                        new StringTokenizer((String) it.next(), "|");
                if (tokenizer.countTokens() > 1) {
                    authnContextList.add(tokenizer.nextToken());
                }
            }
        }
        return authnContextList;
    }
}
