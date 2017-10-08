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
 * $Id: SPSSOFederate.java,v 1.29 2009/11/24 21:53:28 madan_ranganath Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */
package com.sun.identity.saml2.profile;

import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.liberty.ws.paos.PAOSConstants;
import com.sun.identity.liberty.ws.paos.PAOSException;
import com.sun.identity.liberty.ws.paos.PAOSHeader;
import com.sun.identity.liberty.ws.paos.PAOSRequest;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.common.QuerySignatureUtil;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2FailoverUtils;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.common.SOAPCommunicator;
import com.sun.identity.saml2.ecp.ECPFactory;
import com.sun.identity.saml2.ecp.ECPRelayState;
import com.sun.identity.saml2.ecp.ECPRequest;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.jaxb.metadata.AffiliationDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.AssertionConsumerServiceElement;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.SingleSignOnServiceElement;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.plugins.SAML2IDPFinder;
import com.sun.identity.saml2.plugins.SAML2ServiceProviderAdapter;
import com.sun.identity.saml2.plugins.SPAuthnContextMapper;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.Extensions;
import com.sun.identity.saml2.protocol.GetComplete;
import com.sun.identity.saml2.protocol.IDPEntry;
import com.sun.identity.saml2.protocol.IDPList;
import com.sun.identity.saml2.protocol.NameIDPolicy;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.RequestedAuthnContext;
import com.sun.identity.saml2.protocol.Scoping;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.xml.XMLUtils;
import java.io.IOException;
import java.io.OutputStream;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import org.forgerock.openam.federation.saml2.SAML2TokenRepositoryException;
import org.forgerock.openam.saml2.audit.SAML2EventLogger;
import org.forgerock.openam.utils.StringUtils;

/**
 * This class reads the query parameters and performs the required
 * processing logic for sending Authentication Request
 * from SP to IDP.
 *
 */

public class SPSSOFederate {
 
    static SAML2MetaManager sm = null;
    static {
        try {
            sm = new SAML2MetaManager();
        } catch (SAML2MetaException sme) {
            SAML2Utils.debug.error("SPSSOFederate: Error retreiving metadata"
                                    ,sme);
        }
    }

    /**
     * Parses the request parameters and builds the Authentication
     * Request to sent to the IDP.
     *
     * @param request the HttpServletRequest.
     * @param response the HttpServletResponse.
     * @param metaAlias metaAlias to locate the service providers.
     * @param idpEntityID entityID of Identity Provider.
     * @param paramsMap Map of all other parameters.The key in the
     *              map are of the type String. The values in the paramsMap
     *              are of the type List.
     *              Some of the possible keys are:RelayState,NameIDFormat,
     *              reqBinding, binding, AssertionConsumerServiceIndex,
     *              AttributeConsumingServiceIndex (currently not supported),
     *              isPassive, ForceAuthN, AllowCreate, Destination,
     *              AuthnContextDeclRef, AuthnContextClassRef,
     *              AuthComparison, Consent (currently not supported),
     *              AuthLevel, and sunamcompositeadvice.
     * @param auditor the SAML2EventLogger to use to log the saml request - may be null
     * @throws SAML2Exception if error initiating request to IDP.
     */
    public static void initiateAuthnRequest(final HttpServletRequest request,
                                            final HttpServletResponse response,
                                            final String metaAlias,
                                            final String idpEntityID,
                                            final Map paramsMap,
                                            final SAML2EventLogger auditor) throws SAML2Exception {

        try {
            // get the sp entity ID from the metaAlias
            String spEntityID = getSPEntityId(metaAlias);
            String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);

            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("SPSSOFederate : spEntityID is :" + spEntityID);
                SAML2Utils.debug.message("SPSSOFederate realm is :" + realm);
            }

