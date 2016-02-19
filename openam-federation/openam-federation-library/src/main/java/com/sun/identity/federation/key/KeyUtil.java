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
 * $Id: KeyUtil.java,v 1.5 2009/06/08 23:41:03 madan_ranganath Exp $
 *
 * Portions Copyrighted 2013-2016 ForgeRock AS
 */

package com.sun.identity.federation.key;

import java.util.Map;
import java.util.List;
import java.util.Hashtable;
import java.util.Iterator;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import org.apache.xml.security.encryption.XMLCipher;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.liberty.ws.common.jaxb.xmlsig.KeyInfoType;
import com.sun.identity.liberty.ws.common.jaxb.xmlsig.X509DataElement;
import com.sun.identity.liberty.ws.meta.jaxb.ProviderDescriptorType;
import com.sun.identity.liberty.ws.meta.jaxb.KeyDescriptorType;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.xmlsig.KeyProvider;

/**
 * The <code>KeyUtil</code> provides methods to obtain
 * the hosting entity's signing key and decryption key, and
 * to obtain a partner entity's signature verification key
 * and encryption related information
 */
public class KeyUtil {
    
    private static KeyProvider kp = null;

    // key is EntityID|Role
    // value is EncInfo
    public static Hashtable encHash = new Hashtable();

    // key is EntityID|Role
    // value is X509Certificate
    protected static Hashtable sigHash = new Hashtable();
    
    static {
        try {
            kp = (KeyProvider)Class.forName(SystemConfigurationUtil.getProperty(
                SAMLConstants.KEY_PROVIDER_IMPL_CLASS,
                SAMLConstants.JKS_KEY_PROVIDER)).newInstance();
        } catch (ClassNotFoundException cnfe) {
            FSUtils.debug.error(
                "KeyUtil static block:" +
                " Couldn't find the class.",
                cnfe);
            kp = null;
        } catch (InstantiationException ie) {
            FSUtils.debug.error(
                "KeyUtil static block:" +
                " Couldn't instantiate the key provider instance.",
                ie);
            kp = null;
        } catch (IllegalAccessException iae) {
            FSUtils.debug.error(
                "KeyUtil static block:" +
                " Couldn't access the default constructor.",
                iae);
            kp = null;
        }            
    }

    private KeyUtil() {
    }

    /**
     * Returns the instance of <code>KeyProvider</code>.
     * @return <code>KeyProvider</code>
     */
    public static KeyProvider getKeyProviderInstance() {
        return kp;
    }

    /**
     * Returns the host entity's signing certificate alias.
     * @param baseConfig <code>BaseConfigType</code> for the host entity
     * @return <code>String</code> for host entity's signing
     * certificate alias
     */    
    public static String getSigningCertAlias(BaseConfigType baseConfig) {

        Map map = IDFFMetaUtils.getAttributes(baseConfig);
        List list = (List)map.get(IFSConstants.SIGNING_CERT_ALIAS);
        if ((list != null) && (!list.isEmpty())) {
            String alias = (String)list.get(0);
            if ((alias != null) && (alias.length() != 0) && (kp != null)) {
                return alias;
            }
        }
        return null;
    }

    /**
     * Returns the host entity's decryption key.
     * @param baseConfig <code>BaseConfigType</code> for the host entity
     * @return <code>PrivateKey</code> for decrypting a message received
     * by the host entity
     */    
    public static PrivateKey getDecryptionKey(BaseConfigType baseConfig) {

        Map map = IDFFMetaUtils.getAttributes(baseConfig);
        List list = (List)map.get(IFSConstants.ENCRYPTION_CERT_ALIAS);
        String alias = null;
        PrivateKey decryptionKey = null;
        if ((list != null) && (!list.isEmpty())) {
            alias = (String)list.get(0);
            if ((alias != null) && (alias.length() != 0) && (kp != null)) {
                decryptionKey = kp.getPrivateKey(alias);
            } else {
                FSUtils.debug.error("KeyUtil.getDecryptionKey: alias: {} or keyProvider: {} was null.", alias, kp);
            }
        }

        if (decryptionKey == null) {
            FSUtils.debug.error("KeyUtil.getDecryptionKey: No decryptionKey found for alias: {}", alias);
        }

        return decryptionKey;
    }

