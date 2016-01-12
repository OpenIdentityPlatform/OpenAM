/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: FSLoginHelper.java,v 1.5 2008/06/25 05:46:54 qcheng Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */

package com.sun.identity.federation.services;

import com.sun.identity.cot.COTException;
import com.sun.identity.cot.CircleOfTrustDescriptor;
import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.message.FSAuthnRequest;
import com.sun.identity.federation.message.FSAuthnRequestEnvelope;
import com.sun.identity.federation.message.FSIDPList;
import com.sun.identity.federation.message.common.IDPEntries;
import com.sun.identity.federation.message.common.IDPEntry;
import com.sun.identity.federation.message.common.RequestAuthnContext;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.liberty.ws.meta.jaxb.IDPDescriptorType;
import com.sun.identity.liberty.ws.meta.jaxb.SPDescriptorType;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.shared.encode.URLEncDec;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;

/**
 * Helper class for handling login process at Service Provider.
 */
public class FSLoginHelper {
    
    private static String headerKey = IFSConstants.HEADER_KEY;
    private static String responseDataKey = IFSConstants.RESPONSE_DATA_KEY;
    private static String URLKey = IFSConstants.URL_KEY;
    private String interSiteURL = null;
    private static String authnReqIDKey = IFSConstants.AUTH_REQUEST_ID;
    private static String providerIDKey = IFSConstants.PROVIDER_ID_KEY;
    private String amserverURI = null;
    private String authContextString = null;
    private String respondWithString = null;
    private boolean forceAuthn;
    private boolean isPassive;
    private String nameIDPolicy = null;
    private String realm = null;
    private String hostEntityID = null;
    private BaseConfigType hostConfig = null;
    private SPDescriptorType hostDescriptor = null;
    private List cotList = null; 
    private String protocolProfile = null;
    private static IDFFMetaManager metaManager =null;
    private String errorPage = null; 
    private HttpServletRequest request = null;
    private boolean isPassiveQuery = false;
    private String actionOnNoFedCookie = null;
    private String anonymousOnetime = null;

    static {
        metaManager = FSUtils.getIDFFMetaManager();
    }
    
