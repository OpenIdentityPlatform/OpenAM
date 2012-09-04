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
 * $Id: TrustAuthorityConfig.java,v 1.14 2009/02/28 00:59:42 mrudul_uchil Exp $
 *
 */

package com.sun.identity.wss.provider;

import java.security.AccessController;
import java.util.List;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.common.SystemConfigurationUtil;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.security.AdminTokenAction;

/**
 * This abstract class <code>TrustAuthorityConfig</code> represents the 
 * configuration of a Trusted Authority entity.  
 * 
 * <p>The Trusted Authority configuration is used to register the provider
 * configuration information at a Trusted Authority.
 * 
 * <p>This class can be extended to define the trust authority config
 * such as Discovery client configuration, WS-Trust protocol based client 
 * (STS client) configuration.
 * 
 * <p>Pluggable implementation of this abstract class can choose to store this 
 * configuration in desired configuration store. This pluggable implementation
 * class can be configured in client's AMConfig.properties as value of 
 * "com.sun.identity.wss.discovery.config.plugin" property for Discovery client 
 * configuration and "com.sun.identity.wss.sts.config.plugin" property 
 * for STS client configuration.
 * 
 * Having obtained an instance of TrustAuthorityConfig, its methods can be 
 * called to create, delete, modify, retrieve WSS agent profile and 
 * configuration for Discovery client or STS client attributes 
 * (key /value pairs).
 * 
 * <p>All the static methods in this class are for the persistent 
 * operations.
 * @supported.all.api
 */

public abstract class TrustAuthorityConfig {

    protected String endpoint;
    protected String privateKeyAlias = null;
    protected String publicKeyAlias = null;
    protected String name;
    protected String type;
    protected List secMech = null;
    protected boolean isResponseSigned = false;
    protected boolean isResponseEncrypted = false;
    protected boolean isRequestSigned = true;
    protected boolean isRequestEncrypted = false;
    protected boolean isRequestHeaderEncrypted = false;
    protected List usercredentials = null;
    protected String encryptionAlgorithm = "DESede";
    protected int encryptionStrength = 0;
    protected String signingRefType = "DirectReference";
    
    private static Class discoveryConfigClass;
    private static Class stsConfigClass;
    private static Debug debug = ProviderUtils.debug;
    protected static SSOToken customAdminToken = null;

    /**
     * Property string for the web services discovery client configuration 
     * plugin.
    */
    public static final String WSS_DISCOVERY_CONFIG_PLUGIN =
        "com.sun.identity.wss.discovery.config.plugin";
    
    /**
     * Property string for the web services STS client configuration plugin.
     */
    public static final String WSS_STS_CONFIG_PLUGIN = 
        "com.sun.identity.wss.sts.config.plugin";
 
    /**
     * Trusted Authority client configuration type.
     */
    public static final String DISCOVERY_TRUST_AUTHORITY = "DiscoveryAgent";
    
    public static final String STS_TRUST_AUTHORITY = "STSAgent";

    /**
     * Returns the trust authority name.
     * @return the name of the trust authority.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets the trust authority name.
     * @param name the name of the trust authority.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the trust authority type.
     * @return the type of the trust authority.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the trust authority type.
     * @param type the type of the trust authority.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the trust authority end point.
     *
     * @return the endpoint of the trust authority. 
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * Sets the trust authority end point.
     *
     * @param endpoint the end point for the trust authority.
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Returns the key alias for this trust authority client.
     * 
     * @return the key alias of the trust authority client.
     */
    public String getKeyAlias() {
        return privateKeyAlias;
    }
   
    /**
     * Sets the key alias for this trust authority client.
     * 
     * @param alias the key alias for the trust authority client.
     */
    public void setKeyAlias(String alias) {
        this.privateKeyAlias = alias;
    }

    /**
     * Returns the Public key alias for this trust authority client's partner.
     * 
     * @return the Public key alias of the trust authority client's partner.
     */
    public String getPublicKeyAlias() {
        return publicKeyAlias;
    }
   
    /**
     * Sets the Public key alias for this trust authority client's partner.
     * 
     * @param alias the Public key alias for this trust authority client's 
     * partner.
     */
    public void setPublicKeyAlias(String alias) {
        this.publicKeyAlias = alias;
    }
    
    /**
     * Sets the list of security mechanisms that the trust authority supports.
     * @param secMech list of security mechanisms that the trust 
     *                authority supports.
     */
    public void setSecurityMechs(List secMech) {
        this.secMech = secMech;
    }
    
