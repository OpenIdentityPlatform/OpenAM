/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: NameIDMapping.java,v 1.6 2009/11/20 21:41:16 exu Exp $
 *
 */

/*
 * Portions Copyrighted 2013 ForgeRock, Inc.
 */

package com.sun.identity.saml2.profile;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Element;

import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.EncryptedID;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.common.AccountUtils;
import com.sun.identity.saml2.common.NameIDInfo;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.NameIDMappingServiceElement;
import com.sun.identity.saml2.jaxb.metadata.RoleDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorElement;
import com.sun.identity.saml2.key.EncInfo;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.plugins.IDPAccountMapper;
import com.sun.identity.saml2.protocol.NameIDMappingRequest;
import com.sun.identity.saml2.protocol.NameIDMappingResponse;
import com.sun.identity.saml2.protocol.NameIDPolicy;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.Status;

import com.sun.identity.shared.xml.XMLUtils;

/**
 * This class provides methods to send or process
 * <code>NameIDMappingRequest</code>.
 *
 * @supported.api
 */

public class NameIDMapping {
    static ProtocolFactory pf = ProtocolFactory.getInstance();
    static AssertionFactory af = AssertionFactory.getInstance();
    static SAML2MetaManager metaManager = null;
    static KeyProvider keyProvider = KeyUtil.getKeyProviderInstance(); 

    static SessionProvider sessionProvider = null;
    
    static {
        try {
            metaManager= new SAML2MetaManager();
            sessionProvider = SessionManager.getProvider();
        } catch (SAML2MetaException se) {
            SAML2Utils.debug.error(SAML2Utils.bundle.getString(
                "errorMetaManager"), se);
        } catch (SessionException sessE) {
            SAML2Utils.debug.error("Error retrieving session provider.", sessE);
        }
    }
    
    /**
     * Parses the request parameters and builds the NameIDMappingRequest to
     * sent to remote identity provider.
     *
     * @param session user session.
     * @param realm the realm of hosted entity
     * @param spEntityID entity ID of hosted service provider
     * @param idpEntityID entity ID of remote idendity provider
     * @param targetSPEntityID entity ID of target entity ID of service
     *     provider
     * @param targetNameIDFormat format of target Name ID
     * @param paramsMap Map of all other parameters
     *
     * @return the <code>NameIDMappingResponse</code>
     * @throws SAML2Exception if error initiating request to remote entity.
     *
     * @supported.api
     */
    public static NameIDMappingResponse initiateNameIDMappingRequest(
        Object session, String realm, String spEntityID, String idpEntityID,
        String targetSPEntityID, String targetNameIDFormat,
        Map paramsMap) throws SAML2Exception {
            
        if (spEntityID == null) {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("nullSPEntityID"));
        }
                