    /**
     * Returns the partner entity's signature verification certificate.
     * @param providerDescriptor <code>ProviderDescriptorType</code> for
     *     the partner entity
     * @param entityID partner entity's ID
     * @param isIDP whether partner entity's role is IDP or SP 
     * @return <code>X509Certificate</code> for verifying the partner
     * entity's signature
     */    
    public static X509Certificate getVerificationCert(
        ProviderDescriptorType providerDescriptor, String entityID,
        boolean isIDP) {
        
        String role = (isIDP) ? "idp":"sp";        
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("KeyUtil.getVerificationCert: " +
                "Entering... \nEntityID=" + entityID + "\nRole=" + role);
        }
        // first try to get it from cache
        String index = entityID.trim() + "|" + role;
        X509Certificate cert = (X509Certificate)sigHash.get(index);
        if (cert != null) {
            return cert;
        }
        // else get it from meta
        if (providerDescriptor == null) {
            FSUtils.debug.error("KeyUtil.getVerificationCert: " +
                "Null ProviderDescriptorType input for entityID=" +
                entityID + " in " + role + " role.");
            return null;
        }
        KeyDescriptorType kd =
            getKeyDescriptor(providerDescriptor, "signing");
        if (kd == null) {
            FSUtils.debug.error("KeyUtil.getVerificationCert: " +
                "No signing KeyDescriptor for entityID=" +
                entityID + " in " + role + " role.");
            return null;
        }
        cert = getCert(kd);
        if (cert == null) {
            FSUtils.debug.error("KeyUtil.getVerificationCert: " +
                "No signing cert for entityID=" +
                entityID + " in " + role + " role.");
            return null;
        }
        sigHash.put(index, cert);
        return cert;
    }
    
    /**
     * Returns the encryption information which will be used in
     * encrypting messages intended for the partner entity.
     * @param providerDescriptor <code>ProviderDescriptorType</code> for
     *     the partner entity
     * @param entityID partner entity's ID
     * @param isIDP whether partner entity's role is IDP or SP 
     * @return <code>EncInfo</code> which includes partner entity's
     * public key for wrapping the secret key, data encryption algorithm,
     * and data encryption strength 
     */        
    public static EncInfo getEncInfo(ProviderDescriptorType providerDescriptor,
        String entityID, boolean isIDP) {

        String role = (isIDP) ? "idp":"sp";                
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("KeyUtil.getEncInfo: " +
                "Entering... \nEntityID=" + entityID + "\nRole="+role);
        }
        // first try to get it from cache
        String index = entityID.trim()+"|"+role;
        EncInfo encInfo = (EncInfo)encHash.get(index);
        if (encInfo != null) {
            return encInfo;
        }
        // else get it from meta
        if (providerDescriptor == null) {
            FSUtils.debug.error("KeyUtil.getEncInfo: " +
                "Null ProviderDescriptorType input for entityID=" +
                entityID + " in " + role + " role.");
            return null;
        }
        KeyDescriptorType kd =
            getKeyDescriptor(providerDescriptor, "encryption");
        if (kd == null) {
            FSUtils.debug.error("KeyUtil.getEncInfo: " +
                "No encryption KeyDescriptor for entityID=" +
                entityID + " in " + role + " role.");
            return null;
        }
        X509Certificate cert = getCert(kd);
        if (cert == null) {
            FSUtils.debug.error("KeyUtil.getEncInfo: " +
                "No encryption cert for entityID=" +
                entityID + " in " + role + " role.");
            return null;
        }

        String algorithm = kd.getEncryptionMethod();
        int keySize = kd.getKeySize().intValue();

        if ((algorithm == null) || (algorithm.length() == 0)) {
            algorithm = XMLCipher.AES_128;
            keySize = 128;
        }
        PublicKey pk = cert.getPublicKey();
        if (pk != null) {
            encInfo = new EncInfo(pk, algorithm, keySize);
        }
        if (encInfo != null) {
            encHash.put(index, encInfo);
        }
        return encInfo;
    }


    /**
     * Returns <code>KeyDescriptorType</code> from 
     * <code>ProviderDescriptorType</code>.
     * @param providerDescriptor <code>ProviderDescriptorType</code> which
     *     contains <code>KeyDescriptor</code>s.
     * @param usage type of the <code>KeyDescriptorType</code> to be retrieved.
     *     Its value is "encryption" or "signing".
     * @return KeyDescriptorType in <code>ProviderDescriptorType</code> that
     *     matched the usage type.
     */
    public static KeyDescriptorType getKeyDescriptor(
        ProviderDescriptorType providerDescriptor, String usage) {
        
        if (providerDescriptor == null) {
            return null;
        }

        List list = providerDescriptor.getKeyDescriptor();
        Iterator iter = list.iterator();
        KeyDescriptorType kd = null;
        String use = null;
        KeyDescriptorType noUsageKD = null;
        while (iter.hasNext()) {
            kd = (KeyDescriptorType)iter.next();
            use = kd.getUse();
            if ((use == null) || (use.trim().length() == 0)) {
		if (noUsageKD == null) {
                    noUsageKD = kd;
                }
                continue;
            }
            if (use.trim().toLowerCase().equals(usage)) {
                break;
            } else {
                kd = null;
            }
        }
        if (kd != null) {
            return kd;
        } else {
            return noUsageKD;
        }
    }

    /**
     * Returns certificate stored in <code>KeyDescriptorType</code> in
     * <code>ProviderDescriptorType</code>.
     * @param providerDescriptor <code>ProviderDescriptorType</code> which
     *     contains <code>KeyDescriptor</code>s.
     * @param usage type of the <code>KeyDescriptorType</code> to be retrieved.
     *     Its value is "encryption" or "signing".
     * @return X509Certificate contained in <code>KeyDescriptorType</code>; or
     *     <code>null</code> if no certificate is included.
     */
    public static X509Certificate getCert(
        ProviderDescriptorType providerDescriptor, String usage) {

        return getCert(getKeyDescriptor(providerDescriptor, usage));
    }

    /**
     * Returns certificate stored in <code>KeyDescriptorType</code>.
     * @param kd <code>KeyDescriptorType</code> which contains certificate info
     * @return X509Certificate contained in <code>KeyDescriptorType</code>; or
     *     <code>null</code> if no certificate is included.
     */
    public static X509Certificate getCert(KeyDescriptorType kd) {

        if (kd == null) {
            return null;
        }
        KeyInfoType ki = kd.getKeyInfo();
        if (ki == null) {
            FSUtils.debug.error("KeyUtil.getCert: No KeyInfo.");
            
            return null;
        }
        X509DataElement data = (X509DataElement) ki.getContent().get(0);
        byte[] bt = 
            ((com.sun.identity.liberty.ws.common.jaxb.xmlsig.X509DataType.X509Certificate)
             data.getX509IssuerSerialOrX509SKIOrX509SubjectName().get(0)).
            getValue();
        CertificateFactory cf = null;
        try {
            cf = CertificateFactory.getInstance("X.509");
        } catch (java.security.cert.CertificateException ce) {
            FSUtils.debug.error("KeyUtil.getCert: " +
                "Unable to get CertificateFactory for X.509 type", ce);
            return null;
        }                
        ByteArrayInputStream bais = new ByteArrayInputStream(bt);
        X509Certificate retCert = null;
        try {
            while (bais.available() > 0) {
                retCert = (X509Certificate)cf.generateCertificate(bais);
            }
        } catch (java.security.cert.CertificateException ce) {
            FSUtils.debug.error("KeyUtil.getCert: " +
                "Unable to generate certificate from byte "+
                "array input stream.", ce);
            return null;
        }
        return retCert;
    }        
} 
