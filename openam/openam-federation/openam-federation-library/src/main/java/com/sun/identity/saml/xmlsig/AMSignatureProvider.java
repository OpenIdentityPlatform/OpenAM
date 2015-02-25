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
 * $Id: AMSignatureProvider.java,v 1.11 2009/08/29 03:06:47 mallas Exp $
 *
 * Portions Copyrighted 2013-2014 ForgeRock AS.
 */

package com.sun.identity.saml.xmlsig;

import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.security.*;
import java.security.cert.*;
import org.w3c.dom.*;

import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.saml.common.*;

import org.apache.xpath.XPathAPI;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.keys.content.keyvalues.DSAKeyValue;
import org.apache.xml.security.keys.content.keyvalues.RSAKeyValue;
import org.apache.xml.security.keys.storage.StorageResolver;
import org.apache.xml.security.keys.storage.implementations.KeyStoreResolver;
import org.apache.xml.security.keys.keyresolver.implementations.X509CertificateResolver;
import org.apache.xml.security.keys.keyresolver.implementations.X509SubjectNameResolver;
import org.apache.xml.security.keys.keyresolver.implementations.X509IssuerSerialResolver;
import org.apache.xml.security.keys.keyresolver.implementations.X509SKIResolver;
import org.apache.xml.security.utils.Constants;
import org.apache.xml.security.utils.ElementProxy;
import org.apache.xml.security.transforms.Transforms;
import com.sun.identity.liberty.ws.common.wsse.WSSEConstants;
import com.sun.identity.liberty.ws.soapbinding.SOAPBindingConstants;
import javax.xml.transform.TransformerException;

/**
 * <code>SignatureProvider</code> is an interface
 * to be implemented to sign and verify xml signature
 * <p>
 */

public class AMSignatureProvider implements SignatureProvider {
    protected KeyProvider keystore = null;
    private String c14nMethod = null;
    private String transformAlg = null;
    // define default id attribute name
    private static final String DEF_ID_ATTRIBUTE = "id";
    // flag to check if the partner's signing cert is in the keystore.
    private boolean checkCert = true;
    private boolean isJKSKeyStore= false;
    private String wsfVersion = null;
    private String defaultSigAlg = null;

    /**
     * Default Constructor
     */
    public AMSignatureProvider() {
        org.apache.xml.security.Init.init();
        try {
            String kprovider = SystemConfigurationUtil.getProperty(
                SAMLConstants.KEY_PROVIDER_IMPL_CLASS,
                SAMLConstants.JKS_KEY_PROVIDER);
            keystore= (KeyProvider) Class.forName(kprovider).newInstance();
            if (keystore instanceof JKSKeyProvider) {
                isJKSKeyStore=true;
            }
        } catch (Exception e) {
            SAMLUtilsCommon.debug.error("AMSignatureProvider: " +
                "constructor error");
        }
        
        c14nMethod = SystemConfigurationUtil.getProperty(
            SAMLConstants.CANONICALIZATION_METHOD,
            SAMLConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS); 
 
        transformAlg = SystemConfigurationUtil.getProperty(
            SAMLConstants.TRANSFORM_ALGORITHM,
            SAMLConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS); 
        
        defaultSigAlg = SystemConfigurationUtil.getProperty(
            SAMLConstants.XMLSIG_ALGORITHM);

        try {
            String valCert = SystemConfigurationUtil.getProperty(
                "com.sun.identity.saml.checkcert");
            if (valCert != null)  {
                if (valCert.trim().equalsIgnoreCase("off")) {
                    checkCert = false;
                }else if (valCert.trim().equalsIgnoreCase("on")) {
                    checkCert = true;
                }else {
                    if(SAMLUtilsCommon.debug.messageEnabled()) {
                       SAMLUtilsCommon.debug.message("SystemConfigurationUtil:"
                           + " com.sun.identity.saml.checkcert has" 
                  	   + " invalid value. Choose default, turn"
                       	   + " ON checkcert.");
                    }
                    checkCert = true;
                }   
            }
        } catch (Exception e) {
            checkCert = true;
        }
    }
    
    /**
     * Constructor 
     */
    public void initialize(KeyProvider keyProvider) {
        if (keyProvider == null) {
            SAMLUtilsCommon.debug.error("Key Provider is null"); 
        } else {
            keystore = keyProvider;
            if (keystore instanceof JKSKeyProvider) {
                isJKSKeyStore=true;
            }
        }
    }
    
    /**
     * Sign the xml document using enveloped signatures.
     * @param doc XML dom object 
     * @param certAlias Signer's certificate alias name
     * @return signature Element object 
     * @throws XMLSignatureException if the document could not be signed
     */ 
    public org.w3c.dom.Element signXML(org.w3c.dom.Document doc, 
                                       java.lang.String certAlias) 
        throws XMLSignatureException {      
        return signXML(doc, certAlias, null); 
    }
    
    /**
     * Sign the xml document using enveloped signatures.
     * @param doc XML dom object 
     * @param certAlias Signer's certificate alias name
     * @param algorithm XML signature algorithm 
     * @return signature dom object 
     * @throws XMLSignatureException if the document could not be signed
     */ 
    public org.w3c.dom.Element signXML(org.w3c.dom.Document doc, 
                                       java.lang.String certAlias, 
                                       java.lang.String algorithm)
        throws XMLSignatureException {   
        if (doc == null) {
	    SAMLUtilsCommon.debug.error("signXML: doc is null.");
	    throw new XMLSignatureException(
		      SAMLUtilsCommon.bundle.getString("nullInput"));
	}        
        if (certAlias == null || certAlias.length() == 0) {
	    SAMLUtilsCommon.debug.error("signXML: certAlias is null.");
	    throw new XMLSignatureException(
		      SAMLUtilsCommon.bundle.getString("nullInput"));
	}    
        org.w3c.dom.Element root = null; 
        XMLSignature sig = null; 
        try {
            ElementProxy.setDefaultPrefix(Constants.SignatureSpecNS, SAMLConstants.PREFIX_DS);
            if (keystore == null) { 
                throw new XMLSignatureException(
                          SAMLUtilsCommon.bundle.getString("nullkeystore"));
            }   
            PrivateKey privateKey = 
                (PrivateKey) keystore.getPrivateKey(certAlias);
      
            if (privateKey == null) {
                SAMLUtilsCommon.debug.error("private key is null");
                throw new XMLSignatureException(
                          SAMLUtilsCommon.bundle.getString("nullprivatekey"));
            } 
            root = doc.getDocumentElement(); 
        
            if (algorithm == null || algorithm.length() == 0) {
                algorithm = getKeyAlgorithm(privateKey); 
            }
            if (!isValidAlgorithm(algorithm)) {
                throw new XMLSignatureException(
                    SAMLUtilsCommon.bundle.getString("invalidalgorithm"));
            }
            
            if (c14nMethod == null || c14nMethod.length() == 0) {
            	sig = new XMLSignature(doc, "", algorithm); 
            } else {
                if (!isValidCanonicalizationMethod(c14nMethod)) {
                    throw new XMLSignatureException(
                                SAMLUtilsCommon.bundle.
                                getString("invalidCanonicalizationMethod"));
                }    		   	

                sig = new XMLSignature(doc, "", algorithm, c14nMethod);  
            }    
            root.appendChild(sig.getElement());
            sig.getSignedInfo().addResourceResolver(
                        new com.sun.identity.saml.xmlsig.OfflineResolver());
    
            // do transform 
            Transforms transforms = new Transforms(doc);
            transforms.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
            // If exclusive canonicalization is presented in the saml locale
	    // file, we will add a transform for it. Otherwise, will not do
	    // such transform due to performance reason.    
            if (transformAlg != null && transformAlg.length() != 0) {
            	if (!isValidTransformAlgorithm(transformAlg)) { 
            	    throw new XMLSignatureException(
            	    		SAMLUtilsCommon.bundle.getString(
            	    		"invalidTransformAlgorithm"));
            	}
            	transforms.addTransform(transformAlg);
            }
            
            sig.addDocument("", transforms, Constants.ALGO_ID_DIGEST_SHA1);
        
            // add certificate 
            X509Certificate cert =
                    (X509Certificate) keystore.getX509Certificate(certAlias);

            sig.addKeyInfo(cert);
            sig.sign(privateKey);
        } catch (Exception e) {
            SAMLUtilsCommon.debug.error("signXML Exception: ", e);
            throw new XMLSignatureException(e.getMessage());
        }
        return (sig.getElement());
    }
    
    /**
     * Sign the xml string using enveloped signatures.
     * @param xmlString xml string to be signed
     * @param certAlias Signer's certificate alias name
     * @return XML signature string
     * @throws XMLSignatureException if the xml string could not be signed
     */
    public java.lang.String signXML(java.lang.String xmlString,
                                    java.lang.String certAlias) 
        throws XMLSignatureException {
        return signXML(xmlString, certAlias, null);   
    }

    /**
     * Sign the xml string using enveloped signatures.
     * @param xmlString xml string to be signed
     * @param certAlias Signer's certificate alias name
     * @param algorithm XML Signature algorithm 
     * @return XML signature string
     * @throws XMLSignatureException if the xml string could not be signed
     */
    public java.lang.String signXML(java.lang.String xmlString,
                                    java.lang.String certAlias, 
                                    java.lang.String algorithm)  
        throws XMLSignatureException {
        if (xmlString == null || xmlString.length() == 0) {
	    SAMLUtilsCommon.debug.error("signXML: xmlString is null.");
	    throw new XMLSignatureException(
		      SAMLUtilsCommon.bundle.getString("nullInput"));
	}        
        if (certAlias == null || certAlias.length() == 0) {
	    SAMLUtilsCommon.debug.error("signXML: certAlias is null.");
	    throw new XMLSignatureException(
		      SAMLUtilsCommon.bundle.getString("nullInput"));
	}      
        Element el = null;
        try {   
            Document doc = XMLUtils.toDOMDocument(xmlString,
                SAMLUtilsCommon.debug);
            el = signXML(doc, certAlias, algorithm); 
        } catch (Exception e) {
            SAMLUtilsCommon.debug.error("signXML Exception: ", e);
            throw new XMLSignatureException(e.getMessage());
        }
        
        return XMLUtils.print(el);
    }
 
