/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: STSRemoteConfig.java,v 1.6 2009/11/16 21:52:59 mallas Exp $
 */

package com.sun.identity.wss.sts.config;

import java.util.Set;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.wss.sts.STSUtils;
import com.sun.identity.wss.security.PasswordCredential;
import com.sun.identity.wss.security.WSSUtils;

/**
 * This class provides STS service configuration remotely so that the
 * authentication provider could use this configuration while validating
 * the requests to the STS Service.
 */
public class STSRemoteConfig {
       
    private String type;
    private String issuer;
    private String stsEndpoint;    
    private List secMech = null;
    private boolean isResponseSign = false;
    private boolean isResponseEncrypt = false;
    private boolean isRequestSign = false;
    private boolean isRequestEncrypt = false;
    private boolean isRequestHeaderEncrypt = false;
    private String privateKeyType;
    private String privateKeyAlias;
    private String publicKeyAlias;
    private String kerberosDomainServer;
    private String kerberosDomain;
    private String kerberosServicePrincipal;
    private String kerberosKeyTabFile;
    private boolean isVerifyKrbSignature = false;
    private List usercredentials = null;
    private String encryptionAlgorithm = null;
    private int encryptionStrength = 0;
    private String signingRefType = null;
    private String authChain = null;    
    private boolean detectUserTokenReplay = true;
    private boolean detectMessageReplay = true;
    private List signedElements = null;
    
    static final String ISSUER = "stsIssuer";
    static final String SERVICE_NAME = "sunFAMSTSService";
    static final String END_POINT = "stsEndPoint";
    static final String SEC_MECH = "SecurityMech";
    static final String RESPONSE_SIGN = "isResponseSign";
    static final String RESPONSE_ENCRYPT = "isResponseEncrypt";
    static final String REQUEST_SIGN = "isRequestSign";     
    static final String REQUEST_ENCRYPT = "isRequestEncrypt";
    static final String REQUEST_HEADER_ENCRYPT = "isRequestHeaderEncrypt";
    static final String PRIVATE_KEY_TYPE = "privateKeyType";
    static final String PRIVATE_KEY_ALIAS = "privateKeyAlias";
    static final String PUBLIC_KEY_ALIAS = "publicKeyAlias";
    static final String USER_NAME = "UserName";
    static final String USER_PASSWORD = "UserPassword";
    static final String USER_CREDENTIAL = "UserCredential";
    static final String KERBEROS_DOMAIN_SERVER = "KerberosDomainServer";
    static final String KERBEROS_DOMAIN = "KerberosDomain";
    static final String KERBEROS_SERVICE_PRINCIPAL = "KerberosServicePrincipal";
    static final String KERBEROS_KEYTAB_FILE = "KerberosKeyTabFile";
    static final String KERBEROS_VERIFY_SIGNATURE = "isVerifyKrbSignature";
    static final String ENCRYPTION_ALGORITHM = "EncryptionAlgorithm";
    static final String ENCRYPTION_STRENGTH = "EncryptionStrength";
    static final String SIGNING_REF_TYPE = "SigningRefType";
    static final String AUTHENTICATION_CHAIN = "AuthenticationChain";    
    static final String USER_TOKEN_DETECT_REPLAY = "DetectUserTokenReplay";
    static final String MESSAGE_REPLAY_DETECTION = "DetectMessageReplay";
    static final String SIGNED_ELEMENTS = "SignedElements";
    
    public STSRemoteConfig() {
        
        SSOToken adminToken = WSSUtils.getAdminToken();
        Map attrMap = null;
        try {
            ServiceSchemaManager scm = new ServiceSchemaManager(
                    SERVICE_NAME, adminToken);
            ServiceSchema globalSchema = scm.getGlobalSchema();
            attrMap = globalSchema.getAttributeDefaults();
            setValues(attrMap);            
        } catch (SSOException ssoEx) {
           STSUtils.debug.error("FAMSTSConfiguration.static init failed", ssoEx);
        } catch (SMSException smsEx) {
           STSUtils.debug.error("FAMSTSConfiguration.static init failed", smsEx); 
        }
        
    }
    
