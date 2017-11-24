/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: WSSSignatureProvider.java,v 1.13 2009/11/16 21:53:00 mallas Exp $
 *
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.wss.xmlsig;

import org.apache.xml.security.utils.ElementProxy;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.encryption.EncryptedKey;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.transforms.Transform;
import org.apache.xml.security.utils.Constants;
import org.apache.xpath.XPathAPI;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.xmlsig.AMSignatureProvider;
import com.sun.identity.saml.xmlsig.XMLSignatureException;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.wss.security.WSSConstants;
import com.sun.identity.wss.security.WSSUtils;
import com.sun.identity.wss.security.STRTransform;
import com.sun.identity.wss.security.BinarySecurityToken;
import com.iplanet.security.x509.CertUtils;
import javax.xml.transform.TransformerException;

/**
 * <code>WSSSignatureProvider</code> is a class for signing and 
 * signature verification of WSS XML Documents which implements 
 * <code>AMSignatureProvider</code>.
 */ 
public class WSSSignatureProvider extends AMSignatureProvider {
    
    private static final String USE_STR_TRANSFORMATION = 
            "com.sun.identity.wss.signature.usestrtransformation";
    private boolean isSTRTransformRegistered = false;
    private boolean useSTRTransformation = true;
    
    /** Creates a new instance of WSSSignatureProvider */
    public WSSSignatureProvider() {
        super();
        useSTRTransformation = Boolean.valueOf(
                SystemConfigurationUtil.getProperty(
                USE_STR_TRANSFORMATION, "true")).booleanValue();
    }
    
    private synchronized void registerSTRTransform() {
        try {
            Transform.register(STRTransform.STR_TRANSFORM_URI,
                               STRTransform.class.getName());
            isSTRTransformRegistered = true;
        } catch (Exception e) {
            if(WSSUtils.debug.messageEnabled()) {
               WSSUtils.debug.message("WSSSignatureProvider.constructor: STR"+
                    " Transform is already registered");
            }
        }        
    }
    
    /**
     * Sign part of the xml document referered by the supplied a list
     * of id attributes of nodes
     * @param doc XML dom object
     * @param cert Signer's certificate
     * @param assertionID assertion ID
     * @param algorithm XML signature algorithm
     * @param ids list of id attribute values of nodes to be signed
     * @return SAML Security Token signature
     * @throws XMLSignatureException if the document could not be signed
     */
    public org.w3c.dom.Element signWithSAMLToken(
                               org.w3c.dom.Document doc,
                               java.security.cert.Certificate cert,
                               String assertionID,
                               java.lang.String algorithm,
                               java.util.List ids) throws XMLSignatureException{
        String certAlias = keystore.getCertificateAlias(cert);
        PrivateKey privateKey = keystore.getPrivateKey(certAlias);
        return signWithSAMLToken(doc, privateKey, false, cert, null,
               assertionID, algorithm, ids);        
    }
                         