    /**  
     * Sign part of the xml document referered by the supplied id attribute
     * using enveloped signatures and use exclusive xml canonicalization.    
     * @param doc XML dom object   
     * @param certAlias Signer's certificate alias name 
     * @param algorithm XML signature algorithm   
     * @param id id attribute value of the node to be signed 
     * @return signature dom object
     * @throws XMLSignatureException if the document could not be signed
     */                                                                        
     
    public org.w3c.dom.Element signXML(org.w3c.dom.Document doc,   
                                       java.lang.String certAlias, 
                                       java.lang.String algorithm, 
                                       java.lang.String id) 
        throws XMLSignatureException {
        return signXML(doc, certAlias, algorithm, DEF_ID_ATTRIBUTE,
				id, false, null);
    }

    /**
     * Sign part of the xml document referered by the supplied id attribute
     * using enveloped signatures and use exclusive xml canonicalization.
     * @param doc XML dom object
     * @param certAlias Signer's certificate alias name
     * @param algorithm XML signature algorithm
     * @param id id attribute value of the node to be signed
     * @param xpath expression should uniquly identify a node before which
     * @return signature dom object
     * @throws XMLSignatureException if the document could not be signed
     */

    public org.w3c.dom.Element signXML(org.w3c.dom.Document doc,
                                       java.lang.String certAlias,
                                       java.lang.String algorithm,
                                       java.lang.String id,
				       java.lang.String xpath)
        throws XMLSignatureException {
        return signXML(doc, certAlias, algorithm, DEF_ID_ATTRIBUTE,
					id, false, xpath);
    }

     /**
     * Sign part of the xml document referered by the supplied id attribute
     * using enveloped signatures and use exclusive xml canonicalization.
     * @param doc XML dom object
     * @param certAlias Signer's certificate alias name
     * @param algorithm XML signature algorithm
     * @param idAttrName attribute name for the id attribute of the node to
     *        be signed
     * @param id id attribute value of the node to be signed
     * @param includeCert if true, include the signing certificate in KeyInfo.
     *                    if false, does not include the signing certificate.
     * @return signature dom object
     * @throws XMLSignatureException if the document could not be signed
     */

    public org.w3c.dom.Element signXML(org.w3c.dom.Document doc,
                                       java.lang.String certAlias,
                                       java.lang.String algorithm,
                                       java.lang.String idAttrName,
                                       java.lang.String id,
                                       boolean includeCert)
        throws XMLSignatureException {
	return signXML(doc, certAlias, algorithm, idAttrName,
					id, includeCert, null);
    }
       
    /**
     * Sign part of the xml document referered by the supplied id attribute
     * using enveloped signatures and use exclusive xml canonicalization.
     * @param xmlString a string representing XML dom object
     * @param certAlias Signer's certificate alias name
     * @param algorithm XML signature algorithm
     * @param idAttrName attribute name for the id attribute of the node to be
     *        signed.
     * @param id id attribute value of the node to be signed
     * @param includeCert if true, include the signing certificate in KeyInfo.
     *                    if false, does not include the signing certificate.
     * @return a string representing signature dom object
     * @throws XMLSignatureException if the document could not be signed
     */
    public java.lang.String signXML(java.lang.String xmlString,
                                       java.lang.String certAlias,
                                       java.lang.String algorithm,
                                       java.lang.String idAttrName,
                                       java.lang.String id,
                                       boolean includeCert)
        throws XMLSignatureException {
        if (xmlString == null || xmlString.length() == 0) {
	    SAMLUtilsCommon.debug.error("signXML: xmlString is null.");
	    throw new XMLSignatureException(
		      SAMLUtilsCommon.bundle.getString("nullInput"));
	}     
        Document doc = null; 
        try {   
            doc = XMLUtils.toDOMDocument(xmlString, SAMLUtilsCommon.debug);
        } catch (Exception e) {
            SAMLUtilsCommon.debug.error("signXML Exception: ", e);
            throw new XMLSignatureException(e.getMessage());
        }    
	Element el = signXML(doc, certAlias, algorithm, idAttrName,
					id, includeCert, null);
        return XMLUtils.print(el);
    }
    
     /**
     * Sign part of the xml document referred by the supplied id attribute
     * using enveloped signatures and use exclusive xml canonicalization.
     * @param doc XML dom object
     * @param certAlias Signer's certificate alias name
     * @param algorithm XML signature algorithm
     * @param idAttrName attribute name for the id attribute of the node to
     *        be signed
     * @param id id attribute value of the node to be signed
     * @param includeCert if true, include the signing certificate in KeyInfo.
     *                    if false, does not include the signing certificate.
     * @param xpath expression should uniquly identify a node before which
     * @return a signed dom object
     * @throws XMLSignatureException if the document could not be signed
     */
    public Element signXML(Document doc,
                           String certAlias,
                           String algorithm,
                           String idAttrName,
                           String id,
                           boolean includeCert,
				           String xpath) throws XMLSignatureException {

        return signXMLUsingKeyPass(doc,
                certAlias,
                null,
                algorithm,
                idAttrName,
                id,
                includeCert,
                xpath);
    }

