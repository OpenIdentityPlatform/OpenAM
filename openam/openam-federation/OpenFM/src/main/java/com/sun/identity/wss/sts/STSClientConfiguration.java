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
 * $Id: STSClientConfiguration.java,v 1.9 2008/08/05 04:11:01 mallas Exp $
 *
 */

package com.sun.identity.wss.sts;

import com.sun.xml.ws.api.security.trust.Claims;
import java.util.List;
import com.sun.xml.ws.api.security.trust.client.STSIssuedTokenConfiguration;
import com.sun.xml.ws.security.Token;
import com.sun.identity.wss.provider.ProviderConfig;
import com.sun.identity.wss.security.SecurityMechanism;

/**
 * This class implements WSIT <code>STSIssuedTokenConfiguration</code> to
 * give the appropriate run time configuration for the WSIT client to
 * obtain the security tokens from a trusted authority.
 */
public class STSClientConfiguration extends STSIssuedTokenConfiguration {
           
    private String tokenType = null;
    
    private String keyType = null;
    
    private long keySize = -1;
    
    private String signatureAlg = null;
    
    private String encAlg = null;
    
    private String canAlg = null;
    
    private String keyWrapAlg = null;
    
    private Token userToken = null;
    
    private Claims claims = null;
    
    private String signWith = null;
    
    private String encryptWith = null;
            
    public STSClientConfiguration(String stsEndpoint, String stsMEXAddress){
        super(stsEndpoint, stsMEXAddress);
        // We need public key value in the STS issued assertion for signing
        // purposes
        // TODO - we will have to see for Symmetric key
        this.keyType = STSConstants.WST10_PUBLIC_KEY;
    }
    
    public STSClientConfiguration(String protocol, 
            String stsEndpoint, String stsMEXAddress) {            
        super(protocol, stsEndpoint, stsMEXAddress);
        // We need public key value in the STS issued assertion for signing
        // purposes
        // TODO - we will have to see for Symmetric key
        if(protocol.equals(PROTOCOL_13)) {
           this.keyType = STSConstants.WST13_PUBLIC_KEY;
        } else {
           this.keyType = STSConstants.WST10_PUBLIC_KEY;
        }
    }
            
    public STSClientConfiguration(String stsEndpoint,
          String stsWSDLLocation, 
          String stsServiceName,
          String stsPortName,
          String stsNamespace){
        super(stsEndpoint, stsWSDLLocation, stsServiceName, stsPortName,
              stsNamespace);

        this.keyType = STSConstants.WST10_PUBLIC_KEY;;
        
    }
    
    public void setOBOToken(Token userToken) {
        this.userToken = userToken;
    }
    
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
        
    public String getTokenType() {    
        return tokenType;
    }
    
    public String getKeyType() {
        return keyType;
    }
    
    public String getProtocol() {
        return protocol;
    }
    
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    
    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }
    
    public long getKeySize() {
        return keySize;
    }
    
    public String getSignatureAlgorithm() {
        return signatureAlg;
    }
    
    public String getEncryptionAlgorithm() {
        return encAlg;
    }
    
    public String getCanonicalizationAlgorithm() {
        return canAlg;
    }
    
    public String getKeyWrapAlgorithm() {
        return keyWrapAlg;
    }
    
    public Token getOBOToken() {
        return userToken;
    }
    
    public Claims getClaims() {
        return claims;
    }

    @Override
    public String getSignWith() {
        return signWith;
    }

    @Override
    public String getEncryptWith() {
        return encryptWith;
    }

}
