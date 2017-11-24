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
 * $Id: FAMSTSConfiguration.java,v 1.12 2009/11/16 21:52:59 mallas Exp $
 *
 */


package com.sun.identity.wss.sts.config;

import com.sun.xml.ws.api.security.trust.config.STSConfiguration;
import com.sun.xml.ws.api.security.trust.config.TrustSPMetadata;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.StringTokenizer;
import javax.security.auth.callback.CallbackHandler;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;

import com.sun.identity.wss.sts.STSUtils;
import com.sun.identity.wss.security.PasswordCredential;

import com.sun.identity.plugin.configuration.ConfigurationActionEvent;
import com.sun.identity.plugin.configuration.ConfigurationException;
import com.sun.identity.plugin.configuration.ConfigurationInstance;
import com.sun.identity.plugin.configuration.ConfigurationListener;
import com.sun.identity.plugin.configuration.ConfigurationManager;

public class FAMSTSConfiguration implements 
    STSConfiguration, ConfigurationListener {

    private static Map<String, TrustSPMetadata> spMap = 
        new HashMap<String, TrustSPMetadata>();
    private static String type;
    private static String issuer;
    private static boolean encryptIssuedToken = false;
    private static boolean encryptIssuedKey = true;
    private static long issuedTokenTimeout;
    private static String stsEndpoint;
    private static String certAlias;
    private static String clientUserToken;
    private static List secMech = null;
    private static boolean isResponseSign = false;
    private static boolean isResponseEncrypt = false;
    private static boolean isRequestSign = false;
    private static boolean isRequestEncrypt = false;
    private static boolean isRequestHeaderEncrypt = false;
    private static String privateKeyType;
    private static String privateKeyAlias;
    private static String publicKeyAlias;
    private static String kerberosDomainServer;
    private static String kerberosDomain;
    private static String kerberosServicePrincipal;
    private static String kerberosKeyTabFile;
    private static boolean isVerifyKrbSignature = false;
    private static Set samlAttributes = null;
    private static boolean includeMemberships = false;
    private static String nameIDMapper = null;
    private static String attributeNS = null; 
    private static List usercredentials = null;
    private static String encryptionAlgorithm = null;
    private static int encryptionStrength = 0;
    private static String signingRefType = null;
    private static String authChain = null;    
    private static boolean detectUserTokenReplay = true;
    private static boolean detectMessageReplay = true;
    private static List signedElements = null;
    
    private CallbackHandler callbackHandler;
    
    private Map<String, Object> otherOptions = new HashMap<String, Object>();
    private static Set trustedIssuers = null;
    private static Set trustedIPAddresses = null;

    static final String CONFIG_NAME = "STS_CONFIG";
    static final String SERVICE_NAME = "sunFAMSTSService";

    static final String ISSUER = "stsIssuer";
    static final String END_POINT = "stsEndPoint";
    static final String ENCRYPT_ISSUED_KEY = "stsEncryptIssuedKey";
    static final String ENCRYPT_ISSUED_TOKEN = "stsEncryptIssuedToken";
    static final String LIFE_TIME = "stsLifetime";
    static final String TOKEN_IMPL_CLASS = "stsTokenImplClass";
    static final String CERT_ALIAS = "stsCertAlias";
    
    private static final String TRUSTED_ISSUERS = "trustedIssuers";
    private static final String TRUSTED_IP_ADDRESSES = "trustedIPAddresses";
    static final String CLIENT_USER_TOKEN = 
        "com.sun.identity.wss.sts.clientusertoken";
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
    static final String SAML_ATTRIBUTE_MAPPING = 
                                 "SAMLAttributeMapping";
    static final String INCLUDE_MEMBERSHIPS = "includeMemberships";
    static final String SAML_ATTRIBUTE_NS = "AttributeNamespace";
    static final String NAMEID_MAPPER = "NameIDMapper";
    static final String ENCRYPTION_ALGORITHM = "EncryptionAlgorithm";
    static final String ENCRYPTION_STRENGTH = "EncryptionStrength";
    static final String SIGNING_REF_TYPE = "SigningRefType";
    static final String AUTHENTICATION_CHAIN = "AuthenticationChain";    
    static final String USER_TOKEN_DETECT_REPLAY = "DetectUserTokenReplay";
    static final String MESSAGE_REPLAY_DETECTION = "DetectMessageReplay";
    static final String SIGNED_ELEMENTS = "SignedElements";

    private static Debug debug = STSUtils.debug;
    static ConfigurationInstance ci = null;

    static {
        try {
            ci = ConfigurationManager.getConfigurationInstance(CONFIG_NAME);
            ci.addListener(new FAMSTSConfiguration());
            setValues();
        } catch (ConfigurationException ce) {
             debug.error("FAMSTSConfiguration.static:", ce);
        }
    }

    /**
     * Default Constructor.
     */
    public FAMSTSConfiguration() {
    }

    /**
     * This method will be invoked when a component's 
     * configuration data has been changed. The parameters componentName,
     * realm and configName denotes the component name,
     * organization and configuration instance name that are changed 
     * respectively.
     *
     * @param e Configuration action event, like ADDED, DELETED, MODIFIED etc.
     */
    public void configChanged(ConfigurationActionEvent e) {
        if (debug.messageEnabled()) {
            debug.message("FAMSTSConfiguration: configChanged");
        }
        setValues();
    }

    /**
     * This method reads values from service schema.
     */
    static private void setValues() {
        String classMethod = "FAMSTSConfiguration.setValues:";
        Map attrMap = null;
        try {
            attrMap = ci.getConfiguration(null, null);
        } catch (ConfigurationException ce) {
            debug.error(classMethod, ce);
            return;
        }
        
        Set values = (Set)attrMap.get(ISSUER);
        if (values != null && !values.isEmpty()) {
            issuer = (String)values.iterator().next();
        }

        values = (Set)attrMap.get(END_POINT);
        if (values != null && !values.isEmpty()) {
            stsEndpoint = (String)values.iterator().next();
        }

        values = (Set)attrMap.get(ENCRYPT_ISSUED_KEY);
        if (values != null && !values.isEmpty()) {
            encryptIssuedKey = 
                Boolean.valueOf((String)values.iterator().next())
                .booleanValue();
        }

        values = (Set)attrMap.get(ENCRYPT_ISSUED_TOKEN);
        if (values != null && !values.isEmpty()) {
            encryptIssuedToken = 
                Boolean.valueOf((String)values.iterator().next())
                .booleanValue();
        }

        values = (Set)attrMap.get(LIFE_TIME);
        if (values != null && !values.isEmpty()) {
            issuedTokenTimeout = 
                Long.valueOf((String)values.iterator().next())
                .longValue();
        }

        values = (Set)attrMap.get(TOKEN_IMPL_CLASS);
        if (values != null && !values.isEmpty()) {
            type = (String)values.iterator().next();
        }

        values = (Set)attrMap.get(CERT_ALIAS);
        if (values != null && !values.isEmpty()) {
            certAlias = (String)values.iterator().next();
        }
        
        trustedIssuers = (Set)attrMap.get(TRUSTED_ISSUERS);
        trustedIPAddresses = (Set)attrMap.get(TRUSTED_IP_ADDRESSES);
        
        values = (Set)attrMap.get(CLIENT_USER_TOKEN);
        if (values != null && !values.isEmpty()) {
            clientUserToken = (String)values.iterator().next();
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
        
        samlAttributes = (Set)attrMap.get(SAML_ATTRIBUTE_MAPPING);
        
        values = (Set)attrMap.get(SAML_ATTRIBUTE_NS);
        if (values != null && !values.isEmpty()) {
            attributeNS = (String)values.iterator().next();
        }

        values = (Set)attrMap.get(NAMEID_MAPPER);
        if (values != null && !values.isEmpty()) {
            nameIDMapper = (String)values.iterator().next();
        }
        
        values = (Set)attrMap.get(INCLUDE_MEMBERSHIPS);
        if (values != null && !values.isEmpty()) {
            includeMemberships = 
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
        values = (Set)attrMap.get(MESSAGE_REPLAY_DETECTION);
        if (values != null && !values.isEmpty()) {
            detectMessageReplay = 
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
    
    public void addTrustSPMetadata(final TrustSPMetadata data, 
                                   final String spEndpoint){
        spMap.put(spEndpoint, data);
    }
    
    public TrustSPMetadata getTrustSPMetadata(final String spEndpoint){

        FAMTrustSPMetadata data = new FAMTrustSPMetadata(spEndpoint);
        spMap.put(spEndpoint, data);

        return (TrustSPMetadata)spMap.get(spEndpoint);
    }
    
    public Set getTrustedIssuers() {
        return trustedIssuers;
    }
    
    public Set getTrustedIPAddresses() {
        return trustedIPAddresses;
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
      
    public void setEncryptIssuedToken(boolean encryptIssuedToken){
        this.encryptIssuedToken = encryptIssuedToken;
    }
    
    public boolean getEncryptIssuedToken(){
        return this.encryptIssuedToken;
    }
        
    public void setEncryptIssuedKey(boolean encryptIssuedKey){
        this.encryptIssuedKey = encryptIssuedKey;
    }
    
    public boolean getEncryptIssuedKey(){
        return this.encryptIssuedKey;
    }
        
    public void setIssuedTokenTimeout(long issuedTokenTimeout){
        this.issuedTokenTimeout = issuedTokenTimeout;
    }
    
    public long getIssuedTokenTimeout(){
        return this.issuedTokenTimeout;
    }
    
    public void setCallbackHandler(CallbackHandler callbackHandler){
        this.callbackHandler = callbackHandler;
    }
    
    public CallbackHandler getCallbackHandler(){
        return new FAMCallbackHandler(this.certAlias);
    }
    
    public void setClientUserTokenClass(String clientUserTokenClass){
        this.clientUserToken = clientUserTokenClass;
    }
        
    public String getClientUserTokenClass(){
        return this.clientUserToken;
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
    
    public Map<String, Object> getOtherOptions(){
        return this.otherOptions;
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
     * Returns the SAML Attribute Mapping list. This method is used by the
     * WSP configuration when enabled for SAML.
     */
    public Set getSAMLAttributeMapping() {
        return samlAttributes;
    }

    /**
     * Sets the list of SAML attribute mappings. This method is used by the
     * WSP configuration when enabled for SAML.
     * @param attributeMap the list of SAML attribute mapping
     */
    public void setSAMLAttributeMapping(Set attributeMap) {
        this.samlAttributes = attributeMap;
    }

    /**
     * Checks if the memberships should be included in the SAML attribute
     * mapping.
     * @return true if the  memberships are included.
     */
    public boolean shouldIncludeMemberships() {
        return includeMemberships;
    }

    /**
     * Sets a flag to include memberships for SAML attribute mapping.
     * @param include boolean flag to indicate if the memberships needs to 
     *                be included.
     */
    public void setIncludeMemberships(boolean include) {
        this.includeMemberships = include;
    }

    /**
     * Returns the NameID mapper class
     * @return returns the nameid mapper class.
     */
    public String getNameIDMapper() {
        return nameIDMapper;
    }

    /**
     * Sets the NameID Mapper class.
     * @param nameIDMapper NameID Mapper class.
     */
    public void setNameIDMapper(String nameIDMapper){
        this.nameIDMapper = nameIDMapper;
    }

    /**
     * Returns SAML attribute namespace.
     * @return returns SAML attribute namespace.
     */
    public String getSAMLAttributeNamespace() {
        return attributeNS;
    }

    /**
     * Sets SAML attribute namespace.
     * @param attributeNS SAML attribute namespace.
     */
    public void setSAMLAttributeNamespace(String attributeNS) {
        this.attributeNS = attributeNS;
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