    /**
     * Sign part of the XML document referred by the supplied id attribute
     * using enveloped signatures and use exclusive XML canonicalization.
     * @param doc XML dom object
     * @param certAlias Signer's certificate alias name
     * @param encryptedKeyPass Use the supplied encrypted key password to get the private key
     * @param algorithm XML signature algorithm
     * @param idAttrName attribute name for the id attribute of the node to be
     *        signed.
     * @param id id attribute value of the node to be signed
     * @param includeCert if true, include the signing certificate in
     *        <code>KeyInfo</code>.
     *                    if false, does not include the signing certificate.
     * @param xpath expression should uniquely identify a node before which
     * @return a signed dom object
     * @throws XMLSignatureException if the document could not be signed
     */
    public Element signXMLUsingKeyPass(Document doc,
                                       String certAlias,
                                       String encryptedKeyPass,
                                       String algorithm,
                                       String idAttrName,
                                       String id,
                                       boolean includeCert,
                                       String xpath) throws XMLSignatureException {

        if (doc == null) { 
            SAMLUtilsCommon.debug.error("signXML: doc is null.");  
            throw new XMLSignatureException( 
                      SAMLUtilsCommon.bundle.getString("nullInput"));  
        }                                                                      
   
        if (certAlias == null || certAlias.length() == 0) {   
            SAMLUtilsCommon.debug.error("signXML: certAlias is null.");  
            throw new XMLSignatureException(   
                      SAMLUtilsCommon.bundle.getString("nullInput"));
        }    
        Element root = null;
        XMLSignature sig = null; 
        try {      
            ElementProxy.setDefaultPrefix(Constants.SignatureSpecNS, SAMLConstants.PREFIX_DS);
            PrivateKey privateKey;
            if (encryptedKeyPass == null || encryptedKeyPass.isEmpty()) {
                privateKey = keystore.getPrivateKey(certAlias);
            } else {
                privateKey = keystore.getPrivateKey(certAlias, encryptedKeyPass);
            }
            if (privateKey == null) {         
                SAMLUtilsCommon.debug.error("private key is null");  
                throw new XMLSignatureException(   
                    SAMLUtilsCommon.bundle.getString("nullprivatekey"));   
            }                   
            root = (Element) XPathAPI.selectSingleNode(   
                doc, "//*[@" + idAttrName + "=\"" + id + "\"]");

            if (root == null) {   
                SAMLUtilsCommon.debug.error("signXML: could not" 
                    + " resolv id attribute");
                throw new XMLSignatureException(  
                    SAMLUtilsCommon.bundle.getString("invalidIDAttribute"));  
            }
            
            // Set the ID attribute if idAttrName is not the default.
            if (!idAttrName.equals(DEF_ID_ATTRIBUTE)) {
                root.setIdAttribute(idAttrName, true);
            }
            if (algorithm == null || algorithm.length() == 0) {
                algorithm = getKeyAlgorithm(privateKey); ; 
            }    
            if (!isValidAlgorithm(algorithm)) { 
                throw new XMLSignatureException( 
                    SAMLUtilsCommon.bundle.getString("invalidalgorithm")); 
            }   
            sig = new XMLSignature(doc, "", algorithm,   
                Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);  
            if (xpath == null) {
                root.appendChild(sig.getElement());
            } else {
                Node beforeNode = XPathAPI.selectSingleNode(doc, xpath);
                root.insertBefore(sig.getElement(), beforeNode);
            }
            sig.getSignedInfo().addResourceResolver(new OfflineResolver());
            // do transform   
            Transforms transforms = new Transforms(doc); 
            transforms.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE); 
            transforms.addTransform(
				Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);
            String ref = "#" + id;  
            sig.addDocument(ref, transforms, Constants.ALGO_ID_DIGEST_SHA1);
            if (includeCert) {
                X509Certificate cert =
                    (X509Certificate) keystore.getX509Certificate(certAlias);
                sig.addKeyInfo(cert);
            }
            sig.sign(privateKey);  
        } catch (Exception e) {     
	        SAMLUtilsCommon.debug.error("signXML Exception: ", e);
            throw new XMLSignatureException(e.getMessage());     
        }   
        return (sig.getElement());   
    }                                                                          
      
    /**    
     * Sign the xml string using enveloped signatures.
     * @param xmlString xml string to be signed  
     * @param certAlias Signer's certificate alias name  
     * @param algorithm XML Signature algorithm    
     * @param id id attribute value of the node to be signed  
     * @return XML signature string 
     * @throws XMLSignatureException if the xml string could not be signed  
     */                                                                        


    public java.lang.String signXML(java.lang.String xmlString, 
                                    java.lang.String certAlias,  
                                    java.lang.String algorithm, 
                                    java.lang.String id)   
        throws XMLSignatureException {     
        if (xmlString == null || xmlString.length() == 0) { 
            SAMLUtilsCommon.debug.error("signXML: xmlString is null.");
            throw new XMLSignatureException(  
                      SAMLUtilsCommon.bundle.getString("nullInput")); 
        }  
        if (certAlias == null || certAlias.length() == 0) {   
            SAMLUtilsCommon.debug.error("signXML: certAlias is null.");  
            throw new XMLSignatureException(
                      SAMLUtilsCommon.bundle.getString("nullInput"));
        }             
        Element el = null; 
        try {        
            Document doc = XMLUtils.toDOMDocument(xmlString, 
                SAMLUtilsCommon.debug); 
            el = signXML(doc, certAlias, algorithm, id);  
        } catch (Exception e) {        
	    SAMLUtilsCommon.debug.error("signXML Exception: ", e);
            throw new XMLSignatureException(e.getMessage());  
        }    
        return XMLUtils.print(el);
    }                                                                          

      
    /**  
     * Sign part of the xml document referered by the supplied a list
     * of id attributes of nodes
     * @param doc XML dom object
     * @param certAlias Signer's certificate alias name
     * @param algorithm XML signature algorithm
     * @param ids list of id attribute values of nodes to be signed
     * @return signature dom object
     * @throws XMLSignatureException if the document could not be signed
     */
    public org.w3c.dom.Element signXML(org.w3c.dom.Document doc,   
                                       java.lang.String certAlias, 
                                       java.lang.String algorithm, 
                                       java.util.List ids) 
        throws XMLSignatureException {                               

        return signXML(doc, certAlias, algorithm, null, ids); 
    }
                                       
    /**  
     * Sign part of the xml document referered by the supplied a list
     * of id attributes of nodes
     * @param doc XML dom object
     * @param certAlias Signer's certificate alias name
     * @param algorithm XML signature algorithm
     * @param transformAlag XML siganture transform algorithm
     *        Those transfer constants are defined as
     *        SAMLConstants.TRANSFORM_XXX.       
     * @param ids list of id attribute values of nodes to be signed
     * @return signature dom object
     * @throws XMLSignatureException if the document could not be signed
     */ 
    public org.w3c.dom.Element signXML(org.w3c.dom.Document doc,   
                                       java.lang.String certAlias, 
                                       java.lang.String algorithm,
                                       java.lang.String transformAlag, 
                                       java.util.List ids) 
        throws XMLSignatureException {
        if (doc == null) { 
            SAMLUtilsCommon.debug.error("signXML: doc is null.");  
            throw new XMLSignatureException( 
                      SAMLUtilsCommon.bundle.getString("nullInput"));  
        }                                                                      
   
        if (certAlias == null || certAlias.length() == 0) {   
            SAMLUtilsCommon.debug.error("signXML: certAlias is null.");  
            throw new XMLSignatureException(   
                      SAMLUtilsCommon.bundle.getString("nullInput"));
        }    

        org.w3c.dom.Element root = doc.getDocumentElement();

        XMLSignature signature = null;
        try {
            ElementProxy.setDefaultPrefix(Constants.SignatureSpecNS, SAMLConstants.PREFIX_DS);
            PrivateKey privateKey =         
                (PrivateKey) keystore.getPrivateKey(certAlias);
            if (privateKey == null) {         
                SAMLUtilsCommon.debug.error("private key is null");  
                throw new XMLSignatureException(   
                          SAMLUtilsCommon.bundle.getString("nullprivatekey"));   
            }                   

            if (algorithm == null || algorithm.length() == 0) {   
                algorithm = getKeyAlgorithm(privateKey); 
            }    
            if (!isValidAlgorithm(algorithm)) { 
                throw new XMLSignatureException( 
                    SAMLUtilsCommon.bundle.getString("invalidalgorithm")); 
            }   
            signature = new XMLSignature(doc, "", algorithm,   
                  Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);  

	    root.appendChild(signature.getElement());
	    int size = ids.size();
	    for (int i = 0; i < size; ++i) {
		Transforms transforms =  new Transforms(doc);
		if (transformAlag != null) {
		    transforms.addTransform(transformAlag);
                }
		transforms.addTransform(
				Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);
		String id = (String) ids.get(i);
		if (SAMLUtilsCommon.debug.messageEnabled()) {
		    SAMLUtilsCommon.debug.message("id = " +id);
		}
		signature.addDocument("#"+id, transforms,
			Constants.ALGO_ID_DIGEST_SHA1);
	    }

	    X509Certificate cert =
                (X509Certificate) keystore.getX509Certificate(certAlias);
            signature.addKeyInfo(cert);
            signature.sign(privateKey);  

        } catch (Exception e) {     
	    SAMLUtilsCommon.debug.error("signXML Exception: ", e);
            throw new XMLSignatureException(e.getMessage());     
        }   

        return (signature.getElement());   
    }                                                                          
      
    /**
     *
     * Sign part of the xml document referered by the supplied a list
     * of id attributes of nodes
     * @param xmlString XML.
     * @param certAlias Signer's certificate alias name
     * @param algorithm XML signature algorithm
     * @param ids list of id attribute values of nodes to be signed
     * @return XML signature string
     * @throws XMLSignatureException if the document could not be signed
     */

    public java.lang.String signXML(java.lang.String xmlString,   
                                       java.lang.String certAlias, 
                                       java.lang.String algorithm, 
                                       java.util.List ids) 
        throws XMLSignatureException {
        if (xmlString == null || xmlString.length() == 0) { 
            SAMLUtilsCommon.debug.error("signXML: xmlString is null.");
            throw new XMLSignatureException(  
                      SAMLUtilsCommon.bundle.getString("nullInput")); 
        }  
        if (certAlias == null || certAlias.length() == 0) {   
            SAMLUtilsCommon.debug.error("signXML: certAlias is null.");  
            throw new XMLSignatureException(
                      SAMLUtilsCommon.bundle.getString("nullInput"));
        }             
        Element el = null; 
        try {        
            Document doc = XMLUtils.toDOMDocument(xmlString,
                SAMLUtilsCommon.debug);
            el = signXML(doc, certAlias, algorithm, ids);  
        } catch (Exception e) {        
            e.printStackTrace();     
            throw new XMLSignatureException(e.getMessage());  
        }    
        return XMLUtils.print(el);
    }                                                                          

    /**
     * Sign part of the xml document referered by the supplied a list
     * of id attributes of nodes
     * @param doc XML dom object
     * @param cert Signer's certificate
     * @param assertionID assertion ID
     * @param algorithm XML signature algorithm
     * @param ids list of id attribute values of nodes to be signed
     * @return SAML Security Token  signature
     * @throws XMLSignatureException if the document could not be signed
     */
    public Element signWithWSSSAMLTokenProfile(Document doc,
        java.security.cert.Certificate cert, String assertionID,
        String algorithm, List ids) throws XMLSignatureException {

        return signWithWSSSAMLTokenProfile(doc, cert, assertionID, algorithm,
            ids, SOAPBindingConstants.WSF_10_VERSION);
    }

    /**
     * Sign part of the xml document referered by the supplied a list
     * of id attributes of nodes
     * @param doc XML dom object
     * @param cert Signer's certificate
     * @param assertionID assertion ID
     * @param algorithm XML signature algorithm
     * @param ids list of id attribute values of nodes to be signed
     * @param wsfVersion the web services version.
     * @return SAML Security Token  signature
     * @throws XMLSignatureException if the document could not be signed
     */
    public Element signWithWSSSAMLTokenProfile(Document doc,
        java.security.cert.Certificate cert, String assertionID,
        String algorithm, List ids, String wsfVersion)
        throws XMLSignatureException {

	if (doc == null) {
            SAMLUtilsCommon.debug.error("signXML: doc is null.");
            throw new XMLSignatureException(
                      SAMLUtilsCommon.bundle.getString("nullInput"));
        }
        if (cert == null) {
            SAMLUtilsCommon.debug.error("signWithWSSSAMLTokenProfile: " +
					"Certificate is null");
            throw new XMLSignatureException(
                      SAMLUtilsCommon.bundle.getString("nullInput"));
        }
	if (assertionID == null) {
	    SAMLUtilsCommon.debug.error("signWithWSSSAMLTokenProfile: " +
					"AssertionID is null");
	    throw new XMLSignatureException(
		      SAMLUtilsCommon.bundle.getString("nullInput"));
	}

        this.wsfVersion = wsfVersion;
        String wsseNS = SAMLConstants.NS_WSSE;
        String wsuNS = SAMLConstants.NS_WSU;

        if ((wsfVersion != null) &&
            (wsfVersion.equals(SOAPBindingConstants.WSF_11_VERSION))) {
            wsseNS = WSSEConstants.NS_WSSE_WSF11;
            wsuNS = WSSEConstants.NS_WSU_WSF11;
        }

        Element root = (Element)doc.getDocumentElement().
            getElementsByTagNameNS(wsseNS, SAMLConstants.TAG_SECURITY).item(0);
        XMLSignature signature = null;
        try {
            ElementProxy.setDefaultPrefix(Constants.SignatureSpecNS, SAMLConstants.PREFIX_DS);
            Element wsucontext = org.apache.xml.security.utils.
                      XMLUtils.createDSctx(doc, "wsu", wsuNS);
            NodeList wsuNodes = (NodeList)XPathAPI.selectNodeList(doc,
                                "//*[@wsu:Id]", wsucontext);
            if(wsuNodes != null && wsuNodes.getLength() != 0) {
               for(int i=0; i < wsuNodes.getLength(); i++) {
                   Element elem = (Element) wsuNodes.item(i);
                   String id = elem.getAttributeNS(wsuNS, "Id");
                   if (id != null && id.length() != 0) {
                       elem.setIdAttributeNS(wsuNS, "Id", true);
                   }
               }
            }

            String certAlias = keystore.getCertificateAlias(cert);
            PrivateKey privateKey =
                (PrivateKey) keystore.getPrivateKey(certAlias);
            if (privateKey == null) {
                SAMLUtilsCommon.debug.error("private key is null");
                throw new XMLSignatureException(
                          SAMLUtilsCommon.bundle.getString("nullprivatekey"));
            }

            if (algorithm == null || algorithm.length() == 0) {
                algorithm = getKeyAlgorithm(privateKey); 
            }
            if (!isValidAlgorithm(algorithm)) {
                throw new XMLSignatureException(
                    SAMLUtilsCommon.bundle.getString("invalidalgorithm"));
            }
            signature = new XMLSignature(doc, "", algorithm,
                  Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

            root.appendChild(signature.getElement());
            int size = ids.size();
            for (int i = 0; i < size; ++i) {
                Transforms transforms = new Transforms(doc);
                transforms.addTransform(
				Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);
                String id = (String) ids.get(i);
                if (SAMLUtilsCommon.debug.messageEnabled()) {
                    SAMLUtilsCommon.debug.message("id = " +id);
                }
                signature.addDocument("#"+id, transforms,
                        Constants.ALGO_ID_DIGEST_SHA1);
            }
            KeyInfo keyInfo = signature.getKeyInfo();
            Element securityTokenRef = doc.createElementNS(wsseNS,
                SAMLConstants.TAG_SECURITYTOKENREFERENCE);
            keyInfo.addUnknownElement(securityTokenRef);

            securityTokenRef.setAttributeNS(SAMLConstants.NS_XMLNS,
                        SAMLConstants.TAG_XMLNS, wsseNS);
            securityTokenRef.setAttributeNS(SAMLConstants.NS_XMLNS,
                        SAMLConstants.TAG_XMLNS_SEC, SAMLConstants.NS_SEC);
            securityTokenRef.setAttributeNS(null, SAMLConstants.TAG_USAGE,
                        SAMLConstants.TAG_SEC_MESSAGEAUTHENTICATION);

            Element reference = doc.createElementNS(wsseNS,
                SAMLConstants.TAG_REFERENCE);
            reference.setAttributeNS(null, SAMLConstants.TAG_URI,
                        "#"+assertionID);

            securityTokenRef.appendChild(reference);

            signature.sign(privateKey);
        } catch (Exception e) {
            SAMLUtilsCommon.debug.error("signWithWSSX509TokenProfile " +
                "Exception: ", e);
            throw new XMLSignatureException(e.getMessage());
        }

	if (SAMLUtilsCommon.debug.messageEnabled()) {
            SAMLUtilsCommon.debug.message("SAML Signed doc = " +
		XMLUtils.print(doc.getDocumentElement()));
	}

        return signature.getElement();
    }


    /**
     * Sign part of the xml document referered by the supplied a list
     * of id attributes  of nodes
     * @param doc XML dom object
     * @param cert Signer's certificate
     * @param algorithm XML signature algorithm
     * @param ids list of id attribute values of nodes to be signed
     * @return X509 Security Token  signature
     * @throws XMLSignatureException if the document could not be signed
     */
    public Element signWithWSSX509TokenProfile(Document doc,
        java.security.cert.Certificate cert, String algorithm, List ids)
        throws XMLSignatureException {

        return signWithWSSX509TokenProfile(doc, cert, algorithm, ids,
            SOAPBindingConstants.WSF_10_VERSION);
    }

    /**
     * Sign part of the xml document referered by the supplied a list
     * of id attributes  of nodes
     * @param doc XML dom object
     * @param cert Signer's certificate
     * @param algorithm XML signature algorithm
     * @param ids list of id attribute values of nodes to be signed
     * @param wsfVersion the web services version.
     * @return X509 Security Token  signature
     * @throws XMLSignatureException if the document could not be signed
     */
    public Element signWithWSSX509TokenProfile(Document doc,
        java.security.cert.Certificate cert, String algorithm, List ids,
        String wsfVersion) throws XMLSignatureException {
  
        if (doc == null) { 
            SAMLUtilsCommon.debug.error("signXML: doc is null.");  
            throw new XMLSignatureException( 
                      SAMLUtilsCommon.bundle.getString("nullInput"));  
        }                                                                      
   
	if (SAMLUtilsCommon.debug.messageEnabled()) {
	    SAMLUtilsCommon.debug.message("Soap Envlope: " +
		XMLUtils.print(doc.getDocumentElement()));
	}

        this.wsfVersion = wsfVersion;
        String wsseNS = SAMLConstants.NS_WSSE;
        String wsuNS = SAMLConstants.NS_WSU;

        if ((wsfVersion != null) &&
           (wsfVersion.equals(SOAPBindingConstants.WSF_11_VERSION))) {
            wsseNS = WSSEConstants.NS_WSSE_WSF11;
            wsuNS = WSSEConstants.NS_WSU_WSF11;
        }

        Element root = (Element)doc.getDocumentElement().
            getElementsByTagNameNS(wsseNS, SAMLConstants.TAG_SECURITY).item(0);

        XMLSignature signature = null;
        try {
            ElementProxy.setDefaultPrefix(Constants.SignatureSpecNS, SAMLConstants.PREFIX_DS);
            Element wsucontext = org.apache.xml.security.utils.
                XMLUtils.createDSctx(doc, "wsu", wsuNS);
            NodeList wsuNodes = (NodeList)XPathAPI.selectNodeList(doc,
                "//*[@wsu:Id]", wsucontext);
            if ((wsuNodes != null) && (wsuNodes.getLength() != 0)) {
               for(int i=0; i < wsuNodes.getLength(); i++) {
                   Element elem = (Element) wsuNodes.item(i);
                   String id = elem.getAttributeNS(wsuNS, "Id");
                   if (id != null && id.length() != 0) {
                       elem.setIdAttributeNS(wsuNS, "Id", true);
                   }
               }
            }

            String certAlias = keystore.getCertificateAlias(cert);
            PrivateKey privateKey =         
                (PrivateKey) keystore.getPrivateKey(certAlias);
            if (privateKey == null) {         
                SAMLUtilsCommon.debug.error("private key is null");  
                throw new XMLSignatureException(   
                    SAMLUtilsCommon.bundle.getString("nullprivatekey"));   
            }                   

            // TODO: code clean up
            // should find cert alias, add security token and call signXML
            // to avoid code duplication
            if (algorithm == null || algorithm.length() == 0) {   
                algorithm = getKeyAlgorithm(privateKey); 
            }    
            if (!isValidAlgorithm(algorithm)) { 
                throw new XMLSignatureException( 
                    SAMLUtilsCommon.bundle.getString("invalidalgorithm")); 
            }   
            signature = new XMLSignature(doc, "", algorithm,   
                  Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);  

	    root.appendChild(signature.getElement());
	    int size = ids.size();
	    for (int i = 0; i < size; ++i) {
		Transforms transforms = new Transforms(doc);
		transforms.addTransform(
				Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);
		String id = (String) ids.get(i);
		if (SAMLUtilsCommon.debug.messageEnabled()) {
		    SAMLUtilsCommon.debug.message("id = " +id);
		}
		signature.addDocument("#"+id, transforms,
			Constants.ALGO_ID_DIGEST_SHA1);
	    }

	    KeyInfo keyInfo = signature.getKeyInfo();
	    Element securityTokenRef = doc.createElementNS(wsseNS,
                SAMLConstants.TAG_SECURITYTOKENREFERENCE);
	    keyInfo.addUnknownElement(securityTokenRef);
	    securityTokenRef.setAttributeNS(SAMLConstants.NS_XMLNS,
		    SAMLConstants.TAG_XMLNS, wsseNS);
	    securityTokenRef.setAttributeNS(SAMLConstants.NS_XMLNS,
		    SAMLConstants.TAG_XMLNS_SEC, SAMLConstants.NS_SEC);
	    securityTokenRef.setAttributeNS(null, SAMLConstants.TAG_USAGE,
		    SAMLConstants.TAG_SEC_MESSAGEAUTHENTICATION);

            Element bsf = (Element)root.getElementsByTagNameNS(wsseNS,
                SAMLConstants.BINARYSECURITYTOKEN).item(0);

	    String certId = bsf.getAttributeNS(wsuNS, SAMLConstants.TAG_ID);

	    Element reference =	doc.createElementNS(wsseNS,
                SAMLConstants.TAG_REFERENCE);
	    securityTokenRef.appendChild(reference);
	    reference.setAttributeNS(null, SAMLConstants.TAG_URI, "#"+certId);

            signature.sign(privateKey);  

        } catch (Exception e) {     
            SAMLUtilsCommon.debug.error("signWithWSSX509TokenProfile" +
                " Exception: ", e);
            throw new XMLSignatureException(e.getMessage());     
        }   

        return (signature.getElement());   
    }

    /** 
     * Verify all the signatures of the xml document  
     * @param doc XML dom document whose signature to be verified    
     * @param certAlias certAlias alias for Signer's certificate, this is used 
     *     to search signer's public certificate if it is not presented in
     *     ds:KeyInfo      
     * @return true if the xml signature is verified, false otherwise 
     * @throws XMLSignatureException if problem occurs during verification
     */                                                                        
    public boolean verifyXMLSignature(Document doc, String certAlias)  
        throws XMLSignatureException {  

        return verifyXMLSignature(SOAPBindingConstants.WSF_10_VERSION,
            certAlias, doc);
    }

    /**
     * Verify all the signatures of the xml document
     * @param wsfVersion the web services version.
     * @param doc XML dom document whose signature to be verified
     * @param certAlias certAlias alias for Signer's certificate, this is used
     *     to search signer's public certificate if it is not presented in
     *     ds:KeyInfo
     * @return true if the xml signature is verified, false otherwise
     * @exception XMLSignatureException if problem occurs during verification
     */
    public boolean verifyXMLSignature(String wsfVersion, String certAlias,
        Document doc) throws XMLSignatureException {

        if (doc == null) {   
            SAMLUtilsCommon.debug.error("verifyXMLSignature:" +
                " document is null."); 
            throw new XMLSignatureException(    
                SAMLUtilsCommon.bundle.getString("nullInput"));    
        }               

        try {      
            this.wsfVersion = wsfVersion;
            String wsuNS = SAMLConstants.NS_WSU;
            String wsseNS = SAMLConstants.NS_WSSE;

            if((wsfVersion != null) &&
               (wsfVersion.equals(SOAPBindingConstants.WSF_11_VERSION))) {
               wsuNS = WSSEConstants.NS_WSU_WSF11;
               wsseNS = WSSEConstants.NS_WSSE_WSF11;
            }

            Element wsucontext = org.apache.xml.security.utils.XMLUtils.createDSctx(doc, "wsu", wsuNS);

            NodeList wsuNodes = (NodeList) XPathAPI.selectNodeList(doc, "//*[@wsu:Id]", wsucontext);

            if ((wsuNodes != null) && (wsuNodes.getLength() != 0)) {
                for (int i = 0; i < wsuNodes.getLength(); i++) {
                    Element elem = (Element) wsuNodes.item(i);
                    String id = elem.getAttributeNS(wsuNS, "Id");
                    if ((id != null) && (id.length() != 0)) {
                        elem.setIdAttributeNS(wsuNS, "Id", true);
                    }
                }
            }

            Element nscontext = org.apache.xml.security.utils.
                  XMLUtils.createDSctx (doc,"ds",Constants.SignatureSpecNS); 
            NodeList sigElements = XPathAPI.selectNodeList (doc,  
                "//ds:Signature", nscontext);    
	    if (SAMLUtilsCommon.debug.messageEnabled()) {
		SAMLUtilsCommon.debug.message("verifyXMLSignature: " + 
                    "sigElements size = " + sigElements.getLength());
	    }
            X509Certificate newcert= keystore.getX509Certificate (certAlias); 
            PublicKey key = keystore.getPublicKey (certAlias); 
            Element sigElement = null; 
            //loop       
            for (int i = 0; i < sigElements.getLength(); i++) {
                sigElement = (Element)sigElements.item(i);
		if (SAMLUtilsCommon.debug.messageEnabled ()) {
		    SAMLUtilsCommon.debug.message("Sig(" + i + ") = " +
			XMLUtils.print(sigElement));
		}
                Element refElement;
                try {
                    refElement = (Element) XPathAPI.selectSingleNode(sigElement, "//ds:Reference[1]", nscontext);
                } catch (TransformerException te) {
                    throw new XMLSignatureException(te);
                }
                String refUri = refElement.getAttribute("URI");
                String signedId = null;
                Element parentElement = (Element) sigElement.getParentNode();
                if (parentElement != null) {
                    String idAttrName = null;
                    if ("Assertion".equals(parentElement.getLocalName())) {
                        idAttrName = "AssertionID";
                    } else if ("Response".equals(parentElement.getLocalName())) {
                        idAttrName = "ResponseID";
                    } else if ("Request".equals(parentElement.getLocalName())) {
                        idAttrName = "RequestID";
                    } else {
                        throw new UnsupportedOperationException("Enveloping and detached XML signatures are no longer"
                                + " supported");
                    }
                    if (idAttrName != null) {
                        parentElement.setIdAttribute(idAttrName, true);
                        signedId = parentElement.getAttribute(idAttrName);
                    }
                }
                //NB: this validation only works with enveloped XML signatures, enveloping and detached signatures are
                //no longer supported.
                if (refUri == null || signedId == null || !refUri.substring(1).equals(signedId)) {
                    SAMLUtilsCommon.debug.error("Signature reference ID does not match with element ID");
                    throw new XMLSignatureException(SAMLUtilsCommon.bundle.getString("uriNoMatchWithId"));
                }
                XMLSignature signature = new XMLSignature (sigElement, "");
                signature.addResourceResolver (
                    new com.sun.identity.saml.xmlsig.OfflineResolver ());
                KeyInfo ki = signature.getKeyInfo ();
		PublicKey pk = this.getX509PublicKey(doc, ki);
		if (pk!=null) {
		    // verify using public key
		    if (signature.checkSignatureValue (pk)) {
			if (SAMLUtilsCommon.debug.messageEnabled ()) {
			    SAMLUtilsCommon.debug.message (
                                "verifyXMLSignature:" +
				" Signature " + i + " verified");
			}
		    } else {
                        if(SAMLUtilsCommon.debug.messageEnabled()) {
                           SAMLUtilsCommon.debug.message(
                               "verifyXMLSignature:" +
                               " Signature Verfication failed");
                        }
			return false;
		    }
                } else {
                    if (certAlias == null || certAlias.equals ("")) {
                        if(SAMLUtilsCommon.debug.messageEnabled()) {
                           SAMLUtilsCommon.debug.message(
                               "verifyXMLSignature:" +
                                "Certificate Alias is null");
                        }
                        return false;
                    }
                    if (SAMLUtilsCommon.debug.messageEnabled ()) {
                        SAMLUtilsCommon.debug.message (
                            "Could not find a KeyInfo, " +
                            "try to use certAlias");
                    }
                    if (newcert != null) {
                        if (signature.checkSignatureValue (newcert)) {
                            if (SAMLUtilsCommon.debug.messageEnabled ()) {
                                SAMLUtilsCommon.debug.message (
                                    "verifyXMLSignature:" +
			            " Signature " + i + " verified");
                            }
                        } else {
                            return false;
                        }
                    } else {            
                        if (key != null) {
                            if (signature.checkSignatureValue (key)) {
                                if (SAMLUtilsCommon.debug.messageEnabled ()) {
                                    SAMLUtilsCommon.debug.message (
                                    "verifyXMLSignature: Signature " + i +
				    " verified");
                                }
                            } else {
                                return false;
                            }
                        } else {
                            SAMLUtilsCommon.debug.error (
                                "Could not find public key"
				+ " based on certAlias to verify signature");
                            return false;
                        }
                    }                
                }
            }
            return true;  
        } catch (Exception ex) {   
	    SAMLUtilsCommon.debug.error("verifyXMLSignature Exception: ", ex);
            throw new XMLSignatureException (ex.getMessage ());
        }          
    }                                                                                

    /**                             
     * Verify the signature of the xml document 
     * @param doc XML dom document whose signature to be verified      
     * @return true if the xml signature is verified, false otherwise 
     * @throws XMLSignatureException if problem occurs during verification 
     */                                                                        
                                                                               

    public boolean verifyXMLSignature(org.w3c.dom.Document doc) 
        throws XMLSignatureException {       
        if (doc == null) { 
            SAMLUtilsCommon.debug.error(
                "verifyXMLSignature: document is null.");   
            throw new XMLSignatureException(    
                SAMLUtilsCommon.bundle.getString("nullInput"));     
        }                   
	return verifyXMLSignature(doc, (String)null);
    }
    
    /**
     * Verify the signature of the xml element.
     *
     * @param element XML dom element whose signature to be verified 
     * @return true if the xml signature is verified, false otherwise
     * @throws XMLSignatureException if problem occurs during verification
     */
    public boolean verifyXMLSignature(org.w3c.dom.Element element)
        throws XMLSignatureException {
        if (element == null) {
	    SAMLUtilsCommon.debug.error("signXML: element is null.");
	    throw new XMLSignatureException(
		      SAMLUtilsCommon.bundle.getString("nullInput"));
	}    
        return verifyXMLSignature(XMLUtils.print(element));
    }

    /**
     * Verify the signature of the xml document
     * @param element XML Element whose signature to be verified
     * @param certAlias certAlias alias for Signer's certificate, this is used
                        to search signer's public certificate if it is not
                        presented in ds:KeyInfo
     * @return true if the xml signature is verified, false otherwise
     * @throws XMLSignatureException if problem occurs during verification
     */
    public boolean verifyXMLSignature(org.w3c.dom.Element element,
                                      java.lang.String certAlias)
        throws XMLSignatureException {
        return verifyXMLSignature(element, DEF_ID_ATTRIBUTE, certAlias); 
    }

    /**
     * Verify the signature of the xml document 
     * @param element XML Element whose signature to be verified 
     * @param idAttrName Attribute name for the id attribute
     * @param certAlias certAlias alias for Signer's certificate, this is used 
                        to search signer's public certificate if it is not 
                        presented in ds:KeyInfo
     * @return true if the xml signature is verified, false otherwise
     * @throws XMLSignatureException if problem occurs during verification
     */
    public boolean verifyXMLSignature(org.w3c.dom.Element element,
                                      java.lang.String idAttrName, 
                                      java.lang.String certAlias)
        throws XMLSignatureException {
        if (element == null) {
	    SAMLUtilsCommon.debug.error("signXML: element is null.");
	    throw new XMLSignatureException(
		      SAMLUtilsCommon.bundle.getString("nullInput"));
	}            
        Document doc = null;
        try {
            doc = XMLUtils.newDocument();
            doc.appendChild(doc.importNode(element, true));
        } catch (Exception ex) {
            SAMLUtilsCommon.debug.error("verifyXMLSignature Exception: ", ex);
            throw new XMLSignatureException(ex.getMessage());
        }

        return verifyXMLSignature(doc, idAttrName,
           certAlias);                                                
    }

    /**
     * Verify the signature of the xml document
     * @param doc XML dom document whose signature to be verified
     * @param cert Signer's certificate, this is used to search signer's
              public certificate if it is not presented in ds:KeyInfo
     * @return true if the xml signature is verified, false otherwise
     * @throws XMLSignatureException if problem occurs during verification
     */
    public boolean verifyXMLSignature(org.w3c.dom.Document doc,
                                      java.security.cert.Certificate cert)
        throws XMLSignatureException {
        if (doc == null) {
            SAMLUtilsCommon.debug.error("verifyXMLSignature: " +
                "document is null.");
            throw new XMLSignatureException(
                SAMLUtilsCommon.bundle.getString("nullInput"));
        }  
	String certAlias = keystore.getCertificateAlias(cert);
        return verifyXMLSignature(doc, certAlias);

    }


    /**
     * Verify the signature of the xml string 
     * @param xmlString XML string whose signature to be verified 
     * @return true if the xml signature is verified, false otherwise
     * @throws XMLSignatureException if problem occurs during verification
     */
    public boolean verifyXMLSignature(java.lang.String xmlString) 
        throws XMLSignatureException {
        return verifyXMLSignature(xmlString, null);     
    }
            
    /**
     * Verify the signature of the xml string 
     * @param xmlString XML string whose signature to be verified 
     * @param certAlias certAlias alias for Signer's certificate, this is used 
                        to search signer's public certificate if it is not 
                        presented in ds:KeyInfo
     * @return true if the xml signature is verified, false otherwise
     * @throws XMLSignatureException if problem occurs during verification
     */
    public boolean verifyXMLSignature(java.lang.String xmlString, 
                                      java.lang.String certAlias)
        throws XMLSignatureException {
        return verifyXMLSignature(xmlString, DEF_ID_ATTRIBUTE, certAlias);
    }

    /**
     * Verify the signature of the xml string
     * @param xmlString XML string whose signature to be verified
     * @param idAttrName Attribute name for the id attribute 
     * @param certAlias certAlias alias for Signer's certificate, this is used
                        to search signer's public certificate if it is not
                        presented in ds:KeyInfo
     * @return true if the xml signature is verified, false otherwise
     * @throws XMLSignatureException if problem occurs during verification
     */
    public boolean verifyXMLSignature(java.lang.String xmlString,
                                      java.lang.String idAttrName, 
                                      java.lang.String certAlias)
        throws XMLSignatureException {
        if (xmlString == null || xmlString.length() == 0) {
	    SAMLUtilsCommon.debug.error("signXML: xmlString is null.");
	    throw new XMLSignatureException(
		      SAMLUtilsCommon.bundle.getString("nullInput"));
	}   

        Document doc = XMLUtils.toDOMDocument(xmlString,
            SAMLUtilsCommon.debug);
        try {
            return verifyXMLSignature(doc, idAttrName, certAlias);
        } catch (Exception ex) {
            SAMLUtilsCommon.debug.error("verifyXMLSignature Exception: ", ex);
            throw new XMLSignatureException(ex.getMessage());
        }
    }



    /**
     * Verify the signature of a DOM Document
     * @param doc a DOM Document
     * @param idAttrName Attribute name for the id attribute 
     * @param certAlias certAlias alias for Signer's certificate, this is used
                        to search signer's public certificate if it is not
                        presented in ds:KeyInfo
     * @return true if the xml signature is verified, false otherwise
     * @throws XMLSignatureException if problem occurs during verification
     */
    public boolean verifyXMLSignature(Document doc,
                                      java.lang.String idAttrName, 
                                      java.lang.String certAlias)
        throws XMLSignatureException {
        try {
            Element nscontext = org.apache.xml.security.utils.
                 XMLUtils.createDSctx(doc,"ds",Constants.SignatureSpecNS);
            Element sigElement = (Element) XPathAPI.selectSingleNode(doc,
                                 "//ds:Signature[1]", nscontext);
            Element refElement;
            try {
                refElement = (Element) XPathAPI.selectSingleNode(sigElement, "//ds:Reference[1]", nscontext);
            } catch (TransformerException te) {
                throw new XMLSignatureException(te);
            }
            String refUri = refElement.getAttribute("URI");
            String signedId = ((Element) sigElement.getParentNode()).getAttribute(idAttrName);
            if (refUri == null || signedId == null || !refUri.substring(1).equals(signedId)) {
                SAMLUtilsCommon.debug.error("Signature reference ID does not match with element ID");
                throw new XMLSignatureException(SAMLUtilsCommon.bundle.getString("uriNoMatchWithId"));
            }
            XMLSignature signature = new XMLSignature(sigElement, "");
            signature.addResourceResolver(
                        new com.sun.identity.saml.xmlsig.OfflineResolver());

            doc.getDocumentElement().setIdAttribute(idAttrName, true);

            KeyInfo ki = signature.getKeyInfo();
            PublicKey pk = this.getX509PublicKey(doc, ki);
            if (pk!=null) {
                // verify using public key
                if (signature.checkSignatureValue (pk)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                if (certAlias == null || certAlias.length() == 0) {
                    return false; 
                }
                if (SAMLUtilsCommon.debug.messageEnabled()) {
                    SAMLUtilsCommon.debug.message("Could not find a KeyInfo, "
                        + "try to use certAlias");
                }
                X509Certificate newcert= 
                    keystore.getX509Certificate(certAlias);
                if (newcert != null) { 
                    if (signature.checkSignatureValue(newcert)) { 
                        return true; 
                    } else {
                        return false;
                    }
                } else {
                    PublicKey key = keystore.getPublicKey(certAlias); 
                    if (key != null) {
                       if (signature.checkSignatureValue(key)) {
                           return true; 
                       } else {
                           return false;
                       }
                    } else {
                        SAMLUtilsCommon.debug.error("Could not find " +
                            "public key based on certAlias to verify" +
                            " signature");
                        return false; 
                    }
                } 
            }
        } catch (Exception ex) {
            SAMLUtilsCommon.debug.error("verifyXMLSignature Exception: ", ex);
            throw new XMLSignatureException(ex.getMessage());
        }
    }
   
    /**
     * Get the real key provider
     * @return KeyProvider 
     */
    public KeyProvider getKeyProvider() {
        return keystore; 
    }
    
    /**
     * Get the X509Certificate embedded in the KeyInfo
     * @param keyinfo KeyInfo
     * @return a X509Certificate
     */
    protected PublicKey getX509PublicKey(Document doc, KeyInfo keyinfo) {
	PublicKey pk = null;
        try {
            if (keyinfo != null) {
                if (isJKSKeyStore) {
                    StorageResolver storageResolver = new StorageResolver(
                       new KeyStoreResolver(((JKSKeyProvider)
                                        keystore).getKeyStore()));
                    keyinfo.addStorageResolver(storageResolver);
                    keyinfo.registerInternalKeyResolver(new
                                        X509IssuerSerialResolver());
                    keyinfo.registerInternalKeyResolver(new
                                        X509CertificateResolver());
                    keyinfo.registerInternalKeyResolver(new X509SKIResolver());
                    keyinfo.registerInternalKeyResolver(new
                                        X509SubjectNameResolver());
                }
                if (keyinfo.containsX509Data()) {
                    if (SAMLUtilsCommon.debug.messageEnabled()) {
                        SAMLUtilsCommon.debug.message("Found X509Data" +
                                                " element in the KeyInfo");
                    }
                    X509Certificate certificate =
                        keyinfo.getX509Certificate();
                    //use a systemproperty com.sun.identity.saml.checkcert
                    //defined in AMConfig.properties, as a nob to check the
                    // the validity of the cert. 
                    if (checkCert) {
                        // validate the X509Certificate
                        if (keystore.getCertificateAlias(certificate)==null) {
                            SAMLUtilsCommon.debug.error ("verifyXMLSignature:"
                                + " certificate is not trusted.");
                            throw new XMLSignatureException (
                            SAMLUtilsCommon.bundle.getString(
                                "untrustedCertificate"));
                        } else {
                            if (SAMLUtilsCommon.debug.messageEnabled ()) {
                                SAMLUtilsCommon.debug.message(
                                    "verifyXMLSignature:"+ 
                                    " certificate is trused.");
                            }
                        }
                    } else {
                        if (SAMLUtilsCommon.debug.messageEnabled()) {
                            SAMLUtilsCommon.debug.message(
                                "Skip checking whether the"
                                +" cert in the cert db."); 
                        }
                    }
		    pk = getPublicKey(certificate);
                } else {
		    // Do we need to check if the public key is in the
		    // keystore!?
		    pk = getWSSTokenProfilePublicKey(doc);
                }
            }
        } catch (Exception e) {
            SAMLUtilsCommon.debug.error( 
                "getX509Certificate(KeyInfo) Exception: ", e);
        }

	return pk;
    }


    /**
     * Get the PublicKey embedded in the Security Token profile
     * @param doc the document to be verified
     * @return a PublicKey
     */
    private PublicKey getWSSTokenProfilePublicKey(Document doc) {
	PublicKey pubKey = null;
	
	try {
            SAMLUtilsCommon.debug.message("getWSSTTokenProfilePublicKey:"+
                " entering");

            String wsseNS = SAMLConstants.NS_WSSE;
            String wsuNS = SAMLConstants.NS_WSU;
            if ((wsfVersion != null) &&
                (wsfVersion.equals(SOAPBindingConstants.WSF_11_VERSION)) ) {
                wsseNS = WSSEConstants.NS_WSSE_WSF11;
                wsuNS = WSSEConstants.NS_WSU_WSF11;
            }
            Element securityElement = (Element) doc.getDocumentElement().
                getElementsByTagNameNS(wsseNS, SAMLConstants.TAG_SECURITY).
                item(0);
            if (securityElement == null) {
                return null;
            }

            Element nscontext = org.apache.xml.security.utils.
                XMLUtils.createDSctx(doc,"ds",Constants.SignatureSpecNS);
            Element sigElement = (Element) XPathAPI.selectSingleNode(
					securityElement, "ds:Signature[1]",
					nscontext);

	    Element keyinfo = (Element) sigElement.getElementsByTagNameNS(
                Constants.SignatureSpecNS, SAMLConstants.TAG_KEYINFO).item(0);
	    Element str = (Element) keyinfo.getElementsByTagNameNS(wsseNS,
                SAMLConstants.TAG_SECURITYTOKENREFERENCE).item(0);
	    Element reference = (Element) keyinfo.getElementsByTagNameNS(
                wsseNS, SAMLConstants.TAG_REFERENCE).item(0);

            if (reference != null) {
	        String id = reference.getAttribute(SAMLConstants.TAG_URI);
	        id = id.substring(1);
	        nscontext = org.apache.xml.security.utils.
                    XMLUtils.createDSctx(doc, SAMLConstants.PREFIX_WSU, wsuNS);
	        Node n = XPathAPI.selectSingleNode(
		    doc, "//*[@"+ SAMLConstants.PREFIX_WSU + ":" +
                    SAMLConstants.TAG_ID +"=\"" + id + "\"]", nscontext);

                if (n != null) { // X509 Security Token profile
                    SAMLUtilsCommon.debug.message("X509 Token");
	            String format = ((Element) n).getAttribute(
						SAMLConstants.TAG_VALUETYPE);
	            NodeList children = n.getChildNodes();
	            n = children.item(0);
	            String certString = n.getNodeValue().trim();

		    pubKey = getPublicKey(getCertificate(certString, format));

	        } else { // SAML Token profile
                    SAMLUtilsCommon.debug.message("SAML Token");
                    reference = (Element) XPathAPI.selectSingleNode(
                            doc, "//*[@AssertionID=\"" + id + "\"]");
                    if (SAMLUtilsCommon.debug.messageEnabled()) {
                        SAMLUtilsCommon.debug.message("SAML Assertion = " +
                            XMLUtils.print(reference));
                    }
		    // The SAML Statements contain keyinfo, they should be
		    // all the same. get the first keyinfo!
		    reference = (Element) reference.getElementsByTagNameNS(
					Constants.SignatureSpecNS,
					SAMLConstants.TAG_KEYINFO).item(0);
		    if (reference == null) { // no cert found!
			SAMLUtilsCommon.debug.message(
                           "getWSSTokenProfilePublicKey:" +
		           " no KeyInfo found!");
			throw new Exception(
			    SAMLUtilsCommon.bundle.getString("nullKeyInfo"));
		    }
		    Element x509Data =
				(Element) reference.getElementsByTagNameNS(
                			Constants.SignatureSpecNS,
					SAMLConstants.TAG_X509DATA).item(0);
		    if (x509Data !=null) { // Keyinfo constains certificate
			reference = (Element) x509Data.getChildNodes().item(0);
			String certString = x509Data.getChildNodes().item(0).
						getChildNodes().item(0).
						getNodeValue();
			if (SAMLUtilsCommon.debug.messageEnabled()) {
			    SAMLUtilsCommon.debug.message("certString = " +
								certString);
			}
			return getPublicKey(getCertificate(certString, null));
		    } else { // it should contains RSA/DSA key
			pubKey = getPublicKeybyDSARSAkeyValue(doc, reference);
		    }
                }
            } else {
                SAMLUtilsCommon.debug.error("getWSSTokenProfilePublicKey:" +
                                " unknow Security Token Reference");
	    }
        } catch (Exception e) {
	    SAMLUtilsCommon.debug.error(
			"getWSSTokenProfilePublicKey Exception: ", e);
	}
        return pubKey;
    }

    protected PublicKey getPublicKeybyDSARSAkeyValue(Document doc,
						   Element reference)
    throws XMLSignatureException {

	PublicKey pubKey = null;
	Element dsaKey = (Element) reference.getElementsByTagNameNS(
					Constants.SignatureSpecNS,
					SAMLConstants.TAG_DSAKEYVALUE).item(0);
	if (dsaKey != null) { // It's DSAKey
	    NodeList nodes = dsaKey.getChildNodes();
	    int nodeCount = nodes.getLength();
	    if (nodeCount > 0) {
		BigInteger p=null, q=null, g=null, y=null;
	        for (int i = 0; i < nodeCount; i++) {
		    Node currentNode = nodes.item(i);
		    if (currentNode.getNodeType() == Node.ELEMENT_NODE) {	
		        String tagName = currentNode.getLocalName();
			Node sub = currentNode.getChildNodes().item(0);
			String value = sub.getNodeValue();
			BigInteger v = new BigInteger(
                        Base64.decode(
                            SAMLUtilsCommon.removeNewLineChars(value)));
		        if (tagName.equals("P")) {
			    p = v;
		        } else if (tagName.equals("Q")) {
			    q = v;
		        } else if (tagName.equals("G")) {
			    g = v;
		        } else if (tagName.equals("Y")) {
			    y = v;
		        } else {
			    throw new XMLSignatureException(
				SAMLUtilsCommon.bundle.getString(
                                "errorObtainPK"));
		        }
		    }
		}
		DSAKeyValue dsaKeyValue = new DSAKeyValue(doc, p, q, g, y);
		try {
		    pubKey = dsaKeyValue.getPublicKey();
		} catch (Exception e) {
		    throw new XMLSignatureException(
			SAMLUtilsCommon.bundle.getString("errorObtainPK"));
		}
	    }
	} else {
	    Element rsaKey =
		(Element) reference.getElementsByTagNameNS(
				Constants.SignatureSpecNS,
				SAMLConstants.TAG_RSAKEYVALUE).item(0);
	    if (rsaKey != null) { // It's RSAKey
		NodeList nodes = rsaKey.getChildNodes();
		int nodeCount = nodes.getLength();
		BigInteger m=null, e=null;
		if (nodeCount > 0) {
		    for (int i = 0; i < nodeCount; i++) {
			Node currentNode = nodes.item(i);
			if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
			    String tagName = currentNode.getLocalName();
			    Node sub = currentNode.getChildNodes().item(0);
			    String value = sub.getNodeValue();
			    BigInteger v = new BigInteger(
                            Base64.decode(
                                SAMLUtilsCommon.removeNewLineChars(value)));
			    if (tagName.equals("Exponent")) {
				e = v;
			    }
			    else if (tagName.equals("Modulus")){
				m = v;
			    } else {
				throw new XMLSignatureException(
				SAMLUtilsCommon.bundle.getString(
                                    "errorObtainPK"));
			    }
			}
		    }
		}
		RSAKeyValue rsaKeyValue =
		    new RSAKeyValue(doc,m, e);
		try {
		    pubKey = rsaKeyValue.getPublicKey();
		} catch (Exception ex) {
		    throw new XMLSignatureException(
			SAMLUtilsCommon.bundle.getString("errorObtainPK"));
		}
	    }
	}
	return pubKey;
    }

    /**
     * Get the X509Certificate from encoded cert string
     * @param certString BASE64 or PKCS7 encoded certtificate string
     * @param format encoded format
     * @return a X509Certificate
     */
    protected X509Certificate getCertificate(String certString,
                                           String format)
    {
        X509Certificate cert = null;

        try {

            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("getCertificate(Assertion) : " +
                        certString);
            }

            StringBuffer xml = new StringBuffer(100);
            xml.append(SAMLConstants.BEGIN_CERT);
            xml.append(certString);
            xml.append(SAMLConstants.END_CERT);

            byte[] barr = null;
            barr = (xml.toString()).getBytes();

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream bais = new ByteArrayInputStream(barr);

            if ((format !=null) &&
                format.equals(SAMLConstants.TAG_PKCS7)) { // PKCS7 format
                Collection c = cf.generateCertificates(bais);
                Iterator i = c.iterator();
                while (i.hasNext()) {
                    cert = (java.security.cert.X509Certificate) i.next();
                }
            } else { //X509:v3 format
                while (bais.available() > 0) {
                    cert = (java.security.cert.X509Certificate)
                                cf.generateCertificate(bais);
                }
            }
        } catch (Exception e) {
            SAMLUtilsCommon.debug.error("getCertificate Exception: ", e);
        }

        return cert;
    }
    
    /**
     * Returns the public key from the certificate embedded in the KeyInfo.
     *
     * @param cert X509 Certificate
     * @return a public key from the certificate embedded in the KeyInfo.
     */
    protected PublicKey getPublicKey(X509Certificate cert) {
        PublicKey pk = null;
        if (cert != null) {
            pk = cert.getPublicKey();
        }
        return pk;
    }
    
    protected boolean isValidAlgorithm(String algorithm) {
        if (algorithm.equals(SAMLConstants.ALGO_ID_MAC_HMAC_SHA1) ||
            algorithm.equals(SAMLConstants.ALGO_ID_SIGNATURE_DSA) ||
            algorithm.equals(SAMLConstants.ALGO_ID_SIGNATURE_RSA) ||
            algorithm.equals(SAMLConstants.ALGO_ID_SIGNATURE_RSA_SHA1) ||
            algorithm.equals(SAMLConstants.
                             ALGO_ID_SIGNATURE_NOT_RECOMMENDED_RSA_MD5) ||
            algorithm.equals(SAMLConstants.ALGO_ID_SIGNATURE_RSA_RIPEMD160) ||
            algorithm.equals(SAMLConstants.ALGO_ID_SIGNATURE_RSA_SHA256) ||
            algorithm.equals(SAMLConstants.ALGO_ID_SIGNATURE_RSA_SHA384) ||
            algorithm.equals(SAMLConstants.ALGO_ID_SIGNATURE_RSA_SHA512) ||
            algorithm.equals(SAMLConstants.ALGO_ID_MAC_HMAC_NOT_RECOMMENDED_MD5) ||
            algorithm.equals(SAMLConstants.ALGO_ID_MAC_HMAC_RIPEMD160) ||
            algorithm.equals(SAMLConstants.ALGO_ID_MAC_HMAC_SHA256) ||
            algorithm.equals(SAMLConstants.ALGO_ID_MAC_HMAC_SHA384) ||
            algorithm.equals(SAMLConstants.ALGO_ID_MAC_HMAC_SHA512)) { 
            return true;
        } else {
            return false; 
        }
    }
    
    private boolean isValidCanonicalizationMethod(String algorithm) { 
     	if (algorithm.equals(SAMLConstants.ALGO_ID_C14N_OMIT_COMMENTS) ||
   	    algorithm.equals(SAMLConstants.ALGO_ID_C14N_WITH_COMMENTS) ||
   	    algorithm.equals(SAMLConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS) ||
    	    algorithm.equals(SAMLConstants.ALGO_ID_C14N_EXCL_WITH_COMMENTS)) {
    	    return true; 
    	} else { 
    	    return false;
    	}
    }	        

    private boolean isValidTransformAlgorithm(String algorithm) { 
    	if (algorithm.equals(SAMLConstants.TRANSFORM_C14N_OMIT_COMMENTS) ||
    	    algorithm.equals(SAMLConstants.TRANSFORM_C14N_WITH_COMMENTS) ||
    	    algorithm.equals(SAMLConstants.TRANSFORM_C14N_EXCL_OMIT_COMMENTS) ||
            algorithm.equals(SAMLConstants.TRANSFORM_C14N_EXCL_WITH_COMMENTS) ||
            algorithm.equals(SAMLConstants.TRANSFORM_XSLT) ||
            algorithm.equals(SAMLConstants.TRANSFORM_BASE64_DECODE) ||
            algorithm.equals(SAMLConstants.TRANSFORM_XPATH ) ||
            algorithm.equals(SAMLConstants.TRANSFORM_ENVELOPED_SIGNATURE) ||
            algorithm.equals(SAMLConstants.TRANSFORM_XPOINTER) ||
            algorithm.equals(SAMLConstants.TRANSFORM_XPATH2FILTER04) ||
            algorithm.equals(SAMLConstants.TRANSFORM_XPATH2FILTER) ||
            algorithm.equals(SAMLConstants.TRANSFORM_XPATHFILTERCHGP)) {
            return true; 
        } else { 
            return false; 
        }    
    }	       
    
    private String getKeyAlgorithm(PrivateKey pk) {
        if (defaultSigAlg != null && !defaultSigAlg.equals("")) {
            return defaultSigAlg;
        }    
        if (pk.getAlgorithm().equalsIgnoreCase("DSA")) {
            return SAMLConstants.ALGO_ID_SIGNATURE_DSA; 
        } 
        return SAMLConstants.ALGO_ID_SIGNATURE_RSA_SHA1;
    }
    
    /**
     * Sign part of the xml document referered by the supplied a list
     * of id attributes of nodes
     * @param doc XML dom object
     * @param cert Signer's certificate
     * @param assertionID assertion ID
     * @param algorithm XML signature algorithm
     * @param ids list of id attribute values of nodes to be signed
     * @return SAML Security Token  signature
     * @throws XMLSignatureException if the document could not be signed
     */
    public org.w3c.dom.Element signWithSAMLToken(
                                   org.w3c.dom.Document doc,
                                   java.security.cert.Certificate cert,
                                   String assertionID,
                                   java.lang.String algorithm,
                                   java.util.List ids)
        throws XMLSignatureException {
        return null;
    }
    
    /**
     * Sign part of the XML document referred by the supplied a list
     * of id attributes of nodes using SAML Token.
     * @param doc XML dom object
     * @param key the key that will be used to sign the document.
     * @param symmetricKey true if the supplied key is a symmetric key type.     
     * @param sigingCert signer's Certificate. If present, this certificate
     *        will be added as part of signature <code>KeyInfo</code>.
     * @param encryptCert the certificate if present will be used to encrypt
     *        the symmetric key and replay it as part of <code>KeyInfo</code>
     * @param assertionID assertion ID for the SAML Security Token
     * @param algorithm XML signature algorithm
     * @param ids list of id attribute values of nodes to be signed
     * @return SAML Security Token  signature
     * @throws XMLSignatureException if the document could not be signed
     */
    public org.w3c.dom.Element signWithSAMLToken(
        org.w3c.dom.Document doc,
        java.security.Key key,
        boolean symmetricKey,
        java.security.cert.Certificate sigingCert,
        java.security.cert.Certificate encryptCert,
        java.lang.String assertionID,
        java.lang.String algorithm,       
        java.util.List ids)
        throws XMLSignatureException {
        
        return null;
    }
    
    public org.w3c.dom.Element signWithKerberosToken(
            org.w3c.dom.Document doc,
            java.security.Key key,
            java.lang.String algorithm,
            java.util.List ids)
            throws XMLSignatureException {
        return null;
    }

    /**
     * Sign part of the xml document referered by the supplied a list
     * of id attributes  of nodes
     * @param doc XML dom object
     * @param cert Signer's certificate
     * @param algorithm XML signature algorithm
     * @param ids list of id attribute values of nodes to be signed
     * @return X509 Security Token  signature
     * @throws XMLSignatureException if the document could not be signed
     */
    public org.w3c.dom.Element signWithUserNameToken(
                                   org.w3c.dom.Document doc,
                                   java.security.cert.Certificate cert,
                                   java.lang.String algorithm,
                                   java.util.List ids)
        throws XMLSignatureException {
        return null;
    }

    /**
     * Sign part of the xml document referered by the supplied a list
     * of id attributes  of nodes
     * @param doc XML dom object
     * @param cert Signer's certificate
     * @param algorithm XML signature algorithm
     * @param ids list of id attribute values of nodes to be signed
     * @param referenceType signed element reference type
     * @return X509 Security Token  signature
     * @throws XMLSignatureException if the document could not be signed
     */
    public org.w3c.dom.Element signWithBinarySecurityToken(
                                   org.w3c.dom.Document doc,
                                   java.security.cert.Certificate cert,
                                   java.lang.String algorithm,
                                   java.util.List ids,
                                   java.lang.String referenceType)
        throws XMLSignatureException {
        return null;
    }
    
    /**
     * Verify all the signatures of the XML document for the
     * web services security.
     * @param document XML dom document whose signature to be verified
     *
     * @param certAlias alias for Signer's certificate, this is used to search
     *        signer's public certificate if it is not presented in
     *        <code>ds:KeyInfo</code>.
     * @return true if the XML signature is verified, false otherwise
     * @throws XMLSignatureException if problem occurs during verification
     */
    public boolean verifyWSSSignature(org.w3c.dom.Document document,
                                       java.lang.String certAlias)
        throws XMLSignatureException {
        return false;
    }
    
    /**
     * Verify web services message signature using specified key
     * @param document the document to be validated
     * @param key the secret key to be used for validating signature
     * @return true if verification is successful.
     * @throws com.sun.identity.saml.xmlsig.XMLSignatureException
     */
    public boolean verifyWSSSignature(org.w3c.dom.Document document,
            java.security.Key key)
            throws XMLSignatureException {
        return false;
        
    }
    
   /**
     * Verify web services message signature using specified key
     * @param document the document to be validated
     * @param key the secret key to be used for validating signature
     * @param certAlias the certificate alias used for validating the signature
     *        if the key is not available.
     * @param encryptAlias the certificate alias that may be used to decrypt
     *        the symmetric key that may be part of <code>KeyInfo</code>
     * @return true if verification is successful.
     * @throws com.sun.identity.saml.xmlsig.XMLSignatureException
     */
    public boolean verifyWSSSignature(org.w3c.dom.Document document,
                         java.security.Key key,
                         String certAlias,
                         String encryptAlias)
        throws XMLSignatureException {
        return false;
    }
    
    
    /**
     * Return algorithm URI for the given algorithm.
     */
    protected String getAlgorithmURI(String algorithm) {
        if(algorithm == null) {
           return null;
        }
        if(algorithm.equals("RSA")) {
           return SAMLConstants.ALGO_ID_SIGNATURE_RSA;
        } else if(algorithm.equals("DSA")) {
           return SAMLConstants.ALGO_ID_SIGNATURE_DSA;
        } else {
          return null;
        }
    }
}
