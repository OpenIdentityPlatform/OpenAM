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
 * $Id: SigProvider.java,v 1.2 2008/06/25 05:48:04 qcheng Exp $
 *
 */


package com.sun.identity.saml2.xmlsig;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import com.sun.identity.saml2.common.SAML2Exception;

/**
 * <code>SigProvider</code> is an interface for signing
 * and verifying XML documents
 */ 
public interface SigProvider {
    /**
     * Sign the xml document node whose identifying attribute value
     * is as supplied, using enveloped signatures and use exclusive xml
     * canonicalization. The resulting signature is inserted after the
     * first child node (normally Issuer element for SAML2) of the node
     * to be signed.
     * @param xmlString String representing an XML document to be signed
     * @param idValue id attribute value of the root node to be signed
     * @param privateKey Signing key
     * @param cert Certificate which contain the public key correlated to
     *             the signing key; It if is not null, then the signature
     *             will include the certificate; Otherwise, the signature
     *             will not include any certificate
     * @return Element representing the signature element
     * @throws SAML2Exception if the document could not be signed
     */
    public Element sign(
	String xmlString,
	String idValue,
	PrivateKey privateKey,
	X509Certificate cert
    ) throws SAML2Exception;
    
    /** 
     * Verify the signature of the xml document  
     * @param xmlString String representing an signed XML document
     * @param idValue id attribute value of the node whose signature
     *                is to be verified
     * @param senderCert Certificate containing the public key
     *             which may be used for  signature verification;
     *             This certificate may also may be used to check
     *             against the certificate included in the signature
     * @return true if the xml signature is verified, false otherwise 
     * @throws SAML2Exception if problem occurs during verification
     */
    public boolean verify(
	String xmlString,
	String idValue,
	X509Certificate senderCert
    ) throws SAML2Exception;
}