        if (idpEntityID == null)  {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("nullIDPEntityID"));
        }

        String userID = null;

        try {
            userID = sessionProvider.getPrincipalName(session);
        } catch (SessionException e) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(
                    "NameIDMapping.createNameIDMappingRequest: ", e);
            }
        }

        if (userID == null) {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("invalidSSOToken"));
        }
        
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(
                "NameIDMapping.initiateNameMappingRequest:" +
                " IDP EntityID is : " + idpEntityID);
            SAML2Utils.debug.message(
                "NameIDMapping.initiateNameMappingRequest:" +
                " SP HOST EntityID is : " + spEntityID); 
            SAML2Utils.debug.message(
                "NameIDMapping.initiateNameMappingRequest:" +
                " target SP EntityID is : " + targetSPEntityID); 
        }
        
        try {
            // nameIDMappingService
            String binding = 
                SAML2Utils.getParameter(paramsMap, SAML2Constants.BINDING); 
            if (binding == null) {
                binding = SAML2Constants.SOAP;
            } else if (!binding.equals(SAML2Constants.SOAP)) {
                throw new SAML2Exception(
                    SAML2Utils.bundle.getString("nimServiceBindingUnsupport"));
            }

            String nimURL = SAML2Utils.getParameter(paramsMap,
                "nimURL");
            if (nimURL == null) {
                NameIDMappingServiceElement nameIDMappingService =
                    getNameIDMappingService(realm, idpEntityID, binding);

                if (nameIDMappingService != null) {
                    nimURL = nameIDMappingService.getLocation();
                }
            }
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(
                    "NameIDMapping.initiateNameMappingRequest:" +
                    " nimURL" + nimURL);
            }

            if (nimURL == null) {
                throw new SAML2Exception(
                    SAML2Utils.bundle.getString("nimServiceNotFound"));
            }

            NameIDMappingRequest nimRequest = createNameIDMappingRequest(
                userID, realm, spEntityID, idpEntityID, nimURL,
                targetSPEntityID, targetNameIDFormat);

            signNIMRequest(nimRequest, realm, spEntityID, false);

            BaseConfigType config = metaManager.getIDPSSOConfig(realm,
                idpEntityID);

            nimURL = SAML2SDKUtils.fillInBasicAuthInfo(config, nimURL);

            return doNIMBySOAP(nimRequest.toXMLString(true,true), nimURL, 
                realm, spEntityID);

        } catch (SAML2MetaException sme) {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("metaDataError"));            
        }
    }
    
    public static NameIDMappingResponse processNameIDMappingRequest(
        NameIDMappingRequest nimRequest, String realm, String idpEntityID)
        throws SAML2Exception {

        NameIDMappingResponse nimResponse = null;
        String spEntityID = nimRequest.getIssuer().getValue();
        if (spEntityID == null)  {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("nullSPEntityID"));
        }

        String responseID = SAML2Utils.generateID();
        if (responseID == null) {
            SAML2Utils.debug.error(
                SAML2Utils.bundle.getString("failedToGenResponseID"));
        }
        nimResponse = pf.createNameIDMappingResponse();
        nimResponse.setID(responseID);
        nimResponse.setInResponseTo(nimRequest.getID());
        nimResponse.setVersion(SAML2Constants.VERSION_2_0);
        nimResponse.setIssueInstant(new Date());
        nimResponse.setIssuer(SAML2Utils.createIssuer(idpEntityID)); 

        SAML2Utils.verifyRequestIssuer(realm, idpEntityID,
            nimRequest.getIssuer(), nimRequest.getID());


        NameIDPolicy nameIDPolicy = nimRequest.getNameIDPolicy();
        String targetSPEntityID = nameIDPolicy.getSPNameQualifier();
        String format = nameIDPolicy.getFormat();

        Status status = null;

        if ((format != null) && (format.length() != 0) &&
            (!format.equals(SAML2Constants.PERSISTENT)) &&
            (!format.equals(SAML2Constants.UNSPECIFIED))) {

            nimResponse.setNameID(nimRequest.getNameID());
            nimResponse.setEncryptedID(nimRequest.getEncryptedID());
            status = SAML2Utils.generateStatus(
            SAML2Constants.INVALID_NAME_ID_POLICY,
                 SAML2Utils.bundle.getString("targetNameIDFormatUnsupported"));
        } else if ((targetSPEntityID == null) ||
            (targetSPEntityID.length() == 0) ||
            targetSPEntityID.equals(spEntityID)) {

            nimResponse.setNameID(nimRequest.getNameID());
            nimResponse.setEncryptedID(nimRequest.getEncryptedID());
            status = SAML2Utils.generateStatus(
                SAML2Constants.INVALID_NAME_ID_POLICY,
                SAML2Utils.bundle.getString("targetNameIDNoChange"));
        } else {
            // check if source SP has account fed
            // if yes then get nameid of targetSP
            IDPAccountMapper idpAcctMapper = SAML2Utils.getIDPAccountMapper(
                realm, idpEntityID);

            NameID nameID = getNameID(nimRequest, realm, idpEntityID);
            String userID = idpAcctMapper.getIdentity(nameID, idpEntityID,
                spEntityID, realm);
            NameIDInfo targetNameIDInfo = null;
            if (userID != null) {
                targetNameIDInfo = AccountUtils.getAccountFederation(userID,
                    idpEntityID, targetSPEntityID);
            }
            if (targetNameIDInfo == null) {
                nimResponse.setNameID(nimRequest.getNameID());
                nimResponse.setEncryptedID(nimRequest.getEncryptedID());
                status = SAML2Utils.generateStatus(
                    SAML2Constants.INVALID_NAME_ID_POLICY,
                    SAML2Utils.bundle.getString("targetNameIDNotFound"));
            } else {
                NameID targetSPNameID = targetNameIDInfo.getNameID();
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(
                        "NameIDMapping.processNameIDMappingRequest: " +
                        "User ID = " + userID + ", name ID = " +
                        targetSPNameID.toXMLString(true,true));
                }

                nimResponse.setEncryptedID(getEncryptedID(targetSPNameID,
                    realm, spEntityID, SAML2Constants.SP_ROLE));
                status = SAML2Utils.generateStatus(
                    SAML2Constants.SUCCESS, null);
	    }
        }

        nimResponse.setStatus(status);
        signNIMResponse(nimResponse, realm, idpEntityID, false);

        return nimResponse;
    }
    
    static private NameIDMappingRequest createNameIDMappingRequest(
        String userID, String realm, String spEntityID, String idpEntityID,
        String destination, String targetSPEntityID, String targetNameIDFormat)
        throws SAML2Exception {

        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(
                "NameIDMapping.createNameIDMappingRequest: User ID : " +
                userID);
        }
        
        NameIDMappingRequest nimRequest = pf.createNameIDMappingRequest();
        
        nimRequest.setID(SAML2Utils.generateID());
        nimRequest.setVersion(SAML2Constants.VERSION_2_0);
        nimRequest.setDestination(XMLUtils.escapeSpecialCharacters(
            destination));
        nimRequest.setIssuer(SAML2Utils.createIssuer(spEntityID));
        nimRequest.setIssueInstant(new Date());

        setNameIDForNIMRequest(nimRequest, realm, spEntityID, idpEntityID,
            targetSPEntityID, targetNameIDFormat, userID);
        return nimRequest;
    }

    static private NameIDMappingResponse doNIMBySOAP(
        String nimRequestXMLString, String nimURL, String realm,
        String spEntityID) throws SAML2Exception {

        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("NameIDMapping.doNIMBySOAP: " +
                "NIMRequestXMLString : " + nimRequestXMLString);
            SAML2Utils.debug.message("NameIDMapping.doNIMBySOAP: " +
                "NIMRedirectURL : " + nimURL);
        }
        
        SOAPMessage resMsg = null;
        try {
            resMsg = SAML2Utils.sendSOAPMessage(nimRequestXMLString, nimURL,
                true);
        } catch (SOAPException se) {
            SAML2Utils.debug.error("NameIDMapping.doNIMBySOAP: ", se);
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "invalidSOAPMessge"));
        }

        Element nimRespElem = SAML2Utils.getSamlpElement(resMsg,
            SAML2Constants.NAME_ID_MAPPING_RESPONSE);
        NameIDMappingResponse nimResponse = 
             pf.createNameIDMappingResponse(nimRespElem);
        
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("NameIDMapping.doNIMBySOAP: " +
                "NameIDMappingResponse without SOAP envelope:\n" +
                nimResponse.toXMLString(true,true));
        }


        String idpEntityID = nimResponse.getIssuer().getValue();
        Issuer resIssuer = nimResponse.getIssuer();
        String requestId = nimResponse.getInResponseTo();
        SAML2Utils.verifyResponseIssuer(realm, spEntityID, resIssuer,
            requestId);
                    
        if (!verifyNIMResponse(nimResponse, realm, idpEntityID)) {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("invalidSignInResponse"));
        }

        return nimResponse;
    }

    static private void setNameIDForNIMRequest(NameIDMappingRequest nimRequest,
        String realm, String spEntityID, String idpEntityID,
        String targetSPEntityID, String targetNameIDFormat, String userID)
        throws SAML2Exception {

        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("NameIDMapping.setNameIDForNIMRequest: " +
                "user ID = " + userID);
        }

        NameID nameID = AssertionFactory.getInstance().createNameID();
        NameIDInfo info = AccountUtils.getAccountFederation(userID, spEntityID,
            idpEntityID);
        nameID.setValue(info.getNameIDValue());
        nameID.setFormat(info.getFormat());
        nameID.setNameQualifier(idpEntityID);
        nameID.setSPNameQualifier(spEntityID);

        NameIDPolicy nameIDPolicy =
            ProtocolFactory.getInstance().createNameIDPolicy();
        nameIDPolicy.setSPNameQualifier(targetSPEntityID);
        nameIDPolicy.setFormat(targetNameIDFormat);
        nimRequest.setNameIDPolicy(nameIDPolicy);

        boolean needEncryptIt = SAML2Utils.getWantNameIDEncrypted(realm,
            idpEntityID, SAML2Constants.IDP_ROLE);
        if (!needEncryptIt) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(
                    "NameIDMapping.setNameIDForNIMRequest: "  +
                    "NamID doesn't need to be encrypted.");
            }
            nimRequest.setNameID(nameID);
            return;
        }
        
        EncryptedID encryptedID = getEncryptedID(nameID, realm, idpEntityID,
            SAML2Constants.IDP_ROLE);

        nimRequest.setEncryptedID(encryptedID);
    }    

    /**
     * Returns first NameIDMappingService matching specified binding in an
     * entity under the realm.
     *
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved.
     * @param binding bind type need to has to be matched.
     * @return <code>ManageNameIDServiceElement</code> for the entity or null
     * @throws SAML2MetaException if unable to retrieve the first identity
     *     provider's SSO configuration.
     * @throws SessionException invalid or expired single-sign-on session
     */
    static public NameIDMappingServiceElement getNameIDMappingService(
        String realm, String entityId, String binding)
        throws SAML2MetaException {


        IDPSSODescriptorElement idpSSODesc = metaManager.getIDPSSODescriptor(
            realm, entityId);
        if (idpSSODesc == null) {
            SAML2Utils.debug.error(SAML2Utils.bundle.getString("noIDPEntry"));
            return null;
        }

        List list = idpSSODesc.getNameIDMappingService();

        NameIDMappingServiceElement nimService = null;
        if ((list != null) && !list.isEmpty()) {
            if (binding == null) {
                return (NameIDMappingServiceElement)list.get(0);
            }
            Iterator it = list.iterator();
            while (it.hasNext()) {
                nimService = (NameIDMappingServiceElement)it.next();  
                if (binding.equalsIgnoreCase(nimService.getBinding())) {
                    return nimService;
                }
            }
        }
        return null;
    }
        
    static EncryptedID getEncryptedID(NameID nameID, String realm,
        String entityID, String role) throws SAML2Exception {

        RoleDescriptorType roled = null;

        if (role.equals(SAML2Constants.SP_ROLE)) {
            roled = metaManager.getSPSSODescriptor(realm, entityID);
        } else {
            roled = metaManager.getIDPSSODescriptor(realm, entityID);
        }

        EncInfo encInfo = KeyUtil.getEncInfo(roled, entityID, role);
        
        if (encInfo == null) {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("UnableToFindEncryptKeyInfo"));
        }
        
        EncryptedID encryptedID = nameID.encrypt(encInfo.getWrappingKey(), 
            encInfo.getDataEncAlgorithm(), encInfo.getDataEncStrength(),
            entityID);

        return encryptedID;
    }    

    private static void signNIMRequest(NameIDMappingRequest nimRequest, 
        String realm, String spEntityID, boolean includeCert)
        throws SAML2Exception {

        String alias = SAML2Utils.getSigningCertAlias(realm, spEntityID,
            SAML2Constants.SP_ROLE);
        
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("NameIDMapping.signNIMRequest: " +
                "Cert Alias is : " + alias);
            SAML2Utils.debug.message("NameIDMapping.signNIMRequest: " +
                "NIMRequest before sign : " +
                nimRequest.toXMLString(true, true));
        }
        PrivateKey signingKey = keyProvider.getPrivateKey(alias);
        X509Certificate signingCert = null;
        if (includeCert) {
            signingCert = keyProvider.getX509Certificate(alias);
        }
        
        if (signingKey != null) {
            nimRequest.sign(signingKey, signingCert);
        } else {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("missingSigningCertAlias"));
        }
        
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("NameIDMapping.signNIMRequest: " +
                "NIMRequest after sign : " +
                nimRequest.toXMLString(true, true));
        }
    }

    private static boolean verifyNIMRequest(NameIDMappingRequest nimRequest, 
        String realm, String spEntityID) throws SAML2Exception {

        SPSSODescriptorElement spSSODesc =
            metaManager.getSPSSODescriptor(realm, spEntityID);
        X509Certificate signingCert = KeyUtil.getVerificationCert(spSSODesc,
            spEntityID, SAML2Constants.SP_ROLE);

        if (signingCert != null) {
            boolean valid = nimRequest.isSignatureValid(signingCert);
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("NameIDMapping:verifyNIMRequest: " +
                "Signature is : " + valid);
            }
            return valid;
        } else {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("missingSigningCertAlias"));
        }
    }

    static void signNIMResponse(NameIDMappingResponse nimResponse,
        String realm, String idpEntityID, boolean includeCert)
        throws SAML2Exception {

        String alias = SAML2Utils.getSigningCertAlias(realm, idpEntityID,
            SAML2Constants.IDP_ROLE);
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("NameIDMapping.signNIMResponse: " +
                realm);
            SAML2Utils.debug.message("NameIDMapping.signNIMResponse: " +
                idpEntityID);
            SAML2Utils.debug.message("NameIDMapping.signNIMResponse: " +
                alias);

        }

        String encryptedKeyPass =
                SAML2Utils.getSigningCertEncryptedKeyPass(realm, idpEntityID, SAML2Constants.IDP_ROLE);
        PrivateKey signingKey;
        if (encryptedKeyPass == null || encryptedKeyPass.isEmpty()) {
            signingKey = keyProvider.getPrivateKey(alias);
        } else {
            signingKey = keyProvider.getPrivateKey(alias, encryptedKeyPass);
        }
        X509Certificate signingCert = null;
        if (includeCert) {
            signingCert = keyProvider.getX509Certificate(alias);
        }

        if (signingKey != null) {
            nimResponse.sign(signingKey, signingCert); 
        } else {
            SAML2Utils.debug.error("NameIDMapping.signNIMResponse: " +
                "Incorrect configuration for Signing Certificate.");
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("metaDataError"));
        }

    }

    private static boolean verifyNIMResponse(NameIDMappingResponse nimResponse,
        String realm, String idpEntityID) throws SAML2Exception {

        IDPSSODescriptorElement idpSSODesc = metaManager.getIDPSSODescriptor(
            realm, idpEntityID);
        X509Certificate signingCert = KeyUtil.getVerificationCert(idpSSODesc,
            idpEntityID, SAML2Constants.IDP_ROLE);
        
        if (signingCert != null) {
            boolean valid = nimResponse.isSignatureValid(signingCert);
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("NameIDMapping.verifyNIMResponse: " +
                    "Signature is : " + valid);
            }
            return valid;
        } else {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("missingSigningCertAlias"));
        }
    }

    private static NameID getNameID(NameIDMappingRequest nimRequest,
        String realm, String idpEntityID){

        NameID nameID = nimRequest.getNameID();
        if (nameID == null) {
            String alias = SAML2Utils.getSigningCertAlias(realm, idpEntityID, SAML2Constants.IDP_ROLE);
            String encryptedKeyPass =
                    SAML2Utils.getSigningCertEncryptedKeyPass(realm, idpEntityID, SAML2Constants.IDP_ROLE);
            PrivateKey signingKey;
            if (encryptedKeyPass == null || encryptedKeyPass.isEmpty()) {
                signingKey = keyProvider.getPrivateKey(alias);
            } else {
                signingKey = keyProvider.getPrivateKey(alias, encryptedKeyPass);
            }

            EncryptedID encryptedID = nimRequest.getEncryptedID();
            try {
                nameID = encryptedID.decrypt(signingKey);
            } catch (SAML2Exception ex) {
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message("NameIDMapping.getNameID:", ex);
                }
                return null;
            }
        }

        if (!SAML2Utils.isPersistentNameID(nameID)) {
            return null;
        }

        return nameID;
    }
}
