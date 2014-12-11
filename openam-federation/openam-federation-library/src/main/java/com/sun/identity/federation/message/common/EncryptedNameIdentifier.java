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
 * $Id: EncryptedNameIdentifier.java,v 1.4 2008/06/25 05:46:46 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.federation.message.common;

import com.sun.identity.federation.common.FSException;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.key.EncInfo;
import com.sun.identity.federation.key.KeyUtil;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.liberty.ws.meta.jaxb.ProviderDescriptorType;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.xmlenc.EncryptionException;
import com.sun.identity.xmlenc.XMLEncryptionManager;
import java.security.Key;
import java.security.PrivateKey;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * This class <code>EncryptedNameIdentifier</code> represents a
 * <code>EncryptableNameIdentifier</code> in an encrypted form.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class EncryptedNameIdentifier {
    
    /**
     * Returns the encryptable XML document element.
     *
     * @param eni the <code>EncrytableNameIdentifier</code> object.
     *
     * @return the <code>EncryptedNameIdentifier</code> XML Document.
     */
    private static Document getEncryptableDocument(
            EncryptableNameIdentifier eni) {
        
        StringBuffer xml = new StringBuffer(300);
        String NS = IFSConstants.LIB_12_NAMESPACE_STRING;
        String appendNS = IFSConstants.LIB_PREFIX;
        
        xml.append("<").append(appendNS).append("EncryptedNameIdentifier")
        .append(" ").append(NS).append(">").append(eni.toString())
        .append("</").append(appendNS)
        .append("EncryptedNameIdentifier").append(">");
        if(FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("EncryptedNameIdentifier.getEncryptable" +
                    "NameIdentifier: doc =" + xml.toString());
        }
        return XMLUtils.toDOMDocument(xml.toString(), FSUtils.debug);
        
    }
    
    
    /**
     * Returns the <code>EncryptedNameIdentifier</code> for a given name
     * identifier and the provider ID.
     *
     * @param ni the <code>NameIdentifier</code> object.
     * @param realm The realm under which the entity resides.
     * @param providerID the remote provider identifier.
     * @return the <code>NameIdentifier</code> object.
     * @throws FSException on error.
     */
    public static NameIdentifier getEncryptedNameIdentifier(
            NameIdentifier ni, String realm, String providerID)
            throws FSException {
        
        if(ni == null || providerID == null) {
            FSUtils.debug.error("EncryptedNameIdentifier.construct: " +
                    "nullInputParameter");
            throw new FSException("nullInputParameter", null);
        }
        ProviderDescriptorType providerDesc = null;
        try {
            IDFFMetaManager metaManager = FSUtils.getIDFFMetaManager();
            if (metaManager != null) {
                providerDesc = metaManager.getSPDescriptor(realm, providerID);

                if (providerDesc == null) {
                    providerDesc = metaManager.getIDPDescriptor(
                        realm, providerID);
                }
            }
            if (providerDesc == null) {
                throw new IDFFMetaException((String) null);
            }
        } catch (IDFFMetaException ae) {
            FSUtils.debug.error("EncryptedNameIdentifier.construct: Could" +
                    "not retrieve the meta for provider" + providerID);
            throw new FSException(ae);
        }

        EncInfo encInfo = KeyUtil.getEncInfo(providerDesc, providerID, false);
        return getEncryptedNameIdentifier(ni, providerID, 
            encInfo.getWrappingKey(), encInfo.getDataEncAlgorithm(),
            encInfo.getDataEncStrength());
    }

    /**
     * Gets then Encrypted NameIdentifier for a given name identifier 
     * and the provider ID.
     * @param ni NameIdentifier.
     * @param providerID Remote Provider ID.
     * @param enckey Key Encryption Key
     * @param dataEncAlgorithm Data encryption algorithm
     * @param dataEncStrength Data encryption key size
     *
     * @return NameIdentifier EncryptedNameIdentifier. 
     * @exception FSException for failure.
     */
    public static NameIdentifier getEncryptedNameIdentifier(
        NameIdentifier ni, String providerID, Key enckey,
        String dataEncAlgorithm, int dataEncStrength) throws FSException {

        if(ni == null || providerID == null) {
           FSUtils.debug.error("EncryptedNameIdentifier.construct: " +
               "nullInputParameter");
           throw new FSException("nullInputParameter", null);
	}

        EncryptableNameIdentifier eni = new EncryptableNameIdentifier(ni);
        Document encryptableDoc = getEncryptableDocument(eni);
        Document encryptedDoc = null;
        
        try {
            Element encryptElement = (Element)encryptableDoc.
                    getElementsByTagNameNS(IFSConstants.FF_12_XML_NS,
                    "EncryptableNameIdentifier").item(0);
            
            
            XMLEncryptionManager manager = XMLEncryptionManager.getInstance();
            encryptedDoc = manager.encryptAndReplace(
                    encryptableDoc,
                    encryptElement,
                    dataEncAlgorithm,
                    dataEncStrength,
                    enckey,
                    0, // TODO: should we pick it up from extended meta?
                    providerID);
            
        } catch (EncryptionException ee) {
            FSUtils.debug.error("EncryptedNameIdentifier.construct: Unable" +
                    "to encrypt the xml doc", ee);
            throw new FSException(ee);
        }
        
        if(encryptedDoc == null) {
            throw new FSException("EncryptionFailed", null);
        }
        
        String encodedStr = Base64.encode(
                SAMLUtils.stringToByteArray(
                                 XMLUtils.print((Node)(encryptedDoc))));
        
        try {
            return new NameIdentifier(encodedStr, ni.getNameQualifier(),
                    IFSConstants.NI_ENCRYPTED_FORMAT_URI);
            
        } catch(SAMLException se) {
            throw new FSException(se);
        }
        
    }
    
    /**
     * Returns the decrypted <code>NameIdentifier</code> object.
     *
     * @param encNI the <code>EncryptedNameIdentifier</code> object.
     * @param realm The realm under which the entity resides.
     * @param providerID the Hosted Provider Identifer.
     * @return the <code>NameIdentifier</code> object,
     *          the decrypted <code>NameIdentifier</code>.
     * @throws FSException on error.
     */
    public static NameIdentifier getDecryptedNameIdentifier(
        NameIdentifier encNI, String realm, String providerID) 
        throws FSException 
    {
        
        if(encNI == null || providerID == null) {
            FSUtils.debug.error("EncryptedNameIdentifier.getDecryptedName" +
                    "Identifier: null values");
            throw new FSException("nullInputParameter", null);
        }
        
        BaseConfigType providerConfig = null;
        try {
            providerConfig = FSUtils.getIDFFMetaManager().
                getSPDescriptorConfig(realm, providerID);
            if (providerConfig == null) {
                providerConfig = FSUtils.getIDFFMetaManager().
                    getIDPDescriptorConfig(realm, providerID);
            }
        } catch (Exception ae) {
            FSUtils.debug.error("EncryptedNameIdentifier.getDecryptedName" +
                    "Identifier: Unable to find provider", ae);
            throw new FSException(ae);
        }
        
        if (providerConfig == null) {
            FSUtils.debug.error("EncryptedNameIdentifier.getDecryptedName" +
                "Identifier: Unable to find provider " + providerID);
            throw new FSException("noProviderFound", null);
        }

        return getDecryptedNameIdentifier(encNI,
            KeyUtil.getDecryptionKey(providerConfig));
   }

    /**
     * Gets the decrypted NameIdentifier. 
     * @param encNI EncryptedNameIdentifier. 
     * @param decKey decryption key.
     * 
     * @return NameIdentifier Decrypted NameIdentifier.
     * @exception FSException for failures
     */ 
    public static NameIdentifier getDecryptedNameIdentifier(
        NameIdentifier encNI, PrivateKey decKey) throws FSException {


        if(encNI.getFormat() == null ||
                !encNI.getFormat().equals(
                                   IFSConstants.NI_ENCRYPTED_FORMAT_URI)) {
            throw new FSException("notValidFormat", null);
        }
        
        String name = encNI.getName();
        name = FSUtils.removeNewLineChars(name);
        String decodeStr = SAMLUtils.byteArrayToString(Base64.decode(name));
        
        Document encryptedDoc =
                XMLUtils.toDOMDocument(decodeStr, FSUtils.debug);
        
        try {
            XMLEncryptionManager manager = XMLEncryptionManager.getInstance();
            Document doc = manager.decryptAndReplace(encryptedDoc, decKey);
            
            Element element = (Element)doc.getElementsByTagNameNS(
                    IFSConstants.FF_12_XML_NS,
                    "EncryptableNameIdentifier").item(0);
            
            EncryptableNameIdentifier eni =
                    new EncryptableNameIdentifier(element);
            return new NameIdentifier(eni.getName(), eni.getNameQualifier(),
                    eni.getFormat());
            
        } catch (EncryptionException ee) {
            FSUtils.debug.error("EncryptedNameIdentifier.getDecryptedName" +
                    "Identifier: Decryption exception", ee);
            throw new FSException(ee);
        } catch (SAMLException se) {
            throw new FSException(se);
        }
        
    }
}
