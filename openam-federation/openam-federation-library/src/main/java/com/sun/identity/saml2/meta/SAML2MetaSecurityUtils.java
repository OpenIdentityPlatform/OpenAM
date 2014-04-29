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
 * $Id: SAML2MetaSecurityUtils.java,v 1.6 2009/06/08 23:43:18 madan_ranganath Exp $
 *
 * Portions Copyrighted 2010-2014 ForgeRock AS
 */

package com.sun.identity.saml2.meta;

import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.xpath.XPathAPI;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.keys.storage.implementations.KeyStoreResolver;
import org.apache.xml.security.keys.storage.StorageResolver;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.utils.Constants;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.encode.Base64;

import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.saml.xmlsig.XMLSignatureException;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;

import com.sun.identity.saml2.jaxb.entityconfig.AttributeType;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement; 
import com.sun.identity.saml2.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.ObjectFactory;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.KeyDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.RoleDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorElement;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.key.KeyUtil;

/**
 * The <code>SAML2MetaUtils</code> provides metadata security related util
 * methods.
 */
public final class SAML2MetaSecurityUtils {

    private static Debug debug = SAML2MetaUtils.debug;
    private static KeyProvider keyProvider = null;
    private static KeyStore keyStore = null;
    private static boolean checkCert = true;
    private static boolean keyProviderInitialized = false;
    public static final String NS_META = "urn:oasis:names:tc:SAML:2.0:metadata";
    public static final String NS_XMLSIG = "http://www.w3.org/2000/09/xmldsig#";
    public static final String NS_XMLENC = "http://www.w3.org/2001/04/xmlenc#";
    public static final String NS_MD_QUERY =
        "urn:oasis:names:tc:SAML:metadata:ext:query";

    public static final String PREFIX_XMLSIG = "ds";
    public static final String PREFIX_XMLENC = "xenc";
    public static final String PREFIX_MD_QUERY = "query";
    public static final String TAG_KEY_INFO = "KeyInfo";
    public static final String TAG_KEY_DESCRIPTOR = "KeyDescriptor";
    public static final String TAG_ENTITY_DESCRIPTOR = "EntityDescriptor";
    public static final String TAG_SP_SSO_DESCRIPTOR = "SPSSODescriptor";
    public static final String TAG_IDP_SSO_DESCRIPTOR = "IDPSSODescriptor";
    public static final String ATTR_USE = "use";
    public static final String ATTR_ID = "ID";
    private static final String METADATA_SIGNING_KEY = "metadataSigningKey";
    private static final String METADATA_SIGNING_KEY_PASS = "metadataSigningKeyPass";

    private SAML2MetaSecurityUtils() {

    }

    private static void initializeKeyStore() {
        if (keyProviderInitialized) {
            return;
        }

        org.apache.xml.security.Init.init();

        keyProvider = KeyUtil.getKeyProviderInstance();
        if (keyProvider != null) {
            keyStore = keyProvider.getKeyStore();
        }

        try {
            String valCert =
                SystemPropertiesManager.get("com.sun.identity.saml.checkcert", "on");

            checkCert = valCert.trim().equalsIgnoreCase("on");
        } catch (Exception e) {
            checkCert = true;
        }

        keyProviderInitialized = true;
    }

