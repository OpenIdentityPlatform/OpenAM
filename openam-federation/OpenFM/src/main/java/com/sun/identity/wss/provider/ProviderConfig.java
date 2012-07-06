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
 * $Id: ProviderConfig.java,v 1.31 2009/11/16 21:52:58 mallas Exp $
 *
 */
package com.sun.identity.wss.provider; 

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.AccessController;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;

import com.sun.identity.common.SystemConfigurationUtil;
import com.iplanet.services.util.Crypt;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.wss.security.SecurityMechanism;


/**
 * This abstract class <code>ProviderConfig</code> represents the Web Services
 * Server provider or the Web Services Client provider configuration.  
 * <p>Pluggable implementation of this abstract class can choose to store this 
 * configuration in desired configuration store. This pluggable implementation
 * class can be configured in client's AMConfig.properties as value of 
 * "com.sun.identity.wss.provider.config.plugin" property.
 * Having obtained an instance of ProviderConfig, its methods can be called to 
 * create, delete, modify, retrieve WSS agent profile and configuration for WSC 
 * and/or WSP attributes (key /value pairs).
 * 
 * <p>All the static methods in this class are for the persistent 
 * operations.
 * @supported.all.api
 */
public abstract class ProviderConfig {

     /**
      * Constant to define the web services client type.
      */
     public static final String WSC = "WSCAgent";

     /**
      * Constant to define the web services provider type.
      */
     public static final String WSP = "WSPAgent";
     
    /**
     * Property for the web services provider configuration plugin.
     */
    public static final String WSS_PROVIDER_CONFIG_PLUGIN =
         "com.sun.identity.wss.provider.config.plugin";
 
     protected List secMech = null;
     protected String serviceURI = null;
     protected String providerName = null; 
     protected String wspEndpoint = null;
     protected String wssProxyEndpoint = null;
     protected String providerType = null;
     protected KeyStore keyStore = null;
     protected String privateKeyAlias = null;
     protected String privateKeyType = null;
     protected String publicKeyAlias = null;
     protected boolean isResponseSigned = false;
     protected boolean isResponseEncrypted = false;
     protected boolean isRequestSigned = true;
     protected boolean isRequestEncrypted = false;
     protected boolean isRequestHeaderEncrypted = false;
     protected List trustAuthorities = null;
     protected String ksPasswd = null;
     protected String keyPasswd = null;
     protected String ksFile = null;
     protected Properties properties = new Properties();
     protected List usercredentials = null;
     protected String serviceType = null;
     protected boolean isDefaultKeyStore = false;
     protected boolean forceAuthn = false;
     protected boolean preserveSecHeaders = false;
     protected String authenticationChain = null;    
     protected TrustAuthorityConfig taconfig = null;
     protected Set samlAttributes = null;
     protected boolean includeMemberships = false;
     protected String nameIDMapper = null;
     protected String attributeNS = null;
     protected String kdcDomain = null;
     protected String kdcServer = null;
     protected String ticketCacheDir = null;
     protected String servicePrincipal = null;
     protected String keytabFile = null;
     protected boolean verifyKrbSignature = false;
     protected boolean usePassThroughToken = false;
     protected String tokenConversionType = null;
     protected String encryptionAlgorithm = "DESede";
     protected int encryptionStrength = 0;
     protected String signingRefType = "DirectReference";
     protected static SSOToken customAdminToken = null;
    
     protected boolean detectUserTokenReplay = true;
     protected boolean detectMessageReplay = true;
     protected String dnsClaim = null;
     protected List signedElements = new ArrayList();
     private static Class adapterClass;

