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
 * $Id: SAE.java,v 1.5 2009/02/26 23:58:10 exu Exp $
 *
 */

package com.sun.identity.authentication.modules.sae;

import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Map;
import java.util.Iterator;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Utils;

import com.iplanet.am.util.Misc;
import com.sun.identity.shared.debug.Debug;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import com.sun.identity.sae.api.SecureAttrs;

public class SAE extends AMLoginModule {

    private static final int DEFAULT_AUTH_LEVEL = 0;

    private String userTokenId;
    private SAEPrincipal userPrincipal;
    

    private static final String customModule = "SAE";

    private static Debug debug = null;
    static {
	debug = Debug.getInstance(customModule);
    }

    /**     
     * Constructor
     *
     */
    public SAE() {
    }


    public void init(Subject subject, Map sharedState, Map options) {
    }

    /**
     * Validates the authentication credentials.
     *
     * @return ISAuthConstants.LOGIN_SUCCEED on login success
     * @exception AuthLoginException on failure. 
     */
    public int process (Callback[] callbacks, int state) 
			throws AuthLoginException {

        debug.message("SAE AuthModule.process...");
        HttpServletRequest req = getHttpServletRequest();
        if(req == null) {
           debug.error("SAE AuthModule.process: httprequest is null."); 
           throw new AuthLoginException("HttpServletRequest is null");
        }

        String encodedString = req.getParameter(SecureAttrs.SAE_PARAM_DATA); 
        if (debug.messageEnabled()) {
            debug.message("SAE AuthModule.process+encodedStr="+encodedString);
        }
        String realm = req.getParameter(SAML2Constants.SAE_REALM);
        String idpEntityId = req.getParameter(SAML2Constants.SAE_IDP_ENTITYID);
        String idpAppUrl = req.getParameter(SAML2Constants.SAE_IDPAPP_URL);

        debug.message("SAE AuthModule.SAML2Utils.getSAEAttrs");
        Map saeattrs = SAML2Utils.getSAEAttrs(
                  realm, idpEntityId, SAML2Constants.IDP_ROLE, idpAppUrl);
        if(saeattrs == null) {
            debug.error(
              "SAE AuthModule.process:get SAE Attrs failed:null."); 
            throw new AuthLoginException("SAE config Attributes are null");
        }
        String cryptoType = (String) saeattrs.get(SecureAttrs.SAE_CRYPTO_TYPE);
        String encryptAlg = (String) saeattrs.get(
                            SecureAttrs.SAE_CONFIG_DATA_ENCRYPTION_ALG);
        String encryptStrength = (String) saeattrs.get(
                            SecureAttrs.SAE_CONFIG_ENCRYPTION_KEY_STRENGTH);
        String saekey = null;
        String saeprivatekey = null;
        if ("symmetric".equals(cryptoType)) {
            saekey = (String) saeattrs.get(
                                  SecureAttrs.SAE_CONFIG_SHARED_SECRET);
            saeprivatekey = saekey;
        }
        else if ("asymmetric".equals(cryptoType)) {
            saekey = (String) saeattrs.get(
                                  SecureAttrs.SAE_CONFIG_PUBLIC_KEY_ALIAS);
            saeprivatekey = (String) saeattrs.get(
                                  SecureAttrs.SAE_CONFIG_PRIVATE_KEY_ALIAS);
        }

        if (debug.messageEnabled()) {
            debug.message("SAE AuthModule: realm=" + realm +
                ", idpEntityID=" + idpEntityId +
                ", idpAppUrl=" + idpAppUrl +
                ", cryptoType=" + cryptoType +
                ", key=" + saekey);
        }

        Map attrs = null;
        try {
            String saInstanceName = 
                cryptoType + "_" + encryptAlg + "_" + encryptStrength;
            SecureAttrs sa = SecureAttrs.getInstance(saInstanceName);
            if (sa == null) {
                // Initialize SecureAttrs here.
                Properties prop = new Properties();
                prop.setProperty(SecureAttrs.SAE_CONFIG_CERT_CLASS,
                    "com.sun.identity.sae.api.FMCerts");
                 if(encryptAlg != null) {
                   prop.setProperty(
                         SecureAttrs.SAE_CONFIG_DATA_ENCRYPTION_ALG,
                         encryptAlg);
                }
                if(encryptStrength != null) {
                   prop.setProperty(
                        SecureAttrs.SAE_CONFIG_ENCRYPTION_KEY_STRENGTH,
                        encryptStrength);
                }
                SecureAttrs.init(saInstanceName, cryptoType, prop);
                sa = SecureAttrs.getInstance(saInstanceName);
            }

            attrs = sa.verifyEncodedString(encodedString, 
                    saekey, saeprivatekey);

            if (debug.messageEnabled()) 
                debug.message("SAE AuthModule.: SAE attrs:"+attrs); 
        } catch (Exception ex) {
            debug.error("SAE AuthModule.process: verification failed.", ex); 
            throw new AuthLoginException("verify failed");
        }
      
        if(attrs == null) {
            debug.error(
              "SAE AuthModule.process:verification failed:attrs null."); 
            throw new AuthLoginException("Attributes are null");
        }

        userTokenId = (String)attrs.get(SecureAttrs.SAE_PARAM_USERID); 
        Iterator iter = attrs.entrySet().iterator();
        while(iter.hasNext()) {
           Map.Entry entry = (Map.Entry)iter.next();
           String key = (String)entry.getKey();
           String value = (String)entry.getValue();
           if(key.equals(SecureAttrs.SAE_PARAM_USERID)) {
              continue;
           }
           if(debug.messageEnabled()) {
              debug.message("Session Property set: " + key + "= " + value);
           }
           setUserSessionProperty(key, value);
        }

        String authLevel = (String)attrs.get(SecureAttrs.SAE_PARAM_AUTHLEVEL);
        int authLevelInt = DEFAULT_AUTH_LEVEL;
        if (authLevel != null && authLevel.length() != 0) {
            try {
                authLevelInt = Integer.parseInt(authLevel);
            } catch (Exception e) {
                debug.error("Unable to parse auth level " +
                            authLevel + ". Using default.",e);
                authLevelInt = DEFAULT_AUTH_LEVEL;
            }
        }
        if (debug.messageEnabled()) {
            debug.message("SAE AuthModule: auth level = " + authLevelInt);
        }
        setAuthLevel(authLevelInt);
        debug.message("SAE AuthModule:return SUCCESS");
 
        return  ISAuthConstants.LOGIN_SUCCEED; 
    }

    /**     
     * Returns the User Principal
     *
     * @return SAEPrincipal
     */

    public java.security.Principal getPrincipal() {
        if ((userPrincipal == null) && (userTokenId != null)) {
            userPrincipal = new SAEPrincipal(userTokenId);
        }

	return userPrincipal;
    }
    


    /** cleanup module state 
     * 
     */
    public void destroyModuleState() {
	userPrincipal = null;
	userTokenId = null;
    }
}