    /**
     * Sign part of the XML document referred by the supplied a list
     * of id attributes of nodes using SAML Token.
     * @param doc XML dom object
     * @param key the key that will be used to sign the document.
     * @param symmetricKey true if the supplied key is a symmetric key type.     
     * @param signingCert signer's Certificate. If present, this certificate
     *        will be added as part of signature <code>KeyInfo</code>.
     * @param encCert the certificate if present will be used to encrypt
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
                                   java.security.cert.Certificate signingCert,
                                   java.security.cert.Certificate encCert,                                   
                                   String assertionID,
                                   java.lang.String algorithm,
                                   java.util.List ids)
        throws XMLSignatureException {
        
        if(useSTRTransformation && !isSTRTransformRegistered) {
           registerSTRTransform();
        }
        
        if (doc == null) {
            WSSUtils.debug.error("WSSSignatureProvider.signWithSAMLToken: " +
                     "document is null.");
            throw new XMLSignatureException(
                      WSSUtils.bundle.getString("nullInput"));
        }
        
        boolean isSAML2Token = false;
        Element assertionElement = (Element) doc.getDocumentElement().
                getElementsByTagNameNS(SAML2Constants.ASSERTION_NAMESPACE_URI,
                         "Assertion").item(0);
        if(assertionElement != null) {
           isSAML2Token = true; 
        }
                
        if (assertionID == null) {
            WSSUtils.debug.error("WSSSignatureProvider.signWithSAMLToken: " +
                                        "Certificate is null");
            throw new XMLSignatureException(
                      WSSUtils.bundle.getString("nullInput"));
        }
        Element root = (Element) doc.getDocumentElement().
                getElementsByTagNameNS(WSSConstants.WSSE_NS,
                         WSSConstants.WSSE_SECURITY_LNAME).item(0);

        org.w3c.dom.Element timeStamp = (Element) doc.getDocumentElement().
                getElementsByTagNameNS(WSSConstants.WSU_NS,
                         WSSConstants.TIME_STAMP).item(0);
        XMLSignature signature = null;
        try {
            ElementProxy.setDefaultPrefix(Constants.SignatureSpecNS, SAMLConstants.PREFIX_DS);
            if(symmetricKey) {
               algorithm = SAMLConstants.ALGO_ID_MAC_HMAC_SHA1;               
            } else {                                                    
               if (algorithm == null || algorithm.length() == 0) {
                  algorithm = getPublicKey(
                          (X509Certificate)signingCert).getAlgorithm();                                
                  algorithm = getAlgorithmURI(algorithm);
              }
            
              if (!isValidAlgorithm(algorithm)) {
                  throw new XMLSignatureException(
                           WSSUtils.bundle.getString("invalidalgorithm"));
              }
            }
            Element wsucontext = org.apache.xml.security.utils.
                    XMLUtils.createDSctx(doc, "wsu", WSSConstants.WSU_NS);

            NodeList wsuNodes = (NodeList)XPathAPI.selectNodeList(doc,
                    "//*[@wsu:Id]", wsucontext);

            if (wsuNodes != null && wsuNodes.getLength() != 0) {
                for (int i = 0; i < wsuNodes.getLength(); i++) {
                     Element elem = (Element) wsuNodes.item(i);
                     String id = elem.getAttributeNS(WSSConstants.WSU_NS, "Id");
                     if (id != null && id.length() != 0) {
                         elem.setIdAttribute(id, true);
                     }
                }
            }

            signature = new XMLSignature(doc, null, algorithm,
                  Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
            Element sigEl = signature.getElement();
            doc.importNode(sigEl, true);
            sigEl.setPrefix("ds");
            root.insertBefore(sigEl, timeStamp);
            Node textNode = doc.createTextNode("\n");
            root.insertBefore(textNode, sigEl);

            Element transformParams = doc.createElementNS(WSSConstants.WSSE_NS,
                    WSSConstants.WSSE_TAG + ":" +
                    WSSConstants.TRANSFORMATION_PARAMETERS);
            transformParams.setAttributeNS(SAMLConstants.NS_XMLNS,
                    WSSConstants.TAG_XML_WSSE, WSSConstants.WSSE_NS);
            Element canonElem =  doc.createElementNS(
                    SAMLConstants.XMLSIG_NAMESPACE_URI,
                    "ds:CanonicalizationMethod");
            canonElem.setAttributeNS(null, "Algorithm",
                    Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
            transformParams.appendChild(canonElem);
                        Element securityTokenRef =
                doc.createElementNS(WSSConstants.WSSE_NS,
                        SAMLConstants.TAG_SECURITYTOKENREFERENCE);
            securityTokenRef.setPrefix(WSSConstants.WSSE_TAG);
            securityTokenRef.setAttributeNS(
                    WSSConstants.NS_XML,
                    WSSConstants.TAG_XML_WSU,
                    WSSConstants.WSU_NS);
            securityTokenRef.setAttributeNS(
                    WSSConstants.NS_XML,
                    WSSConstants.TAG_XML_WSSE11,
                    WSSConstants.WSSE11_NS);
            
            String secRefId = SAMLUtils.generateID();
            securityTokenRef.setAttributeNS(WSSConstants.WSU_NS,
                    WSSConstants.WSU_ID, secRefId);

            if(isSAML2Token) {
               securityTokenRef.setAttributeNS(WSSConstants.WSSE11_NS,
                    WSSConstants.TOKEN_TYPE, WSSConstants.SAML2_TOKEN_TYPE);
            } else {
               securityTokenRef.setAttributeNS(WSSConstants.WSSE11_NS,
                    WSSConstants.TOKEN_TYPE, WSSConstants.SAML11_TOKEN_TYPE); 
            }

            KeyInfo keyInfo = signature.getKeyInfo();
            keyInfo.addUnknownElement(securityTokenRef);
            
            if(symmetricKey && (encCert != null)) {
               EncryptedKey encKey =  
                    WSSUtils.encryptKey(doc, key.getEncoded(), 
                    (X509Certificate)encCert,null);
               keyInfo.add(encKey);                
            } else {               
               signature.addKeyInfo((X509Certificate)signingCert);                              
            }
            
            securityTokenRef.setIdAttribute(secRefId, true);
            int size = ids.size();
            for (int i = 0; i < size; ++i) {
                Transforms transforms = new Transforms(doc);
                transforms.addTransform(
                                Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);
                String id = (String) ids.get(i);
                if (WSSUtils.debug.messageEnabled()) {
                    WSSUtils.debug.message("id = " +id);
                }
                signature.addDocument("#"+id, transforms,
                        Constants.ALGO_ID_DIGEST_SHA1);
            }

            if(useSTRTransformation) {
               Transforms strtransform = new Transforms(doc);
               strtransform.addTransform(STRTransform.STR_TRANSFORM_URI,
                            transformParams);
               signature.addDocument("#"+secRefId, strtransform,
                            Constants.ALGO_ID_DIGEST_SHA1);
            }

            Element keyIdentifier = doc.createElementNS(
                        WSSConstants.WSSE_NS,
                        WSSConstants.TAG_KEYIDENTIFIER);
            keyIdentifier.setPrefix(WSSConstants.WSSE_TAG);
            keyIdentifier.setAttribute(WSSConstants.WSU_ID,
                        SAMLUtils.generateID());
            Text value = doc.createTextNode(assertionID);
            keyIdentifier.appendChild(value);
            if(isSAML2Token) {
               keyIdentifier.setAttributeNS(null, SAMLConstants.TAG_VALUETYPE,
                        WSSConstants.SAML2_ASSERTION_VALUE_TYPE);
            } else {
                keyIdentifier.setAttributeNS(null, SAMLConstants.TAG_VALUETYPE,
                        WSSConstants.SAML_VALUETYPE);
            }
                                          
            securityTokenRef.appendChild(keyIdentifier);
            signature.sign(key);
        } catch (Exception e) {
            WSSUtils.debug.error("WSSSignatureProvider.signWithSAMLToken" +
                      " Exception: ", e);
            throw new XMLSignatureException(e.getMessage());
        }

        if (WSSUtils.debug.messageEnabled()) {
            WSSUtils.debug.message("WSSSignatureProvider.signWithSAMLToken" +
                 "Signed document"+ XMLUtils.print(doc.getDocumentElement()));
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
     * @return X509 Security Token signature
     * @throws XMLSignatureException if the document could not be signed
     */
    public org.w3c.dom.Element signWithUserNameToken(
                                   org.w3c.dom.Document doc,
                                   java.security.cert.Certificate cert,
                                   java.lang.String algorithm,
                                   java.util.List ids)
        throws XMLSignatureException {
        return signWithBinarySecurityToken(doc, cert, algorithm, ids,
                                          WSSConstants.TAG_USERNAME_TOKEN, null);
    }