    /**
     * This method reads values from service schema.
     */
     private void setValues(Map attrMap) {
      
        if(STSUtils.debug.messageEnabled()) {
           STSUtils.debug.message("STSServiceConfigMap: " + attrMap);
        }
        
        Set values = (Set)attrMap.get(END_POINT);
        if (values != null && !values.isEmpty()) {
            stsEndpoint = (String)values.iterator().next();
        }

        values = (Set)attrMap.get(ISSUER);
        if (values != null && !values.isEmpty()) {
            issuer = (String)values.iterator().next();
        }

        values = (Set)attrMap.get(SEC_MECH);
        if (values != null && !values.isEmpty()) {            
            if (secMech == null) {
               secMech = new ArrayList();
               secMech.addAll(values);
            } else {
               secMech.clear();
               secMech.addAll(values);
            }
                       
        }
        
        values = (Set)attrMap.get(RESPONSE_SIGN);
        if (values != null && !values.isEmpty()) {
            isResponseSign = 
                Boolean.valueOf((String)values.iterator().next())
                .booleanValue();
        }
        
        values = (Set)attrMap.get(RESPONSE_ENCRYPT);
        if (values != null && !values.isEmpty()) {
            isResponseEncrypt = 
                Boolean.valueOf((String)values.iterator().next())
                .booleanValue();
        }
        
        values = (Set)attrMap.get(REQUEST_SIGN);
        if (values != null && !values.isEmpty()) {
            isRequestSign = 
                Boolean.valueOf((String)values.iterator().next())
                .booleanValue();
        }
        
        values = (Set)attrMap.get(REQUEST_ENCRYPT);
        if (values != null && !values.isEmpty()) {
            isRequestEncrypt = 
                Boolean.valueOf((String)values.iterator().next())
                .booleanValue();
        }
        
        values = (Set)attrMap.get(REQUEST_HEADER_ENCRYPT);
        if (values != null && !values.isEmpty()) {
            isRequestHeaderEncrypt = 
                Boolean.valueOf((String)values.iterator().next())
                .booleanValue();
        }
        
        values = (Set)attrMap.get(PRIVATE_KEY_TYPE);
        if (values != null && !values.isEmpty()) {
            privateKeyType = (String)values.iterator().next();
        }
        
        values = (Set)attrMap.get(PRIVATE_KEY_ALIAS);
        if (values != null && !values.isEmpty()) {
            privateKeyAlias = (String)values.iterator().next();
        }
        
        values = (Set)attrMap.get(PUBLIC_KEY_ALIAS);
        if (values != null && !values.isEmpty()) {
            publicKeyAlias = (String)values.iterator().next();
        }
        
        String value = null;
        values = (Set)attrMap.get(USER_CREDENTIAL);
        if (values != null && !values.isEmpty()) {
            value = (String)values.iterator().next();
        }
        if ((value != null) && (value.length() != 0)) {
            if(usercredentials == null) {
                usercredentials = new ArrayList();
            }
            StringTokenizer stVal = new StringTokenizer(value, ","); 
            while(stVal.hasMoreTokens()) {
                String tmpVal = (String)stVal.nextToken();
                int index = tmpVal.indexOf("|");
                if(index == -1) {
                    return;
                }
                String usertmp = tmpVal.substring(0, index);
                String passwordtmp = tmpVal.substring(index+1, 
                    tmpVal.length()); 

                String user = null;
                String password = null;
                StringTokenizer st = new StringTokenizer(usertmp, ":"); 
                if(USER_NAME.equals(st.nextToken())) {
                    if(st.hasMoreTokens()) {
                        user = st.nextToken();
                    }               
                }
                StringTokenizer st1 = new StringTokenizer(passwordtmp, ":"); 
                if(USER_PASSWORD.equals(st1.nextToken())) {
                    if(st1.hasMoreTokens()) {
                        password = st1.nextToken();
                    }              
                }

                if((user != null) && (password != null)) {
                    PasswordCredential credential = 
                        new PasswordCredential(user, password);
                    usercredentials.add(credential);
                }   
            }
        }
        
        values = (Set)attrMap.get(KERBEROS_DOMAIN_SERVER);
        if (values != null && !values.isEmpty()) {
            kerberosDomainServer = (String)values.iterator().next();
        }
        
        values = (Set)attrMap.get(KERBEROS_DOMAIN);
        if (values != null && !values.isEmpty()) {
            kerberosDomain = (String)values.iterator().next();
        }
        
        values = (Set)attrMap.get(KERBEROS_SERVICE_PRINCIPAL);
        if (values != null && !values.isEmpty()) {
            kerberosServicePrincipal = (String)values.iterator().next();
        }
        
        values = (Set)attrMap.get(KERBEROS_KEYTAB_FILE);
        if (values != null && !values.isEmpty()) {
            kerberosKeyTabFile = (String)values.iterator().next();
        }
        
        values = (Set)attrMap.get(KERBEROS_VERIFY_SIGNATURE);
        if (values != null && !values.isEmpty()) {
            isVerifyKrbSignature = 
                Boolean.valueOf((String)values.iterator().next())
                .booleanValue();
        }        
        
        values = (Set)attrMap.get(ENCRYPTION_ALGORITHM);
        if (values != null && !values.isEmpty()) {
            encryptionAlgorithm = (String)values.iterator().next();
        }
        
        values = (Set)attrMap.get(ENCRYPTION_STRENGTH);
        if (values != null && !values.isEmpty()) {
            String tmp  = (String)values.iterator().next();
            encryptionStrength = Integer.parseInt(tmp);
        }
        
        values = (Set)attrMap.get(SIGNING_REF_TYPE);
        if (values != null && !values.isEmpty()) {
            signingRefType = (String)values.iterator().next();
        }
        
        values = (Set)attrMap.get(AUTHENTICATION_CHAIN);
        if (values != null && !values.isEmpty()) {
            String tmp = (String)values.iterator().next();
            if(!tmp.equals("[Empty]")) {
               authChain = (String)values.iterator().next();
            }
        }                        
        values = (Set)attrMap.get(USER_TOKEN_DETECT_REPLAY);
        if (values != null && !values.isEmpty()) {
            detectUserTokenReplay = 
                Boolean.valueOf((String)values.iterator().next())
                .booleanValue();
        }

        values = (Set)attrMap.get(SIGNED_ELEMENTS);
        if (values != null && !values.isEmpty()) {
            if(signedElements == null) {
               signedElements = new ArrayList();
               signedElements.addAll(values);
            } else {
               signedElements.clear();
               signedElements.addAll(values);
            }

        }
    }    
    
