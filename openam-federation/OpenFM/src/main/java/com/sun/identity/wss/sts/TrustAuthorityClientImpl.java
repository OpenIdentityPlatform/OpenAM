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
 * $Id: TrustAuthorityClientImpl.java,v 1.8 2008/08/31 15:50:03 mrudul_uchil Exp $
 *
 */

package com.sun.identity.wss.sts;

import org.w3c.dom.Element;

import com.sun.identity.shared.debug.Debug;
import com.sun.xml.ws.security.Token;
import com.sun.xml.ws.api.security.trust.client.IssuedTokenManager;
import com.sun.xml.ws.security.IssuedTokenContext;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.wss.security.SecurityToken;

/**
 * The class <code>TrustAuthorityClientImpl</code> is the implementation of
 * <code>TrustAuthorityClient</code> class.
 */
public class TrustAuthorityClientImpl {
    
    private static Debug debug = STSUtils.debug;
    private static Class clientTokenClass;
    
    
    /** Creates a new instance of TrustAuthorityClientImpl */
    public TrustAuthorityClientImpl() {
    }
        
    /**
     * Returns security token element obtained from Security Token Service.
     */
    public Element getSTSTokenElement(String wspEndPoint,
                                      String stsEndpoint,
                                      String stsMexAddress,
                                      Object credential,
                                      String keyType,
                                      String tokenType,
                                      String version) 
                                      throws FAMSTSException {

        String protocolNS = STSConstants.WST13_NAMESPACE;
        if(STSConstants.WST_VERSION_10.equals(version)) {
           protocolNS = STSConstants.WST10_NAMESPACE;
        }
        STSClientConfiguration config =
            new STSClientConfiguration(protocolNS, stsEndpoint, stsMexAddress);        
        config.setKeyType(keyType);
        if(tokenType != null) {
           config.setTokenType(tokenType); 
        }
        if(credential != null) {
           config.setOBOToken(getClientUserToken(credential));
        }
        try {
            IssuedTokenManager manager = IssuedTokenManager.getInstance();
            IssuedTokenContext ctx =
                manager.createIssuedTokenContext(config,wspEndPoint);
            manager.getIssuedToken(ctx);
            Token issuedToken = ctx.getSecurityToken();
            Element element = (Element)issuedToken.getTokenValue();

            return element;
        } catch (Exception ex) {
            debug.error("TrustAuthorityClientImpl.getSTSToken:: Failed in" +
                "obtainining STS Token Element: ", ex);            
            throw new FAMSTSException(
                    STSUtils.bundle.getString("wstrustexception"));
        }        
    }
    
    /**
     * Returns Client's or End user's token to be converted to Security token.
     */
    private Token getClientUserToken(Object credential) 
                throws FAMSTSException {
        if (clientTokenClass == null) {
            String className =   SystemConfigurationUtil.getProperty(
                STSConstants.STS_CLIENT_USER_TOKEN_PLUGIN, 
                "com.sun.identity.wss.sts.STSClientUserToken");
            try {                
                clientTokenClass = 
                       (Thread.currentThread().getContextClassLoader()).
                        loadClass(className);                               
            } catch (Exception ex) {
                 debug.error("TrustAuthorityClientImpl.getClientUserToken:"
                           +  "Failed in obtaining class", ex);
                 throw new FAMSTSException(
                       STSUtils.bundle.getString("initializationFailed"));
            }
        }
        
        try {
            ClientUserToken userToken =
                (ClientUserToken) clientTokenClass.newInstance();
            userToken.init(credential);
            if(debug.messageEnabled()) {
                debug.message("TrustAuthorityClientImpl:getClientUserToken: " + 
                    "Client User Token : " + userToken);
            }
            return userToken;

        } catch (Exception ex) {
            debug.error("TrustAuthorityClientImpl.getClientUserToken: " +
                 "Failed in initialization", ex);
             throw new FAMSTSException(
                     STSUtils.bundle.getString("usertokeninitfailed"));
        }
    }
                 
}