    /**
     * Returns the list of security mechanims that the provider is configured.
     *
     * @return list of security mechanisms.
     */
    public List getSecurityMechanisms() {
         return secMech;
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
     * Returns the name of the Provider.
     *
     * @return the provider name.
     */
    public String getProviderName() {
        return providerName;
    }

    /**
     * Returns the value of the property.
     *
     * @param property the name of property for which value is being retrieved.
     * 
     * @return the value of the property.
     */
    public String getProperty(String property) {
        return properties.getProperty(property);
    }

    /**
     * Sets the value for the given property in Provider Configuration.
     *
     * @param property the name of the property being set.
     *
     * @param value the property value being set.
     */
    public void setProperty(String property, String value) {
        properties.put(property, value);
    }

    /**
     * Returns the endpoint of the web services provider.
     *
     * @return the endpoint of the web services provider.
     */
    public String getWSPEndpoint() {
        return wspEndpoint;        
    }

    /**
     * Sets the web services provider endpoint.
     *
     * @param endpoint the web services provider endpoint.
     */
    public void setWSPEndpoint(String endpoint) {
        this.wspEndpoint = endpoint;
    }

    /**
     * Returns the endpoint of the web services security proxy.
     *
     * @return the endpoint of the web services security proxy.
     */
    public String getWSSProxyEndpoint() {
        return wssProxyEndpoint;        
    }

    /**
     * Sets the web services security proxy endpoint.
     *
     * @param endpoint the web services security proxy endpoint.
     */
    public void setWSSProxyEndpoint(String endpoint) {
        this.wssProxyEndpoint = endpoint;
    }

    /**
     * Sets the service type.
     * @param serviceType the service type.
     */
    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    /**
     * Returns the service type.
     *
     * @return the service type.
     */
    public String getServiceType() {
        return serviceType;
    }

    /**
     * Sets the user credentials list.
     * @param usercredentials list of <code>PasswordCredential</code> objects.
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
     * Returns the provider type. It will be {@link #WSP} or {@link #WSC}
     *
     * @return the provider type.
     */
    public String getProviderType() {
        return providerType;
    }

    /**
     * Returns the provider JKS <code>KeyStore</code> 
     *
     * @return the JKS <code>KeyStore</code>
     */
    public KeyStore getKeyStore() {
        return keyStore;
    }

    /**
     * Returns the keystore file.
     *
     * @return the keystore file name.
     */
    public String getKeyStoreFile() {
        return ksFile;
    }

    /**
     * Returns the keystore password.
     *
     * @return the keystore password.
     */
    public String getKeyStorePassword() {
        return Crypt.decrypt(ksPasswd);
    }

    /**
     * Returns the keystore encrypted password.
     *
     * @return the keystore encrypted password.
     */
    public String getKeyStoreEncryptedPasswd() {
         return ksPasswd;
    }

    /**
     * Returns the key password in the keystore.
     *
     * @return the key password in the keystore.
     */
    public String getKeyPassword() {
        return Crypt.decrypt(keyPasswd);
    }

    /**
     * Returns the keystore encrypted password.
     *
     * @return the keystore encrypted password.
     */
    public String getKeyEncryptedPassword() {
        return keyPasswd;
    }

    /**
     * Sets the keystore for this provider.
     * 
     * @param fileName the provider key store fully qualified file name.
     *
     * @param keyStorePassword the password required to access the key 
     *         store file.
     *
     * @param keyPassword the password required to access the key from the
     *        keystore.
     *
     * @exception ProviderException if the key store file does not exist
     *        or an invalid password. 
     */
    public void setKeyStore(String fileName, 
             String keyStorePassword, String keyPassword)
      throws ProviderException {

        this.ksFile = fileName;
        this.ksPasswd = Crypt.encrypt(keyStorePassword);
        this.keyPasswd = Crypt.encrypt(keyPassword);
        try {
            File file = new File(fileName);
            if(file.exists()) {
               InputStream inputStream = new FileInputStream(fileName);
               keyStore = KeyStore.getInstance("JKS");
               keyStore.load(inputStream, keyStorePassword.toCharArray());
            }
        } catch (Exception ex) {
            ProviderUtils.debug.error("ProviderConfig.setKeyStore: Could not" +
                 "set the key store file information", ex);
            throw new ProviderException(ProviderUtils.bundle.getString(
            "invalidKeyStore"));
        }
    }

    /**
     * Sets the keystore for this provider.
     * 
     * @param keyStore the provider key store.
     *
     * @param password the password required to access the key store file.
     *
     */
    public void setKeyStore(KeyStore keyStore, String password) {
        this.keyStore = keyStore;
        this.ksPasswd = password;
    }

    /**
     * Returns the key type for this provider.
     * 
     * @return the key type of the provider.
     */
    public String getKeyType() {
        return privateKeyType;
    }
   
    /**
     * Sets the key type for this provider.
     * 
     * @param keyType the key type for this provider.
     */
    public void setKeyType(String keyType) {
        this.privateKeyType = keyType;
    }

    /**
     * Returns the key alias for this provider.
     * 
     * @return the key alias of the provider.
     */
    public String getKeyAlias() {
        return privateKeyAlias;
    }
   
    /**
     * Sets the key alias for this provider.
     * 
     * @param alias the key alias for this provider.
     */
    public void setKeyAlias(String alias) {
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
     * Returns true if the provider uses default keystore.
     * @return true if the provider uses default keystore.
     */
    public boolean useDefaultKeyStore() {
        return isDefaultKeyStore;
    }

    /**
     * Sets the provider to use the default keystore.
     * @param set boolean variable to enable or disable to use the default
     *            keystore.
     */
    public void setDefaultKeyStore(boolean set) {
        this.isDefaultKeyStore = set;
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
     * Returns Kerberos Domain Controller Domain
     * @return Kerberos Domain Controller Domain
     */
     
    public String getKDCDomain() {
        return kdcDomain;
    }
    
    /**
     * Sets Kerberos Domain Controller Domain
     * @param domain Kerberos Domain Controller Domain
     */
    public void setKDCDomain(String domain) {
        this.kdcDomain = domain;
    }
    
    /**
     * Returns Kerberos Domain Controller Server.
     * @return Kerberos Domain Controller Server.
     */
    public String getKDCServer() {
        return kdcServer;
    }
    
    /**
     * Sets Kerberos Domain Controller Server
     * @param kdcServer Kerberos Domain Controller Server
     */
    public void setKDCServer(String kdcServer) {
        this.kdcServer = kdcServer;
    }
    
    /**
     * This method is used by the web services client to get the kerberos
     * ticket cache directory.
     * @return the kerberos ticket cache dir
     */
    public String getKerberosTicketCacheDir() {
        return ticketCacheDir;
    }
    
    /**
     * Sets kerberos ticket cache dir.
     * @param cacheDir kerberos ticket cache dir
     */
    public void setKerberosTicketCacheDir(String cacheDir) {
        this.ticketCacheDir = cacheDir;
    }
    
    /**
     * This method is used by the web services provider to get the key tab file.     
     * @return the keytab file.
     */
    public String getKeyTabFile() {
        return keytabFile;
    }
    
    /**
     * Sets the keytab file 
     * @param file the fully qualified file path
     */
    public void setKeyTabFile(String file) {
        this.keytabFile = file;
    }
    
    /**
     * Returns kerberos service principal
     * @return the kerberos service principal
     */
    public String getKerberosServicePrincipal() {
        return servicePrincipal;
    }
    
    /**
     * Sets kerberos service principal.
     * @param principal the kerberos service principal.
     */
    public void setKerberosServicePrincipal(String principal) {
        this.servicePrincipal = principal;
    }
    
    /**
     * Returns true if kerberos signature needs to be validated.
     * The signature validation is supported only with JDK6 onwards.
     * @return true if the signature validation needs to be validated.
     */
    public boolean isValidateKerberosSignature() {
        return verifyKrbSignature;
    }
    
    /**
     * Sets a boolean flag to enable or disable validate kerberos signature.
     * @param validate boolean flag to enable or disable validate krb signature.
     */
    public void setValidateKerberosSignature(boolean validate) {
        this.verifyKrbSignature = validate;
    }

    /**
     * Returns the DNS claim name.
     * @return the DNS claim name.
     */
    public String getDNSClaim() {
        return dnsClaim;
    }

    /**
     * Sets the DNS claim name
     * @param dnsClaim the DNS claim name
     */
    public void setDNSClaim(String dnsClaim) {
        this.dnsClaim = dnsClaim;
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
  
    /**
     * Returns the provider's trusted authorities list.
     *
     * @return the list of the <code>TrustAuthorityConfig</code>s. 
     */
    public TrustAuthorityConfig getTrustAuthorityConfig() {
        return taconfig;
    }
    
    /**
     * Sets the trusted authority configurations.
     * 
     * @param taconfig instance of the <code>TrustAuthorityConfig</code>.
     */
    public void setTrustAuthorityConfig(TrustAuthorityConfig taconfig) {
        this.taconfig = taconfig;
    }

    /**
     * Checks if the response needs to be signed or not.
     *
     * @return true if the response needs to be signed.
     */
    public boolean isResponseSignEnabled() {
        return isResponseSigned;
    }

    /**
     * Sets the response sign enable flag.
     *
     * @param enable enables the response signing.
     */
    public void setResponseSignEnabled(boolean enable) {
         isResponseSigned = enable;
    }
    
    /**
     * Checks if the response needs to be encrypted or not.
     *
     * @return true if the response needs to be encrypted.
     */
    public boolean isResponseEncryptEnabled() {
        return isResponseEncrypted;
    }

    /**
     * Sets the response encrypt enable flag.
     *
     * @param enable enables the response encryption.
     */
    public void setResponseEncryptEnabled(boolean enable) {
         isResponseEncrypted = enable;
    }
    
    /**
     * Checks if the request needs to be signed or not.
     *
     * @return true if the request needs to be signed.
     */
    public boolean isRequestSignEnabled() {
        return isRequestSigned;
    }

    /**
     * Sets the request sign enable flag.
     *
     * @param enable enables the request signing.
     */
    public void setRequestSignEnabled(boolean enable) {
         isRequestSigned = enable;
    }
    
    /**
     * Checks if the request needs to be encrypted or not.
     *
     * @return true if the request needs to be encrypted.
     */
    public boolean isRequestEncryptEnabled() {
        return isRequestEncrypted;
    }

    /**
     * Sets the request encrypt enable flag.
     *
     * @param enable enables the request encryption.
     */
    public void setRequestEncryptEnabled(boolean enable) {
         isRequestEncrypted = enable;
    }

    /**
     * Checks if the request header needs to be encrypted or not.
     *
     * @return true if the request header needs to be encrypted.
     */
    public boolean isRequestHeaderEncryptEnabled() {
        return isRequestHeaderEncrypted;
    }

    /**
     * Sets the request header encrypt enable flag.
     *
     * @param enable enables the request header encryption.
     */
    public void setRequestHeaderEncryptEnabled(boolean enable) {
        isRequestHeaderEncrypted = enable;
    }
    
    /**
     * Returns true if the user force authentication is enabled.
     * @return true if the user force authentication is enabled.
     */
    public boolean forceUserAuthentication() {
        return forceAuthn;
    }
    
    /**
     * Sets the user force authentication attribute.
     * @param forceAuthn the user force authentication attribute.
     */
    public void setForceUserAuthentication(boolean forceAuthn) {
        this.forceAuthn = forceAuthn;
    }

    /**
     * Returns true if security header needs to be preserved.
     * @return true if the security header needs to be preserved.
     */
    public boolean preserveSecurityHeader() {
        return preserveSecHeaders;
    }

    /**
     * Sets if security header needs to be preserved.
     * @param preserve value to be set, true if the security header needs 
     *    to be preserved, false otherwise.
     */
    public void setPreserveSecurityHeader(boolean preserve) {
        this.preserveSecHeaders = preserve;
    }

    /**
     * Returns the authentication chain mechanism to be used. This method
     * is used only by the WSP configuration.
     *
     * @return the name of the authentication chain mechanism.
     */
    public String getAuthenticationChain() {
        return authenticationChain;
    }
    /**
     * Sets the authentication chain mechanism. This method is used only by
     * the WSP configuration.
     * @param authenticationChain the name of the authentication chain
     *        mechanism.
     */
    public void setAuthenticationChain(String authenticationChain) {
        this.authenticationChain = authenticationChain;
    }
    
    /**
     * Returns true if passthrough security token needs to be used.
     * This is valid for a proxy web services client.
     * @return true if passthrough security token needs to be used.
     */
    public boolean usePassThroughSecurityToken() {
        return usePassThroughToken;
    }
    
    /**
     * Sets if passthrough security token needs to be used
     * This is valid for a proxy web services client.
     * @param usepassthrough flag to if the wsc needs to use passthrough
     *        security token.     
     */
    public void setPassThroughSecurityToken(boolean usepassthrough) {
        this.usePassThroughToken = usepassthrough;
    }
    
    /**
     * Returns the type of the token that needs to be converted to.
     * This method is used by the web service providers to convert a
     * SAMLToken to the desired token type.
     * @return the type of the token that needs to be converted to.
     */
    public String getTokenConversionType() {        
        return tokenConversionType;
    }
    
    /**
     * Sets the type of the token that needs to be converted to.     
     * This method is used by the web service providers to convert a
     * SAMLToken to the desired token type.
     * @param tokenType the type of the token that needs to be converted to.
     */
    public void setTokenConversionType(String tokenType) {
        this.tokenConversionType = tokenType;
    }
    
    /**
     * Returns signing reference type.
     * @return the signing reference type.     
     */
    public String getSigningRefType() {
        return signingRefType;
    }
    
    /**
     * Sets the signing reference type.
     * @param refType the signing reference type.
     */
    public void setSigningRefType(String refType) {
        this.signingRefType = refType;
    }
    
    /**
     * Returns the encryption algorithm
     * @return the encryption algorithm
     */
    public String getEncryptionAlgorithm() {
        return encryptionAlgorithm;
    }
    
    /**
     * Sets the encryption algorithm.
     * @param encAlg the encryption algorithm.
     */
    public void setEncryptionAlgorithm(String encAlg) {
        this.encryptionAlgorithm = encAlg;
    }
    
    /**
     * Returns the encryption data strength.
     * @return the encryption data strength.
     */
    public int getEncryptionStrength() {
        return encryptionStrength;
    }
    
    /**
     * Sets the encryption data strength.     * 
     * @param keyStrength the encryption data strength.
     */
    public void setEncryptionStrength(int keyStrength) {
        this.encryptionStrength = keyStrength;
    }
    
    /**
     * Stores the provider configuration.
     *
     * @exception ProviderException if there is any failure.
     */
    protected abstract void store() throws ProviderException;

    /**
     * Deletes the provider configuration.
     *
     * @exception ProviderException if there is any failure.
     */
    protected abstract void delete() throws ProviderException;

    /**
     * Checks if the provider configuration exists.
     *
     * @return true if the provider exists.
     */
     protected abstract boolean isExists();

    /**
     * Initializes the provider.
     *
     * @param providerName the provider name.
     *
     * @param providerType the provider type.
     *
     * @param token Single Sign-on token.
     * 
     * @param isEndPoint Boolean flag indicating whether provider needs to be 
     * searched based on its end point value.
     *
     * @exception ProviderException if there is any failure.
     */
    protected abstract void init(String providerName, 
        String providerType, SSOToken token, boolean isEndPoint) 
        throws ProviderException;

    /**
     * Saves the Provider in the configuration repository.
     *
     * @param config the provider configuration.
     *
     * @exception ProviderException if the creation is failed.
     */
    public static void saveProvider(ProviderConfig config)
                  throws ProviderException {
        config.store();
    }

    /**
     * Returns the provider configuration for a given provider name.
     *
     * @param providerName the provider name.
     *
     * @param providerType the provider type.
     *
     * @exception ProviderException if unable to retrieve.
     */
    public static ProviderConfig getProvider(
           String providerName, String providerType) throws ProviderException {

         ProviderConfig pc = getConfigAdapter(); 
         SSOToken adminToken = getAdminToken();
         pc.init(providerName, providerType, adminToken, false);
         return pc; 
    }
    
    /**
     * Returns the provider configuration for a given provider name.     
     * @param providerName the provider name.    
     * @param providerType the provider type.
     * @param initialize if set to false the provider configuration will not
     *        be retrieved from the persistent store and returns just the
     *        memory image of the provider configuration. Also if set to
     *        false the provider configuration can not be saved persistently
     *        using {@link #store()}.
     * @exception ProviderException if unable to retrieve.
     */
    public static ProviderConfig getProvider(
           String providerName, String providerType, boolean initialize)
           throws ProviderException {
           
         if(!initialize) {
            return getConfigAdapter();
         }
         return getProvider(providerName, providerType);
    }
    
    /**
     * Returns the provider configuration for a given end point     
     *
     * @param endpoint the end point is the search string to retrieve the
     *        provider configuration.
     *
     * @param providerType the provider type.
     *          
     * @exception ProviderException if unable to retrieve.
     */
    public static ProviderConfig getProviderByEndpoint(
        String endpoint, String providerType) 
        throws ProviderException {

         ProviderConfig pc = getConfigAdapter(); 
         SSOToken adminToken = getAdminToken();
         pc.init(endpoint, providerType, adminToken, true);
         return pc; 
    }

    /**
     * Checks if the provider of given type does exists.
     * 
     * @param providerName the name of the provider.
     *
     * @param providerType type of the provider.
     *
     * @return true if the provider exists with a given name and type.
     */
    public static boolean isProviderExists(String providerName, 
                  String providerType) {
        try {
            ProviderConfig config = getProvider(providerName, providerType);
            return config.isExists();
        } catch (ProviderException pe) {
            ProviderUtils.debug.error("ProviderConfig.isProviderExists:: " +
            "Provider Exception ", pe);
            return false;
        }
    }
    
    /**
     * Checks if the provider of given type does exists.
     * 
     * @param providerName the name of the provider.
     *
     * @param providerType type of the provider.
     * 
     * @param isEndPoint flag to indicate check/search based on WSP end point.
     *
     * @return true if the provider exists with a given name and type.
     */
    public static boolean isProviderExists(String providerName, 
                  String providerType, boolean isEndPoint) {
        try {
            ProviderConfig config = getProviderByEndpoint(
                    providerName, providerType);
            return config.isExists();
        } catch (ProviderException pe) {
            ProviderUtils.debug.error("ProviderConfig.isProviderExists:: " +
            "Provider Exception ", pe);
            return false;
        }
    }

    /**
     * Removes the provider configuration.
     * 
     * @param providerName the name of the provider.
     * 
     * @param providerType the type of the provider.
     * 
     * @exception ProviderException if any failure.
     */
    public static void deleteProvider(
           String providerName, String providerType) throws ProviderException {

        ProviderConfig pc = getConfigAdapter();
        pc.init(providerName, providerType, getAdminToken(), false);
        pc.delete();
    }

    /**
     * Returns the list of all available security mechanism objects.
     *
     * @return the list of <code>SecurityMechanism</code> objects.
     */ 
    public static List getAllSupportedSecurityMech() {
        List list = new ArrayList();
        list.add(SecurityMechanism.WSS_NULL_SAML_SV);
        list.add(SecurityMechanism.WSS_TLS_SAML_SV);
        list.add(SecurityMechanism.WSS_CLIENT_TLS_SAML_SV);
        list.add(SecurityMechanism.WSS_NULL_SAML_HK);
        list.add(SecurityMechanism.WSS_TLS_SAML_HK);
        list.add(SecurityMechanism.WSS_CLIENT_TLS_SAML_HK);
        list.add(SecurityMechanism.WSS_NULL_X509_TOKEN);
        list.add(SecurityMechanism.WSS_TLS_X509_TOKEN);
        list.add(SecurityMechanism.WSS_CLIENT_TLS_X509_TOKEN);
        list.add(SecurityMechanism.WSS_NULL_USERNAME_TOKEN);
        list.add(SecurityMechanism.WSS_TLS_USERNAME_TOKEN);
        list.add(SecurityMechanism.WSS_CLIENT_TLS_USERNAME_TOKEN);
        list.add(SecurityMechanism.WSS_NULL_SAML2_SV);
        list.add(SecurityMechanism.WSS_TLS_SAML2_SV);
        list.add(SecurityMechanism.WSS_CLIENT_TLS_SAML2_SV);
        list.add(SecurityMechanism.WSS_NULL_SAML2_HK);
        list.add(SecurityMechanism.WSS_TLS_SAML2_HK);
        list.add(SecurityMechanism.WSS_CLIENT_TLS_SAML2_HK);
        list.add(SecurityMechanism.WSS_NULL_ANONYMOUS);
        list.add(SecurityMechanism.WSS_TLS_ANONYMOUS);
        list.add(SecurityMechanism.WSS_CLIENT_TLS_ANONYMOUS);
        list.add(SecurityMechanism.WSS_NULL_USERNAME_TOKEN_PLAIN);
        list.add(SecurityMechanism.WSS_TLS_USERNAME_TOKEN_PLAIN);
        list.add(SecurityMechanism.WSS_CLIENT_TLS_USERNAME_TOKEN_PLAIN);
        list.add(SecurityMechanism.WSS_NULL_KERBEROS_TOKEN);
        list.add(SecurityMechanism.WSS_TLS_KERBEROS_TOKEN);
        list.add(SecurityMechanism.WSS_CLIENT_TLS_KERBEROS_TOKEN);
        list.add(SecurityMechanism.STS_SECURITY);
        return list;
    }

    /**
     * Returns the list of message level security mechanism objects.
     *
     * @return the list of message level <code>SecurityMechanism</code> objects.
     */
    public static List getAllMessageLevelSecurityMech() {
        List list = new ArrayList();
        list.add(SecurityMechanism.WSS_NULL_SAML_SV);
        list.add(SecurityMechanism.WSS_NULL_SAML_HK);
        list.add(SecurityMechanism.WSS_NULL_X509_TOKEN);
        list.add(SecurityMechanism.WSS_NULL_USERNAME_TOKEN);
        list.add(SecurityMechanism.WSS_NULL_SAML2_SV);
        list.add(SecurityMechanism.WSS_NULL_SAML2_HK);
        list.add(SecurityMechanism.WSS_NULL_ANONYMOUS);
        list.add(SecurityMechanism.WSS_NULL_USERNAME_TOKEN_PLAIN);
        list.add(SecurityMechanism.WSS_NULL_KERBEROS_TOKEN);
        list.add(SecurityMechanism.STS_SECURITY);
        return list;
    }

    private static ProviderConfig getConfigAdapter() throws ProviderException {
        if (adapterClass == null) {
            String adapterName =   SystemConfigurationUtil.getProperty(
                WSS_PROVIDER_CONFIG_PLUGIN, 
                "com.sun.identity.wss.provider.plugins.AgentProvider");
            try {
                adapterClass = Class.forName(adapterName);
            } catch (Exception ex) {
                 ProviderUtils.debug.error("ProviderConfig.getConfigAdapter: " +
                     "Failed in obtaining class", ex);
                 throw new ProviderException(
                     ProviderUtils.bundle.getString("initializationFailed"));
            }
        }
        try {
            return ((ProviderConfig) adapterClass.newInstance());
        } catch (Exception ex) {
             ProviderUtils.debug.error("ProviderConfig.getConfigAdapter: " +
                 "Failed in initialization", ex);
             throw new ProviderException(
                 ProviderUtils.bundle.getString("initializationFailed"));
        }
    }

    private static SSOToken getAdminToken() throws ProviderException {
        if (customAdminToken != null) {
            return customAdminToken;
        }
        SSOToken adminToken = null;
        try {
            adminToken = (SSOToken) AccessController.doPrivileged(
                    AdminTokenAction.getInstance());
            
            if(adminToken != null) {
                if (!SSOTokenManager.getInstance().isValidToken(adminToken)) {
                    if (ProviderUtils.debug.messageEnabled()) {
                        ProviderUtils.debug.message("ProviderConfig." + 
                            "getAdminToken: AdminTokenAction returned " + 
                            "expired or invalid token, trying again...");
                    }
                    adminToken = (SSOToken) AccessController.doPrivileged(
                            AdminTokenAction.getInstance());
                }
            }
        } catch (SSOException se) {
            ProviderUtils.debug.message("ProviderConfig.getAdminToken:: " +
                   "Trying second time ....");
            adminToken = (SSOToken) AccessController.doPrivileged(
                   AdminTokenAction.getInstance());            
        }
        return adminToken;
    }
    
    /**
     * Sets the admin token.
     * This admin token is required to be set if "create", "delete" or "save"
     * operations are invoked on this <code>ProviderConfig</code> object.
     * This admin token needs to be the valid SSOToken of the user who has
     * "Agent Administrator" privileges.
     * 
     * @param adminToken the agent admin token.
     */
    public void setAdminToken(SSOToken adminToken) {
        this.customAdminToken = adminToken;
    }
}
