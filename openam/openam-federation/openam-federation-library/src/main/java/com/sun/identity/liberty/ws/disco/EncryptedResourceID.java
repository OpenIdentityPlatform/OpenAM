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
 * $Id: EncryptedResourceID.java,v 1.4 2008/06/25 05:47:10 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.disco;

import org.w3c.dom.*;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.liberty.ws.disco.common.DiscoConstants;
import com.sun.identity.liberty.ws.disco.common.DiscoUtils;
import com.sun.identity.liberty.ws.util.ProviderManager;
import com.sun.identity.liberty.ws.util.ProviderUtil;
import com.sun.identity.xmlenc.*;

/**
 * The class <code>EncryptedResourceID</code> represents an Encryption
 * Resource ID element for the Discovery Service.
 * <p>The following schema fragment specifies the expected content within the
 * <code>EncryptedResourceID</code> object.
 * <p>
 * <pre>
 * &lt;xs:element name="EncryptedResourceID" type="EncryptedResourceIDType"/>
 * &lt;complexType name="EncryptedResourceIDType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.w3.org/2001/04/xmlenc#}EncryptedData"/>
 *         &lt;element ref="{http://www.w3.org/2001/04/xmlenc#}EncryptedKey"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * @supported.all.api
 */
public class EncryptedResourceID {

    private Element data = null;
    private Element key = null;
    private String namespaceURI = null;

    /**
     * Default constructor.
     */
    public EncryptedResourceID() {}

    /**
     * Constructor.
     * @param elem <code>EncryptedResourceID</code> DOM element
     * @exception DiscoveryException if error occurs
     */
    public EncryptedResourceID(Element elem) throws DiscoveryException {
        init(elem, DiscoConstants.DISCO_NS);
    }

    /**
     * Constructs a encrypted resource ID.
     *
     * @param elem <code>EncryptedResourceID</code> DOM element
     * @param nspaceURI Name space URI for this element. By default, Discovery
     *        name space is used.
     * @exception DiscoveryException if error occurs.
     */ 
    public EncryptedResourceID(Element elem, String nspaceURI)
        throws DiscoveryException {
        init(elem, nspaceURI);
    }