    public void setType(String type){
        this.type = type;
    } 
    
    public String getType(){
        return this.type;
    }
    
    public void setIssuer(String issuer){
        this.issuer = issuer;
    }
        
    public String getIssuer(){
        return this.issuer;
    }
      
    /**
     * Returns the list of security mechanims that the STS service is configured.
     *
     * @return list of security mechanisms.
     */
    public List getSecurityMechanisms() {
         return this.secMech;
    }

    /**
     * Sets the list of security mechanisms.
     *
     * @param authMech the list of security mechanisms.
     */
    public void setSecurityMechanisms(List authMech) {
        this.secMech = authMech;
    }
    
    /**
     * Checks if the response needs to be signed or not.
     *
     * @return true if the response needs to be signed.
     */
    public boolean isResponseSignEnabled() {
        return this.isResponseSign;
    }

    /**
     * Sets the response sign enable flag.
     *
     * @param enable enables the response signing.
     */
    public void setResponseSignEnabled(boolean enable) {
         this.isResponseSign = enable;
    }
    
    /**
     * Checks if the response needs to be encrypted or not.
     *
     * @return true if the response needs to be encrypted.
     */
    public boolean isResponseEncryptEnabled() {
        return this.isResponseEncrypt;
    }

    /**
     * Sets the response encrypt enable flag.
     *
     * @param enable enables the response encryption.
     */
    public void setResponseEncryptEnabled(boolean enable) {
         this.isResponseEncrypt = enable;
    }
    
