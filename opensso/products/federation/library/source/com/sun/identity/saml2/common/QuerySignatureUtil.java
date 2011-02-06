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
 * $Id: QuerySignatureUtil.java,v 1.2 2008/06/25 05:47:45 qcheng Exp $
 *
 */


package com.sun.identity.saml2.common;

import java.util.StringTokenizer;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.InvalidKeyException;
import java.security.cert.X509Certificate;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.saml.common.SAMLConstants;
import java.security.NoSuchAlgorithmException;

/**
 * The <code>QuerySignatureUtil</code> provides methods to
 * sign query string and to verify signature on query string
 */
public class QuerySignatureUtil {

    private QuerySignatureUtil() {
    }

    /**
     * Signs the query string.
     * @param queryString Query String
     * @param privateKey siging key
     * @return String signed query string
     * @exception SAML2Exception if the signing fails
     */
    public static String sign(
        String queryString,
        PrivateKey privateKey
    ) throws SAML2Exception {
        
        String classMethod =
            "QuerySignatureUtil.sign: ";
        if (queryString == null ||
            queryString.length() == 0 ||
            privateKey == null) {
            SAML2Utils.debug.error(
                classMethod +
                "Either input query string or private key is null."
            );  
            throw new SAML2Exception( 
                SAML2Utils.bundle.getString("nullInput"));  
        }                                                 
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(
                classMethod +
                "Input query string:\n" +
                queryString);
        }
        String alg = privateKey.getAlgorithm();
        Signature sig = null;
        String algURI = null;
        if (alg.equals("RSA")) {
            try {
                sig = Signature.getInstance(
                    SAML2Constants.SHA1_WITH_RSA);
                algURI = SAMLConstants.ALGO_ID_SIGNATURE_RSA;
            } catch (NoSuchAlgorithmException nsae) {
                throw new SAML2Exception(nsae);
            }
        } else if (alg.equals("DSA")) {
            try {
                sig = Signature.getInstance(
                    SAML2Constants.SHA1_WITH_DSA);
                algURI = SAMLConstants.ALGO_ID_SIGNATURE_DSA;
            } catch (NoSuchAlgorithmException nsae) {
                throw new SAML2Exception(nsae);
            }
        } else {
            SAML2Utils.debug.error(
                classMethod +
                "Algorithm not supported: " + alg
            );
            throw new SAML2Exception(
                SAML2Utils.bundle.getString(
                    "algorithmNotSupported")
            );
        }
        if(queryString.charAt(queryString.length()-1)
           != '&'){
            queryString = queryString + "&";
        }
        queryString += SAML2Constants.SIG_ALG + "=" +
            URLEncDec.encode(algURI);
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(
                classMethod +
                "Final string to be signed:\n" +
                queryString);
        }       
        try {
            sig.initSign(privateKey);
        } catch (InvalidKeyException ike) {
            throw new SAML2Exception(ike);
        }
        try {
            sig.update(queryString.getBytes());
        } catch (SignatureException se1) {
            throw new SAML2Exception(se1);
        }
        byte[] sigBytes = null;
        try {
            sigBytes = sig.sign();
        } catch (SignatureException se2) {
            throw new SAML2Exception(se2);
        }
        if (sigBytes == null ||
            sigBytes.length == 0) {
            SAML2Utils.debug.error(
                classMethod +
                "Generated signature is null");
            throw new SAML2Exception(
                SAML2Utils.bundle.getString(
                    "nullSigGenerated"
                )
            );
        }
        Base64 encoder = new Base64();
        String encodedSig = encoder.encode(sigBytes);
        queryString +=
            "&" + SAML2Constants.SIGNATURE + "=" +
            URLEncDec.encode(encodedSig);

        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(
                classMethod +
                "Signed query string:\n" +
                queryString);
        }
        return queryString;
    }
    
    /**
     * Verifies the query string signature
     * @param queryString Signed query String
     * @param cert Verification certificate
     * @return boolean whether the verification is successful or not
     * @exception SAML2Exception if there is an error during verification
     */
    public static boolean verify(
        String queryString, 
        X509Certificate cert
    ) throws SAML2Exception {
        
        String classMethod =
            "QuerySignatureUtil.verify: ";
        if (queryString == null ||
            queryString.length() == 0 || cert == null) {
            
            SAML2Utils.debug.error(
                classMethod +
                "Input query string or certificate is null");       
            throw new SAML2Exception( 
                SAML2Utils.bundle.getString("nullInput"));
        }
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(
                classMethod +
                "Query string to be verifed:\n" + queryString);
        }
        StringTokenizer st = new
            StringTokenizer(queryString, "&");
        String token = null;
        String samlReq = null;
        String samlRes = null;
        String relay = null;
        String sigAlg = null;
        String encSig = null;
        while (st.hasMoreTokens()) {
            token = st.nextToken();
            if (token.startsWith(SAML2Constants.SAML_REQUEST)) {
                samlReq=token;
            } else if (token.startsWith(SAML2Constants.SAML_RESPONSE)) {
                samlRes=token;
            } else if (token.startsWith(SAML2Constants.RELAY_STATE)) {
                relay=token;
            } else if (token.startsWith(SAML2Constants.SIG_ALG)) {
                sigAlg = token;
            } else if (token.startsWith(SAML2Constants.SIGNATURE)) {
                encSig = token;
            }
        }
        if (sigAlg == null || sigAlg.equals("")) {
            SAML2Utils.debug.error(
                classMethod +
                "Null SigAlg query parameter.");
            throw new SAML2Exception( 
                SAML2Utils.bundle.getString("nullSigAlg"));         
        }
        if (encSig == null || encSig.equals("")) {
            SAML2Utils.debug.error(
                classMethod +
                "Null Signature query parameter.");
            throw new SAML2Exception( 
                SAML2Utils.bundle.getString("nullSig"));            
        }       
        // The following manipulation is necessary because
        // other implementations could send the query
        // parameters out of order, i.e., not in the same
        // order when signature is produced
        String newQueryString = null;
        if (samlReq != null) {
            newQueryString = samlReq;
        } else {
            newQueryString = samlRes;
        }
        if (relay != null) {
            newQueryString += "&"+relay;
        }
        newQueryString += "&"+sigAlg;
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(
                classMethod+
                "Query string to be verifed (re-arranged):\n" +
                newQueryString);
        }
        int sigAlgValueIndex = sigAlg.indexOf('=');
        String sigAlgValue =
            sigAlg.substring(sigAlgValueIndex+1);
        if (sigAlgValue == null || sigAlgValue.equals("")) {
            SAML2Utils.debug.error(
                classMethod +
                "Null SigAlg query parameter value.");
            throw new SAML2Exception( 
                SAML2Utils.bundle.getString("nullSigAlg"));         
        }
        sigAlgValue = URLEncDec.decode(sigAlgValue);
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(
                classMethod +
                "SigAlg query parameter value: " +
                sigAlgValue);
        }
        int encSigValueIndex = encSig.indexOf('=');
        String encSigValue =
            encSig.substring(encSigValueIndex+1);
        if (encSigValue == null || encSigValue.equals("")) {
            SAML2Utils.debug.message(
                classMethod +
                "Null Signature query parameter value.");
            throw new SAML2Exception( 
                SAML2Utils.bundle.getString("nullSig"));            
        }
        encSigValue = URLEncDec.decode(encSigValue);
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(
                classMethod +
                "Signature query parameter value:\n" +
                encSigValue);
        }
        // base-64 decode the signature value
        byte[] signature = null;
        Base64 decoder = new Base64();
        signature = decoder.decode(encSigValue);

        // get Signature instance based on algorithm
        Signature sig = null;
        if (sigAlgValue.equals(
            SAMLConstants.ALGO_ID_SIGNATURE_DSA)) {
            try {
                sig = Signature.getInstance(
                    SAML2Constants.SHA1_WITH_DSA);
            } catch (NoSuchAlgorithmException nsae) {
                throw new SAML2Exception(nsae);
            }
        } else if (sigAlgValue.equals(
            SAMLConstants.ALGO_ID_SIGNATURE_RSA)) {
            try {
                sig = Signature.getInstance(
                    SAML2Constants.SHA1_WITH_RSA);
            } catch (NoSuchAlgorithmException nsae) {
                throw new SAML2Exception(nsae);
            }
        } else {
            SAML2Utils.debug.error(
                classMethod +
                "Signature algorithm not supported.");
            throw new SAML2Exception( 
                SAML2Utils.bundle.getString(
                    "algNotSupported")
            );              
        }
        // now verify signature
        try {
            sig.initVerify(cert);
        } catch (InvalidKeyException ike) {
            throw new SAML2Exception(ike);
        }
        try {
            sig.update(newQueryString.getBytes());
        } catch (SignatureException se1) {
            throw new SAML2Exception(se1);
        }
        boolean result = false;
        try {
            result = sig.verify(signature);
        } catch (SignatureException se2) {
            throw new SAML2Exception(se2);
        }
        return result;
    }
}
    