    /**
     * Sign part of the xml document referered by the supplied a list
     * of id attributes  of nodes
     * @param doc XML dom object
     * @param cert Signer's certificate
     * @param algorithm XML signature algorithm
     * @param ids list of id attribute values of nodes to be signed
     * @return X509 Security Token signature
     * @throws XMLSignatureException if the document could not be signed
     */
    public org.w3c.dom.Element signWithBinarySecurityToken(
                                   org.w3c.dom.Document doc,
                                   java.security.cert.Certificate cert,
                                   java.lang.String algorithm,
                                   java.util.List ids, String referenceType)
        throws XMLSignatureException {
        return signWithBinarySecurityToken(doc, cert, algorithm, ids,
                              SAMLConstants.BINARYSECURITYTOKEN, referenceType);
    }

    /**
     * Sign part of the xml document referered by the supplied a list
     * of id attributes  of nodes
     * @param doc XML dom object
     * @param cert Signer's certificate
     * @param algorithm XML signature algorithm
     * @param ids list of id attribute values of nodes to be signed
     * @param tokenType Token type
     * @return X509 Security Token signature
     * @throws XMLSignatureException if the document could not be signed
     */
    private org.w3c.dom.Element signWithBinarySecurityToken(
                                   org.w3c.dom.Document doc,
                                   java.security.cert.Certificate cert,
                                   java.lang.String algorithm,
                                   java.util.List ids,
                                   String tokenType,
                                   String referenceType)
        throws XMLSignatureException {

        if(useSTRTransformation && !isSTRTransformRegistered) {
           registerSTRTransform();
        }
        
        if (doc == null) {
            SAMLUtils.debug.error("WSSSignatureProvider.signWithBinarySecurity" +
            "Token:: XML doc is null.");
            throw new XMLSignatureException(
                      SAMLUtils.bundle.getString("nullInput"));
        }

        if (SAMLUtils.debug.messageEnabled()) {
            SAMLUtils.debug.message("WSSSignatureProvider.signWithWSSToken: " +
               "Document to be signed : " +
                XMLUtils.print(doc.getDocumentElement()));
        }

        org.w3c.dom.Element root = (Element) doc.getDocumentElement().
                getElementsByTagNameNS(WSSConstants.WSSE_NS,
                                        SAMLConstants.TAG_SECURITY).item(0);

        XMLSignature signature = null;
        try {

            ElementProxy.setDefaultPrefix(Constants.SignatureSpecNS, SAMLConstants.PREFIX_DS);
            String certAlias = keystore.getCertificateAlias(cert);
            PrivateKey privateKey =
                                (PrivateKey) keystore.getPrivateKey(certAlias);
            if (privateKey == null) {
                SAMLUtils.debug.error("WSSSignatureProvider.signWithWSSToken:" +
                   " private key is null");
                throw new XMLSignatureException(
                          SAMLUtils.bundle.getString("nullprivatekey"));
            }

            if (algorithm == null || algorithm.length() == 0) {
                algorithm = getPublicKey((X509Certificate)cert).getAlgorithm();
                algorithm = getAlgorithmURI(algorithm);
            }

            if (!isValidAlgorithm(algorithm)) {
                throw new XMLSignatureException(
                           SAMLUtils.bundle.getString("invalidalgorithm"));
            }

            Element wsucontext = org.apache.xml.security.utils.
                XMLUtils.createDSctx(doc, "wsu", WSSConstants.WSU_NS);

            NodeList wsuNodes = (NodeList)XPathAPI.selectNodeList(doc,
                    "//*[@wsu:Id]", wsucontext);

            if (wsuNodes != null && wsuNodes.getLength() != 0) {
                for (int i = 0; i < wsuNodes.getLength(); i++) {
                     Element elem = (Element) wsuNodes.item(i);
                     String id = elem.getAttributeNS(WSSConstants.WSU_NS, "Id");                     
                     if (id != null && id.length() != 0) {
                         elem.setIdAttribute(id, true);
                     }
                }
            }

            signature = new XMLSignature(doc, "", algorithm,
                  Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

            Element sigEl = signature.getElement();
            root.appendChild(sigEl);

            Element transformParams = doc.createElementNS(WSSConstants.WSSE_NS,
                    WSSConstants.WSSE_TAG + ":" + 
                    WSSConstants.TRANSFORMATION_PARAMETERS);
            transformParams.setAttributeNS(SAMLConstants.NS_XMLNS,
                    WSSConstants.TAG_XML_WSSE, WSSConstants.WSSE_NS);
            Element canonElem =  doc.createElementNS(
                    SAMLConstants.XMLSIG_NAMESPACE_URI, 
                    "ds:CanonicalizationMethod");
            canonElem.setAttributeNS(null, "Algorithm",  
                    Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
            transformParams.appendChild(canonElem);

            Element securityTokenRef = null;
            String secRefId = null;
            
            securityTokenRef = doc.createElementNS(WSSConstants.WSSE_NS,
                "wsse:" + SAMLConstants.TAG_SECURITYTOKENREFERENCE);
            securityTokenRef.setAttributeNS(SAMLConstants.NS_XMLNS,
                WSSConstants.TAG_XML_WSSE, WSSConstants.WSSE_NS);
            securityTokenRef.setAttributeNS(SAMLConstants.NS_XMLNS,
                WSSConstants.TAG_XML_WSU, WSSConstants.WSU_NS);
            secRefId = SAMLUtils.generateID();
            securityTokenRef.setAttributeNS(WSSConstants.WSU_NS, 
                WSSConstants.WSU_ID, secRefId);
            KeyInfo keyInfo = signature.getKeyInfo();
            keyInfo.addUnknownElement(securityTokenRef);
            securityTokenRef.setIdAttribute(secRefId, true);

            int size = ids.size();
            for (int i = 0; i < size; ++i) {
                Transforms transforms = new Transforms(doc);
                transforms.addTransform(
                                Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);
                String id = (String) ids.get(i);
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("id = " +id);
                }
                signature.addDocument("#"+id, transforms,
                        Constants.ALGO_ID_DIGEST_SHA1);
            }
            
            Element tokenE = (Element)root.getElementsByTagNameNS(
                      WSSConstants.WSSE_NS,tokenType).item(0);
            if (tokenE != null && useSTRTransformation) {
                Transforms strtransforms = new Transforms(doc);
                strtransforms.addTransform(
                               STRTransform.STR_TRANSFORM_URI, transformParams);
                signature.addDocument("#"+secRefId, strtransforms,
                        Constants.ALGO_ID_DIGEST_SHA1);
            }

            if(referenceType == null || referenceType.equals(
                    WSSConstants.DIRECT_REFERENCE)) {
               Element reference = doc.createElementNS(WSSConstants.WSSE_NS,
                        SAMLConstants.TAG_REFERENCE);
               reference.setPrefix(WSSConstants.WSSE_TAG);
               securityTokenRef.appendChild(reference);                            
               Element bsf = (Element)root.getElementsByTagNameNS(
                      WSSConstants.WSSE_NS,tokenType).item(0);
               if (bsf != null) {        
                   String certId = bsf.getAttributeNS(WSSConstants.WSU_NS,
                                   SAMLConstants.TAG_ID);                
                   reference.setAttributeNS(null, SAMLConstants.TAG_URI,"#"
                                         +certId);                                     
               }
               if (SAMLConstants.BINARYSECURITYTOKEN.equals(tokenType)) {
                   reference.setAttributeNS(null, WSSConstants.TAG_VALUETYPE, 
                               WSSConstants.WSSE_X509_NS + "#X509v3");
               } else if (WSSConstants.TAG_USERNAME_TOKEN.equals(tokenType)) {
                   reference.setAttributeNS(null, WSSConstants.TAG_VALUETYPE, 
                             WSSConstants.TAG_USERNAME_VALUE_TYPE);
                   signature.addKeyInfo((X509Certificate)cert);
               }
            } else if(
                  WSSConstants.KEYIDENTIFIER_REFERENCE.equals(referenceType)) {
               Element keyIdentifier = createKeyIdentifierReference(doc, cert);
               if(keyIdentifier == null) {
                  throw new XMLSignatureException(
                          WSSUtils.bundle.getString("noSubjectKeyIdentifier"));
               }
               securityTokenRef.appendChild(
                       createKeyIdentifierReference(doc, cert));
               
            } else if (WSSConstants.X509DATA_REFERENCE.equals(referenceType)) {
               securityTokenRef.appendChild(
                       createX509DataReference(doc, cert));
            }
            signature.sign(privateKey);

        } catch (Exception e) {
            SAMLUtils.debug.error("WSSSignatureProvider: " + 
                                  "signWithBinaryTokenProfile Exception: ", e);
            throw new XMLSignatureException(e.getMessage());
        }

        return (signature.getElement());
    }


    /**
     * Verify all the signatures of the WSS xml document
     * @param doc XML dom document whose signature to be verified
     * @param certAlias certAlias alias for Signer's certificate, this is used
                        to search signer's public certificate if it is not
                        presented in ds:KeyInfo
     * @return true if the xml signature is verified, false otherwise
     * @throws XMLSignatureException if problem occurs during verification
     */
    public boolean verifyWSSSignature(org.w3c.dom.Document doc,
                                       java.lang.String certAlias)
        throws XMLSignatureException {
        
        if(useSTRTransformation && !isSTRTransformRegistered) {
           registerSTRTransform();
        }

        if (doc == null) {
            WSSUtils.debug.error("WSSSignatureProvider.verifyWSSSignature: " +
                    "document is null.");
            throw new XMLSignatureException(
                      WSSUtils.bundle.getString("nullInput"));
        }
        
        try {
            Element wsucontext = org.apache.xml.security.utils.
                    XMLUtils.createDSctx(doc, "wsu", WSSConstants.WSU_NS);

            NodeList wsuNodes = (NodeList)XPathAPI.selectNodeList(doc,
                    "//*[@wsu:Id]", wsucontext);

            if(wsuNodes != null && wsuNodes.getLength() != 0) {
               for(int i=0; i < wsuNodes.getLength(); i++) {
                   Element elem = (Element) wsuNodes.item(i);
                   String id = elem.getAttributeNS(WSSConstants.WSU_NS, "Id");
                   if (id != null && id.length() != 0) {
                       elem.setIdAttribute(id, true);
                   }
               }
            }
            NodeList aList = (NodeList)XPathAPI.selectNodeList(
                     doc, "//*[@" + "AssertionID" + "]");
            if (aList != null && aList.getLength() != 0) {
                int len = aList.getLength();
                for (int i = 0; i < len; i++) {
                     Element elem = (Element) aList.item(i);
                     String id = elem.getAttribute("AssertionID");
                     if (id != null && id.length() != 0) {
                         elem.setIdAttribute(id, true);
                     }
                }
            }

            Element nscontext = org.apache.xml.security.utils.
                  XMLUtils.createDSctx (doc,"ds",Constants.SignatureSpecNS);
            NodeList sigElements = XPathAPI.selectNodeList (doc,
                "//ds:Signature", nscontext);
            int sigElementsLength = sigElements.getLength();
            if (WSSUtils.debug.messageEnabled()) {
                WSSUtils.debug.message("WSSSignatureProvider.verifyWSSSignature"
                      + ": sigElements " + "size = " + sigElements.getLength());
            }
            if(sigElementsLength == 0) {
               return false;
            }

            X509Certificate newcert= keystore.getX509Certificate (certAlias);
            PublicKey key = keystore.getPublicKey (certAlias);
            Element sigElement = null;
            //loop
            for(int i = 0; i < sigElements.getLength(); i++) {
                sigElement = (Element)sigElements.item(i);
                if (WSSUtils.debug.messageEnabled ()) {
                    WSSUtils.debug.message("Sig(" + i + ") = " +
                        XMLUtils.print(sigElement));
                }
                Element refElement;
                try {
                    refElement = (Element) XPathAPI.selectSingleNode(sigElement, "//ds:Reference[1]", nscontext);
                } catch (TransformerException te) {
                    throw new XMLSignatureException(te);
                }
                String refUri = refElement.getAttribute("URI");
                String signedId = ((Element) sigElement.getParentNode()).getAttribute("AssertionID");
                //NB: this validation only works with enveloped XML signatures, enveloping and detached signatures are
                //no longer supported.
                if (refUri == null || signedId == null || !refUri.substring(1).equals(signedId)) {
                    WSSUtils.debug.error("Signature reference ID does not match with element ID");
                    throw new XMLSignatureException(WSSUtils.bundle.getString("uriNoMatchWithId"));
                }
                XMLSignature signature = new XMLSignature (sigElement, "");
                signature.addResourceResolver (
                    new com.sun.identity.saml.xmlsig.OfflineResolver ());
                KeyInfo ki = signature.getKeyInfo ();
                EncryptedKey encKey = ki.itemEncryptedKey(0);
                if(encKey != null) {                   
                   Key verificationKey = WSSUtils.getXMLEncryptionManager().
                           decryptKey(ki.getElement(), certAlias);
                   if (signature.checkSignatureValue (verificationKey)) {
                       return true;
                   } else {
                       return false; 
                   }
                   
                }
                PublicKey pk = this.getX509PublicKey(doc, ki);
                if (pk!=null) {
                    if (signature.checkSignatureValue (pk)) {
                        if (WSSUtils.debug.messageEnabled ()) {
                            WSSUtils.debug.message ("verifyWSSSignature:" +
                                " Signature " + i + " verified");
                        }
                    } else {
                        if(WSSUtils.debug.messageEnabled()) {
                           WSSUtils.debug.message("verifyWSSSignature:" +
                           " Signature Verfication failed");
                        }
                        return false;
                    }
                } else {
                    if (certAlias == null || certAlias.equals ("")) {
                        if(WSSUtils.debug.messageEnabled()) {
                           WSSUtils.debug.message("verifyWSSSignature:" +
                           "Certificate Alias is null");
                        }
                        return false;
                    }
                    if (WSSUtils.debug.messageEnabled ()) {
                        WSSUtils.debug.message ("Could not find a KeyInfo, " +
                        "try to use certAlias");
                    }
                    if (newcert != null) {
                        if (signature.checkSignatureValue (newcert)) {
                            if (WSSUtils.debug.messageEnabled ()) {
                                WSSUtils.debug.message ("verifyWSSSignature:" +
                                        " Signature " + i + " verified");
                            }
                        } else {
                            return false;
                        }
                    } else {
                        if (key != null) {
                            if (signature.checkSignatureValue (key)) {
                                if (WSSUtils.debug.messageEnabled ()) {
                                    WSSUtils.debug.message (
                                    "verifyWSSSignature: Signature " + i +
                                    " verified");
                                }
                            } else {
                                return false;
                            }

                        } else {
                            WSSUtils.debug.error ("Could not find public key"
                                + " based on certAlias to verify signature");
                            return false;
                        }
                    }
                }
            }
            return true;
        } catch (Exception ex) {
            WSSUtils.debug.error("WSSSignatureProvider: " + 
                                 "verifyWSSSignature Exception: ", ex);
            throw new XMLSignatureException (ex.getMessage ());
        }
    }

    /**
     * Returns the public key from the security token.
     * This is required WS-Security.
     */
    private PublicKey getPublicKeyFromWSSToken(Document doc) {
        PublicKey pubKey = null;
        try {
            Element securityElement = (Element) doc.getDocumentElement().
                getElementsByTagNameNS(WSSConstants.WSSE_NS,
                         SAMLConstants.TAG_SECURITY).item(0);

            if(securityElement == null) {
               return null;
            }

            Element nscontext = org.apache.xml.security.utils.
                XMLUtils.createDSctx(doc,"ds",Constants.SignatureSpecNS);
            Element sigElement = (Element) XPathAPI.selectSingleNode(
                          securityElement, "ds:Signature[1]", nscontext);

            Element keyinfo = (Element) sigElement.getElementsByTagNameNS(
                Constants.SignatureSpecNS, SAMLConstants.TAG_KEYINFO).item(0);
            Element str = (Element) keyinfo.getElementsByTagNameNS(
                          WSSConstants.WSSE_NS,
                          SAMLConstants.TAG_SECURITYTOKENREFERENCE).item(0);

            Element reference = (Element) keyinfo.getElementsByTagNameNS(
                   WSSConstants.WSSE_NS, SAMLConstants.TAG_REFERENCE).item(0);

            if (reference != null) {
                String id = reference.getAttribute(SAMLConstants.TAG_URI);
                id = id.substring(1);
                nscontext = org.apache.xml.security.utils.
                    XMLUtils.createDSctx(doc,SAMLConstants.PREFIX_WSU,
                                         WSSConstants.WSU_NS);
                Node n = XPathAPI.selectSingleNode(
                    doc, "//*[@"+ SAMLConstants.PREFIX_WSU + ":" +
                    SAMLConstants.TAG_ID +"=\"" + id + "\"]", nscontext);

                if (n != null) { // X509 Security Token profile
                    SAMLUtils.debug.message("X509 Token");
                    String format = ((Element) n).getAttribute(
                                                SAMLConstants.TAG_VALUETYPE);
                    NodeList children = n.getChildNodes();
                    n = children.item(0);
                    String certString = n.getNodeValue().trim();

                    pubKey = getPublicKey(getCertificate(certString, format));

                } else { // SAML Token profile
                    SAMLUtils.debug.message("SAML Token");
                    reference = (Element) XPathAPI.selectSingleNode(
                            doc, "//*[@AssertionID=\"" + id + "\"]");
                    // The SAML Statements contain keyinfo, they should be
                    // all the same. get the first keyinfo!
                    reference = (Element) reference.getElementsByTagNameNS(
                                        Constants.SignatureSpecNS,
                                        SAMLConstants.TAG_KEYINFO).item(0);
                    if (reference == null) { // no cert found!
                        throw new Exception(
                            SAMLUtils.bundle.getString("nullKeyInfo"));
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
                        if (SAMLUtils.debug.messageEnabled()) {
                            SAMLUtils.debug.message("certString = " +
                                                                certString);
                        }

                        return getPublicKey(getCertificate(certString, null));
                    } else { // it should contains RSA/DSA key
                        pubKey = getPublicKeybyDSARSAkeyValue(doc, reference);
                    }
                }
            } else {
                SAMLUtils.debug.error("WSSSignatureProvider:" + 
                                      "getPublicKeyFromWSSToken:" + 
                                      " unknow Security Token Reference");
            }
        } catch (Exception e) {
            SAMLUtils.debug.error("WSSSignatureProvider:" + 
                                  "getPublicKeyFromWSSToken Exception: ", e);
        }
        return pubKey;
    }
    
    /**
     * Sign with Kerberos Token
     * @param doc
     * @param key
     * @param algorithm
     * @param ids
     * @return Kerberos Security Token signature
     * @throws com.sun.identity.saml.xmlsig.XMLSignatureException
     */
    public org.w3c.dom.Element signWithKerberosToken(
            org.w3c.dom.Document doc,
            java.security.Key key,
            java.lang.String algorithm,
            java.util.List ids)
            throws XMLSignatureException {
        
        if (doc == null) {
            SAMLUtils.debug.error("WSSSignatureProvider.signWithKerberos" +
            "Token:: XML doc is null.");
            throw new XMLSignatureException(
                      SAMLUtils.bundle.getString("nullInput"));
        }

        if (SAMLUtils.debug.messageEnabled()) {
            SAMLUtils.debug.message("WSSSignatureProvider.signWithKerberosToken:" +                   
               "Document to be signed : " +
                XMLUtils.print(doc.getDocumentElement()));
        }

        org.w3c.dom.Element root = (Element) doc.getDocumentElement().
                getElementsByTagNameNS(WSSConstants.WSSE_NS,
                                        SAMLConstants.TAG_SECURITY).item(0);

        XMLSignature signature = null;
        try {
            ElementProxy.setDefaultPrefix(Constants.SignatureSpecNS, SAMLConstants.PREFIX_DS);

            if (!isValidAlgorithm(algorithm)) {
                throw new XMLSignatureException(
                           SAMLUtils.bundle.getString("invalidalgorithm"));
            }

            Element wsucontext = org.apache.xml.security.utils.
                XMLUtils.createDSctx(doc, "wsu", WSSConstants.WSU_NS);

            NodeList wsuNodes = (NodeList)XPathAPI.selectNodeList(doc,
                    "//*[@wsu:Id]", wsucontext);

            if (wsuNodes != null && wsuNodes.getLength() != 0) {
                for (int i = 0; i < wsuNodes.getLength(); i++) {
                     Element elem = (Element) wsuNodes.item(i);
                     String id = elem.getAttributeNS(WSSConstants.WSU_NS, "Id");                     
                     if (id != null && id.length() != 0) {
                         elem.setIdAttribute(id, true);
                     }
                }
            }

            signature = new XMLSignature(doc, "", algorithm,
                  Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

            Element sigEl = signature.getElement();
            root.appendChild(sigEl);

            Element transformParams = doc.createElementNS(WSSConstants.WSSE_NS,
                    WSSConstants.WSSE_TAG + ":" + 
                    WSSConstants.TRANSFORMATION_PARAMETERS);
            transformParams.setAttributeNS(SAMLConstants.NS_XMLNS,
                    WSSConstants.TAG_XML_WSSE, WSSConstants.WSSE_NS);
            Element canonElem =  doc.createElementNS(
                    SAMLConstants.XMLSIG_NAMESPACE_URI, 
                    "ds:CanonicalizationMethod");
            canonElem.setAttributeNS(null, "Algorithm",  
                    Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
            transformParams.appendChild(canonElem);

            Element securityTokenRef = null;
            String secRefId = null;
            
            securityTokenRef = doc.createElementNS(WSSConstants.WSSE_NS,
                "wsse:" + SAMLConstants.TAG_SECURITYTOKENREFERENCE);
            securityTokenRef.setAttributeNS(SAMLConstants.NS_XMLNS,
                WSSConstants.TAG_XML_WSSE, WSSConstants.WSSE_NS);
            securityTokenRef.setAttributeNS(SAMLConstants.NS_XMLNS,
                WSSConstants.TAG_XML_WSU, WSSConstants.WSU_NS);
            secRefId = SAMLUtils.generateID();
            securityTokenRef.setAttributeNS(WSSConstants.WSU_NS, 
                WSSConstants.WSU_ID, secRefId);
            KeyInfo keyInfo = signature.getKeyInfo();
            keyInfo.addUnknownElement(securityTokenRef);
            securityTokenRef.setIdAttribute(secRefId, true);

            int size = ids.size();
            for (int i = 0; i < size; ++i) {
                Transforms transforms = new Transforms(doc);
                transforms.addTransform(
                                Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);
                String id = (String) ids.get(i);
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("id = " +id);
                }
                signature.addDocument("#"+id, transforms,
                        Constants.ALGO_ID_DIGEST_SHA1);
            }

            Element reference = doc.createElementNS(WSSConstants.WSSE_NS,
                        SAMLConstants.TAG_REFERENCE);
            securityTokenRef.appendChild(reference);                

            Element bsf = (Element)root.getElementsByTagNameNS(
                            WSSConstants.WSSE_NS,"BinarySecurityToken").item(0);
            String certId = bsf.getAttributeNS(WSSConstants.WSU_NS,
                    SAMLConstants.TAG_ID);                
            reference.setAttributeNS(null, SAMLConstants.TAG_URI,"#"
                                         +certId);                                                             
            reference.setAttributeNS(null, WSSConstants.TAG_VALUETYPE, 
                    WSSConstants.KERBEROS_VALUE_TYPE);            
            signature.sign(key);

        } catch (Exception e) {
            SAMLUtils.debug.error("WSSSignatureProvider: " + 
                                  "signWithBinaryTokenProfile Exception: ", e);
            throw new XMLSignatureException(e.getMessage());
        }
        return (signature.getElement());        
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
        return verifyWSSSignature(document, key, null, null);
    }
    
    /**
     * Verify web services message signature using specified key
     * @param doc the document to be validated
     * @param key the secret key to be used for validating signature
     * @param certAlias the certificate alias used for validating the signature
     *        if the key is not available.
     * @param encryptAlias the certificate alias that may be used to decrypt
     *        the symmetric key that is part of <code>KeyInfo</code>
     * @return true if verification is successful.
     * @throws com.sun.identity.saml.xmlsig.XMLSignatureException
     */
    public boolean verifyWSSSignature(Document doc, java.security.Key key,
          String certAlias, String encryptAlias) throws XMLSignatureException {
        throw new UnsupportedOperationException("Enveloping and detached XML signatures are no longer supported");
    }
    
    /**
     * Creates the key identifier reference using certificate.
     * @param doc
     * @param cert
     * @return
     */
    private Element createKeyIdentifierReference(Document doc, 
            java.security.cert.Certificate cert) {
        
        Element keyIdentifier = doc.createElementNS(
                                WSSConstants.WSSE_NS,
                               WSSConstants.TAG_KEYIDENTIFIER);
        keyIdentifier.setPrefix(WSSConstants.WSSE_TAG);
        keyIdentifier.setAttribute(WSSConstants.WSU_ID,
                        SAMLUtils.generateID());
        X509Certificate x509Cert = (X509Certificate)cert;
        byte[] data = x509Cert.getExtensionValue("2.5.29.14");
        if(data == null) {
           return null;
        }
        String certValue = Base64.encode(data);
        Text value = doc.createTextNode(certValue);
        keyIdentifier.appendChild(value);
        keyIdentifier.setAttributeNS(null, WSSConstants.TAG_VALUETYPE,
                      WSSConstants.KEY_IDENTIFIER_VALUE_TYPE);
        keyIdentifier.setAttributeNS(null, 
                      WSSConstants.TAG_ENCODING_TYPE,
                      BinarySecurityToken.BASE64BINARY);
        return keyIdentifier;
    }

    private Element createX509DataReference(Document doc, 
            java.security.cert.Certificate cert) {
        
        X509Certificate x509Cert = (X509Certificate)cert;
        Element x509Data = doc.createElementNS(
                                WSSConstants.XMLSIG_NAMESPACE_URI,
                               WSSConstants.TAG_X509DATA);
        x509Data.setPrefix("ds");
        Element issuerSerial = doc.createElementNS(
                                WSSConstants.XMLSIG_NAMESPACE_URI,
                               WSSConstants.TAG_X509_ISSUERSERIAL);
        Element issuerName = doc.createElementNS(
                             WSSConstants.XMLSIG_NAMESPACE_URI,
                             WSSConstants.TAG_X509_ISSUERNAME);
        Text value = doc.createTextNode(CertUtils.getIssuerName(x509Cert));
        issuerName.appendChild(value);
        Element serialNumber = doc.createElementNS(
                             WSSConstants.XMLSIG_NAMESPACE_URI,
                             WSSConstants.TAG_X509_SERIALNUMBER);
        value = doc.createTextNode(x509Cert.getSerialNumber().toString());
        serialNumber.appendChild(value);
        issuerSerial.appendChild(issuerName);
        issuerSerial.appendChild(serialNumber);
        x509Data.appendChild(issuerSerial);
        return x509Data;
    }
    
}