    /**
     * Checks if the request needs to be signed or not.
     *
     * @return true if the request needs to be signed.
     */
    public boolean isRequestSignEnabled() {
        return this.isRequestSign;
    }

    /**
     * Sets the request sign enable flag.
     *
     * @param enable enables the request signing.
     */
    public void setRequestSignEnabled(boolean enable) {
         this.isRequestSign = enable;
    }
    
    /**
     * Checks if the request needs to be encrypted or not.
     *
     * @return true if the request needs to be encrypted.
     */
    public boolean isRequestEncryptEnabled() {
        return this.isRequestEncrypt;
    }

    /**
     * Sets the request encrypt enable flag.
     *
     * @param enable enables the request encryption.
     */
    public void setRequestEncryptEnabled(boolean enable) {
         this.isRequestEncrypt = enable;
    }

    /**
     * Checks if the request header needs to be encrypted or not.
     *
     * @return true if the request header needs to be encrypted.
     */
    public boolean isRequestHeaderEncryptEnabled() {
        return this.isRequestHeaderEncrypt;
    }

    /**
     * Sets the request header encrypt enable flag.
     *
     * @param enable enables the request header encryption.
     */
    public void setRequestHeaderEncryptEnabled(boolean enable) {
        this.isRequestHeaderEncrypt = enable;
    }
    
    /**
     * Returns the key type for the security provider at STS service.
     * 
     * @return the key type of the security provider at STS service.
     */
    public String getPrivateKeyType() {
        return privateKeyType;
    }
   
    /**
     * Sets the key type for the security provider at STS service.
     * 
     * @param keyType the key type for the security provider at STS service.
     */
    public void setPrivateKeyType(String keyType) {
        this.privateKeyType = keyType;
    }

    /**
     * Returns the key alias for the security provider at STS service.
     * 
     * @return the key alias of the security provider at STS service.
     */
    public String getPrivateKeyAlias() {
        return privateKeyAlias;
    }
   
    /**
     * Sets the key alias for the security provider at STS service.
     * 
     * @param alias the key alias for the security provider at STS service.
     */
    public void setPrivateKeyAlias(String alias) {
        this.privateKeyAlias = alias;
    }

    /**
     * Returns the Public key alias for this provider's partner.
     * 
     * @return the Public key alias of the provider's partner.
     */
    public String getPublicKeyAlias() {
        return publicKeyAlias;
    }
   
    /**
     * Sets the Public key alias for this provider's partner.
     * 
     * @param alias the Public key alias for this provider's partner.
     */
    public void setPublicKeyAlias(String alias) {
        this.publicKeyAlias = alias;
    }
    
    /**
     * Returns STS Endpoint
     * @return the STS endpoint
     */
    public String getSTSEndpoint() {
        return stsEndpoint;
    }
    
    /**
     * Returns Kerberos Domain Controller Domain
     * @return Kerberos Domain Controller Domain
     */
     
    public String getKDCDomain() {
        return kerberosDomain;
    }
    
    /**
     * Sets Kerberos Domain Controller Domain
     * @param domain Kerberos Domain Controller Domain
     */
    public void setKDCDomain(String domain) {
        this.kerberosDomain = domain;
    }
    
    /**
     * Returns Kerberos Domain Controller Server.
     * @return Kerberos Domain Controller Server.
     */
    public String getKDCServer() {
        return kerberosDomainServer;
    }
    
    /**
     * Sets Kerberos Domain Controller Server
     * @param kdcServer Kerberos Domain Controller Server
     */
    public void setKDCServer(String kdcServer) {
        this.kerberosDomainServer = kdcServer;
    }
    
      
    /**
     * This method is used by the web services provider to get the key tab file.     
     * @return the keytab file.
     */
    public String getKeyTabFile() {
        return kerberosKeyTabFile;
    }
    
    /**
     * Sets the keytab file 
     * @param file the fully qualified file path
     */
    public void setKeyTabFile(String file) {
        this.kerberosKeyTabFile = file;
    }
    
