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
 * $Id: IDFFMetaSecurityUtils.java,v 1.5 2009/06/08 23:40:42 madan_ranganath Exp $
 *
 * Portions Copyrighted 2011-2014 ForgeRock AS
 */

package com.sun.identity.federation.meta;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.jaxb.entityconfig.AttributeType;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.federation.jaxb.entityconfig.IDPDescriptorConfigElement;
import com.sun.identity.federation.jaxb.entityconfig.ObjectFactory;
import com.sun.identity.federation.jaxb.entityconfig.SPDescriptorConfigElement;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.List;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;

import com.sun.identity.saml.xmlsig.KeyProvider;

import com.sun.identity.federation.key.KeyUtil;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.liberty.ws.meta.jaxb.EntityDescriptorElement;
import com.sun.identity.liberty.ws.meta.jaxb.KeyDescriptorElement;
import com.sun.identity.liberty.ws.meta.jaxb.IDPDescriptorType;
import com.sun.identity.liberty.ws.meta.jaxb.SPDescriptorType;
import com.sun.identity.liberty.ws.meta.jaxb.ProviderDescriptorType;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.xml.bind.JAXBException;

/**
 * The <code>IDFFMetaSecurityUtils</code> class provides metadata security 
 * related utility functions.
 */
public final class IDFFMetaSecurityUtils {

    public static final String NS_XMLSIG = "http://www.w3.org/2000/09/xmldsig#";
    public static final String NS_XMLENC = "http://www.w3.org/2001/04/xmlenc#";
    public static final String NS_META="urn:liberty:metadata:2003-08";

    private static Debug debug = IDFFMetaUtils.debug;
    private static KeyProvider keyProvider = null;
    private static KeyStore keyStore = null;
    private static boolean keyProviderInitialized = false;

    private IDFFMetaSecurityUtils() {

    }

    private static synchronized void initializeKeyStore() {
        if (keyProviderInitialized) {
            return;
        }

        org.apache.xml.security.Init.init();

        keyProvider = KeyUtil.getKeyProviderInstance();
        if (keyProvider != null) {
            keyStore = keyProvider.getKeyStore();
        }

        keyProviderInitialized = true;
    }

    /**
     * Returns BASE64 encoded X509 Certificate string corresponding to the 
     * certificate alias.
     * @param certAlias Alias of the Certificate to be retrieved.
     * @return BASE64 encoded X509 Certificate string, return null if null
     *    or empty certificate alias is specified.
     * @throws IDFFMetaException if unable to retrieve the certificate from the
     *     internal key store.
     */
    public static String buildX509Certificate(String certAlias)
        throws IDFFMetaException
    {
        if ((certAlias == null) || (certAlias.trim().length() == 0)) {
            return null;
        }

        if (!keyProviderInitialized) {
            initializeKeyStore();
        }

        X509Certificate cert = keyProvider.getX509Certificate(certAlias);

        if (cert != null) {
            try {
                return Base64.encode(cert.getEncoded(), true);
            } catch (Exception ex) {
                if (debug.messageEnabled()) {
                    debug.message(
                          "IDFFMetaSecurityUtils.buildX509Certificate:", ex);
                }
            }
        }

        Object[] objs = { certAlias };
        throw new IDFFMetaException("invalid_cert_alias", objs);
    }

