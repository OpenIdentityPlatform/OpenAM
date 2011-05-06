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
 * $Id: FSSignatureUtil.java,v 1.3 2008/07/17 16:56:39 exu Exp $
 *
 */


package com.sun.identity.federation.services.util;

import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.saml.common.*;
import com.sun.identity.saml.xmlsig.*;
import com.sun.identity.federation.common.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.security.cert.X509Certificate;
import java.util.*;


/**
 * Util methods to sign and verify signature on query string.
 */
public class FSSignatureUtil {
    /**
     * Signs a string using enveloped signatures.
     * @param queryString the string to be signed
     * @param certAlias signer's certificate alias
     * @return string with encoded signature or <code>null</code> if it
     *  couldn't be signed.
     */
    public static String signAndReturnQueryString(
        String queryString, String certAlias)
    {
        FSUtils.debug.message(
            "FSSignatureUtil.signAndReturnQueryString: Called");
        
        if (queryString == null || queryString.length() == 0){
            FSUtils.debug.error("FSSignatureUtil."
                + "signAndReturnQueryString: " 
                + FSUtils.bundle.getString("nullInput"));
            return null;
        } else {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSSignatureUtil.signAndReturnQueryString: queryString: " +
                    queryString);
            }
        }
        
        if(certAlias == null || certAlias.length() == 0){
            FSUtils.debug.error(
                "FSSignatureUtil.signAndReturnQueryString: " 
                + FSUtils.bundle.getString("nullInput"));
            return null;
        } else {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSSignatureUtil.signAndReturnQueryString: certAlias: " +
                    certAlias);
            }
        }
        
        FSSignatureManager manager = FSSignatureManager.getInstance();
        String sigAlg = IFSConstants.DEF_SIG_ALGO_JCA;
        String algoId = null;
        if(manager.getKeyProvider().getPrivateKey(certAlias).
            getAlgorithm().equals(IFSConstants.KEY_ALG_RSA))
        {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSSignatureUtil.signAndReturnQueryString: "
                        + "private key algorithm is: RSA");
            }
            sigAlg = IFSConstants.ALGO_ID_SIGNATURE_RSA_JCA;
            algoId = IFSConstants.ALGO_ID_SIGNATURE_RSA;
        } else if(manager.getKeyProvider().getPrivateKey(certAlias).
            getAlgorithm().equals(IFSConstants.KEY_ALG_DSA))
        {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSSignatureUtil.signAndReturnQueryString: "
                        + "private key algorithm is: DSA");
            }
            sigAlg = IFSConstants.ALGO_ID_SIGNATURE_DSA_JCA; 
            algoId = IFSConstants.ALGO_ID_SIGNATURE_DSA;
        } else {
            FSUtils.debug.error(
                "FSSignatureUtil.signAndReturnQueryString: "
                     + "private key algorithm is not supported");
            return null;
        }
        
        byte[] signature = null;
        
        if(queryString.charAt(queryString.length()-1) != '&'){
            queryString = queryString + "&";
        }
        queryString = queryString + "SigAlg=" + URLEncDec.encode(algoId);
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "FSSignatureUtil.signAndReturnQueryString: "
                + "Querystring to be signed: " + queryString);
        }
        try {
            signature = manager.signBuffer(queryString, certAlias, sigAlg);
        } catch(FSSignatureException se){
            FSUtils.debug.error("FSSignatureUtil."
                + "signAndReturnQueryString: FSSignatureException occured "
                + "while signing query string: " , se);
            return null;
        }
        if(signature == null){
            FSUtils.debug.error("FSSignatureUtil."
                + "signAndReturnQueryString: Signature generated is null");
            return null;
        }
        String encodedSig = Base64.encode(signature);
        queryString = queryString + "&" + "Signature=" 
                        + URLEncDec.encode(encodedSig);
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSSignatureUtil."
                + "signAndReturnQueryString:Signed Querystring: " 
                + queryString);
        }
        return queryString;
    }

    /**
     * Verifies signature on the request.
     * @param request <code>HttpServletRequest</code> object
     * @param cert Signer's certificate.
     * @return <code>true</code> if the signature is valid; <code>false</code>
     *  otherwise.
     */
    public static boolean verifyRequestSignature(
        HttpServletRequest request, 
        X509Certificate cert
    )
    {
        FSUtils.debug.message("FSSignatureUtil.verifyRequestSignature: Called");
        try{              
            // to make sure always use the public key in provider's
            // configuration to verify signature
            if(cert == null) {
                if(FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSSignatureUtil.verifyRequestSignature: "
                        + "couldn't obtain this site's cert.");
                }
                return false;
            }
            
            String sigAlg = request.getParameter("SigAlg");
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("sigAlg : " + sigAlg);
            }
            String encSig = request.getParameter("Signature");
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("encSig : " + encSig);
            }
            if (sigAlg == null || sigAlg.length() == 0 || 
                encSig == null || encSig.length() == 0)
            {
                return false;
            }
            String algoId = null;
            if(sigAlg.equals(IFSConstants.ALGO_ID_SIGNATURE_DSA)) {
                algoId = IFSConstants.ALGO_ID_SIGNATURE_DSA_JCA;
            } else if (sigAlg.equals(IFSConstants.ALGO_ID_SIGNATURE_RSA)) {
                algoId = IFSConstants.ALGO_ID_SIGNATURE_RSA_JCA;
            } else {
                FSUtils.debug.error(
                    "FSSignatureUtil.signAndReturnQueryString: "
                    + "Invalid signature algorithim");
                return false;
            }
            String queryString = request.getQueryString();
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSignatureUtil.verifyRequestSignature:"
                    + "queryString to be verifed:" + queryString);
            }
            int sigIndex = queryString.indexOf("&Signature");
            String newQueryString = queryString.substring(0, sigIndex);
            byte[] signature = null;
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSignatureUtil.verifyRequestSignature: "
                    + "Signature: " + encSig);
            }
            signature = Base64.decode(encSig);
        
            FSSignatureManager fsmanager = FSSignatureManager.getInstance();
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSignatureUtil.verifyRequestSignature: "
                    + "String to be verified: " + newQueryString);
            }
            return fsmanager.verifySignature(newQueryString, 
                                            signature, 
                                            algoId, 
                                            cert);
        }catch(Exception e){
            FSUtils.debug.error("FSSignatureUtil.verifyRequestSignature: "
                + "Exception occured while verifying SP's signature:" , e);
            return false;
        }
    } 
}

