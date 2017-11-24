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
 * $Id: STSConfig.java,v 1.10 2009/11/16 21:52:58 mallas Exp $
 *
 */

/*
 * Portions Copyright 2013 ForgeRock AS
 */
package com.sun.identity.wss.provider;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import com.sun.identity.wss.sts.STSConstants;

/**
 * This abstract class <code>STSConfig</code> represents the
 * configuration of a Security Token Service client entity. It extends
 * <code>TrustAuthorityConfig</code>.
 *
 * <p>This class can be extended to define the trust authority config
 * which is WS-Trust protocol based client (STS client) configuration.
 *
 * <p>Pluggable implementation of this abstract class can choose to store this
 * configuration in desired configuration store. This pluggable implementation
 * class can be configured in client's AMConfig.properties as value of
 * "com.sun.identity.wss.sts.config.plugin" property
 * for STS client configuration.
 *
 * <p>All the static methods in this class are for the persistent
 * operations.
 *
 * @supported.all.api
 */
public abstract class STSConfig extends TrustAuthorityConfig {
    
    protected String mexEndpoint = null;
    protected String stsConfigName = null;
    protected String kdcDomain = null;
    protected String kdcServer = null;
    protected String ticketCacheDir = null;
    protected String servicePrincipal = null;
    protected String protocolVersion = "1.0";
    protected Set samlAttributes = null;
    protected boolean includeMemberships = false;
    protected String nameIDMapper = null;
    protected String attributeNS = null;
    protected String keyType = STSConstants.PUBLIC_KEY;
    protected List<String> requestedClaims = new ArrayList();
    protected String dnsClaim = null;
    protected List signedElements = new ArrayList();
    
    /** Creates a new instance of STSConfig */
    public STSConfig() {
    }
    
    /**
     * Returns STS Mex endpoint.
     * @return STS Mex endpoint
     */
    public String getMexEndpoint() {
        return mexEndpoint;
    }         
    
    /**
     * Sets STS Mex endpoint.
     * @param mexEndpoint STS Mex endpoint
     *
     */
    public void setMexEndpoint(String mexEndpoint) {
        this.mexEndpoint = mexEndpoint;
    }
    
    /**
     * Returns the keytype. Example of keytype are symmetric or asymmetric
     * @return the keytype.
     */
    public String getKeyType() {
        return keyType;
    }
    
    /**
     * Sets the keytype
     * @param keyType
     */
    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }

    /**
     * Returns STS configuration name.
     * @return STS configuration name
     */
    public String getSTSConfigName() {
        return stsConfigName;
    }         
    
    /**
     * Sets STS configuration name.
     * @param stsConfigName STS configuration name
     *
     */
    public void setSTSConfigName(String stsConfigName) {
        this.stsConfigName = stsConfigName;
    }
    
    /**
     * Returns Kerberos Domain Controller Domain.
     * @return Kerberos Domain Controller Domain
     */
     
    public String getKDCDomain() {
        return kdcDomain;
    }
    
    /**
     * Sets Kerberos Domain Controller Domain.
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
     * Sets Kerberos Domain Controller Server.
     * @param kdcServer Kerberos Domain Controller Server
     */
    public void setKDCServer(String kdcServer) {
        this.kdcServer = kdcServer;
    }
    
    /**
     * Returns the kerberos ticket cache directory.
     * This method is used by the web services client to get the kerberos
     * ticket cache directory.
     * @return kerberos ticket cache dir
     */
    public String getKerberosTicketCacheDir() {
        return ticketCacheDir;
    }
    
    /**
     * Sets kerberos ticket cache directory.
     * @param cacheDir kerberos ticket cache dir
     */
    public void setKerberosTicketCacheDir(String cacheDir) {
        this.ticketCacheDir = cacheDir;
    }
    
    /**
     * Returns kerberos service principal.
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
     * Returns the protocol version.
     * @return the protocol version
     */
    public String getProtocolVersion() {
        return protocolVersion;
    }
    /**
     * Sets the protocol version.
     * @param version the protocol version.
     */
    public void setProtocolVersion(String version) {
        this.protocolVersion = version;
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
     * Returns the list of requested claims
     * @return the list of requested claims.
     */
    public List getRequestedClaims() {
        return requestedClaims;        
    }
    
    /**
     * Sets the list of requested claims
     * @param requestedClaims the list of requested claims.
     */
    public void setRequestedClaims(List requestedClaims) {
        this.requestedClaims  = requestedClaims;
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
}