    /**
     * Returns kerberos service principal
     * @return the kerberos service principal
     */
    public String getKerberosServicePrincipal() {
        return kerberosServicePrincipal;
    }
    
    /**
     * Sets kerberos service principal.
     * @param principal the kerberos service principal.
     */
    public void setKerberosServicePrincipal(String principal) {
        this.kerberosServicePrincipal = principal;
    }
    
    /**
     * Returns true if kerberos signature needs to be validated.
     * The signature validation is supported only with JDK6 onwards.
     * @return true if the signature validation needs to be validated.
     */
    public boolean isValidateKerberosSignature() {
        return isVerifyKrbSignature;
    }
    
    /**
     * Sets a boolean flag to enable or disable validate kerberos signature.
     * @param validate  boolean flag to enable or disable validate krb signature.
     */
    public void setValidateKerberosSignature(boolean validate) {
        this.isVerifyKrbSignature = validate;
    }    
    
    /**
     * Sets the user credentials list.
     * @param usercredentials list of <code>PasswordCredential</code>objects.
     */
    public void setUsers(List usercredentials) {
        this.usercredentials = usercredentials;
    }

    /**
     * Returns the list of <code>PasswordCredential</code>s of the user.
     *
     * @return the list of <code>PasswordCredential</code> objects.
     */
    public List getUsers() {
        return usercredentials;
    }
    
    /**
     * Returns encryption algorithm
     * @return the encryption algorithm
     */
    public String getEncryptionAlgorithm() {
        return encryptionAlgorithm;
    }
    /**
     * Sets the encryption algorithm
     * @param algorithm the encryption algorithm
     */
    public void setEncryptionAlgorithm(String algorithm) {
        this.encryptionAlgorithm = algorithm;
    }
    
    /**
     * Returns the encryption strength;
     * @return the encryption strength;
     */
    public int getEncryptionStrength() {
        return encryptionStrength;    
    }
    
    /**
     * Sets the encryption data strength.
     * @param encryptionStrength the encryption data strength.
     */
    public void setEncryptionStrength(int encryptionStrength) {
        this.encryptionStrength = encryptionStrength;
    }
    
    /**
     * Returns signing reference type.
     * @return the signing reference type.
     */
    public String getSigningRefType() {
        return signingRefType;
    }
    
    /**
     * Set signing reference type.
     * @param refType the signing reference type.
     */
    public void setSigningRefType(String refType) {
        this.signingRefType = refType;
    }
    
    /**
     * Returns authentication chain used for authenticating sts clients.
     * @return the authentication chain name.
     */
    public String getAuthenticationChain() {
        return authChain;
    }
    
    /**
     * Sets the authentication chain name.
     * @param authChain the authentication chain name.
     */
    public void setAuthenticationChain(String authChain) {
        this.authChain = authChain;
    }           
    /**
     * Returns true if the user name token replay is enabled.
     * @return true if the user name token replay is enabled.
     */
    public boolean isUserTokenDetectReplayEnabled() {
        return detectUserTokenReplay;    
    }
    
    /**
     * Enable or disable the detection of user token replay
     * @param enable true if the detection of user token replay is enabled.
     */
    public void setDetectUserTokenReplay(boolean enable) {
        this.detectUserTokenReplay = enable;
    }
    
    /**
     * Returns true if the message replay detection is enabled.
     * @return true if the message replay detection is enabled.
     */
    public boolean isMessageReplayDetectionEnabled() {
        return detectMessageReplay;
    }
    
    /**
     * Enable or disable the message replay detection.
     * @param enable true if the detection of the message replay is enabled.
     */
    public void setMessageReplayDetection(boolean enable) {
        this.detectMessageReplay = enable;
    }

    /**
     * Returns the list of signed elements.
     * @return the list of signed elements.
     */
    public List getSignedElements() {
        return signedElements;
    }

    /**
     * Sets the signed elements
     * @param signedElements the signed elements.
     */
    public void setSignedElements(List signedElements) {
        this.signedElements = signedElements;
    }
}