    /**
     * Returns the list of security mechanisms that the trust authority supports.
     */
    public List getSecurityMech() {
        return secMech;
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
     * Returns the encryption algorithm.
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
     * Sets the encryption data strength.
     * @param keyStrength the encryption data strength.
     */
    public void setEncryptionStrength(int keyStrength) {
        this.encryptionStrength = keyStrength;
    }
    
    /**
     * Initialize the trust authority.
     * @param name the name of the trust authority.
     * @param type the type of the trust authority.
     * @param ssoToken Single sign-on token.
     * @exception ProviderException if the initialization fails.
     */
    protected abstract void init(String name, String type, SSOToken ssoToken) 
        throws ProviderException;

    /**
     * Saves the trust authority configuration.
     * @exception ProviderException if the trust authority configuration 
     *            is unable to save.
     */
    protected abstract void store() throws ProviderException;

    /**
     * Deletes the trust authrority configuration.
     * @exception ProviderException
     */
    protected abstract void delete() throws ProviderException;

    /**
     * Returns the trust authority configuration object.
     *
     * @param name the name of the trust authority.
     * @param type the type of the trust authority. The type must have
     *         one of the following values.
     *         <p> {@link #DISCOVERY_TRUST_AUTHORITY}
     *         <p> {@link #STS_TRUST_AUTHORITY}
     * @exception ProviderException if any failure in 
     *                retrieving the trust authority configuration.
     */
    public static TrustAuthorityConfig getConfig(String name, String type)
        throws ProviderException {
        TrustAuthorityConfig config = null;

        if (DISCOVERY_TRUST_AUTHORITY.equals(type)) {
            config = getDiscoveryConfig();
            config.init(name, type, getAdminToken());
        } else  if(STS_TRUST_AUTHORITY.equals(type)) {
            config = getSTSConfig();
            config.init(name, type, getAdminToken());
        }else {
            throw new ProviderException(
               ProviderUtils.bundle.getString("unsupportedConfigType"));
        }
        return config;
    }

    /**
     * Saves the trust authority configuration.
     *
     * @param config the trust authority configuration.
     * @exception ProviderException if any failure in 
     *            saving the configuration.
     */
    public static void saveConfig(TrustAuthorityConfig config) 
        throws ProviderException {
        config.store();
    }

    /**
     * Deletes the trust authority configuration.
     *
     * @param name the name of the trust authority configuration.
     * @param type the type of the trust authority. The type must have
     *         one of the values.
     *         <p> {@link #DISCOVERY_TRUST_AUTHORITY}
     *         <p> {@link #STS_TRUST_AUTHORITY}
     * @exception ProviderException if any failure in 
     *            deleting the trust authority configuration.
     */
    public static void deleteConfig(String name, String type ) 
        throws ProviderException {
        TrustAuthorityConfig config = getConfig(name, type);
        config.delete();
    }

    private static DiscoveryConfig getDiscoveryConfig() 
        throws ProviderException {
        if (discoveryConfigClass == null) {
            String adapterName = SystemConfigurationUtil.getProperty(
                WSS_DISCOVERY_CONFIG_PLUGIN, 
                "com.sun.identity.wss.provider.plugins.DiscoveryAgent");
            try {
                discoveryConfigClass = Class.forName(adapterName);
            }  catch (Exception ex) {
                debug.error("TrustAuthorityConfig.getDiscoveryConfig: " +
                    " Failed in creating the discovery config class.");
                throw new ProviderException(ex.getMessage());
            }
        }
        try {
            return ((DiscoveryConfig) discoveryConfigClass.newInstance());
        } catch (Exception ex) {
            debug.error("TrustAuthorityConfig.getDiscoveryConfig: " +
                "Failed in initialization", ex);
            throw new ProviderException(ex.getMessage());
        }
    }
    
    private static STSConfig getSTSConfig() throws ProviderException {
        if (stsConfigClass == null) {
            String adapterName = SystemConfigurationUtil.getProperty(
                WSS_STS_CONFIG_PLUGIN, 
                "com.sun.identity.wss.provider.plugins.STSAgent");
            try {
                stsConfigClass = Class.forName(adapterName);
            }  catch (Exception ex) {
                debug.error("TrustAuthorityConfig.getSTSConfig: " +
                    " Failed in creating the STS config class.");
                throw new ProviderException(ex.getMessage());
            }
        }
        
        try {
            return ((STSConfig) stsConfigClass.newInstance());
        } catch (Exception ex) {
            debug.error("TrustAuthorityConfig.getSTSConfig: " +
                "Failed in initialization", ex);
            throw new ProviderException(ex.getMessage());
        }
    }

    private static SSOToken getAdminToken() throws ProviderException {
        if(customAdminToken != null) {
           return customAdminToken;
        }
        SSOToken adminToken = null;
        try {
            adminToken = (SSOToken) AccessController.doPrivileged(
                    AdminTokenAction.getInstance());
            
            if(adminToken != null) {
                if (!SSOTokenManager.getInstance().isValidToken(adminToken)) {
                    if (ProviderUtils.debug.messageEnabled()) {
                        ProviderUtils.debug.message("TrustAuthorityConfig." + 
                            "getAdminToken:" + "AdminTokenAction returned " + 
                            " expired or invalid token, trying again...");
                    }
                    adminToken = (SSOToken) AccessController.doPrivileged(
                            AdminTokenAction.getInstance());
                }
            }
        } catch (SSOException se) {
            ProviderUtils.debug.message(
                "TrustAuthorityConfig.getAdminToken: Trying second time..");
            adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        }
        
        return adminToken;
    }
    
    /**
     * Sets the admin token.
     * This admin token is required to be set if "create", "delete" or "save"
     * operations are invoked on this <code>TrustAuthorityConfig</code> object.
     * This admin token needs to be the valid SSOToken of the user who has
     * "Agent Administrator" privileges.
     * 
     * @param adminToken the agent admin token.
     */
    public void setAdminToken(SSOToken adminToken) {
        this.customAdminToken = adminToken;
    }
}