    private void init(Element elem, String nspaceURI)
        throws DiscoveryException {
        if (elem == null) {
            DiscoUtils.debug.message("EncryptedResourceID(Element):null input");
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("nullInput"));
        }
        String tag = null;
        String nameSpaceURI = null;
        if (((tag = elem.getLocalName()) == null) ||
            (!tag.equals("EncryptedResourceID")) ||
            ((nameSpaceURI = elem.getNamespaceURI()) == null) ||
            (!nameSpaceURI.equals(nspaceURI)))
        {
            DiscoUtils.debug.message("EncryptedResourceID(Ele):wrong input");
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("wrongInput"));
        }

        namespaceURI = nspaceURI;
        NodeList contentnl = elem.getChildNodes();
        String nodeName = null;
        Node child;
        for (int i = 0, length = contentnl.getLength(); i < length; i++) {
            child = contentnl.item(i);
            if ((nodeName = child.getLocalName()) != null) {
                if (nodeName.equals("EncryptedData")) {
                    if (data != null) {
                        if (DiscoUtils.debug.messageEnabled()) {
                            DiscoUtils.debug.message("EncryptedResourceID(Elem"
                                + "ent):Included more than one EncryptedData.");
                        }
                        throw new DiscoveryException(
                            DiscoUtils.bundle.getString("moreEncryptedData"));
                    }
                    data = (Element) child;
                } else if (nodeName.equals("EncryptedKey")) {
                    if (key != null) {
                        if (DiscoUtils.debug.messageEnabled()) {
                            DiscoUtils.debug.message("EncryptedResourceID(Elem"
                                + "ent):Included more than one EncryptedKey.");
                        }
                        throw new DiscoveryException(
                            DiscoUtils.bundle.getString("moreEncryptedKey"));
                    }
                    key = (Element) child;
                } else {
                    if (DiscoUtils.debug.messageEnabled()) {
                        DiscoUtils.debug.message("EncryptedResourceID(Element):"
                            + "invalid node" + nodeName);
                    }
                    throw new DiscoveryException(
                        DiscoUtils.bundle.getString("wrongInput"));
                }
            }
        }
        if (data == null) {
            if (DiscoUtils.debug.messageEnabled()) {
                DiscoUtils.debug.message("EncryptedResourceID(Element): missing"
                    + " EncryptedData element.");
            }
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("missingEncryptedData"));
        }

    }

    /**
     * Constructor.
     * @param encryptedData Encrypted data in DOM Element.
     * @param encryptedKey Encrypted key in DOM Element.
     */
    public EncryptedResourceID(Element encryptedData,
                                Element encryptedKey)
    {
        data = encryptedData;
        key = encryptedKey;
    } 

    /**
     * Sets encrypted data element.
     *
     * @param data encrypted data element.
     * @see #getEncryptedData()
     */
    public void setEncryptedData(Element data) {
        this.data = data;
    }

    /** 
     * Gets encrypted data.
     *
     * @return encrypted data.
     * @see #setEncryptedData(Element)
     */
    public Element getEncryptedData() {
        return data;
    }

    /**
     * Gets encrypted key element.
     *
     * @return encrypted key element.
     * @see #setEncryptedKey(Element)
     */
    public Element getEncryptedKey() {
        return key;
    }

    /**
     * Sets encrypted key element.
     *
     * @param key encrypted key element.
     * @see #getEncryptedKey()
     */
    public void setEncryptedKey(Element key) {
        this.key = key;
    }

    /**
     * Returns an instance of <code>ResourceID</code> object. It takes an
     * instance of <code>EncryptedResourceID</code> and decrypts the contents
     * using the decryption key of the provider ID.
     *
     * @param eri <code>EncryptedResourceID</code> instance that needs to be
     *        decrypted.
     * @param providerID The provider ID whose decryption key that needs to be
     *        used for decryption.
     * @throws DiscoveryException if error occurs during the operation.
     */ 
    public static ResourceID getDecryptedResourceID(
        EncryptedResourceID eri, String providerID)
        throws DiscoveryException {

        if ((eri == null) || (providerID == null)) {
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("nullInput"));
        }
        ResourceID result = null;
        try {
            XMLEncryptionManager manager = XMLEncryptionManager.getInstance();
            Document encDoc = XMLUtils.toDOMDocument(eri.toString(),
                                                DiscoUtils.debug);

            Document decryptDoc = manager.decryptAndReplace(encDoc,
                ProviderUtil.getProviderManager().getDecryptionKey(providerID));
            Element riEl = (Element) decryptDoc.getElementsByTagNameNS(
                                DiscoConstants.DISCO_NS,
                                "ResourceID").item(0);
            result = new ResourceID(riEl);
        } catch (Exception e) {
            DiscoUtils.debug.error("EncryptedResourceID.getDecryptedResource"
                + "ID: decryption exception:", e);
            throw new DiscoveryException(e);
        }
        return result;
    }

    /**
     * Returns an <code>EncryptedResourceID</code> object. It takes a
     * resource ID and provider ID, encrypts the resource ID based on the
     * encryption key of the provider ID.
     *
     * @param ri The resource ID instance that needs to be encrypted.
     * @param providerID The provider ID whose encryption key needs to be used
     *        for encryption.
     * @throws DiscoveryException if error occurs during this operation.
     */ 
    public static EncryptedResourceID getEncryptedResourceID(
        ResourceID ri,
        String providerID
    ) throws DiscoveryException {
        if ((ri == null) || (providerID == null)) {
            DiscoUtils.debug.error("EncryptedResourceID.getEncryptedResource"
                + "ID: null input value");
            throw new DiscoveryException(
                        DiscoUtils.bundle.getString("nullInput"));
        }
        EncryptedResourceID eri = null;
        try {
            ProviderManager pm = ProviderUtil.getProviderManager();
            Document doc = XMLUtils.toDOMDocument(ri.toString(),
                                                DiscoUtils.debug);

            XMLEncryptionManager manager = XMLEncryptionManager.getInstance();
            Document encDoc = manager.encryptAndReplaceResourceID(doc,
                doc.getDocumentElement(),
                pm.getEncryptionKeyAlgorithm(providerID),
                pm.getEncryptionKeyStrength(providerID),
                pm.getEncryptionKey(providerID),
                0,
                providerID);
            eri = new EncryptedResourceID(encDoc.getDocumentElement());
        } catch (Exception e) {
            DiscoUtils.debug.error("EncryptedResourceID.getEncryptedResource"
                + "ID: encryption exception:", e);
            throw new DiscoveryException(e);
        }
        return eri;
    }

    /**
     * Returns string format.
     *
     * @return formatted string.
     */ 
    public java.lang.String toString() {
        return toString(namespaceURI);
    }

    /**
     * Returns string format.
     *
     * @param ns  namespace value
     * @return formatted string.
     */
    public java.lang.String toString(String ns) {
        StringBuffer sb = new StringBuffer(1000);
        sb.append("<EncryptedResourceID xmlns=\"").
            append(ns).append("\">");
        if (data != null) {
            sb.append(XMLUtils.print(data));
        }
        if (key != null) {
            sb.append(XMLUtils.print(key));
        }
        sb.append("</EncryptedResourceID>");
        return sb.toString();
    }
}
