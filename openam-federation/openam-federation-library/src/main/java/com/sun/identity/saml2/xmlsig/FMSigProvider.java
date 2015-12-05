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
 * $Id: FMSigProvider.java,v 1.5 2009/05/09 15:43:59 mallas Exp $
 *
 *  Portions Copyrighted 2011-2015 ForgeRock AS.
 */

package com.sun.identity.saml2.xmlsig;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Set;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import org.apache.xml.security.utils.Constants;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.signature.XMLSignatureException;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.keys.keyresolver.KeyResolverException;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.transforms.TransformationException;
import org.apache.xml.security.utils.ElementProxy;
import org.apache.xpath.XPathAPI;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.xml.XMLUtils;

import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Utils;

/**
 * <code>FMSigProvider</code> is an class for signing
 * and verifying XML documents, it implements <code>SigProvider</code>
 */

public final class FMSigProvider implements SigProvider {

    private static String c14nMethod = null;
    private static String transformAlg = null;
    private static String sigAlg = null;
    // flag to check if the partner's signing cert included in
    // the XML doc is the same as the one in its meta data
    private static boolean checkCert = true;

    static {
        org.apache.xml.security.Init.init();

	c14nMethod = SystemPropertiesManager.get(
	    SAML2Constants.CANONICALIZATION_METHOD,
	    Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
	transformAlg = SystemPropertiesManager.get(
	    SAML2Constants.TRANSFORM_ALGORITHM,
	    Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS); 
	sigAlg = SystemPropertiesManager.get(
	    SAML2Constants.XMLSIG_ALGORITHM); 
	
	String valCert = 
	    SystemPropertiesManager.get(
		"com.sun.identity.saml.checkcert",
		"on");
	if (valCert != null &&
	    valCert.trim().equalsIgnoreCase("off")) {
	    checkCert = false;
	}
    }
    
    /**
     * Default Constructor
     */
    public FMSigProvider() {
    }
    
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
    ) throws SAML2Exception {
	
	String classMethod = "FMSigProvider.sign: ";
        if (xmlString == null ||
	    xmlString.length() == 0 ||
	    idValue == null ||
	    idValue.length() == 0 ||
	    privateKey == null) {
	    
            SAML2SDKUtils.debug.error(
		classMethod +
		"Either input xml string or id value or "+
		"private key is null.");  
            throw new SAML2Exception( 
		SAML2SDKUtils.bundle.getString("nullInput"));  
        }                                                 
	Document doc =
	    XMLUtils.toDOMDocument(xmlString, SAML2SDKUtils.debug);
        if (doc == null) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString(
		    "errorObtainingElement")
	    );
        }
	Element root = doc.getDocumentElement();
	XMLSignature sig = null;
	try {
        ElementProxy.setDefaultPrefix(Constants.SignatureSpecNS, SAMLConstants.PREFIX_DS);
	} catch (XMLSecurityException xse1) {
	    throw new SAML2Exception(xse1);
	}
    root.setIdAttribute(SAML2Constants.ID, true);
	try {
	    if ((sigAlg == null) || (sigAlg.trim().length() == 0)) {
	       if (privateKey.getAlgorithm().equalsIgnoreCase(
			SAML2Constants.DSA)) {
	           sigAlg = 
	               XMLSignature.ALGO_ID_SIGNATURE_DSA;
	       } else { 
	           if (privateKey.getAlgorithm().equalsIgnoreCase(
			SAML2Constants.RSA)) {
	               sigAlg = 
	                   XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1;
                   }
	       }  
	    }        
	    sig = new XMLSignature(
		doc, "", sigAlg, c14nMethod);
	} catch (XMLSecurityException xse2) {
	    throw new SAML2Exception(xse2);
	}	    
	Node firstChild = root.getFirstChild();
	while (firstChild != null &&
	       (firstChild.getLocalName() == null ||
		!firstChild.getLocalName().equals("Issuer"))) {
	    firstChild = firstChild.getNextSibling();
	}
	Node nextSibling = null;
	if (firstChild != null) {
	    nextSibling = firstChild.getNextSibling();
	}
	if (nextSibling == null) {
	    root.appendChild(sig.getElement());  
	} else {
	    root.insertBefore(sig.getElement(), nextSibling);
	}
	sig.getSignedInfo().addResourceResolver(   
	    new com.sun.identity.saml.xmlsig.OfflineResolver()); 
	Transforms transforms = new Transforms(doc);
	try {
	    transforms.addTransform(
		Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
	} catch (TransformationException te1) {
	    throw new SAML2Exception(te1);
	}	    
	try {
	    transforms.addTransform(transformAlg);
	} catch (TransformationException te2) {
	    throw new SAML2Exception(te2);
	}	    
	String ref = "#" + idValue;
	try {
	    sig.addDocument(
		ref,
		transforms,
		Constants.ALGO_ID_DIGEST_SHA1);
	} catch (XMLSignatureException sige1) {
	    throw new SAML2Exception(sige1);
	}	    
	if (cert != null) {
	    try {
		sig.addKeyInfo(cert);
	    } catch (XMLSecurityException xse3) {
		throw new SAML2Exception(xse3);
	    }		
	}
	try {
	    sig.sign(privateKey);
	} catch (XMLSignatureException sige2) {
	    throw new SAML2Exception(sige2);
	}	    
	if (SAML2SDKUtils.debug.messageEnabled()) {
	    SAML2SDKUtils.debug.message(
		classMethod +
		"Signing is successful.");
	}
        return sig.getElement();   
    }

    public boolean verify(
	String xmlString,
	String idValue,
	Set<X509Certificate> verificationCerts
    ) throws SAML2Exception {

        String classMethod = "FMSigProvider.verify: ";
        if (xmlString == null ||
                xmlString.length() == 0 ||
                idValue == null ||
                idValue.length() == 0) {

            SAML2SDKUtils.debug.error(
                    classMethod +
                            "Either input xmlString or idValue is null.");
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("nullInput"));
        }
        Document doc =
                XMLUtils.toDOMDocument(xmlString, SAML2SDKUtils.debug);
        if (doc == null) {
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString(
                            "errorObtainingElement")
            );
        }
        Element nscontext =
                org.apache.xml.security.utils.XMLUtils.
                        createDSctx(doc, "ds", Constants.SignatureSpecNS);
        Element sigElement = null;
        try {
            sigElement = (Element) org.apache.xpath.XPathAPI.selectSingleNode(
                    doc,
                    "//ds:Signature[1]", nscontext);
        } catch (TransformerException te) {
            throw new SAML2Exception(te);
        }
        Element refElement;
        try {
            refElement = (Element) XPathAPI.selectSingleNode(
                    doc,
                    "//ds:Reference[1]", nscontext);
        } catch (TransformerException te) {
            throw new SAML2Exception(te);
        }
        String refUri = refElement.getAttribute("URI");
        String signedId = ((Element) sigElement.getParentNode()).getAttribute(SAML2Constants.ID);
        if (refUri == null || signedId == null || !refUri.substring(1).equals(signedId)) {
            SAML2SDKUtils.debug.error(classMethod + "Signature reference ID does "
                    + "not match with element ID");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("uriNoMatchWithId"));
        }

        doc.getDocumentElement().setIdAttribute(SAML2Constants.ID, true);
        XMLSignature signature = null;
        try {
            signature = new
                    XMLSignature((Element) sigElement, "");
        } catch (XMLSignatureException sige) {
            throw new SAML2Exception(sige);
        } catch (XMLSecurityException xse) {
            throw new SAML2Exception(xse);
        }
        signature.addResourceResolver(
                new com.sun.identity.saml.xmlsig.
                        OfflineResolver());
        KeyInfo ki = signature.getKeyInfo();
        X509Certificate certToUse = null;
        if (ki != null && ki.containsX509Data()) {
            try {
                certToUse = ki.getX509Certificate();
            } catch (KeyResolverException kre) {
                SAML2SDKUtils.debug.error(
                        classMethod +
                                "Could not obtain a certificate " +
                                "from inside the document."
                );
                certToUse = null;
            }
            if (certToUse != null && checkCert) {
                if (!verificationCerts.contains(certToUse)) {
                    SAML2SDKUtils.debug.error(classMethod + "The cert contained in the document is NOT trusted");
                    throw new SAML2Exception(SAML2SDKUtils.bundle.getString("invalidCertificate"));
                }
                if (SAML2SDKUtils.debug.messageEnabled()) {
                    SAML2SDKUtils.debug.message(classMethod + "The cert contained in the document is trusted");
                }
            }
        }

        if (certToUse != null) {
            verificationCerts = Collections.singleton(certToUse);
        }

        if (!isValidSignature(signature, verificationCerts)) {
            SAML2SDKUtils.debug.error(classMethod + "Signature verification failed.");
            return false;
        }

        if (SAML2SDKUtils.debug.messageEnabled()) {
            SAML2SDKUtils.debug.message(classMethod + "Signature verification successful.");
        }
        return true;
    }

    private boolean isValidSignature(XMLSignature signature, Set<X509Certificate> certificates) throws SAML2Exception {
        final String classMethod = "FMSigProvider.isValidSignature: ";
        XMLSignatureException firstException = null;
        for (X509Certificate certificate : certificates) {
            if (!SAML2Utils.validateCertificate(certificate)) {
                SAML2SDKUtils.debug.error(classMethod + "Signing Certificate is validated as bad.");
            } else {
                try {
                    if (signature.checkSignatureValue(certificate)) {
                        return true;
                    }
                } catch (XMLSignatureException xse) {
                    SAML2SDKUtils.debug.warning(classMethod + "XML signature validation failed due to " + xse);
                    if (firstException == null) {
                        firstException = xse;
                    }
                }
            }
        }
        if (firstException != null) {
            throw new SAML2Exception(firstException);
        }

        return false;
    }
}