    /**
     * Signs the entity descriptor root element by the following rules:
     * <ul>
     *  <li>Hosted Entity</li>
     *  <ul>
     *   <li>If there is a signature already on the EntityDescriptor, removes it, then signs the EntityDescriptor.
     *   </li>
     *   <li>Simply signs the EntityDescriptor otherwise.</li>
     *  </ul>
     *  <li>Remote Entity</li>
     *  <ul>
     *   <li>If there is a signature already on the EntityDescriptor, then does not change it, but returns the
     *       Document with the original signature.
     *   </li>
     *   <li>Simply signs the EntityDescriptor otherwise</li>
     *  </ul>
     * </ul>
     * If there is no extended metadata for the entity, the entity is considered as remote.
     *
     * @param realm The realm where the EntityDescriptor belongs to.
     * @param descriptor The entity descriptor.
     * @return Signed <code>Document</code> for the entity descriptor or null if no metadata signing key is found in
     * the configuration.
     * @throws SAML2MetaException if unable to sign the entity descriptor. 
     * @throws JAXBException if the entity descriptor is invalid.
     */
    public static Document sign(String realm, EntityDescriptorElement descriptor)
            throws JAXBException, SAML2MetaException {
        if (descriptor == null) {
            throw new SAML2MetaException("Unable to sign null descriptor");
        }


        SAML2MetaManager metaManager = new SAML2MetaManager();
        EntityConfigElement cfgElem = metaManager.getEntityConfig(realm, descriptor.getEntityID());
        boolean isHosted;
        if (cfgElem == null) {
            //if there is no EntityConfig, this is considered as a remote entity
            isHosted = false;
        } else {
            isHosted = cfgElem.isHosted();
        }

        String signingCert = getRealmSetting(METADATA_SIGNING_KEY, realm);
        if (signingCert == null) {
            return null;
        }

        initializeKeyStore();

        String xmlstr = SAML2MetaUtils.convertJAXBToString(descriptor);
        xmlstr = formatBase64BinaryElement(xmlstr);

        Document doc = XMLUtils.toDOMDocument(xmlstr, debug);
        NodeList childNodes = doc.getDocumentElement().getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getLocalName() != null
                    && node.getLocalName().equals("Signature") && node.getNamespaceURI().equals(NS_XMLSIG)) {
                if (isHosted) {
                    node.getParentNode().removeChild(node);
                    break;
                } else {
                    //one signature found for this remote entity on the root element,
                    //in this case returning the entry with the original signature
                    //as that may be judged more accurately
                    return doc;
                }
            }
        }

        //we need to sign or re-sign the document, let's generate a new ID
        String descriptorId = SAMLUtils.generateID();
        doc.getDocumentElement().setAttribute(ATTR_ID, descriptorId);

        XMLSignatureManager sigManager = XMLSignatureManager.getInstance();
        try {
            String xpath = "//*[local-name()=\"" + TAG_ENTITY_DESCRIPTOR
                    + "\" and namespace-uri()=\"" + NS_META
                    + "\"]/*[1]";
            sigManager.signXMLUsingKeyPass(doc, signingCert, getRealmSetting(METADATA_SIGNING_KEY_PASS, realm), null,
                    SAML2Constants.ID, descriptorId, true, xpath);
        } catch (XMLSignatureException xmlse) {
            if (debug.messageEnabled()) {
                debug.message("SAML2MetaSecurityUtils.sign:", xmlse);
            }
        }

        return doc;
    }

    /**
     * Verifies signatures in entity descriptor represented by the 
     * <code>Document</code>.
     * @param doc The document.
     * @throws SAML2MetaException if unable to verify the entity descriptor. 
     */
    public static void verifySignature(Document doc)
        throws SAML2MetaException
    {
        NodeList sigElements = null;
        try {
            Element nscontext =
                    org.apache.xml.security.utils.XMLUtils
                            .createDSctx (doc,"ds", Constants.SignatureSpecNS);
            sigElements =
                    XPathAPI.selectNodeList(doc, "//ds:Signature", nscontext);
        } catch (Exception ex) {
            if (debug.messageEnabled()) {
                debug.message("SAML2MetaSecurityUtils.verifySignature:", ex);
                throw new SAML2MetaException(ex.getMessage());
            }
        }
        int numSigs = sigElements.getLength();
        if (debug.messageEnabled()) {
            debug.message("SAML2MetaSecurityUtils.verifySignature:" +
                          " # of signatures = " + numSigs);
        }

        if (numSigs == 0) {
            return;
        }

        // If there are signatures then explicitly identify the ID Attribute, See comments section of
        // http://bugs.java.com/bugdatabase/view_bug.do?bug_id=8017265
        doc.getDocumentElement().setIdAttribute(SAML2Constants.ID, true);

        initializeKeyStore();

        for(int i = 0; i < numSigs; i++) {
            Element sigElement = (Element)sigElements.item(i);
            String sigParentName = sigElement.getParentNode().getLocalName();
            Object[] objs = { sigParentName };
            if (debug.messageEnabled()) {
                debug.message("SAML2MetaSecurityUtils.verifySignature: " +
                              "verifying signature under " + sigParentName);
            }

            try {
                XMLSignature signature = new XMLSignature(sigElement, "");
                signature.addResourceResolver (
                        new com.sun.identity.saml.xmlsig.OfflineResolver());
                KeyInfo ki = signature.getKeyInfo ();

                X509Certificate x509cert = null;
                if (ki !=null && ki.containsX509Data()) {
                    if (keyStore != null) {
                        StorageResolver sr =
                           new StorageResolver(new KeyStoreResolver(keyStore));
                        ki.addStorageResolver(sr);
                    }
                    x509cert = ki.getX509Certificate();
                }

                if (x509cert == null) {
                    if (debug.messageEnabled()) {
                        debug.message("SAML2MetaSecurityUtils.verifySignature:"
                                      + " try to find cert in KeyDescriptor");
                    }
                    String xpath = "following-sibling::*[local-name()=\"" +
                                   TAG_KEY_DESCRIPTOR +
                                   "\" and namespace-uri()=\"" + NS_META +
                                   "\"]";
                    Node node = XPathAPI.selectSingleNode(sigElement, xpath);
                
                    if (node != null) {
                        Element kd = (Element)node;
                        String use = kd.getAttributeNS(null, ATTR_USE);
                        if ((use.length() == 0) || use.equals("signing")) {
                            NodeList nl = kd.getChildNodes();
                            for(int j=0; j<nl.getLength(); j++) {
                                Node child = nl.item(j);
                                if (child.getNodeType() == Node.ELEMENT_NODE) {
                                    String localName = child.getLocalName();
                                    String ns = child.getNamespaceURI();
                                    if (TAG_KEY_INFO.equals(localName)&&
                                        NS_XMLSIG.equals(ns)){

                                        ki = new KeyInfo((Element)child, "");
                                        if (ki.containsX509Data()) {
                                            if (keyStore != null) {
                                              KeyStoreResolver ksr =
                                                new KeyStoreResolver(keyStore);
                                              StorageResolver sr =
                                                new StorageResolver(ksr);
                                              ki.addStorageResolver(sr);
                                            }

                                            x509cert = ki.getX509Certificate();
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }

                }

                if (x509cert == null) {
                    throw new SAML2MetaException("verify_no_cert", objs);
                }

                if (checkCert && ((keyProvider == null) ||
                   (keyProvider.getCertificateAlias(x509cert) == null))) {
                        throw new SAML2MetaException("untrusted_cert", objs);
                }

                PublicKey pk = x509cert.getPublicKey();

                if (!signature.checkSignatureValue(pk)) {
                    throw new SAML2MetaException("verify_fail", objs);
                }
            } catch (SAML2MetaException sme) {
                throw sme;
            } catch (Exception ex) {
                debug.error("SAML2MetaSecurityUtils.verifySignature: ", ex);
                throw new SAML2MetaException(
                        Locale.getString(SAML2MetaUtils.resourceBundle,
                                         "verify_fail", objs) + "\n" +
                        ex.getMessage());
            }
        }


    }


    /** 
     * Restores Base64 encoded format.
     * JAXB will change
     *      <ds:X509Data>
     *          <ds:X509Certificate>
     *  .........
     *  .........
     *          </ds:X509Certificate>
     *      </ds:X509Data>
     *  to
     *      <ds:X509Data>
     *          <ds:X509Certificate>..................</ds:X509Certificate>
     *      </ds:X509Data>
     *
     *  This method will restore the format.
     *  @param xmlstr The xml string containing element 'X509Certificate'.
     *  @return the restored xmls string.
     */
    public static String formatBase64BinaryElement(String xmlstr) {
        int from = 0;
        int index = xmlstr.indexOf("<ds:X509Certificate>");
        int xmlLength = xmlstr.length();

        StringBuffer sb = new StringBuffer(xmlLength + 100);
        while (index != -1) {
            sb.append(xmlstr.substring(from, index));

            int indexEnd = xmlstr.indexOf("</ds:X509Certificate>", index);
            String encoded = xmlstr.substring(index + 20, indexEnd);
            int encodedLength = encoded.length();

            sb.append("<ds:X509Certificate>\n");
            int i;
            for(i=0; i<encodedLength - 76; i += 76) {
                sb.append(encoded.substring(i, i + 76)).append("\n");
            }

            int nlIndex = xmlstr.lastIndexOf('\n', index);
            String indention = xmlstr.substring(nlIndex + 1, index);

            sb.append(encoded.substring(i, encodedLength))
              .append("\n").append(indention).append("</ds:X509Certificate>");

            from = indexEnd + 21;
            index = xmlstr.indexOf("<ds:X509Certificate>", from);
        }

        sb.append(xmlstr.substring(from, xmlLength));

        return sb.toString();
    }

    public static String buildX509Certificate(String certAlias)
        throws SAML2MetaException
    {
        if ((certAlias == null) || (certAlias.trim().length() == 0)) {
            return null;
        }

        X509Certificate cert =
                KeyUtil.getKeyProviderInstance().getX509Certificate(certAlias);

        if (cert != null) {
            try {
                return Base64.encode(cert.getEncoded(), true);
            } catch (Exception ex) {
                if (debug.messageEnabled()) {
                    debug.message(
                        "SAML2MetaSecurityUtils.buildX509Certificate:", ex);
                }
            }
        }

        Object[] objs = { certAlias };
        throw new SAML2MetaException("invalid_cert_alias", objs);
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
     * @throws SAML2MetaException if failed to update the certificate alias 
     *        for the entity.
     */
    public static void updateProviderKeyInfo(String realm,
        String entityID, String certAlias, boolean isSigning, boolean isIDP,
        String encAlgo, int keySize) throws SAML2MetaException { 
        SAML2MetaManager metaManager = new SAML2MetaManager();
        EntityConfigElement config = 
            metaManager.getEntityConfig(realm, entityID);
        if (!config.isHosted()) {
            String[] args = {entityID, realm};
            throw new SAML2MetaException("entityNotHosted", args);
        }
        EntityDescriptorElement desp = metaManager.getEntityDescriptor(
            realm, entityID);
        if (isIDP) {
            IDPSSOConfigElement idpConfig = 
                SAML2MetaUtils.getIDPSSOConfig(config);
            IDPSSODescriptorElement idpDesp = 
                SAML2MetaUtils.getIDPSSODescriptor(desp);
            if ((idpConfig == null) || (idpDesp == null)) {
                String[] args = {entityID, realm};
                throw new SAML2MetaException("entityNotIDP", args);
            }
            // update standard metadata
            if ((certAlias == null) || (certAlias.length() == 0)) {
                // remove key info
                removeKeyDescriptor(idpDesp, isSigning); 
                if (isSigning) {
                    setExtendedAttributeValue(idpConfig,
                        SAML2Constants.SIGNING_CERT_ALIAS, null);
                } else {
                    setExtendedAttributeValue(idpConfig,
                        SAML2Constants.ENCRYPTION_CERT_ALIAS, null);
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
                        SAML2Constants.SIGNING_CERT_ALIAS, value);
                } else {
                    setExtendedAttributeValue(idpConfig,
                        SAML2Constants.ENCRYPTION_CERT_ALIAS, value);
                }
            }
            metaManager.setEntityDescriptor(realm, desp);
            metaManager.setEntityConfig(realm, config); 
        } else {
            SPSSOConfigElement spConfig = SAML2MetaUtils.getSPSSOConfig(config);
            SPSSODescriptorElement spDesp = 
                SAML2MetaUtils.getSPSSODescriptor(desp);
            if ((spConfig == null) || (spDesp == null)) {
                String[] args = {entityID, realm};
                throw new SAML2MetaException("entityNotSP", args);
            }
            // update standard metadata
            if ((certAlias == null) || (certAlias.length() == 0)) {
                // remove key info
                removeKeyDescriptor(spDesp, isSigning); 
                if (isSigning) {
                    setExtendedAttributeValue(spConfig, 
                        SAML2Constants.SIGNING_CERT_ALIAS, null); 
                } else {
                    setExtendedAttributeValue(spConfig, 
                        SAML2Constants.ENCRYPTION_CERT_ALIAS, null); 
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
                        SAML2Constants.SIGNING_CERT_ALIAS, value);
                } else {
                    setExtendedAttributeValue(spConfig,
                        SAML2Constants.ENCRYPTION_CERT_ALIAS, value);
                }
            }
            metaManager.setEntityDescriptor(realm, desp);
            metaManager.setEntityConfig(realm, config); 
        }
    }

    private static void updateKeyDescriptor(RoleDescriptorType desp, 
        KeyDescriptorElement newKey) {
        // NOTE : we only support one signing and one encryption key right now
        // the code need to be change if we need to support multiple signing
        // and/or encryption keys in one entity
        List keys = desp.getKeyDescriptor();
        for (Iterator iter = keys.iterator(); iter.hasNext();) {
            KeyDescriptorElement key = (KeyDescriptorElement) iter.next();
            if ((key.getUse() != null) && 
                key.getUse().equalsIgnoreCase(newKey.getUse())) {
                iter.remove();
            }
        }
        desp.getKeyDescriptor().add(newKey);
    }

    private static void removeKeyDescriptor(RoleDescriptorType desp,
        boolean isSigningUse) {
        List keys = desp.getKeyDescriptor();
        for (Iterator iter = keys.iterator(); iter.hasNext();) {
            KeyDescriptorElement key = (KeyDescriptorElement) iter.next();
            String keyUse = "encryption";
            if (isSigningUse) {
                keyUse = "signing";
            }
            if ((key.getUse() != null) && 
                key.getUse().equalsIgnoreCase(keyUse)) {
                iter.remove();
            }
        }
    }
  
    private static void setExtendedAttributeValue(
        BaseConfigType config,
        String attrName, Set attrVal) throws SAML2MetaException {
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
            throw new SAML2MetaException(e);
        }
    }

    private static KeyDescriptorElement getKeyDescriptor(
        String certAlias, boolean isSigning, String encAlgo, int keySize) 
        throws SAML2MetaException {
     
        try {
            String certString = 
                SAML2MetaSecurityUtils.buildX509Certificate(certAlias);
            StringBuilder sb = new StringBuilder(4000);
            sb.append("<KeyDescriptor xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\" use=\"");
            if (isSigning) {
                sb.append("signing");
            } else {
                sb.append("encryption");
            }
            sb.append("\">\n")
              .append("<KeyInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\">\n")
              .append("<X509Data>\n")
              .append("<X509Certificate>\n")
              .append(certString).append("\n")
              .append("</X509Certificate>")
              .append("</X509Data>")
              .append("</KeyInfo>");
            if (!isSigning && (encAlgo != null)) {
                sb.append("<EncryptionMethod Algorithm=\"").append(encAlgo)
                  .append("\">\n");
                sb.append("<KeySize xmlns=\"http://www.w3.org/2001/04/xmlenc#\">")
                  .append(keySize).append("</KeySize>\n")
                  .append("</EncryptionMethod>");
            }
            sb.append("</KeyDescriptor>");
            return (KeyDescriptorElement) 
                SAML2MetaUtils.convertStringToJAXB(sb.toString());
        } catch (JAXBException e) {
            throw new SAML2MetaException(e);
        }
    }

    /**
     * Retrieves the Metadata signing key/pass configured either in the realm, or if that is not present (the SAML
     * service is not added to the realm), the globally configured signing key alias/password will be used.
     *
     * @param attribute The name of the attribute that needs to be retrieved
     * @param realm The realm where the signing key alias/password should be looked up
     * @return The requested attribute configured in the realm or in the global settings.
     */
    private static String getRealmSetting(String attribute, String realm) throws SAML2MetaException {
        return SystemPropertiesManager.get(attribute + "[" + realm + "]");
    }
}
