/**
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
 * $Id: FSNameMappingHandler.java,v 1.3 2008/06/25 05:47:02 qcheng Exp $
 *
 */


package com.sun.identity.federation.services.namemapping;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import com.sun.identity.federation.accountmgmt.FSAccountManager;
import com.sun.identity.federation.accountmgmt.FSAccountMgmtException;
import com.sun.identity.federation.accountmgmt.FSAccountFedInfo;
import com.sun.identity.federation.accountmgmt.FSAccountFedInfoKey;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.key.KeyUtil;
import com.sun.identity.federation.message.FSNameIdentifierMappingResponse;
import com.sun.identity.federation.message.FSNameIdentifierMappingRequest;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.liberty.ws.meta.jaxb.ProviderDescriptorType;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import com.sun.identity.shared.xml.XMLUtils;
import javax.xml.soap.SOAPMessage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Handles <code>ID-FF</code> name identifier mapping.
 */
public class FSNameMappingHandler {
    
    private FSAccountManager accountMgr = null;
    private String hostedEntityID = null;
    private ProviderDescriptorType hostedProviderDesc = null;
    private BaseConfigType hostedConfig = null;
    private String metaAlias = null;
    private String realm = null;
    private static IDFFMetaManager metaManager = FSUtils.getIDFFMetaManager();
    
    /**
     * Construct a <code>FSNameMappingHandler</code> object for a provider.
     * @param entityID hosted provider's entity id
     * @param hostedDesc hosted provider's meta descriptor
     * @param hostedConfig hosted provider's extended meta config
     * @param metaAlias hsoted provider's meta alias
     */
    public FSNameMappingHandler(
        String entityID, 
        ProviderDescriptorType hostedDesc,
        BaseConfigType hostedConfig,
        String metaAlias) 
    {
        FSUtils.debug.message("FSNameMappingHandler: entering constructor");
        hostedEntityID = entityID;
        hostedProviderDesc = hostedDesc;
        this.hostedConfig = hostedConfig;
        this.metaAlias = metaAlias;
        this.realm = IDFFMetaUtils.getRealmByMetaAlias(metaAlias);
        try {
            accountMgr = FSAccountManager.getInstance(metaAlias);        
        } catch (FSAccountMgmtException e){
            FSUtils.debug.error("FSNameMappingHandler: " + 
                FSUtils.bundle.getString(
                    IFSConstants.FEDERATION_FAILED_ACCOUNT_INSTANCE));
        }
        
    }
 
    /**
     * Returns <code>NameIdentifier</code> of a provider.
     * @param userID user id.
     * @param remoteEntityID the provider id whose
     *  <code>NameIdentifier</code> is to be returned.
     * @param local <code>true</code> if <code>remoteProviderID</code> is
     *  a local provider; <code>false</code> otherwise.
     * @return <code>NameIdentifier</code> of an user corresponding to
     *  <code>remoteProviderID</code>.
     * @exception FSAccountMgmtException, SAMLException if an error occurred.
     */
    public NameIdentifier getNameIdentifier(
        String userID,
        String remoteEntityID,
        boolean local)
        throws FSAccountMgmtException, SAMLException
    {
        FSAccountFedInfo accountInfo = 
            accountMgr.readAccountFedInfo(userID, remoteEntityID);
        NameIdentifier nameIdentifier = null;
        if (local) {
            nameIdentifier = accountInfo.getLocalNameIdentifier();
            if (nameIdentifier == null) {
                NameIdentifier remoteNI = accountInfo.getRemoteNameIdentifier();
                if (remoteNI != null) {
                    nameIdentifier = new NameIdentifier(
                        remoteNI.getName(),
                        hostedEntityID,
                        remoteNI.getFormat());
                }
            }
        } else {
            nameIdentifier = accountInfo.getRemoteNameIdentifier();
            if (nameIdentifier == null) {
                NameIdentifier localNI = accountInfo.getLocalNameIdentifier();
                if (localNI != null) {
                    nameIdentifier = new NameIdentifier(
                        localNI.getName(),
                        remoteEntityID,
                        localNI.getFormat());
                }
            }
        }
        if (nameIdentifier != null &&
            (nameIdentifier.getFormat().length() == 0 ||
             nameIdentifier.getFormat()==null))
        {
            nameIdentifier.setFormat(IFSConstants.NI_FEDERATED_FORMAT_URI);
        }
        return nameIdentifier;
    }

