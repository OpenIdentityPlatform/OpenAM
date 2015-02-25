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
 * $Id: FAMCallbackHandler.java,v 1.5 2008/07/02 16:57:24 mallas Exp $
 *
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.wss.sts.config;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.Callback;
import com.sun.xml.wss.impl.callback.EncryptionKeyCallback;
import com.sun.xml.wss.impl.callback.SignatureKeyCallback;
import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.wss.sts.STSUtils;

import java.security.cert.X509Certificate;
import java.security.PrivateKey;

public class FAMCallbackHandler implements CallbackHandler {
    
    protected KeyProvider keystore = null;
    private String certAlias;

    static {
        org.apache.xml.security.Init.init();
    }

    /** Creates a new instance of FAMCallbackHandler */
    public FAMCallbackHandler(String certAlias) {
        this.certAlias = certAlias;
    }
    
    public void handle (Callback[] callbacks) {

        try {
            String kprovider = SystemConfigurationUtil.getProperty(
                SAMLConstants.KEY_PROVIDER_IMPL_CLASS,
                SAMLConstants.JKS_KEY_PROVIDER);
            keystore= (KeyProvider) Class.forName(kprovider).newInstance();
        } catch (Exception e) {
            STSUtils.debug.error("FAMCallbackHandler: " +
                "get keystore error", e);
            throw new RuntimeException(e.getMessage());
        }
        
        try {
            int length = callbacks.length;
            for (int i=0; i < length; i++) {
                Callback cb = callbacks[i];
                if(cb instanceof EncryptionKeyCallback) {
                    EncryptionKeyCallback eb = (EncryptionKeyCallback)cb;
                    EncryptionKeyCallback.AliasX509CertificateRequest x509Req = 
                        (EncryptionKeyCallback.AliasX509CertificateRequest)eb.
                        getRequest();
                    String alias = x509Req.getAlias();
                    X509Certificate cert = 
                        (X509Certificate) keystore.getX509Certificate(alias);
                    x509Req.setX509Certificate(cert);
                } else if(cb instanceof SignatureKeyCallback) {
                    SignatureKeyCallback sb = (SignatureKeyCallback)cb;
                    SignatureKeyCallback.DefaultPrivKeyCertRequest privKey = 
                        (SignatureKeyCallback.DefaultPrivKeyCertRequest)sb.
                        getRequest();
                    privKey.setX509Certificate(
                        (X509Certificate) keystore.
                        getX509Certificate(certAlias));
                    privKey.setPrivateKey(
                        (PrivateKey) keystore.getPrivateKey(certAlias));
                }
            }
        } catch(Exception ex) {
            STSUtils.debug.error("FAMCallbackHandler: " +
                "handle callbacks error", ex);
            throw new RuntimeException(ex.getMessage());
        }
        
    }
    
}