    /**
     * Creates a new <code>FSLoginHelper</code> object.
     *
     * @param request HTTP Servlet request.
     */
    public FSLoginHelper(HttpServletRequest request) {
        this.request = request;
        amserverURI = FSServiceUtils.getBaseURL(request);
        interSiteURL = amserverURI + "/" + IFSConstants.INTERSITE_URL;
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSLoginHepler::Constructor called. Setting "
                + "BaseURL to " + amserverURI 
                + request.getParameter("metaAlias"));
        }
        String passiveQuery = request.getParameter(
                                IFSConstants.IS_PASSIVE_QUERY);
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSLoginHepler::isPassive query param"
                + passiveQuery);
        }
        if ((passiveQuery != null) && (passiveQuery.equals("true") )) {
            isPassiveQuery = true;
        }

        actionOnNoFedCookie = request.getParameter(
            IFSConstants.ACTION_ON_NO_FED_COOKIE);

        if (actionOnNoFedCookie == null || actionOnNoFedCookie.length() == 0) {
            actionOnNoFedCookie = IFSConstants.PASSIVE;
        }

        anonymousOnetime = request.getParameter(IFSConstants.ANONYMOUS_ONETIME);
    }
    
    private void setMetaInfo(String metaAlias, String authLevel) 
        throws FSLoginHelperException
    {
       try {
           realm = IDFFMetaUtils.getRealmByMetaAlias(metaAlias);
            if (metaManager != null) {
                hostEntityID = metaManager.getEntityIDByMetaAlias(metaAlias);
                hostDescriptor = metaManager.getSPDescriptor(
                    realm, hostEntityID);
                hostConfig = metaManager.getSPDescriptorConfig(
                    realm, hostEntityID);
            } else {
                FSUtils.debug.error("FSLoginHelper::setMetaInfo " 
                    + "could not get meta manager handle "
                    + "Cannot proceed so throwing error page");
                throw new FSLoginHelperException(
                    "FSLoginHelper:: could not get meta manager handle.");
            }
            if (hostDescriptor == null ) {
                FSUtils.debug.error("FSLoginHelper::setMetaInfo " 
                    + "getHostedProviderByMetaAlias retured null. "
                    + "Cannot proceed so throwing error page");
                throw new FSLoginHelperException("FSLoginHelper:: could not get"
                    + " host provider Descriptor handle.");
            }
            if (hostConfig != null) {
                nameIDPolicy = IDFFMetaUtils.getFirstAttributeValueFromConfig(
                    hostConfig, IFSConstants.NAMEID_POLICY);
                cotList = IDFFMetaUtils.getAttributeValueFromConfig(
                    hostConfig, IFSConstants.COT_LIST);
                respondWithString = IFSConstants.RESPOND_WITH;
                forceAuthn = IDFFMetaUtils.getBooleanAttributeValueFromConfig(
                    hostConfig, IFSConstants.FORCE_AUTHN);
                isPassive = IDFFMetaUtils.getBooleanAttributeValueFromConfig(
                    hostConfig, IFSConstants.IS_PASSIVE);
                protocolProfile = 
                    IDFFMetaUtils.getFirstAttributeValueFromConfig(
                        hostConfig, IFSConstants.SUPPORTED_SSO_PROFILE);
                authContextString = 
                    IDFFMetaUtils.getFirstAttributeValueFromConfig(
                        hostConfig, IFSConstants.DEFAULT_AUTHNCONTEXT);
                errorPage = FSServiceUtils.getErrorPageURL(
                    request, hostConfig, metaAlias);
                if (authLevel != null) {
                    FSUtils.debug.message(
                        "FSLoginHelper()::authLevel not null"); 
                    Map spAuthInfoMap = FSServiceUtils.getSPAuthContextInfo(
                        hostConfig); 
                    Set mapSet = spAuthInfoMap.entrySet();
                    Iterator iter = mapSet.iterator();
                    FSSPAuthenticationContextInfo tmpObj = null;
                    while (iter.hasNext()) { 
                        Map.Entry mapEntry = (Map.Entry)iter.next();
                        tmpObj = 
                            (FSSPAuthenticationContextInfo)mapEntry.getValue();
                        int authLevelInt = Integer.parseInt(authLevel);
                        if (tmpObj.getAuthenticationLevel() == authLevelInt){ 
                            authContextString = 
                                tmpObj.getAuthenticationContext();
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message(
                                    "FSLoginHelper()::Found auth context " 
                                    + authContextString
                                    + "for auth level " + authLevel ); 
                            }
                            break;
                        }
                    }

                }
                if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSLoginHelper()::respondWithString: "
                    + respondWithString );
                FSUtils.debug.message("FSLoginHelper()::providerID: "
                    + hostEntityID );
                FSUtils.debug.message("FSLoginHelper()::forceAuthn: " 
                    + forceAuthn );
                FSUtils.debug.message("FSLoginHelper()::isPassive: " 
                    + isPassive );
                }

            } else {
                FSUtils.debug.error("FSLoginHelper:setMetaInfo failed" 
                    + "host extended meta is null");
                throw new FSLoginHelperException(
                    "FSLoginHelper::could not get host meta config.");
            }
        } catch (IDFFMetaException metaExp) {
            FSUtils.debug.error("FSLoginHelper::setMetaInfo " 
                + "meta management Failed.", metaExp);
            throw new FSLoginHelperException(
                "FSLoginHelper::IDFFMetaException:" +
                metaExp.getMessage());
        } catch (Exception exp) {
            FSUtils.debug.error("FSLoginHelper::setMetaInfo " 
                + "General Exception caugth."  , exp);
            throw new FSLoginHelperException(
                "FSLoginHelper::Exception:" + exp.getMessage());
        }
    }
    
    /**
     * Returns a Map of headers,lrurl/responsedata.
     * @param headers Map of headers
     * @param LRURL relay state url
     * @param authLevel authentication level
     * @param metaAlias meta alias of hosted provider
     * @param remoteEntityID remote provider's entity ID
     * @param isFedCookiePresent if fed cookie present or not
     * @return Map of headers and lrurl/responedata
     * @exception FSLoginHelperException if error occurrs
     */
    public Map createAuthnRequest(
        Map headers, 
        String LRURL, 
        String authLevel,
        String metaAlias,
        String remoteEntityID,
        boolean isFedCookiePresent) 
        throws FSLoginHelperException
    {
            
        Map retHeaderMap = new HashMap();
             
        setMetaInfo(metaAlias, authLevel);
        FSUtils.debug.message("FSLoginHelper.createAuthnRequest(): called");
        FSAuthnRequest authnRequest = null;
        if (remoteEntityID != null) {
            authnRequest = getAuthnReq(headers, LRURL, true);
        } else {
            authnRequest = getAuthnReq(headers, LRURL, false);
        }
   
        if (authnRequest == null ) {
            FSUtils.debug.error(
                "FSLoginHelper.createAuthnRequest()::AuthnRequest is null");
            String redirectURL = errorPage + "&" 
                + IFSConstants.FEDERROR + "="
                + URLEncDec.encode("Unable to create AuthnRequest") + "&"
                + IFSConstants.FEDREMARK + "="
                + URLEncDec.encode(
                    "Please check your Federation Configuration.") ;
            return createMap(redirectURL, null, retHeaderMap);
        }
        String requestID = authnRequest.getRequestID();
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "FSLoginHelper.createAuthnRequest()::RequestID: " + requestID);
        }
        
        FSSessionManager sessMngr = FSSessionManager.getInstance(metaAlias);
        sessMngr.setAuthnRequest(requestID, authnRequest);
        sessMngr.setIDPEntityID(requestID, remoteEntityID);

        String redirectURL = null;
        //check if dontgotothird level domain flag is off is yes
        //if yes then get one provider and and send to intersiteurl
        //else send to third level domain for provider id
        //also check if there is a single idp if yes go to intersite transfer 
        //with providerid
        boolean isSingleIDP = true;
        boolean isSSO = false;
        Set idpSet = getIDPs(metaAlias);
        if (remoteEntityID == null) {
            isSSO = true;
        }
        if (idpSet.size() > 1) {
            isSingleIDP = false;
        }
        
        //*****************
        if (isLECPProfile(headers)){
            FSUtils.debug.message(
                "FSLoginHelper.createAuthnRequest():LECP Request Identified" );
            retHeaderMap.put(
                "content-type", IFSConstants.LECP_CONTENT_TYPE_HEADER);
            String responseData = null;
            try {
                FSAuthnRequestEnvelope authnRequestEnvelope = null;
                boolean isPassive = false;
                
                FSIDPList idpList = null;
                //get IDPList from directory
                
                String assertionConsumerURL = 
                    FSServiceUtils.getAssertionConsumerServiceURL(
                        hostDescriptor, null);
                List idpEntryList = null;
                String idpID = null;
                String idpName = null;
                String idpLocation = null;
                Iterator iter = idpSet.iterator();
                while (iter.hasNext()){
                    idpID = (String)iter.next();
                    if (idpID != null){
                        IDPDescriptorType idpDescr = 
                            metaManager.getIDPDescriptor(realm, idpID);
                        idpLocation = idpDescr.getSingleSignOnServiceURL();
                        if (idpEntryList == null){
                            idpEntryList = new ArrayList();
                        }
                        idpEntryList.add(
                            new IDPEntry(idpID, idpID, idpLocation));
                    }
                }
                IDPEntries idpEntries = new IDPEntries(idpEntryList);
                idpList = new FSIDPList(idpEntries, null);
                int minorVersion = FSServiceUtils.getMinorVersion(
                    hostDescriptor.getProtocolSupportEnumeration());
                idpList.setMinorVersion(minorVersion);
                authnRequest.setMinorVersion(minorVersion);
                if (FSServiceUtils.isSigningOn ()) {
                    if (hostDescriptor.isAuthnRequestsSigned())
                    {
                        authnRequest.signXML(
                            IDFFMetaUtils.getFirstAttributeValueFromConfig(
                                hostConfig, IFSConstants.SIGNING_CERT_ALIAS));
                    }
                }
                authnRequestEnvelope = new FSAuthnRequestEnvelope(
                            authnRequest, 
                            hostEntityID, 
                            hostEntityID, 
                            assertionConsumerURL, 
                            idpList, 
                            isPassive);
                authnRequestEnvelope.setMinorVersion(minorVersion);
                
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSLoginHelper.createAuthnRequest: "
                        + "AuthnRequestEnvelope: " 
                        + authnRequestEnvelope.toXMLString() );
                }
                responseData = authnRequestEnvelope.toXMLString();
               // responseData = authnRequestEnvelope.toBASE64EncodedString();
            } catch (Exception e) {                
                FSUtils.debug.error("FSLoginHelper.createAuthnRequest(): "
                    + "Exception Occured: " + e.getMessage() );
            }
            Map retMap = createMap(null, responseData, retHeaderMap);
            retMap.put(authnReqIDKey,requestID);
            return retMap;
        }
        //*****************
        
        String tldURL = getTLDURL();
        if (isSSO && tldURL != null && !isSingleIDP ) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSLoginHelper:: createAuthnRequest " 
                    + "In case where isSSO true and tldURL is true and not "
                    + "single idp. So redirecting to thirdlevel domain");
            }
            redirectURL = tldURL + "?" + IFSConstants.LRURL + "=" + 
            URLEncDec.encode(interSiteURL + "?" + authnReqIDKey 
                + "=" + URLEncDec.encode(requestID)
                + "&" + IFSConstants.META_ALIAS + "=" + metaAlias);
        } else if (isSSO && !isSingleIDP) {
            if(FSUtils.debug.messageEnabled())
                FSUtils.debug.message("FSLoginHelper:: createAuthnRequest "
                    + " In case where isSSO true and not a single idp so have "
                    + "show common login page");
            Map retMap = createMap(null,null,retHeaderMap);
            retMap.put(authnReqIDKey,requestID);
            return retMap;
        } else {
            boolean noIDP = false;
            if (remoteEntityID == null) {
                if (idpSet != null && idpSet.size() > 0) {
                    remoteEntityID = (String)idpSet.iterator().next();
                } else {
                    FSUtils.debug.error(
                        "FSLoginHelper:: no idps found in config."
                        + " Cannot proceed.");
                    redirectURL = errorPage + "&" 
                        + IFSConstants.FEDERROR + "="
                        + URLEncDec.encode("No IDPs Found in Configuration.")
                        + "&"
                        + IFSConstants.FEDREMARK + "="
                        + URLEncDec.encode(
                        "Please configure you Federation Services for an IDP.");
                    noIDP = true;
                }
            }
            if (!noIDP) {
                String succintID = FSUtils.generateSourceID(remoteEntityID);
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSLoginHelper:: createAuthnRequest "
                        + " Redirecting to intersiteTransfer URL " 
                        + interSiteURL
                        + " with providerID and Base64 encoded SuccintID. " 
                        + remoteEntityID + "---" + succintID);
                }
                //check for presence of federate cookie
                if (!isFedCookiePresent) {
                    if (actionOnNoFedCookie.equals(IFSConstants.COMMON_LOGIN)) {
                        Map retMap = createMap(null,null,retHeaderMap);
                        retMap.put(authnReqIDKey,requestID);
                        return retMap;
                    } else if(actionOnNoFedCookie.equals(IFSConstants.ACTIVE)) {
                        changeToPassiveAuthnRequest(
                            requestID, false, metaAlias);
                    } else {
                        changeToPassiveAuthnRequest(
                            requestID, true, metaAlias);
                    }
                }

                String providerID = FSUtils.stringToBase64(succintID);
                redirectURL = interSiteURL
                    + "?" + authnReqIDKey + "=" + URLEncDec.encode(requestID)
                    + "&" + providerIDKey + "=" + URLEncDec.encode(providerID)
                    + "&" + IFSConstants.META_ALIAS + "=" + metaAlias ;
                }
        }
        /**
         * If this flag is set via the query param, we will always make
         * a passive call to the IDP.
         */
        if (isPassiveQuery) {
            changeToPassiveAuthnRequest(requestID, true, metaAlias);
        }
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSLoginHelper.createAuthnRequest()::"
                + "redirectURL : " + redirectURL);
        }

        if (isWMLProfile(headers)) {
            retHeaderMap.put("content-type","text/vnd.wap.wml");
        } else {
            retHeaderMap.put("content-type","text/html");
        }
        Map retMap = createMap(redirectURL, null, retHeaderMap);
        retMap.put(authnReqIDKey,requestID);
        return retMap;
    }

    void changeToPassiveAuthnRequest(
        String requestID, boolean isPassiveFlag, String metaAlias) 
    {
        FSUtils.debug.message("FSPreLogin.changeToPassiveAuthnRequest called");
        FSSessionManager sessMngr = FSSessionManager.getInstance(metaAlias);
        if (sessMngr == null) {
            FSUtils.debug.message("Session Manager null");
            return;
        }
        FSAuthnRequest authnRequest = sessMngr.getAuthnRequest(requestID);
        if (authnRequest != null && !(authnRequest.getFederate())){
            authnRequest.setIsPassive(isPassiveFlag);
            authnRequest.setForceAuthn(false);
        }
        sessMngr.setAuthnRequest(requestID, authnRequest);
    }
 
        
    private Set getIDPs(String metaAlias) {
        Set idpSet = new HashSet();
        try {
            String provider = "";
            String providerStatus = "";
            String role = IFSConstants.IDP.toLowerCase();
            IDPDescriptorType providerDesc = null;
            BaseConfigType providerConfig = null;
            Set trustedProviders = metaManager.getAllTrustedProviders(
                metaAlias);
            if (trustedProviders != null && !trustedProviders.isEmpty()) {
                Iterator it = trustedProviders.iterator();
                while (it.hasNext()) {
                    provider = (String) it.next();
                    providerDesc = metaManager.getIDPDescriptor(realm,provider);
                    providerConfig = 
                        metaManager.getIDPDescriptorConfig(realm, provider);
                    if (providerDesc == null || providerConfig == null) {
                        continue;
                    } 
                    providerStatus = 
                        IDFFMetaUtils.getFirstAttributeValueFromConfig(
                            providerConfig, IFSConstants.PROVIDER_STATUS);
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("FSLoginHelper::getIDPs For " +
                        "providerId " + provider + " status is " + 
                         providerStatus);
                    }
                    if (providerStatus == null ||
                        providerStatus.length() == 0 ||
                        (providerStatus != null &&
                        providerStatus.equalsIgnoreCase(IFSConstants.ACTIVE)))
                    {
                        idpSet.add(provider);
                    }
                }
            }
        } catch (IDFFMetaException ame) {
            FSUtils.debug.error(
                "FSLoginHelper::getIDPs Error in getting idp List:", ame);
        }
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "FSLoginHelper::getIDPs returing idpset as " + idpSet);
        }
        return idpSet;
    }
         
    private FSAuthnRequest getAuthnReq(
        Map headers,
        String LRURL, 
        boolean federate) 
        throws FSLoginHelperException
    {
        FSAuthnRequest authnRequest = null;
        RequestAuthnContext authnContext = null;
        List respondWithArray = new ArrayList();
        List authnContextProfileClassRefArray = new ArrayList();
        List authnContextStatementRefArray = new ArrayList();
        try {
            if (isLECPProfile(headers)) {
                protocolProfile=IFSConstants.SSO_PROF_LECP;
            } else if(isWMLProfile(headers)) {
                protocolProfile = IFSConstants.SSO_PROF_WML_POST;
            } 
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSLoginHelper::getAuthnReq():"
                    + "Federation profile is:" + protocolProfile);
            }
            
            if (respondWithString!=null){
                StringTokenizer st = 
                    new StringTokenizer(respondWithString, ",");
                while (st.hasMoreTokens()) {
                    String tmpString = (String)st.nextToken();
                    respondWithArray.add(tmpString);
                }
            } else {
                respondWithArray = null;
            }
            
            authnContextProfileClassRefArray.add(authContextString) ;
            authnContextStatementRefArray = null;

            // this should be configurable
            String authnContextComparison = IFSConstants.MINIMUM;

            authnContext = new RequestAuthnContext(
                authnContextProfileClassRefArray,
                authnContextStatementRefArray, 
                authnContextComparison);

            String nameIDPolicyForReal;

            if (!federate && nameIDPolicy.equals(
                     IFSConstants.NAME_ID_POLICY_FEDERATED)) 
            {
                nameIDPolicyForReal = IFSConstants.NAME_ID_POLICY_NONE;
            } else {
                nameIDPolicyForReal = nameIDPolicy;
            }

            if (anonymousOnetime != null && anonymousOnetime.equals("true")) {
                nameIDPolicyForReal = IFSConstants.NAME_ID_POLICY_ONETIME;
                federate = true;
            }

            authnRequest = new FSAuthnRequest(
                null, 
                respondWithArray, 
                hostEntityID,
                forceAuthn,
                isPassive, 
                federate, 
                nameIDPolicyForReal,
                protocolProfile, 
                authnContext, 
                LRURL, 
                IFSConstants.MINIMUM);
            
        } catch(Exception ex){
            FSUtils.debug.error(
                "FSLoginHelper.getAuthnReq():Error during procesing:", ex);
            throw new FSLoginHelperException(
                "FSLoginHelper.getAuthnReq() In Exception " + ex.getMessage());
        }
        return authnRequest;
    }
    

    private Map createMap(String redirectURL, String content, Map retHeaderMap){
        Map map = new HashMap();
        map.put(headerKey, retHeaderMap);
        if (redirectURL != null) {
            map.put(URLKey, redirectURL);
        } else if (content != null) {
            map.put(responseDataKey, content);
        }
        return map;
    }
    
    private boolean isLECPProfile(Map headers) {
        FSUtils.debug.message("FSLoginHelper.isLECPProfile called");
        if (headers != null) {
            String lecpHeaderValue = 
                (String)headers.get(IFSConstants.LECP_HEADER_NAME);
            if (lecpHeaderValue == null) {
                String header = (IFSConstants.LECP_HEADER_NAME).toLowerCase();
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSLoginHelper.isLECPProfile checking for "
                        + "lower case header: " + header);
                }
                lecpHeaderValue = (String)headers.get(header);
            }
            if (lecpHeaderValue != null) {
                return true;
            } else {
                return false;              
            }
        }    
        return false;
    }
    
    private boolean isWMLProfile(Map headers) {
        if (headers != null) {
            String wmlHeaderValue  =(String)headers.get("accept");
            if (wmlHeaderValue != null) {
                if ((wmlHeaderValue.toLowerCase().
                    indexOf((IFSConstants.WML_HEADER_VALUE))) != -1)
                {
                    FSUtils.debug.message(
                        "FSLoginHelper.isWMLProfile() :: true ");
                    return true;
                } 
            }
        } 
        return false;
    }
    
    private String getTLDURL() {
        String tldURL = null;
        FSUtils.debug.message("FSLoginHelper.getTLDURL() :: called");
        try {
            if ((cotList == null) || (cotList.isEmpty())){
                FSUtils.debug.error("FSLoginHelper::getTLDURL():"
                    + "Received COT Set is Invalid");
            } else  {
                if (cotList.size() > 1) {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("FSLoginHelper::getTLDURL() "
                            +"Multiple COTs found will do polling " );
                    }
                } else {
                    FSUtils.debug.message(
                        "FSLoginHelper::getTLDURL() Single COT found");
                }
                Iterator iter = cotList.iterator();
                CircleOfTrustManager cotManager = new CircleOfTrustManager();
                while (iter.hasNext()) {
                    CircleOfTrustDescriptor cotDesc = 
                        cotManager.getCircleOfTrust(
                            realm, (String)iter.next());
                    if (cotDesc != null && 
                        (cotDesc.getCircleOfTrustStatus()).
                            equalsIgnoreCase(IFSConstants.ACTIVE)) 
                    {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSLoginHelper::getTLDURL "
                                + "found a active cot with cotid : "
                                + cotDesc.getCircleOfTrustName());
                        }
                        tldURL = cotDesc.getIDFFReaderServiceURL();
                        break;
                    }
                }
            }
        } catch (COTException ame) {
            FSUtils.debug.error("FSLoginHelper.getTLDURL():"
                + "COTException:", ame);
        } catch (Exception exp) {
            FSUtils.debug.error(
                "FSLoginHelper.getTLDURL():General Exception:", exp);
        }
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSLoginHelper::getTLDURL().tldURL  " 
                + tldURL);
        }
        return tldURL;
    }
    
    /**
     * Creates <code>AuthnRequestEnvelope</code> for <code>LECP</code> profile.
     * @param request <code>HttpServletRequest</code> object
     * @return xml string of an <code>AuthnRequestEnvelope</code> object
     */
    public String createAuthnRequestEnvelope(HttpServletRequest request) {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "FSLoginHelper.createAuthnRequestEnvelope(): called" + 
                request.getParameter("metaAlias"));
        }
      
        try {
            String metaAlias = request.getParameter("metaAlias");
            //FSServiceUtils.getMetaAlias(request);
            setMetaInfo(metaAlias, null);
            FSAuthnRequest authnRequest = null;
            Map headerMap = new HashMap();
            Enumeration headerNames = request.getHeaderNames();
            while(headerNames.hasMoreElements()) {
                String hn = headerNames.nextElement().toString();
                String hv = request.getHeader(hn);
                headerMap.put(hn, hv);
            }

            Set idpSet = getIDPs(metaAlias);

            String LRURL = request.getParameter (IFSConstants.LRURL);
            if (LRURL == null || LRURL.equals ("")) {
                LRURL = FSServiceUtils.getFederationDonePageURL(
                    request, hostConfig, metaAlias);
            } 
            authnRequest = getAuthnReq(headerMap, LRURL, true);
            authnRequest.setMinorVersion(FSServiceUtils.getMinorVersion(
                hostDescriptor.getProtocolSupportEnumeration()));
            if (authnRequest == null ) {
                FSUtils.debug.error(
                    "FSLoginHelper.createAuthnRequest()::AuthnRequest is null");
                return errorPage + "&" 
                    + IFSConstants.FEDERROR + "="
                    + URLEncDec.encode("Unable to create AuthnRequest") + "&"
                    + IFSConstants.FEDREMARK + "="
                    + URLEncDec.encode(
                        "Please check your Federation Configuration.") ;
            }
            String requestID = authnRequest.getRequestID();
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSLoginHelper.createAuthnRequest()::RequestID: " +
                    requestID);
            }
            FSSessionManager sessMngr = FSSessionManager.getInstance(metaAlias);
            sessMngr.setAuthnRequest(requestID, authnRequest);

            Object ssoToken = SessionManager.getProvider().getSession(request);
            if (ssoToken != null) {
                sessMngr.setLocalSessionToken(requestID, ssoToken);
            }

            //check if dontgotothird level domain flag is off is yes
            //if yes then get one provider and and send to intersiteurl
            //else send to third level domain for provider id
            //also check if there is a single idp if yes go to intersite 
            //transfer with providerid

            //*****************

            FSAuthnRequestEnvelope authnRequestEnvelope = null;
            boolean isPassive = false;

            FSIDPList idpList = null;
            //get IDPList from directory

            String assertionConsumerURL = 
                FSServiceUtils.getAssertionConsumerServiceURL(
                    hostDescriptor, null);
            List idpEntryList = null;
            String idpID = null;
            String idpLocation = null;
            Iterator iter = idpSet.iterator();
            while (iter.hasNext()){
                idpID = (String)iter.next();
                if (idpID != null){
                    IDPDescriptorType idpDescr = 
                        metaManager.getIDPDescriptor(realm, idpID);
                    idpLocation = idpDescr.getSingleSignOnServiceURL(); 
                    if (idpEntryList == null){
                        idpEntryList = new ArrayList();
                    }
                    idpEntryList.add(new IDPEntry(idpID, idpID, idpLocation));
                }
            }
            int minorVersion = FSServiceUtils.getMinorVersion(
                hostDescriptor.getProtocolSupportEnumeration());
            IDPEntries idpEntries = new IDPEntries(idpEntryList);
            idpList = new FSIDPList(idpEntries, null);
            idpList.setMinorVersion(minorVersion);
            if (FSServiceUtils.isSigningOn ()) {
                if (hostDescriptor.isAuthnRequestsSigned()) {
                    authnRequest.signXML(
                        IDFFMetaUtils.getFirstAttributeValueFromConfig(
                            hostConfig, IFSConstants.SIGNING_CERT_ALIAS));
                }
            }
            authnRequestEnvelope = 
                new FSAuthnRequestEnvelope(
                        authnRequest, 
                        hostEntityID, 
                        hostEntityID, 
                        assertionConsumerURL, 
                        idpList, 
                        isPassive);

            authnRequestEnvelope.setMinorVersion(minorVersion);

            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSLoginHelper.createAuthnRequest: "
                    + "AuthnRequestEnvelope: " 
                    + authnRequestEnvelope.toXMLString() );
            }
            return authnRequestEnvelope.toXMLString();
        } catch (Exception e) {
            FSUtils.debug.error(
                "FSLoginHelper.createAuthnRequest():Exception Occured: ", e);
            return null;
        }           
    }
}