    /**
     * Returns <code>NameIdentifier</code> of a remote provider.
     * @param mappingRequest name ID mapping request object
     * @param remoteEntityID the remote provider id whose
     *  <code>NameIdentifier</code> is to be returned.
     * @param local <code>true</code> if <code>remoteProviderID</code> is
     *  a local provider; <code>false</code> otherwise.
     * @return <code>NameIdentifier</code> corresponding to
     *  <code>remoteProviderID</code>.
     * @exception FSAccountMgmtException, SAMLException if an error occurred.
     */
    public NameIdentifier getNameIdentifier(
        FSNameIdentifierMappingRequest mappingRequest,
        String remoteEntityID,
        boolean local)
        throws FSAccountMgmtException, SAMLException
    {
        FSAccountFedInfoKey acctkey = new FSAccountFedInfoKey(
                mappingRequest.getProviderID(),
                mappingRequest.getNameIdentifier().getName().trim());
        Map env = new HashMap();
        env.put(IFSConstants.FS_USER_PROVIDER_ENV_NAMEMAPPING_KEY,
                        mappingRequest);
        String userID = accountMgr.getUserID(acctkey, realm, env);
        return getNameIdentifier(userID, remoteEntityID, local);
    }
    
    /**
     * Verifies signature on name identifier mapping response.
     * @param elt <code>DOM</code> element which contains
     *  <code>FSNameIdentifierMappingResopnse</code>
     * @param msg <code>SOAPMessage</code> object which contains signed
     *  name identifier mapping response.
     * @param realm the realm in which the provider resides
     * @return <code>true</code> if the signature is valid; <code>false</code>
     *  otherwise.
     */
    public static boolean verifyNameIdMappingResponseSignature(
        Element elt,
        SOAPMessage msg,
        String realm
    ) {
        FSUtils.debug.message(
            "FSNameMappingHandler.verifyNameIdMappingResponseSignature:Called");
        try {
            FSNameIdentifierMappingResponse nimRes =
                new FSNameIdentifierMappingResponse(elt);
            if(metaManager == null) {
                FSUtils.debug.error(
                    "FSNameMappingHandler.verifyNameIdMappingResponseSignature:"
                    + " Unable to get meta manager");
                return false;
            }
            
            String entityId = nimRes.getProviderID();
            X509Certificate cert = KeyUtil.getVerificationCert(
                metaManager.getIDPDescriptor(realm, entityId), entityId, true);
               
            if (cert == null) {
                FSUtils.debug.error("FSNameMappingHandler."
                    + "verifyNameIdMappingResponseSignature: couldn't obtain "
                    + "the cert for signature verification.");
                return false;
            }
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSNameMappingHandler.verifyNameIdMappingResponseSignature:"
                    + " Provider's cert is found.");
                FSUtils.debug.message(
                    "FSNameMappingHandler.verifyNameIdMappingResponseSignature:"
                    + "xmlString to be verified: " + XMLUtils.print(elt));
            }
            Document doc = (Document)FSServiceUtils.createSOAPDOM(msg);
            XMLSignatureManager manager = XMLSignatureManager.getInstance();
            return manager.verifyXMLSignature(doc, cert);
        } catch(Exception e){
            FSUtils.debug.error(
                "FSNameMappingHandler.verifyNameIdMappingResponseSignature: "
                + "Exception occured while verifying signature:", e);
            return false;
        }
    }
}