    /**
     * Updates signing or encryption key info for SP or IDP. 
     * This will update both signing/encryption alias on extended metadata and
     * certificates in standard metadata. 
     * @param realm Realm the entity resides.
     * @param entityID ID of the entity to be updated.  
     * @param certAlias Alias of the certificate to be set to the entity. If
     *        null, will remove existing key information from the SP or IDP.
     * @param isSigning true if this is signing certificate alias, false if 
     *        this is encryption certification alias.
     * @param isIDP true if this is for IDP signing/encryption alias, false
     *        if this is for SP signing/encryption alias
     * @param encAlgo Encryption algorithm URI, this is applicable for
     *        encryption cert only.
     * @param keySize Encryption key size, this is applicable for
     *        encryption cert only. 
     * @throws IDFFMetaException if failed to update the certificate alias for 
     *        the entity.
     */
    public static void updateProviderKeyInfo(String realm,
        String entityID, String certAlias, boolean isSigning, boolean isIDP,
        String encAlgo, int keySize) throws IDFFMetaException { 
        IDFFMetaManager metaManager = FSUtils.getIDFFMetaManager();
        EntityConfigElement config = 
            metaManager.getEntityConfig(realm, entityID);
        if (!config.isHosted()) {
            String[] args = {entityID, realm};
            throw new IDFFMetaException("entityNotHosted", args);
        }
        EntityDescriptorElement desp = metaManager.getEntityDescriptor(
            realm, entityID);
        if (isIDP) {
            IDPDescriptorConfigElement idpConfig = 
                IDFFMetaUtils.getIDPDescriptorConfig(config);
            IDPDescriptorType idpDesp = 
                IDFFMetaUtils.getIDPDescriptor(desp);
            if ((idpConfig == null) || (idpDesp == null)) {
                String[] args = {entityID, realm};
                throw new IDFFMetaException("entityNotIDP", args);
            }
   
            // update standard metadata
            if ((certAlias == null) || (certAlias.length() == 0)) {
                // remove key info
                removeKeyDescriptor(idpDesp, isSigning); 
                if (isSigning) {
                    setExtendedAttributeValue(idpConfig, 
                        IFSConstants.SIGNING_CERT_ALIAS, null); 
                } else {
                    setExtendedAttributeValue(idpConfig, 
                        IFSConstants.ENCRYPTION_CERT_ALIAS, null); 
                }
            } else {
                KeyDescriptorElement kde = 
                    getKeyDescriptor(certAlias, isSigning, encAlgo, keySize);
                updateKeyDescriptor(idpDesp, kde);
                // update extended metadata
                Set value = new HashSet();
                value.add(certAlias);
                if (isSigning) {
                    setExtendedAttributeValue(idpConfig, 
                        IFSConstants.SIGNING_CERT_ALIAS, value); 
                } else {
                    setExtendedAttributeValue(idpConfig, 
                        IFSConstants.ENCRYPTION_CERT_ALIAS, value); 
                }
            }
            metaManager.setEntityDescriptor(realm, desp);
            metaManager.setEntityConfig(realm, config); 
        } else {
            SPDescriptorConfigElement spConfig = 
                IDFFMetaUtils.getSPDescriptorConfig(config);
            SPDescriptorType spDesp = 
                IDFFMetaUtils.getSPDescriptor(desp);
            if ((spConfig == null) || (spDesp == null)) {
                String[] args = {entityID, realm};
                throw new IDFFMetaException("entityNotSP", args);
            }
            // update standard metadata
            if ((certAlias == null) || (certAlias.length() == 0)) {
                // remove key info
                removeKeyDescriptor(spDesp, isSigning); 
                if (isSigning) {
                    setExtendedAttributeValue(spConfig, 
                        IFSConstants.SIGNING_CERT_ALIAS, null); 
                } else {
                    setExtendedAttributeValue(spConfig, 
                        IFSConstants.ENCRYPTION_CERT_ALIAS, null); 
                }
            } else {
                KeyDescriptorElement kde = 
                    getKeyDescriptor(certAlias, isSigning, encAlgo, keySize);
                updateKeyDescriptor(spDesp, kde);
                // update extended metadata
                Set value = new HashSet();
                value.add(certAlias);
                if (isSigning) {
                    setExtendedAttributeValue(spConfig, 
                        IFSConstants.SIGNING_CERT_ALIAS, value); 
                } else {
                    setExtendedAttributeValue(spConfig, 
                        IFSConstants.ENCRYPTION_CERT_ALIAS, value); 
                }
            }
            metaManager.setEntityDescriptor(realm, desp);
            metaManager.setEntityConfig(realm, config); 
        }
    }

    private static void updateKeyDescriptor(ProviderDescriptorType desp, 
        KeyDescriptorElement newKey) {
        // NOTE : we only support one signing and one encryption key right now
        // the code need to be change if we need to support multiple signing
        // and/or encryption keys in one entity
        List keys = desp.getKeyDescriptor();
        for (Iterator iter = keys.iterator(); iter.hasNext();) {
            KeyDescriptorElement key = (KeyDescriptorElement) iter.next();
            if (key.getUse().equalsIgnoreCase(newKey.getUse())) {
                iter.remove();
            }
        }
        desp.getKeyDescriptor().add(newKey);
    }

    private static void removeKeyDescriptor(ProviderDescriptorType desp,
        boolean isSigningUse) {
        List keys = desp.getKeyDescriptor();
        String keyUse = "encryption";
        if (isSigningUse) {
            keyUse = "signing";
        }
        for (Iterator iter = keys.iterator(); iter.hasNext();) {
            KeyDescriptorElement key = (KeyDescriptorElement) iter.next();
            if (key.getUse().equalsIgnoreCase(keyUse)) {
                iter.remove();
            }
        }
    }
  
    private static void setExtendedAttributeValue(
        BaseConfigType config,
        String attrName, Set attrVal) throws IDFFMetaException {
        try {
            List attributes = config.getAttribute();
            for(Iterator iter = attributes.iterator(); iter.hasNext();) {
                AttributeType avp = (AttributeType)iter.next();
                if (avp.getName().trim().equalsIgnoreCase(attrName)) {
                     iter.remove(); 
                }
            }
            if (attrVal != null) {
                ObjectFactory factory = new ObjectFactory();
                AttributeType atype = factory.createAttributeType();
                atype.setName(attrName);
                atype.getValue().addAll(attrVal);
                config.getAttribute().add(atype);
            }
        } catch (JAXBException e) {
            throw new IDFFMetaException(e);
        }
    }

    private static KeyDescriptorElement getKeyDescriptor(
        String certAlias, boolean isSigning, String encAlgo, int keySize) 
        throws IDFFMetaException {
     
        try {
            String certString = 
                IDFFMetaSecurityUtils.buildX509Certificate(certAlias);
            StringBuffer sb = new StringBuffer(4000);
            sb.append("<KeyDescriptor xmlns=\"").append(NS_META)
                .append("\" use=\"");
            if (isSigning) {
                sb.append("signing");
            } else {
                sb.append("encryption");
            }
            sb.append("\">\n");
            if (!isSigning && (encAlgo != null)) {
                sb.append("<EncryptionMethod>").append(encAlgo)
                  .append("</EncryptionMethod>\n");
                sb.append("<KeySize>").append(keySize).append("</KeySize>\n");
            }
            sb.append("<KeyInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\">\n")
              .append("<X509Data>\n")
              .append("<X509Certificate>\n")
              .append(certString)
              .append("</X509Certificate>\n")
              .append("</X509Data>\n")
              .append("</KeyInfo>\n");
            sb.append("</KeyDescriptor>\n");
            return (KeyDescriptorElement) 
                IDFFMetaUtils.convertStringToJAXB(sb.toString());
        } catch (JAXBException e) {
            throw new IDFFMetaException(e);
        }
    } 
}
