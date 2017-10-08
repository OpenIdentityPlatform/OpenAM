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
 * $Id: KeyUtil.java,v 1.10 2009/08/28 23:42:14 exu Exp $
 *
 * Portions Copyrighted 2013-2016 ForgeRock AS.
 */
package com.sun.identity.saml2.key;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;
import java.util.Hashtable;
import java.util.Iterator;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Set;

import com.sun.identity.saml2.common.SAML2Utils;
import org.apache.xml.security.encryption.XMLCipher;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.jaxb.metadata.KeyDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.RoleDescriptorType;
import com.sun.identity.saml2.jaxb.xmlsig.*;
import com.sun.identity.saml2.jaxb.xmlenc.*;

import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.saml2.jaxb.metadata.XACMLAuthzDecisionQueryDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.XACMLPDPDescriptorElement;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;

/**
 * The <code>KeyUtil</code> provides methods to obtain
 * the hosting entity's signing key and decryption key, and
 * to obtain a partner entity's signature verification key
 * and encryption related information
 */
public class KeyUtil {
    
    private static KeyProvider keyProvider = null;

    // key is EntityID|Role
    // value is EncInfo
    protected static Hashtable encHash = new Hashtable();

    // key is EntityID|Role
    // value is X509Certificate
    protected static Map<String, Set<X509Certificate>> sigHash = new Hashtable<>();
    
    static {
        try {
            keyProvider = (KeyProvider)Class.forName(SystemConfigurationUtil.getProperty(
                SAMLConstants.KEY_PROVIDER_IMPL_CLASS,
                SAMLConstants.JKS_KEY_PROVIDER)).newInstance();
        } catch (ClassNotFoundException cnfe) {
            SAML2SDKUtils.debug.error(
                "KeyUtil static block:" +
                " Couldn't find the class.",
                cnfe);
            keyProvider = null;
        } catch (InstantiationException ie) {
            SAML2SDKUtils.debug.error(
                "KeyUtil static block:" +
                " Couldn't instantiate the key provider instance.",
                ie);
            keyProvider = null;
        } catch (IllegalAccessException iae) {
            SAML2SDKUtils.debug.error(
                "KeyUtil static block:" +
                " Couldn't access the default constructor.",
                iae);
            keyProvider = null;
        }            
    }

    private KeyUtil() {
    }

    /**
     * Returns the instance of <code>KeyProvider</code>.
     * @return <code>KeyProvider</code>
     */
    public static KeyProvider getKeyProviderInstance() {
        return keyProvider;
    }

    /**
     * Returns the host entity's signing certificate alias.
     * @param baseConfig <code>BaseConfigType</code> for the host entity
     * @return <code>String</code> for host entity's signing
     * certificate alias
     */    
    public static String getSigningCertAlias(BaseConfigType baseConfig) {

        Map map = SAML2MetaUtils.getAttributes(baseConfig);
        List list = (List)map.get(SAML2Constants.SIGNING_CERT_ALIAS);
        if (list != null && !list.isEmpty()) {
            String alias = (String)list.get(0);
            if (alias != null && alias.length() != 0 && keyProvider != null) {
                return alias;
            }
        }
        return null;
    }

    /**
     * Returns the hosted entity's decryption keys.
     *
     * @param realm The realm the hosted entity belongs to.
     * @param entityID The entity ID.
     * @param role The role of the hosted entity.
     * @return The Set of <code>PrivateKey</code>s for decrypting a message received by the hosted entity.
     */
    public static Set<PrivateKey> getDecryptionKeys(String realm, String entityID, String role) {
        return getDecryptionKeys(SAML2Utils.getEncryptionCertAliases(realm, entityID, role));
    }