            initiateAuthnRequest(request, response, spEntityID,  idpEntityID, realm, paramsMap, auditor);
        } catch (SAML2MetaException sme) {
            SAML2Utils.debug.error("SPSSOFederate: Error retreiving spEntityID from MetaAlias",sme);
            throw new SAML2Exception(SAML2Utils.bundle.getString("metaAliasError"));
        }
    }

    /**
     * Gets the SP Entity ID from the metaAlias.
     *
     * @param metaAlias the metaAlias String
     * @return the EntityId of the SP from the meta Alias
     * @throws SAML2MetaException if there was a problem extracting
     */
    public static String getSPEntityId(String metaAlias) throws SAML2MetaException {
        return sm.getEntityByMetaAlias(metaAlias);
    }

    /**
     * Parses the request parameters and builds the Authentication
     * Request to sent to the IDP.
     *
     * @param request the HttpServletRequest.
     * @param response the HttpServletResponse.
     * @param spEntityID entityID of Service Provider.
     * @param idpEntityID entityID of Identity Provider.
     * @param paramsMap Map of all other parameters.The key in the
     *              map are the parameter names of the type String. 
     *              The values in the paramsMap are of the type List.
     *              Some of the possible keys are:RelayState,NameIDFormat,
     *              reqBinding, binding, AssertionConsumerServiceIndex,
     *              AttributeConsumingServiceIndex (currently not supported),
     *              isPassive, ForceAuthN, AllowCreate, Destination,
     *              AuthnContextDeclRef, AuthnContextClassRef,
     *              AuthComparison, Consent (currently not supported),
     *              AuthLevel, and sunamcompositeadvice.
     * @param auditor the auditor for logging SAML2 Events - may be null
     * @throws SAML2Exception if error initiating request to IDP.
     */
    private static void initiateAuthnRequest(
            final HttpServletRequest request, final HttpServletResponse response, final String spEntityID,
            final String idpEntityID, final String realmName, final Map paramsMap, final SAML2EventLogger auditor)
            throws SAML2Exception {

        if (FSUtils.needSetLBCookieAndRedirect(request, response, false)) {
            return;
        }

        if (spEntityID == null) {
            SAML2Utils.debug.error("SPSSOFederate:Service Provider ID  is missing.");
            String[] data = { spEntityID };
            LogUtil.error(Level.INFO, LogUtil.INVALID_SP, data, null);
            throw new SAML2Exception(SAML2Utils.bundle.getString("nullSPEntityID"));
        }
        
        if (idpEntityID == null)  {
            SAML2Utils.debug.error("SPSSOFederate: Identity Provider ID is missing .");
            String[] data = { idpEntityID };
            LogUtil.error(Level.INFO, LogUtil.INVALID_IDP, data, null);
            throw new SAML2Exception(SAML2Utils.bundle.getString("nullIDPEntityID"));
        }
        

        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("SPSSOFederate: in initiateSSOFed");
            SAML2Utils.debug.message("SPSSOFederate: spEntityID is : " + spEntityID);
            SAML2Utils.debug.message("SPSSOFederate: idpEntityID : "  + idpEntityID);
        }
        
        String realm = getRealm(realmName);
        
        try {
            // Retrieve MetaData
            if (sm == null) {
                throw new SAML2Exception(SAML2Utils.bundle.getString("errorMetaManager"));
            }

            Map spConfigAttrsMap = getAttrsMapForAuthnReq(realm, spEntityID);

             // get SPSSODescriptor
            SPSSODescriptorElement spsso = getSPSSOForAuthnReq(realm, spEntityID);

            if (spsso == null) {
                String[] data = { spEntityID };
                LogUtil.error(Level.INFO, LogUtil.SP_METADATA_ERROR, data, null);
                throw new SAML2Exception(SAML2Utils.bundle.getString("metaDataError"));
            }

            List extensionsList = getExtensionsList(spEntityID, realm);
            
            // get IDP Descriptor
            IDPSSODescriptorElement idpsso = getIDPSSOForAuthnReq(realm, idpEntityID);

            if (idpsso == null) {
                String[] data = { idpEntityID };
                LogUtil.error(Level.INFO, LogUtil.IDP_METADATA_ERROR, data, null);
                throw new SAML2Exception(SAML2Utils.bundle.getString("metaDataError"));
            }

            String binding = getParameter(paramsMap, SAML2Constants.REQ_BINDING);
            List<SingleSignOnServiceElement> ssoServiceList = idpsso.getSingleSignOnService();
            final SingleSignOnServiceElement endPoint = getSingleSignOnServiceEndpoint(ssoServiceList, binding);

            if (endPoint == null || StringUtils.isEmpty(endPoint.getLocation())) {
                String[] data = { idpEntityID };
                LogUtil.error(Level.INFO, LogUtil.SSO_NOT_FOUND, data, null);
                throw new SAML2Exception(SAML2Utils.bundle.getString("ssoServiceNotfound"));
            }

            String ssoURL = endPoint.getLocation();
            SAML2Utils.debug.message("SPSSOFederate: SingleSignOnService URL : {}", ssoURL);
            if (binding == null) {
                SAML2Utils.debug.message("SPSSOFederate: reqBinding is null using endpoint binding: {} ",
                        endPoint.getBinding());
                binding = endPoint.getBinding();
                if (binding == null) {
                    String[] data = { idpEntityID };
                    LogUtil.error(Level.INFO, LogUtil.NO_RETURN_BINDING, data, null);
                    throw new SAML2Exception(SAML2Utils.bundle.getString("UnableTofindBinding"));
                }
            }

            // create AuthnRequest 
            AuthnRequest authnRequest = createAuthnRequest(realm, spEntityID, paramsMap, spConfigAttrsMap,
                    extensionsList, spsso, idpsso, ssoURL, false);
            if (null != auditor && null != authnRequest) {
                auditor.setRequestId(authnRequest.getID());
            }

            // invoke SP Adapter class if registered
            SAML2ServiceProviderAdapter spAdapter = SAML2Utils.getSPAdapterClass(spEntityID, realmName);
            if (spAdapter != null) {
                spAdapter.preSingleSignOnRequest(spEntityID, idpEntityID, realmName, request, response, authnRequest);
            }

            String authReqXMLString = authnRequest.toXMLString(true, true);
        
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("SPSSOFederate: AuthnRequest:" + authReqXMLString);
            }

            // Default URL if relayState not present? in providerConfig?
            // TODO get Default URL from metadata 
            String relayState = getParameter(paramsMap, SAML2Constants.RELAY_STATE);

            // Validate the RelayState URL.
            SAML2Utils.validateRelayStateURL(realm, spEntityID, relayState, SAML2Constants.SP_ROLE);

            // check if relayState is present and get the unique
            // id which will be appended to the SSO URL before
            // redirecting.
            String relayStateID = null;
            if (relayState != null && relayState.length() > 0) {
                relayStateID = getRelayStateID(relayState, authnRequest.getID());
            }

            if (binding.equals(SAML2Constants.HTTP_POST)) {
                String encodedReqMsg = getPostBindingMsg(idpsso, spsso, spConfigAttrsMap, authnRequest);
                SAML2Utils.postToTarget(request, response, "SAMLRequest", encodedReqMsg, "RelayState", relayStateID, ssoURL);
            } else {
                String redirect = getRedirect(authReqXMLString, relayStateID, ssoURL, idpsso, spsso, spConfigAttrsMap);
                response.sendRedirect(redirect);
            }

            String[] data = { ssoURL };
            LogUtil.access(Level.INFO, LogUtil.REDIRECT_TO_IDP, data, null);
            AuthnRequestInfo reqInfo = 
                new AuthnRequestInfo(request, response, realm, spEntityID,
                        idpEntityID, authnRequest, relayState, paramsMap);

            synchronized(SPCache.requestHash) {             
                SPCache.requestHash.put(authnRequest.getID(),reqInfo);
            }

            if (SAML2FailoverUtils.isSAML2FailoverEnabled()) {
                // sessionExpireTime is counted in seconds
                long sessionExpireTime = currentTimeMillis() / 1000 + SPCache.interval;
                String key = authnRequest.getID();
                try {
                    SAML2FailoverUtils.saveSAML2TokenWithoutSecondaryKey(key, new AuthnRequestInfoCopy(reqInfo), sessionExpireTime);
                    if (SAML2Utils.debug.messageEnabled()) {
                        SAML2Utils.debug.message("SPSSOFederate.initiateAuthnRequest:"
                                + " SAVE AuthnRequestInfoCopy for requestID " + key);
                    }
                } catch (SAML2TokenRepositoryException e) {
                    SAML2Utils.debug.error("SPSSOFederate.initiateAuthnRequest: There was a problem saving the " +
                            "AuthnRequestInfoCopy in the SAML2 Token Repository for requestID " + key, e);
                    throw new SAML2Exception(SAML2Utils.bundle.getString("metaDataError"));
                }
            }
        } catch (IOException ioe) {
            SAML2Utils.debug.error("SPSSOFederate: Exception :",ioe);
            throw new SAML2Exception(SAML2Utils.bundle.getString("errorCreatingAuthnRequest"));
        } catch (SAML2MetaException sme) {
            SAML2Utils.debug.error("SPSSOFederate:Error retrieving metadata", sme);
            throw new SAML2Exception(SAML2Utils.bundle.getString("metaDataError"));
        }
    }

    /**
     * Gets the redirect String.
     *
     * @param authReqXMLString Auth Request XML.
     * @param relayStateID the id of the relay state
     * @param ssoURL the url for the reidrect
     * @param idpsso the idp descriptor to use
     * @param spsso the sp descriptor to use
     * @param spConfigAttrsMap the sp configuration details
     * @return a String to use for the redirect request.
     * @throws SAML2Exception if there is a problem creating the redirect string
     */
    public static String getRedirect(String authReqXMLString, String relayStateID, String ssoURL,
                                      IDPSSODescriptorElement idpsso, SPSSODescriptorElement spsso, Map spConfigAttrsMap)
            throws SAML2Exception {

        // encode the xml string
        String encodedXML = SAML2Utils.encodeForRedirect(authReqXMLString);

        StringBuilder queryString = new StringBuilder();
        queryString.append(SAML2Constants.SAML_REQUEST).append(SAML2Constants.EQUAL).append(encodedXML);

        if ((relayStateID != null) && (relayStateID.length() > 0)) {
            queryString.append("&").append(SAML2Constants.RELAY_STATE)
                    .append("=")
                    .append(URLEncDec.encode(relayStateID));
        }

        StringBuilder redirectURL =
                new StringBuilder().append(ssoURL).append(ssoURL.contains("?") ? "&" : "?");
        // sign the query string
        if (idpsso.isWantAuthnRequestsSigned() || spsso.isAuthnRequestsSigned()) {
            String certAlias = getParameter(spConfigAttrsMap, SAML2Constants.SIGNING_CERT_ALIAS);
            String signedQueryStr = signQueryString(queryString.toString(), certAlias);
            redirectURL.append(signedQueryStr);
        } else {
            redirectURL.append(queryString);
        }

        return redirectURL.toString();
    }

    /**
     * Gets the SP SSO Descriptor for the given sp entity id in the given realm.
     *
     * @param realm the realm the sp is configured in
     * @param spEntityID the entity id of the sp to get the Descriptor for
     * @return the SPSSODescriptorElement for the requested sp entity
     * @throws SAML2MetaException if there is a problem looking up the SPSSODescriptorElement.
     */
    public static SPSSODescriptorElement getSPSSOForAuthnReq(String realm, String spEntityID)
            throws SAML2MetaException {
        return sm.getSPSSODescriptor(realm, spEntityID);
    }

    /**
     * Gets the Configuration attributes for the given sp entity id in the given realm.
     * @param realm the realm the sp is configured in
     * @param spEntityID the entity id of the sp to get the attributes map for
     * @return a map of SAML2 Attributes with String keys mapped to a collection of values
     * @throws SAML2MetaException
     */
    public static Map<String, Collection<String>> getAttrsMapForAuthnReq(String realm, String spEntityID)
            throws SAML2MetaException {

        SPSSOConfigElement spEntityCfg = sm.getSPSSOConfig(realm, spEntityID);
        Map spConfigAttrsMap = null;

        if (spEntityCfg != null) {
            spConfigAttrsMap = SAML2MetaUtils.getAttributes(spEntityCfg);
        }

        return spConfigAttrsMap;
    }

    /**
     * Gets the IDP SSO Descriptor for the given sp entity id in the given realm.
     *
     * @param realm the realm the idp is configured in
     * @param idpEntityID the entity id of the idp[ to get the Descriptor for
     * @return the SPSSODescriptorElement for the requested idp entity
     * @throws SAML2MetaException if there is a problem looking up the IDPSSODescriptorElement.
     */
    public static IDPSSODescriptorElement getIDPSSOForAuthnReq(String realm, String idpEntityID)
            throws SAML2MetaException {
        return sm.getIDPSSODescriptor(realm, idpEntityID);
    }

    /**
     * Gets the Post Binding message
     *
     * @param idpsso
     * @param spsso
     * @param spConfigAttrsMap
     * @param authnRequest
     * @return
     * @throws SAML2Exception
     */
    public static String getPostBindingMsg(IDPSSODescriptorElement idpsso, SPSSODescriptorElement spsso,
                                            Map spConfigAttrsMap, AuthnRequest authnRequest)
            throws SAML2Exception {

        if (idpsso.isWantAuthnRequestsSigned() || spsso.isAuthnRequestsSigned()) {
            String certAlias = getParameter(spConfigAttrsMap, SAML2Constants.SIGNING_CERT_ALIAS);
            signAuthnRequest(certAlias, authnRequest);
        }
        String authXMLString = authnRequest.toXMLString(true, true);

        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("SPSSOFederate.initiateAuthnRequest: SAML Response content :\n" + authXMLString);
        }

        return SAML2Utils.encodeForPOST(authXMLString);
    }

    /**
     * Parses the request parameters and builds ECP Request to sent to the IDP.
     *
     * @param request the HttpServletRequest.
     * @param response the HttpServletResponse.
     *
     * @throws SAML2Exception if error creating AuthnRequest.
     * @throws IOException if error sending AuthnRequest to ECP.
     */
    public static void initiateECPRequest(HttpServletRequest request,
        HttpServletResponse response)
        throws SAML2Exception, IOException {

        if (!isFromECP(request)) {
            SAML2Utils.debug.error("SPSSOFederate.initiateECPRequest: " +
                "invalid HTTP request from ECP.");
            SAMLUtils.sendError(request, response, 
                HttpServletResponse.SC_BAD_REQUEST,  
                "invalidHttpRequestFromECP",
                SAML2Utils.bundle.getString("invalidHttpRequestFromECP"));
            return;
        }

        String metaAlias = request.getParameter("metaAlias");
        Map paramsMap = SAML2Utils.getParamsMap(request);

        // get the sp entity ID from the metaAlias
        String spEntityID = sm.getEntityByMetaAlias(metaAlias);
        String realm = getRealm(SAML2MetaUtils.getRealmByMetaAlias(metaAlias));
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("SPSSOFederate.initiateECPRequest: " +
                "spEntityID is " + spEntityID + ", realm is " + realm);
        }
        
        try {
            // Retreive MetaData 
            if (sm == null) {
                throw new SAML2Exception(
                    SAML2Utils.bundle.getString("errorMetaManager"));
            }

            SPSSOConfigElement spEntityCfg = 
                sm.getSPSSOConfig(realm,spEntityID);
            Map spConfigAttrsMap=null;
            if (spEntityCfg != null) {
                spConfigAttrsMap = SAML2MetaUtils.getAttributes(spEntityCfg);
            }
             // get SPSSODescriptor
            SPSSODescriptorElement spsso = 
                sm.getSPSSODescriptor(realm,spEntityID);

            if (spsso == null) {
                String[] data = { spEntityID };
                LogUtil.error(Level.INFO,LogUtil.SP_METADATA_ERROR,data, null);
                throw new SAML2Exception(
                    SAML2Utils.bundle.getString("metaDataError"));
            }

            String[] data = { spEntityID, realm };
            LogUtil.access(Level.INFO, LogUtil.RECEIVED_HTTP_REQUEST_ECP, data,
                null);

            List extensionsList = getExtensionsList(spEntityID, realm);

            // create AuthnRequest 
            AuthnRequest authnRequest = createAuthnRequest(realm, spEntityID,
                paramsMap, spConfigAttrsMap, extensionsList, spsso, null, null,
                true);

            // invoke SP Adapter class if registered
            SAML2ServiceProviderAdapter spAdapter =
                SAML2Utils.getSPAdapterClass(spEntityID, realm);
            if (spAdapter != null) {
                spAdapter.preSingleSignOnRequest(spEntityID, realm, null,
                    request, response, authnRequest);
            }

            String alias = SAML2Utils.getSigningCertAlias(realm, spEntityID,
                SAML2Constants.SP_ROLE);

            PrivateKey signingKey =
                KeyUtil.getKeyProviderInstance().getPrivateKey(alias);
            if (signingKey != null) {
                authnRequest.sign(signingKey, null);
            } else {
                SAML2Utils.debug.error("SPSSOFederate.initiateECPRequest: " +
                    "Unable to find signing key.");
                throw new SAML2Exception(
                    SAML2Utils.bundle.getString("metaDataError"));
            }

            ECPFactory ecpFactory = ECPFactory.getInstance(); 

            // Default URL if relayState not present? in providerConfig?
            // TODO get Default URL from metadata 
            String relayState = getParameter(paramsMap,
                SAML2Constants.RELAY_STATE);

            String ecpRelayStateXmlStr = "";
            if (relayState != null && relayState.length()> 0) {
                String relayStateID = getRelayStateID(relayState,
                    authnRequest.getID());
                ECPRelayState ecpRelayState = ecpFactory.createECPRelayState();
                ecpRelayState.setValue(relayStateID);
                ecpRelayState.setMustUnderstand(Boolean.TRUE);
                ecpRelayState.setActor(SAML2Constants.SOAP_ACTOR_NEXT);
                ecpRelayStateXmlStr = ecpRelayState.toXMLString(true, true);
            }

            ECPRequest ecpRequest = ecpFactory.createECPRequest();
            ecpRequest.setIssuer(createIssuer(spEntityID));
            ecpRequest.setMustUnderstand(Boolean.TRUE);
            ecpRequest.setActor(SAML2Constants.SOAP_ACTOR_NEXT);
            ecpRequest.setIsPassive(authnRequest.isPassive());
            SAML2IDPFinder ecpIDPFinder =
                SAML2Utils.getECPIDPFinder(realm, spEntityID);
            if (ecpIDPFinder != null) {
                List idps = ecpIDPFinder.getPreferredIDP(authnRequest,
                    spEntityID, realm, request, response);
                if ((idps != null) && (!idps.isEmpty())) {
                    SAML2MetaManager saml2MetaManager =
                        SAML2Utils.getSAML2MetaManager();
                    List idpEntries = null;
                    for(Iterator iter = idps.iterator(); iter.hasNext();) {
                        String idpEntityID = (String)iter.next();
                        IDPSSODescriptorElement idpDesc = saml2MetaManager
                            .getIDPSSODescriptor(realm, idpEntityID);
                        if (idpDesc != null) {
                            IDPEntry idpEntry = ProtocolFactory.getInstance()
                                .createIDPEntry();
                            idpEntry.setProviderID(idpEntityID);
                            String description =
                                SAML2Utils.getAttributeValueFromSSOConfig(
                                realm, idpEntityID, SAML2Constants.IDP_ROLE,
                                SAML2Constants.ENTITY_DESCRIPTION);
                            idpEntry.setName(description);
                            List<SingleSignOnServiceElement> ssoServiceList = idpDesc.getSingleSignOnService();
                            SingleSignOnServiceElement endPoint = getSingleSignOnServiceEndpoint(ssoServiceList, SAML2Constants.SOAP);
                            if (endPoint == null || StringUtils.isEmpty(endPoint.getLocation())) {
                                throw new SAML2Exception(SAML2Utils.bundle.getString("ssoServiceNotfound"));
                            }
                            String ssoURL = endPoint.getLocation();
                            SAML2Utils.debug.message("SPSSOFederate.initiateECPRequest URL : {}", ssoURL);
                            idpEntry.setLoc(ssoURL);
                            if (idpEntries == null) {
                                idpEntries = new ArrayList();
                            }
                            idpEntries.add(idpEntry);
                        }
                    }
                    if (idpEntries != null) {
                        IDPList idpList = ProtocolFactory.getInstance()
                            .createIDPList();
                        idpList.setIDPEntries(idpEntries);
                        ecpRequest.setIDPList(idpList);
                        Map attrs = SAML2MetaUtils.getAttributes(spEntityCfg);
                        List values = (List)attrs.get(
                            SAML2Constants.ECP_REQUEST_IDP_LIST_GET_COMPLETE);
                        if ((values != null) && (!values.isEmpty())) {
                            GetComplete getComplete =
                                ProtocolFactory.getInstance()
                                .createGetComplete();
                            getComplete.setValue((String)values.get(0));
                            idpList.setGetComplete(getComplete);
                        }
                    }
                }
            }
            String paosRequestXmlStr = "";
            try {
                PAOSRequest paosRequest = new PAOSRequest(
                    authnRequest.getAssertionConsumerServiceURL(),
                    SAML2Constants.PAOS_ECP_SERVICE, null, Boolean.TRUE,
                    SAML2Constants.SOAP_ACTOR_NEXT);
                paosRequestXmlStr =  paosRequest.toXMLString(true, true);
            } catch (PAOSException paosex) {
                SAML2Utils.debug.error("SPSSOFederate.initiateECPRequest:",
                    paosex);
                throw new SAML2Exception(paosex.getMessage());
            }
            String header = paosRequestXmlStr +
                ecpRequest.toXMLString(true, true) + ecpRelayStateXmlStr;

            String body = authnRequest.toXMLString(true, true);
            try {
                SOAPMessage reply = SOAPCommunicator.getInstance().createSOAPMessage(header, body,
                        false);

                String[] data2 = { spEntityID, realm, "" };
                if (LogUtil.isAccessLoggable(Level.FINE)) {
                    data2[2] = SOAPCommunicator.getInstance().soapMessageToString(reply);
                }
                LogUtil.access(Level.INFO, LogUtil.SEND_ECP_PAOS_REQUEST, data2,
                    null);

                // Need to call saveChanges because we're
                // going to use the MimeHeaders to set HTTP
                // response information. These MimeHeaders
                // are generated as part of the save.
                if (reply.saveRequired()) {
                    reply.saveChanges();
                }

                response.setStatus(HttpServletResponse.SC_OK);
                SAML2Utils.putHeaders(reply.getMimeHeaders(), response);
                response.setContentType(PAOSConstants.PAOS_MIME_TYPE);
                // Write out the message on the response stream
                OutputStream os = response.getOutputStream();
                reply.writeTo(os);
                os.flush();
            } catch (SOAPException soapex) {
                SAML2Utils.debug.error("SPSSOFederate.initiateECPRequest",
                    soapex);
                String[] data3 = { spEntityID, realm };
                LogUtil.error(Level.INFO, LogUtil.SEND_ECP_PAOS_REQUEST_FAILED,
                    data3, null);
                SAMLUtils.sendError(request, response, 
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "soapError", soapex.getMessage());
                return;
            }

            AuthnRequestInfo reqInfo = 
                new AuthnRequestInfo(request,response,realm,spEntityID,
                                     null, authnRequest,relayState,
                                     paramsMap);
            synchronized(SPCache.requestHash) {             
                SPCache.requestHash.put(authnRequest.getID(),reqInfo);
            }
            if (SAML2FailoverUtils.isSAML2FailoverEnabled()) {
                // sessionExpireTime is counted in seconds
                long sessionExpireTime = currentTimeMillis() / 1000 + SPCache.interval;
                String key = authnRequest.getID();
                try {
                    SAML2FailoverUtils.saveSAML2TokenWithoutSecondaryKey(key, new AuthnRequestInfoCopy(reqInfo), sessionExpireTime);
                    if (SAML2Utils.debug.messageEnabled()) {
                        SAML2Utils.debug.message("SPSSOFederate.initiateECPRequest:"
                                + " SAVE AuthnRequestInfoCopy for requestID " + key);
                    }
                } catch (SAML2TokenRepositoryException e) {
                    SAML2Utils.debug.error("SPSSOFederate.initiateECPRequest: There was a problem saving the " +
                            "AuthnRequestInfoCopy in the SAML2 Token Repository for requestID " + key, e);
                }
            }
        } catch (SAML2MetaException sme) {
            SAML2Utils.debug.error("SPSSOFederate:Error retrieving metadata" ,sme);
            throw new SAML2Exception(SAML2Utils.bundle.getString("metaDataError"));
        }
    }

    /**
     * Checks if the request is from ECP.
     *
     * @param request the HttpServletRequest.
     * @return true if the request is from ECP.
     */
    public static boolean isFromECP(HttpServletRequest request) {
        PAOSHeader paosHeader = null;
        try {
            paosHeader = new PAOSHeader(request);
        } catch (PAOSException pex) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("SPSSOFederate.initiateECPRequest:" +
                    "no PAOS header");
            }
            return false;
        }

        Map svcOpts = paosHeader.getServicesAndOptions();
        if ((svcOpts == null) ||
           (!svcOpts.containsKey(SAML2Constants.PAOS_ECP_SERVICE))) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("SPSSOFederate.initiateECPRequest:" +
                    "PAOS header doesn't contain ECP service");
            }
            return false;
        }

        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader == null) {
            return false;
        }

        return (acceptHeader.indexOf(PAOSConstants.PAOS_MIME_TYPE) != -1);
    }

    /* Create NameIDPolicy Element */
    private static NameIDPolicy createNameIDPolicy(String spEntityID,
        String format, boolean allowCreate, SPSSODescriptorElement spsso,
        IDPSSODescriptorElement idpsso, String realm, Map paramsMap)
        throws SAML2Exception {

        format = SAML2Utils.verifyNameIDFormat(format, spsso, idpsso);

        NameIDPolicy nameIDPolicy =
                ProtocolFactory.getInstance().createNameIDPolicy();


        String affiliationID = getParameter(paramsMap,
            SAML2Constants.AFFILIATION_ID);
        if (affiliationID != null) {
            AffiliationDescriptorType affiDesc =
                sm.getAffiliationDescriptor(realm, affiliationID);
            if (affiDesc == null) {
                throw new SAML2Exception(SAML2Utils.bundle.getString(
                    "affiliationNotFound"));
            }
            if (!affiDesc.getAffiliateMember().contains(spEntityID)) {
                throw new SAML2Exception(SAML2Utils.bundle.getString(
                    "spNotAffiliationMember"));
            }
            nameIDPolicy.setSPNameQualifier(affiliationID);
        } else {
            nameIDPolicy.setSPNameQualifier(spEntityID);
        }

        nameIDPolicy.setAllowCreate(allowCreate);
        nameIDPolicy.setFormat(format);
        return nameIDPolicy;
    }
    
    /* Create Issuer */
    private static Issuer createIssuer(String spEntityID) 
                throws SAML2Exception {
        Issuer issuer = AssertionFactory.getInstance().createIssuer();
        issuer.setValue(spEntityID);
        return issuer;
    }

    /**
     * Create an AuthnRequest.
     *
     * @param realmName the authentication realm for this request
     * @param spEntityID the entity id for the service provider
     * @param paramsMap the map of parameters for the authentication request
     * @param spConfigMap the configuration map for the service provider
     * @param extensionsList a list of extendsions for the authentication request
     * @param spsso the SPSSODescriptorElement for theservcie provider
     * @param idpsso the IDPSSODescriptorElement for the identity provider
     * @param ssourl the url for the single sign on request
     * @param isForECP boolean to indicatge if the request originated from an ECP
     * @return a new AuthnRequest object
     * @throws SAML2Exception
     */
    public static AuthnRequest createAuthnRequest(final String realmName,
                                                  final String spEntityID,
                                                  final Map paramsMap,
                                                  final Map spConfigMap,
                                                  final List extensionsList,
                                                  final SPSSODescriptorElement spsso,
                                                  final IDPSSODescriptorElement idpsso,
                                                  final String ssourl,
                                                  final boolean isForECP) throws SAML2Exception {
        // generate unique request ID
        String requestID = SAML2Utils.generateID();
        if ((requestID == null) || (requestID.length() == 0)) {
            throw new SAML2Exception(
                    SAML2Utils.bundle.getString("cannotGenerateID"));
        }

        // retrieve data from the params map and if not found get
        // default values from the SPConfig Attributes
        // destinationURI required if message is signed.
         String destinationURI= getParameter(paramsMap,
                                             SAML2Constants.DESTINATION);
         Boolean isPassive = doPassive(paramsMap, spConfigMap);
         Boolean isforceAuthn = isForceAuthN(paramsMap, spConfigMap);
         boolean allowCreate = isAllowCreate(paramsMap, spConfigMap);
         boolean includeRequestedAuthnContextFlag = includeRequestedAuthnContext(paramsMap, spConfigMap);

         String consent = getParameter(paramsMap,SAML2Constants.CONSENT);
         Extensions extensions = createExtensions(extensionsList);
         String nameIDPolicyFormat = getParameter(paramsMap,
             SAML2Constants.NAMEID_POLICY_FORMAT);
         // get NameIDPolicy Element 
         NameIDPolicy nameIDPolicy = createNameIDPolicy(spEntityID,
             nameIDPolicyFormat, allowCreate, spsso, idpsso, realmName,
             paramsMap);
         Issuer issuer = createIssuer(spEntityID);
         Integer acsIndex = getIndex(paramsMap,SAML2Constants.ACS_URL_INDEX);
         Integer attrIndex = getIndex(paramsMap,SAML2Constants.ATTR_INDEX);
         
         String protocolBinding = isForECP ? SAML2Constants.PAOS : 
             getParameter(paramsMap, "binding");
         OrderedSet acsSet = getACSUrl(spsso,protocolBinding);
         String acsURL = (String) acsSet.get(0);
         protocolBinding = (String)acsSet.get(1);
         if (!SAML2Utils.isSPProfileBindingSupported(
             realmName, spEntityID, SAML2Constants.ACS_SERVICE, 
             protocolBinding))
         {
             SAML2Utils.debug.error("SPSSOFederate.createAuthnRequest:" +
                 protocolBinding +
                 "is not supported for " + spEntityID);
             String[] data = { spEntityID, protocolBinding };
             LogUtil.error(
                 Level.INFO, LogUtil.BINDING_NOT_SUPPORTED, data, null);
             throw new SAML2Exception(
                 SAML2Utils.bundle.getString("unsupportedBinding"));
         }
         
         AuthnRequest authnReq = 
                ProtocolFactory.getInstance().createAuthnRequest();    
         if (!isForECP) {
             if ((destinationURI == null) || (destinationURI.length() == 0)) {
                 authnReq.setDestination(XMLUtils.escapeSpecialCharacters(
                     ssourl));
             } else {
                 authnReq.setDestination(XMLUtils.escapeSpecialCharacters(
                     destinationURI));
             }
         }
         authnReq.setConsent(consent);
         authnReq.setIsPassive(isPassive);
         authnReq.setForceAuthn(isforceAuthn);
         authnReq.setAttributeConsumingServiceIndex(attrIndex);
         authnReq.setAssertionConsumerServiceIndex(acsIndex);
         authnReq.setAssertionConsumerServiceURL(
              XMLUtils.escapeSpecialCharacters(acsURL));
         authnReq.setProtocolBinding(protocolBinding);
         authnReq.setIssuer(issuer);
         authnReq.setNameIDPolicy(nameIDPolicy);
         if (includeRequestedAuthnContextFlag) {
             authnReq.setRequestedAuthnContext(createReqAuthnContext(realmName, spEntityID, paramsMap, spConfigMap));
         }
         if (extensions != null) {
               authnReq.setExtensions(extensions);
         }
        
        // Required attributes in authn request
        authnReq.setID(requestID);
        authnReq.setVersion(SAML2Constants.VERSION_2_0);
        authnReq.setIssueInstant(newDate());
        //IDP Proxy 
        Boolean enableIDPProxy = 
            getAttrValueFromMap(spConfigMap, 
            SAML2Constants.ENABLE_IDP_PROXY); 
        if ((enableIDPProxy != null) && enableIDPProxy.booleanValue()) 
        {
            Scoping scoping = 
                ProtocolFactory.getInstance().createScoping();
            String proxyCountParam = getParameter(spConfigMap,
                SAML2Constants.IDP_PROXY_COUNT);
            if (proxyCountParam != null && (!proxyCountParam.equals(""))) { 
                scoping.setProxyCount(new Integer(proxyCountParam));
            }
            List proxyIDPs = (List) spConfigMap.get(
                SAML2Constants.IDP_PROXY_LIST);
            if (proxyIDPs != null && !proxyIDPs.isEmpty()) {
                Iterator iter = proxyIDPs.iterator();
                ArrayList list = new ArrayList();
                while(iter.hasNext()) {
                    IDPEntry entry = ProtocolFactory.getInstance().
                        createIDPEntry(); 
                    entry.setProviderID((String)iter.next());
                    list.add(entry);
                }
                IDPList idpList = ProtocolFactory.getInstance().
                    createIDPList();
                idpList.setIDPEntries(list);
                scoping.setIDPList(idpList);
             }
             authnReq.setScoping(scoping);
         }
 
        return authnReq;        
    }

    /**
     * Returns value of an boolean parameter in the SP SSO Config.
     * @param attrMap the map of attributes for the sso config
     * @param attrName the key to get the boolean value for
     * @return the value of the parameter in the sso config or null if the attribute was not found or was
     * not a boolean parameter
     */
    public static Boolean getAttrValueFromMap(final Map attrMap, final String attrName) {
        Boolean boolVal = null;
        if (attrMap!=null && attrMap.size()> 0) {
            String attrVal = getParameter(attrMap,attrName);
            if ((attrVal != null) 
                && ( (attrVal.equals(SAML2Constants.TRUE)) 
                || (attrVal.equals(SAML2Constants.FALSE)))) {
                boolVal = new Boolean(attrVal);
            }
          }
          return boolVal;
     }

    /**
     * Returns the SingleSignOnService service. If no binding is specified
     * it will return the first endpoint in the list matching either HTTP-Redirect or HTTP-Post.
     * If the binding is specified it will attempt to return a match.
     * If either of the above is not found it will return null.
     *
     * @param ssoServiceList list of sso services
     * @param binding        binding of the sso service to get the url for
     * @return a SingleSignOnServiceElement or null if no match found.
     */
    public static SingleSignOnServiceElement getSingleSignOnServiceEndpoint(
            List<SingleSignOnServiceElement> ssoServiceList, String binding) {
        SingleSignOnServiceElement preferredEndpoint = null;
        boolean noPreferredBinding = StringUtils.isEmpty(binding);
        for (SingleSignOnServiceElement endpoint : ssoServiceList) {
            if (noPreferredBinding && (SAML2Constants.HTTP_REDIRECT.equals(endpoint.getBinding())
                    || SAML2Constants.HTTP_POST.equals(endpoint.getBinding()))) {
                preferredEndpoint = endpoint;
                break;
            } else if (binding.equals(endpoint.getBinding())) {
                preferredEndpoint = endpoint;
                break;
            }
        }
        return preferredEndpoint;
    }
          
    /**
     * Returns an Ordered Set containing the AssertionConsumerServiceURL
     * and AssertionConsumerServiceIndex.
     */
    static OrderedSet getACSUrl(SPSSODescriptorElement spsso,
                                        String binding) {
        String responseBinding = binding;
        if ((binding != null) && (binding.length() > 0) &&
                (binding.indexOf(SAML2Constants.BINDING_PREFIX) == -1)) {
            responseBinding = 
                new StringBuffer().append(SAML2Constants.BINDING_PREFIX)
                                  .append(binding).toString();
        }
        List acsList = spsso.getAssertionConsumerService();
        String acsURL=null;
        if (acsList != null && !acsList.isEmpty()) {
            Iterator ac = acsList.iterator();
            while (ac.hasNext()) {
                AssertionConsumerServiceElement ace =
                    (AssertionConsumerServiceElement) ac.next();
                if ((ace != null && ace.isIsDefault()) && 
                  (responseBinding == null || responseBinding.length() ==0 )) {
                    acsURL = ace.getLocation();
                    responseBinding = ace.getBinding();
                    break;
                } else if ((ace != null) &&
                       (ace.getBinding().equals(responseBinding))) {
                    acsURL = ace.getLocation();
                    break;
                }
            }
        }
        OrderedSet ol = new OrderedSet();
        ol.add(acsURL);
        ol.add(responseBinding);
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("SPSSOFederate: AssertionConsumerService :"
                                + " URL :" + acsURL);
            SAML2Utils.debug.message("SPSSOFederate: AssertionConsumerService :"
                                     + " Binding Passed in Query: " + binding);
            SAML2Utils.debug.message("SPSSOFederate: AssertionConsumerService :"
                                     + " Binding : " + responseBinding);
        }
        return ol;
    }

    /**
     * Fills in the realm with the default top level realm if it does not contain a more specific subrealm.
     * i.e. if it is null or empty it becomes "/"
     * @param realm the current realm
     * @return the realm to use
     */
    public static String getRealm(final String realm) {
        return ((realm == null) || (realm.length() == 0)) ? "/" : realm;
    }

    /**
     * Gets isPassive attribute from the config map and parameters map.
     *
     * @param paramsMap the map of the parameters
     * @param spConfigAttrsMap the map of the configuration
     * @return boolean to indicate if the request should be passive
     */
    private static Boolean doPassive(Map paramsMap,Map spConfigAttrsMap){
        // get isPassive
        Boolean isPassive=Boolean.FALSE;
        String isPassiveStr =
                getParameter(paramsMap,SAML2Constants.ISPASSIVE);
        
        if ((isPassiveStr != null) &&
                ((isPassiveStr.equals(SAML2Constants.TRUE) ||
                (isPassiveStr.equals(SAML2Constants.FALSE))))) {
            isPassive = new Boolean(isPassiveStr);
        } else {
            isPassive = getAttrValueFromMap(spConfigAttrsMap,
                                            SAML2Constants.ISPASSIVE);
        }
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("SPSSOFederate: isPassive : " + isPassive);
        }
        return (isPassive == null) ? Boolean.FALSE : isPassive;
    }

    /* Returns value of ForceAuthn */
    private static Boolean isForceAuthN(Map paramsMap,Map spConfigAttrsMap) {
        Boolean isforceAuthn;
        String forceAuthn = getParameter(paramsMap,SAML2Constants.FORCEAUTHN);
        if ((forceAuthn != null) && 
                ((forceAuthn.equals(SAML2Constants.TRUE) ||
                (forceAuthn.equals(SAML2Constants.FALSE))))) {
                isforceAuthn = new Boolean(forceAuthn);
        } else {
            isforceAuthn = getAttrValueFromMap(spConfigAttrsMap,
                                               SAML2Constants.FORCEAUTHN);
        }
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("SPSSOFederate:ForceAuthn: " + forceAuthn);
        }
        return (isforceAuthn == null) ? Boolean.FALSE : isforceAuthn;
    }
    
    /* get value of AllowCreate */
    private static boolean isAllowCreate(Map paramsMap,Map spConfigAttrsMap) {
        //assuming default true? 
        boolean allowCreate=true;
        String allowCreateStr=getParameter(paramsMap,
                                           SAML2Constants.ALLOWCREATE);
        if ((allowCreateStr != null) &&
                ((allowCreateStr.equals(SAML2Constants.TRUE) ||
                (allowCreateStr.equals(SAML2Constants.FALSE))))
            ) {
            allowCreate = new Boolean(allowCreateStr).booleanValue();
        } else {
            Boolean val = getAttrValueFromMap(spConfigAttrsMap,
                                              SAML2Constants.ALLOWCREATE);
            if (val != null) {
                allowCreate = val.booleanValue();
            }
        }
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("SPSSOFederate:AllowCreate:"+ allowCreate);
        }
        return allowCreate;
    }

    private static boolean includeRequestedAuthnContext(Map paramsMap, Map spConfigAttrsMap) {

        // Default to true if this flag is not found to be backwards compatible.
        boolean result = true;

        // Check the parameters first in case the request wants to override the metadata value.
        Boolean val = getAttrValueFromMap(paramsMap, SAML2Constants.INCLUDE_REQUESTED_AUTHN_CONTEXT);
        if (val != null) {
            result = val;
        } else {
            val = getAttrValueFromMap(spConfigAttrsMap, SAML2Constants.INCLUDE_REQUESTED_AUTHN_CONTEXT);
            if (val != null) {
                result = val;
            }
        }

        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("SPSSOFederate:includeRequestedAuthnContext:" + result);
        }

        return result;
    }

    /* Returns the AssertionConsumerServiceURL Index */
    private static Integer getIndex(Map paramsMap,String attrName) {
        Integer attrIndex = null;
        String index = getParameter(paramsMap,attrName);
        if ((index != null) && (index.length() > 0)) {
          attrIndex = new Integer(index);
        }
        return attrIndex;      
    }
  
    /**
     * Gets the query parameter value for the param specified.
     * @param paramsMap the map of parameters
     * @param attrName the parameter name to get the value for
     * @return the string value for the given parameter
     */
    public static String getParameter(Map paramsMap,String attrName) {
        String attrVal = null;
        if ((paramsMap != null) && (!paramsMap.isEmpty())) { 
            List attrValList = (List)paramsMap.get(attrName);
            if (attrValList != null && !attrValList.isEmpty()) {
                attrVal = (String) attrValList.iterator().next();
            }
        }
        return attrVal;
    }
    
    /**
     * Gets the extensions list for the sp entity.
     *
     * @param entityID the entity of the id for get the extensions list for
     * @param realm the realm that the entity is configured in
     * @return a List ofd the extensions for the sso request
     */
    public static List getExtensionsList(String entityID,String realm) {
        List extensionsList = null;
        try {
            EntityDescriptorElement ed = sm.getEntityDescriptor(realm,entityID);
            if (ed != null) {
                 com.sun.identity.saml2.jaxb.metadata.ExtensionsType ext =
                                    ed.getExtensions();
                if (ext != null) {
                    extensionsList = ext.getAny();
                }
            }
        } catch (SAML2Exception e) {
            SAML2Utils.debug.error("SPSSOFederate:Error retrieving " +
                                "EntityDescriptor");
        }
        return extensionsList;
    }
    
    private static com.sun.identity.saml2.protocol.Extensions 
    createExtensions(List extensionsList) throws SAML2Exception {
        com.sun.identity.saml2.protocol.Extensions extensions=null;
        if (extensionsList != null && !extensionsList.isEmpty()) {
            extensions = 
                        ProtocolFactory.getInstance().createExtensions();
            extensions.setAny(extensionsList);
        }
        return extensions;
    }


    /**
     * Gets the Relay State ID for the request.
     *
     * @param relayState the relay state
     * @param requestID the request id
     * @return the relay state id
     */
    public static String getRelayStateID(String relayState, String requestID) {
        
        SPCache.relayStateHash.put(requestID, new CacheObject(relayState));
        
        if (SAML2FailoverUtils.isSAML2FailoverEnabled()) {
            // sessionExpireTime is counted in seconds
            long sessionExpireTime = currentTimeMillis() / 1000 + SPCache.interval;
            // Need to make the key unique due to the requestID also being used to
            // store a copy of the AuthnRequestInfo
            String key = requestID + requestID;
            try {
                SAML2FailoverUtils.saveSAML2TokenWithoutSecondaryKey(key, relayState, sessionExpireTime);
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message("SPSSOFederate.getRelayStateID: SAVE relayState for requestID "
                            + key);
                }
            } catch (SAML2TokenRepositoryException se) {
                SAML2Utils.debug.error("SPSSOFederate.getRelayStateID: Unable to SAVE relayState for requestID "
                        + key, se);
            }
        }
        
        return requestID;
    }

   /* Creates RequestedAuthnContext Object */
   private static RequestedAuthnContext createReqAuthnContext(String realmName,
                                String spEntityID,Map paramsMap,
                                Map spConfigMap) {
        RequestedAuthnContext reqCtx = null;
        String className = null;
        if ((spConfigMap != null) && (!spConfigMap.isEmpty())) {
            List listVal = 
                (List) spConfigMap.get(
                            SAML2Constants.SP_AUTHCONTEXT_MAPPER);
            if (listVal != null && listVal.size() != 0) {
                className = ((String) listVal.iterator().next()).trim();
            }
        }

        SPAuthnContextMapper spAuthnContextMapper = 
            SAML2Utils.getSPAuthnContextMapper(realmName,spEntityID,className);

        try {
            reqCtx = 
                spAuthnContextMapper.getRequestedAuthnContext(
                            realmName,spEntityID,paramsMap);

        } catch (SAML2Exception e) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("SPSSOFederate:Error creating " +
                                         "RequestedAuthnContext",e);
            }
        }

        return reqCtx;
   }

    /**
     * Signs the query string.
     *
     * @param queryString the query string
     * @param certAlias the certificate alias
     * @return the signed query string
     * @throws SAML2Exception
     */
    public static String signQueryString(final String queryString, final String certAlias)
        throws SAML2Exception {
        if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("SPSSOFederate:queryString:" 
                                            + queryString);
                SAML2Utils.debug.message("SPSSOFederate: certAlias :" 
                                            + certAlias);
        }
        KeyProvider kp = KeyUtil.getKeyProviderInstance();
        PrivateKey privateKey = kp.getPrivateKey(certAlias);
        return QuerySignatureUtil.sign(queryString,privateKey);
    }

    /**
     * Sign an authentication request.
     *
     * @param certAlias the certificate alias
     * @param authnRequest the authentication request to sign
     * @throws SAML2Exception the signed authentication request
     */
    public static void signAuthnRequest(final String certAlias,
                                        final AuthnRequest authnRequest) throws SAML2Exception {

        KeyProvider kp = KeyUtil.getKeyProviderInstance();
        if (kp == null) {
            SAML2Utils.debug.error("SPSSOFederate:signAuthnRequest: " +
                "Unable to get a key provider instance.");
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "nullKeyProvider"));
        }
        authnRequest.sign(kp.getPrivateKey(certAlias),
            kp.getX509Certificate(certAlias));
    }
}
