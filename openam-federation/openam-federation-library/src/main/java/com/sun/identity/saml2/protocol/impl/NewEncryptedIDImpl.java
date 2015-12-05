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
 * $Id: NewEncryptedIDImpl.java,v 1.2 2008/06/25 05:48:00 qcheng Exp $
 *
 * Portions copyright 2014-2015 ForgeRock AS.
 */
package com.sun.identity.saml2.protocol.impl;

import java.security.PrivateKey;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.assertion.impl.EncryptedElementImpl;
import com.sun.identity.saml2.protocol.NewEncryptedID;
import com.sun.identity.saml2.protocol.NewID;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.xmlenc.EncManager;

/**
 * Java content class for NewEncryptedID element declaration.
 * <p>The following schema fragment specifies the expected 	
 * content contained within this java content object. 	
 * <p>
 * <pre>
 * &lt;element name="NewEncryptedID" 
 *     type="{urn:oasis:names:tc:SAML:2.0:assertion}EncryptedElementType"/>
 * </pre>
 */

public class NewEncryptedIDImpl extends EncryptedElementImpl implements NewEncryptedID {
    public final String elementName = "NewEncryptedID";
    private NewID newID = null;

    // used by the constructors.
    private void parseElement(Element element)
        throws SAML2Exception {
        // make sure that the input xml block is not null
        if (element == null) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("NewEncryptedIDImpl.parseElement: "
                    + "Input is null.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("nullInput"));
        }

        // Make sure this is an EncryptedID.
        String tag = null;
        tag = element.getLocalName();
        if ((tag == null) || (!tag.equals(elementName))) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("NewEncryptedIDImpl.parseElement: "
                    + "not EncryptedIDImpl.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("wrongInput"));
        }
    }

    /**
     * Constructor to create <code>NewEncryptedID</code> Object. 
     *
     * @param element Document Element of 
     *         <code>NewEncryptedID<code> object.
     * @throws SAML2Exception 
     *         if <code>NewEncryptedID<code> cannot be created.
     */
    public NewEncryptedIDImpl(Element element) 
    throws SAML2Exception {
        parseElement(element);
        xmlString = XMLUtils.print(element);
    }

    /**
     * Constructor to create <code>NewEncryptedID</code> Object. 
     *
     * @param xmlString XML Representation of 
     *        the <code>NewEncryptedID<code> object.
     * @throws SAML2Exception 
     *        if <code>NewEncryptedID<code> cannot be created.
     */
    public NewEncryptedIDImpl(String xmlString)
    throws SAML2Exception {
        Document doc = XMLUtils.toDOMDocument(xmlString, SAML2SDKUtils.debug);
        if (doc == null) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseElement(doc.getDocumentElement());
        this.xmlString = xmlString;
    }

    @Override
    public NewID decrypt(Set<PrivateKey> privateKeys) throws SAML2Exception {
        Element el = EncManager.getEncInstance().decrypt(xmlString, privateKeys);
        SAML2SDKUtils.decodeXMLToDebugLog("NewEncryptedIDImpl.decrypt: ", el);
        return ProtocolFactory.getInstance().createNewID(el);
    }
}