    /**
     * Returns the host entity's decryption keys.
     *
     * @param baseConfig <code>BaseConfigType</code> for the host entity.
     * @return The Set of <code>PrivateKey</code>s for decrypting a message received by the hosted entity.
     */
    public static Set<PrivateKey> getDecryptionKeys(BaseConfigType baseConfig) {
        Map<String, List<String>> attrs = SAML2MetaUtils.getAttributes(baseConfig);
        List<String> aliases = attrs.get(SAML2Constants.ENCRYPTION_CERT_ALIAS);
        return getDecryptionKeys(aliases);
    }

    private static Set<PrivateKey> getDecryptionKeys(List<String> aliases) {
        final String classMethod = "KeyUtil.getDecryptionKeys: ";
        final Set<PrivateKey> decryptionKeys = new LinkedHashSet<>(3);
        if (aliases != null) {
            if (keyProvider != null) {
                for (String alias : aliases) {
                    if (StringUtils.isNotEmpty(alias)) {
                        PrivateKey decryptionKey = keyProvider.getPrivateKey(alias);
                        if (decryptionKey != null) {
                            decryptionKeys.add(decryptionKey);
                        } else {
                            SAML2SDKUtils.debug.error(classMethod + "No decryptionKey found for alias: {}", alias);
                        }
                    } else {
                        SAML2SDKUtils.debug.error(classMethod + "alias was empty.");
                    }
                }
            } else {
                SAML2SDKUtils.debug.error(classMethod + "keyProvider was null.");
            }
        } else {
            SAML2SDKUtils.debug.error(classMethod + "passed aliases list was null.");
        }

        return decryptionKeys;
    }
    /**
     * Returns the host entity's decryption key.
     * @param baseConfig <code>BaseConfigType</code> for the host entity
     * @return <code>PrivateKey</code> for decrypting a message received
     * by the host entity
     */    
    public static PrivateKey getDecryptionKey(BaseConfigType baseConfig) {
        return CollectionUtils.getFirstItem(getDecryptionKeys(baseConfig), null);
    }

    /**
     * Returns the partner entity's signature verification certificate.
     *
     * @param roleDescriptor <code>RoleDescriptor</code> for the partner entity.
     * @param entityID Partner entity's ID.
     * @param role Entity's role.
     * @return The set of signing {@link X509Certificate} for verifying the partner entity's signature.
     */
    public static Set<X509Certificate> getVerificationCerts(RoleDescriptorType roleDescriptor, String entityID,
            String role) {
        String classMethod = "KeyUtil.getVerificationCerts: ";

        // first try to get it from cache
        String index = entityID.trim() + "|" + role;
        Set<X509Certificate> certificates = sigHash.get(index);
        if (certificates != null) {
            return certificates;
        }

        certificates = new LinkedHashSet<>(3);
        // else get it from meta
        if (roleDescriptor == null) {
            SAML2SDKUtils.debug.error(
                classMethod+
                "Null RoleDescriptorType input for entityID=" +
                entityID + " in "+role+" role."
            );
            return null;
        }
        List<KeyDescriptorType> keyDescriptors = getKeyDescriptors(roleDescriptor, SAML2Constants.SIGNING);
        if (keyDescriptors.isEmpty()) {
            SAML2SDKUtils.debug.error(
                classMethod+
                "No signing KeyDescriptor for entityID=" +
                entityID + " in "+role+" role."
            );
            return certificates;
        }

        for (KeyDescriptorType keyDescriptor : keyDescriptors) {
            certificates.add(getCert(keyDescriptor));
        }
        if (certificates.isEmpty()) {
            SAML2SDKUtils.debug.error(
                classMethod +
                "No signing cert for entityID=" +
                entityID + " in "+role+" role."
            );
            return null;
        }
        sigHash.put(index, certificates);
        return certificates;
    }

