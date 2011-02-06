/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AMCertPath.java,v 1.5 2009/07/16 00:02:24 beomsuk Exp $
 *
 */

package com.sun.identity.security.cert;

import java.lang.reflect.Method;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorResult;
import java.security.cert.CertStore;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXParameters;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.security.SecurityDebug;

/**
 * Class AMCertPath is special cased Certpath validation class.
 * It does cert path validation together with CRL check and ocsp checking 
 * if they are properly configured.
 */

public class AMCertPath {

    private static CertificateFactory cf = null;
    private static CertPathValidator cpv = null;
    private CertStore store = null;
    public static Debug debug = SecurityDebug.debug;
    public static boolean OCSPCheck = false;
    
    static {
    	try {
    	    cf= CertificateFactory.getInstance("X509");
            cpv= CertPathValidator.getInstance("PKIX");
    	} catch (Exception e) {
    		debug.error("AMCertPath.Static:",e);
    	}
    }

    /**
     * Class constructor
     * param Vector crls
     */
    public AMCertPath(Vector crls) 
         throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {

        if ((crls != null) && (crls.size() > 0)) {
            if (debug.messageEnabled()) {
	        X509CRL crl = (X509CRL) crls.elementAt(0);
                debug.message("AMCertPath:AMCertPath: crl =" + crl.toString());
            }
		
            CollectionCertStoreParameters collection = 
                                new CollectionCertStoreParameters(crls);
            store = CertStore.getInstance("Collection", collection);
        } else {
            if (debug.messageEnabled()) {
                debug.message("AMCertPath:AMCertPath: no crl");
            }
        }
    }

    /**
     * It does cert path validation together with CRL check and ocsp checking 
     * if they are properly configured.
     * @param certs
     **/
    public boolean verify(X509Certificate[] certs, boolean crlEnabled,
                          boolean ocspEnabled) {
        if (debug.messageEnabled()) {
            debug.message("AMCertPath.verify: invoked !");
        }
        try {
            List certList = Arrays.asList(certs);
            CertPath cp= (CertPath) cf.generateCertPath(certList);

            // init PKIXParameters
            Class trustMgrClass = Class.forName(
                  "com.sun.identity.security.keystore.AMX509TrustManager");
            Object trustMgr = (Object) trustMgrClass.newInstance();
            Method method = trustMgrClass.getMethod("getKeyStore", (Class)null);
            KeyStore keystore = (KeyStore) method.invoke(trustMgr, (Object)null);
            PKIXParameters pkixparams= new PKIXParameters(keystore);
            if (debug.messageEnabled()) {
                debug.message("AMCertPath.verify: crlEnabled ---> " + crlEnabled);
                debug.message("AMCertPath.verify: ocspEnabled ---> " + ocspEnabled);
            }

            if (ocspEnabled && !OCSPCheck) {
                Security.setProperty("ocsp.enable", "true"); 
                OCSPCheck = true;
            }

            pkixparams.setRevocationEnabled(crlEnabled);
            if (store != null) {
            	pkixparams.addCertStore(store);
            }
            
            // validate
            CertPathValidatorResult cpvResult= cpv.validate(cp, pkixparams);

            if (debug.messageEnabled()) {
                debug.message("AMCertPath.verify: PASS" + cpvResult.toString());
            }
        } catch (java.security.cert.CertPathValidatorException e) {
            debug.error("AMCertPath.verify: FAILED - " + e.getMessage());
            if (debug.messageEnabled()) {
                debug.message("AMCertPath.verify: FAILED", e);
            }
            return false;
        } catch (Throwable t) {
            debug.error("AMCertPath.verify: FAILED", t);
            return false;
        }

	return true;
    }
    
    
}
