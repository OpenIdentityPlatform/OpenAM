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
 * $Id: STRTransform.java,v 1.6 2008/07/21 17:18:32 mallas Exp $
 *
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.wss.security;

import java.io.IOException;
import java.security.cert.X509Certificate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.c14n.CanonicalizationException;
import org.apache.xml.security.c14n.InvalidCanonicalizerException;
import org.apache.xml.security.signature.XMLSignatureInput;
import org.apache.xml.security.transforms.Transform;
import org.apache.xml.security.transforms.TransformSpi;
import org.apache.xml.security.transforms.TransformationException;
import org.apache.xml.security.keys.content.X509Data;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * This class <code>STRTransform</code> extends from <code>TransformSpi</code>
 * and will be used to transform the <code>XMLSignatureInput</code> as 
 * required by the WS-Security specification.
 */
public class STRTransform extends TransformSpi {

    public static final String STR_TRANSFORM_URI = 
                "http://docs.oasis-open.org/wss/2004/01/" +
                "oasis-200401-wss-soap-message-security-1.0#STR-Transform";


    private static String XMLNS = "xmlns=";
    //private static Debug debug = WSSUtils.debug;
    static {
       try {
           Transform.register(STR_TRANSFORM_URI, STRTransform.class.getName());
       } catch (Exception e) {
           //debug.message("STRTransform.static already registered");
       }
    }

    /**
     * Returns the transformation engine URI.
     */
    protected String engineGetURI() {
        return STR_TRANSFORM_URI;
    }

    /**
     * Perform the XMLSignature transformation for the given input.
     */
    protected XMLSignatureInput enginePerformTransform(XMLSignatureInput input, Transform transformObject)
          throws IOException, CanonicalizationException, 
          InvalidCanonicalizerException, TransformationException {
  
        WSSUtils.debug.message("STRTransform.enginePerformTransform:: Start");
        Document doc = transformObject.getDocument();
        Element str = null;
        if(input.isElement()) {
        } else {
           WSSUtils.debug.error("STRTransform.enginePerformTransform:: Input" +
              " is not an element");
           throw new  CanonicalizationException(
                 WSSUtils.bundle.getString("invalidElement"));
        }
        Element element = (Element)input.getSubNode();
        if(!WSSConstants.TAG_SECURITYTOKEN_REFERENCE.equals(
                     element.getLocalName())) {
           WSSUtils.debug.error("STRTransform.enginePerformTransform:: input" +
            " must be security token reference");
           throw new IOException(
                 WSSUtils.bundle.getString("invalidElement"));
        }
        Element dereferencedToken = null;
        SecurityTokenReference ref = null; 
        try {
            ref = new SecurityTokenReference(element);
            dereferencedToken = dereferenceSTR(doc, ref);
        } catch (SecurityException se) {
            WSSUtils.debug.error("STRTransform.enginePerformTransform:: error",
             se);
            throw new TransformationException(
                  WSSUtils.bundle.getString("transformfailed"));
        }
        String canonAlgo = getCanonicalizationAlgo(transformObject);
        Canonicalizer canon = Canonicalizer.getInstance(canonAlgo);
        byte[] buf = canon.canonicalizeSubtree(dereferencedToken, "#default");
        StringBuffer bf = new StringBuffer(new String(buf));
        String bf1 = bf.toString();

        int lt = bf1.indexOf("<");
        int gt = bf1.indexOf(">");
        int idx = bf1.indexOf(XMLNS);
        if (idx < 0 || idx > gt) {
            idx = bf1.indexOf(" ");
            bf.insert(idx + 1, "xmlns=\"\" ");
            bf1 = bf.toString();
        }
        return new XMLSignatureInput(bf1.getBytes());
    }

    /**
     * Derefence the security token reference from the given document.
     */
    private Element dereferenceSTR(Document doc, SecurityTokenReference secRef)
         throws SecurityException {

        WSSUtils.debug.message("STRTransform.deferenceSTR:: start");
        Element tokenElement = null;
        String refType = secRef.getReferenceType();

        if(SecurityTokenReference.DIRECT_REFERENCE.equals(refType)) {
           WSSUtils.debug.message("STRTRansform.deferenceSTR:: Direct " +
                  "reference");
           tokenElement = secRef.getTokenElement(doc);

        } else if(SecurityTokenReference.X509DATA_REFERENCE.equals(refType)) {
           WSSUtils.debug.message("STRTRansform.deferenceSTR:: X509 data " +
                  "reference");
           X509Data x509Data = secRef.getX509IssuerSerial();
           X509Certificate cert = 
                     AMTokenProvider.getX509Certificate(x509Data);
           tokenElement = createBinaryToken(doc, cert);

        } else if(SecurityTokenReference.KEYIDENTIFIER_REFERENCE.
                  equals(refType)) {
           WSSUtils.debug.message("STRTRansform.deferenceSTR:: keyidentifier" +
               " reference");
           KeyIdentifier keyIdentifier = secRef.getKeyIdentifier();
           String valueType = keyIdentifier.getValueType();           
           if(WSSConstants.ASSERTION_VALUE_TYPE.equals(valueType) ||
                   WSSConstants.SAML2_ASSERTION_VALUE_TYPE.equals(valueType)) {
              tokenElement = keyIdentifier.getTokenElement(doc);
           } else {
              X509Certificate cert = keyIdentifier.getX509Certificate();
              tokenElement = createBinaryToken(doc, cert);
           }
        }
        return tokenElement;
    }

    /**
     * Creates binary security token using the given x509 certificate.
     */
    private Element createBinaryToken(Document doc, 
         X509Certificate cert) throws SecurityException {

        BinarySecurityToken token =  new BinarySecurityToken(cert, 
            BinarySecurityToken.X509V3, BinarySecurityToken.BASE64BINARY);

        Element tokenE = token.toDocumentElement();
        doc.importNode(tokenE, true);
        return tokenE;
    }

    /**
     * Returns the canonicalization algorithm in transformation params.
     */
    private String getCanonicalizationAlgo(Transform transformObject) {
        String canonAlgo = null;
        if (transformObject.length(WSSConstants.WSSE_NS,
                    WSSConstants.TRANSFORMATION_PARAMETERS) == 1) {
            Node tmpE = XMLUtils.getChildNode(
                transformObject.getElement(), WSSConstants.WSSE_TAG + ":"
                       + WSSConstants.TRANSFORMATION_PARAMETERS);
            Element canonElem = (Element) WSSUtils.getDirectChild(
                        tmpE, "CanonicalizationMethod", 
                        WSSConstants.XMLSIG_NAMESPACE_URI);
            canonAlgo = canonElem.getAttribute("Algorithm");
        }
        return canonAlgo;
    }
    
    
    public boolean wantsOctetStream () { 
        return true;
    }
    
    public boolean wantsNodeSet () {
        return true;
    }
    public boolean returnsOctetStream () {
        return true;
    }
    
    public boolean returnsNodeSet () {
        return false;
    }
    
}