    /**
     * Returns the encryption information which will be used in
     * encrypting messages intended for the partner entity.
     * @param roled <code>RoleDescriptor</code> for the partner entity
     * @param entityID partner entity's ID
     * @param role entity's role
     * @return <code>EncInfo</code> which includes partner entity's
     * public key for wrapping the secret key, data encryption algorithm,
     * and data encryption strength 
     */        
    public static EncInfo getEncInfo(
        RoleDescriptorType roled,
        String entityID,
        String role
    ) {

        String classMethod = "KeyUtil.getEncInfo: ";
        if (SAML2SDKUtils.debug.messageEnabled()) {
            SAML2SDKUtils.debug.message(
                classMethod +
                "Entering... \nEntityID=" +
                entityID + "\nRole="+role
            );
        }
        // first try to get it from cache
        String index = entityID.trim()+"|"+role;
        EncInfo encInfo = (EncInfo)encHash.get(index);
        if (encInfo != null) {
            return encInfo;
        }
        // else get it from meta
        if (roled == null) {
            SAML2SDKUtils.debug.error(
                classMethod+
                "Null RoleDescriptorType input for entityID=" +
                entityID + " in "+role+" role."
            );
            return null;
        }
        KeyDescriptorType kd =
            getKeyDescriptor(roled, SAML2Constants.ENCRYPTION);
        if (kd == null) {
            SAML2SDKUtils.debug.error(
                classMethod+
                "No encryption KeyDescriptor for entityID=" +
                entityID + " in "+role+" role."
            );
            return null;
        }
        java.security.cert.X509Certificate cert = getCert(kd);
        if (cert == null) {
            SAML2SDKUtils.debug.error(
                classMethod +
                "No encryption cert for entityID=" +
                entityID + " in "+role+" role."
            );
            return null;
        }
        List emList = kd.getEncryptionMethod();
        EncryptionMethodType em = null;
        String algorithm = null;
        int keySize = 0;
        if (emList != null && !emList.isEmpty()) {            
            em = (EncryptionMethodType)emList.get(0);
            if (em != null) {
                algorithm = em.getAlgorithm();
                List cList = em.getContent();
                if (cList != null) {
                    Iterator cIter = cList.iterator();
                    while (cIter.hasNext()) {
                        Object cObject = cIter.next();
                        if (cObject instanceof EncryptionMethodType.KeySize) {
                            keySize =
                                ((EncryptionMethodType.KeySize)(cList.get(0))).
                                    getValue().intValue();
                            break;
                        }
                    }
                }
            }
        }
        if (algorithm == null || algorithm.length() == 0) {
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
     * Returns the {@link KeyDescriptorType}s from {@link RoleDescriptorType} that matches the requested usage.
     * KeyDescriptors without usage defined are also included in this list, as by definition they should be suitable for
     * any purposes.
     *
     * @param roleDescriptor {@link RoleDescriptorType} which contains {@link KeyDescriptorType}s.
     * @param usage Type of the {@link KeyDescriptorType}s to be retrieved. Its value is "encryption" or "signing".
     * @return {@link KeyDescriptorType}s in {@link RoleDescriptorType} that matched the usage type.
     */
    public static List<KeyDescriptorType> getKeyDescriptors(RoleDescriptorType roleDescriptor, String usage) {
        List<KeyDescriptorType> keyDescriptors = roleDescriptor.getKeyDescriptor();
        List<KeyDescriptorType> matches = new ArrayList<>(keyDescriptors.size());
        List<KeyDescriptorType> keyDescriptorsWithoutUsage = new ArrayList<>(keyDescriptors.size());

        for (KeyDescriptorType keyDescriptor : keyDescriptors) {
            String use = keyDescriptor.getUse();
            if (StringUtils.isBlank(use)) {
                keyDescriptorsWithoutUsage.add(keyDescriptor);
            } else if (use.trim().toLowerCase().equals(usage)) {
                matches.add(keyDescriptor);
            }
        }

        matches.addAll(keyDescriptorsWithoutUsage);
        return matches;
    }

    /**
     * Returns <code>KeyDescriptorType</code> from 
     * <code>RoleDescriptorType</code>.
     * @param roled <code>RoleDescriptorType</code> which contains
     *                <code>KeyDescriptor</code>s.
     * @param usage type of the <code>KeyDescriptorType</code> to be retrieved.
     *                Its value is "encryption" or "signing".
     * @return KeyDescriptorType in <code>RoleDescriptorType</code> that matched
     *                the usage type.
     */
    public static KeyDescriptorType getKeyDescriptor(
        RoleDescriptorType roled,
        String usage
    ) {
        final List<KeyDescriptorType> keyDescriptors = getKeyDescriptors(roled, usage);
        return CollectionUtils.getFirstItem(keyDescriptors, null);
    }

    /**
     * Returns certificate stored in <code>KeyDescriptorType</code>.
     * @param kd <code>KeyDescriptorType</code> which contains certificate info
     * @return X509Certificate contained in <code>KeyDescriptorType</code>; or
     *                <code>null</code> if no certificate is included.
     */
    public static java.security.cert.X509Certificate getCert(
        KeyDescriptorType kd
    ) {

        String classMethod = "KeyUtil.getCert: ";
        KeyInfoType ki = kd.getKeyInfo();
        if (ki == null) {
            SAML2SDKUtils.debug.error(classMethod +
                    "No KeyInfo.");
            
            return null;
        }
        //iterate and search the X509DataElement node
        Iterator it = ki.getContent().iterator();
        X509DataElement data = null;
        while ((data == null) && it.hasNext()) {
            Object content = it.next();
            if (content instanceof X509DataElement) {
                data = (X509DataElement) content;
            }
        }
        if (data == null) {
            SAML2SDKUtils.debug.error(classMethod + "No X509DataElement.");
            return null;
        }
        //iterate and search the X509Certificate node
        it = data.getX509IssuerSerialOrX509SKIOrX509SubjectName().iterator();
        com.sun.identity.saml2.jaxb.xmlsig.X509DataType.X509Certificate cert = null;
        while ((cert == null) && it.hasNext()) {
            Object content = it.next();
            if (content instanceof 
                com.sun.identity.saml2.jaxb.xmlsig.X509DataType.X509Certificate) {
                cert = (com.sun.identity.saml2.jaxb.xmlsig.X509DataType.X509Certificate) content;
            }
        }
        if (cert == null) {
            SAML2SDKUtils.debug.error(classMethod + "No X509Certificate.");
            return null;
        }
        byte[] bt = cert.getValue();
        CertificateFactory cf = null;
        try {
            cf = CertificateFactory.getInstance("X.509");
        } catch (java.security.cert.CertificateException ce) {
            SAML2SDKUtils.debug.error(
                classMethod +
                "Unable to get CertificateFactory "+
                "for X.509 type", ce);
            return null;
        }                
        ByteArrayInputStream bais = new ByteArrayInputStream(bt);
        java.security.cert.X509Certificate retCert = null;
        try {
            while (bais.available() > 0) {
                retCert = (java.security.cert.X509Certificate) 
                    cf.generateCertificate(bais);
            }
        } catch (java.security.cert.CertificateException ce) {
            SAML2SDKUtils.debug.error(
                classMethod +
                "Unable to generate certificate from byte "+
                "array input stream.", ce);
            return null;
        }
        return retCert;
    }

    /**
     * Returns the partner entity's signature verification certificates.
     *
     * @param pepDescriptor <code>XACMLAuthzDecisionQueryDescriptorElement</code> for the partner entity.
     * @param entityID Policy Enforcement Point (PEP) entity identifier.
     * @return The Set of signing {@link X509Certificate}s for verifying the partner entity's signature.
     */
    public static Set<X509Certificate> getPEPVerificationCerts(XACMLAuthzDecisionQueryDescriptorElement pepDescriptor,
            String entityID) {
        return getVerificationCerts(pepDescriptor, entityID, SAML2Constants.PEP_ROLE);
    }

    /**
     * Returns the encryption information which will be used in
     * encrypting messages intended for the partner entity.
     *
     * @param pepDesc <code>XACMLAuthzDecisionQueryDescriptorElement</code> 
     * for the partner entity
     * @param pepEntityID partner entity's ID
     * @return <code>EncInfo</code> which includes partner entity's
     * public key for wrapping the secret key, data encryption algorithm,
     * and data encryption strength 
     */        
    public static EncInfo getPEPEncInfo(
       XACMLAuthzDecisionQueryDescriptorElement pepDesc,String pepEntityID) {

        String classMethod = "KeyUtil.getEncInfo: ";
        String role=SAML2Constants.PEP_ROLE;
        
        if (SAML2SDKUtils.debug.messageEnabled()) {
            SAML2SDKUtils.debug.message(
                classMethod +
                "Entering... \nEntityID=" +
                pepEntityID + "\nRole="+role
            );
        }
        // first try to get it from cache
        String index = pepEntityID.trim()+"|"+role;
        EncInfo encInfo = (EncInfo)encHash.get(index);
        if (encInfo != null) {
            return encInfo;
        }
        // else get it from meta
        if (pepDesc == null) {
            SAML2SDKUtils.debug.error(
                classMethod+
                "Null PEP Descriptor input for entityID=" +
                pepEntityID + " in "+role+" role."
            );
            return null;
        }
        KeyDescriptorType kd = getKeyDescriptor(pepDesc,SAML2Constants.ENCRYPTION);
        if (kd == null) {
            SAML2SDKUtils.debug.error(
                classMethod+
                "No encryption KeyDescriptor for entityID=" +
                pepEntityID + " in "+role+" role."
            );
            return null;
        }
        return  getEncryptionInfo(kd,pepEntityID,role);
    }
    
    /**
     * Returns the <code>EncInfo</code> from the <code>KeyDescriptor</code>.
     *
     * @param kd the M<code>KeyDescriptor</code> object.
     * @param entityID the entity identfier
     * @param role the role of the entity . Value can be PEP or PDP.
     * @return <code>EncInfo</code> the encryption info.
     */
    private static EncInfo getEncryptionInfo(KeyDescriptorType kd,
                                             String entityID, String role) {
        String classMethod = "KeyUtil:getEncryptionInfo:";
        java.security.cert.X509Certificate cert = getCert(kd);
        if (cert == null) {
            SAML2SDKUtils.debug.error(
                classMethod +
                "No encryption cert for entityID=" +
                entityID + " in "+role+" role."
            );
            return null;
        }
        List emList = kd.getEncryptionMethod();
        EncryptionMethodType em = null;
        String algorithm = null;
        int keySize = 0;
        if (emList != null && !emList.isEmpty()) {            
            em = (EncryptionMethodType)emList.get(0);
            if (em != null) {
                algorithm = em.getAlgorithm();
                List cList = em.getContent();
                if (cList != null) {
                    keySize =
                        ((EncryptionMethodType.KeySize)(cList.get(0))).
                        getValue().intValue();
                }
            }
        }
        if (algorithm == null || algorithm.length() == 0) {
            algorithm = XMLCipher.AES_128;
            keySize = 128;
        }
        PublicKey pk = cert.getPublicKey();
        EncInfo encInfo = null;
        if (pk != null) {
            encInfo = new EncInfo(pk, algorithm, keySize);
        }
        String index = entityID.trim()+"|"+role;
        if (encInfo != null) {
            encHash.put(index, encInfo);
        }
        return encInfo;
    }

    /**
     * Returns the partner entity's signature verification certificates.
     *
     * @param pdpDescriptor <code>XACMLPDPDescriptorElement</code> of partner entity.
     * @param entityID partner entity's ID.
     * @return The Set of signing {@link X509Certificate}s for verifying the partner entity's signature.
     */
    public static Set<X509Certificate> getPDPVerificationCerts(XACMLPDPDescriptorElement pdpDescriptor,
            String entityID) {
        return getVerificationCerts(pdpDescriptor, entityID, SAML2Constants.PDP_ROLE);
    }

    /**
     * Clears the cache. This method is called when metadata is updated.
     */
    public static void clear() {
        sigHash.clear();
        encHash.clear();
    }
} 
