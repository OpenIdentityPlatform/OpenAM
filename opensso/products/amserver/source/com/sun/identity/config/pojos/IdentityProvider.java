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
 * $Id: IdentityProvider.java,v 1.2 2008/06/25 05:42:36 qcheng Exp $
 *
 */
package com.sun.identity.config.pojos;

/**
 * @author jperez
 */
public class IdentityProvider extends Provider{

    private int identityProviderId;
    private boolean incomingAssertionsSignature;
    private String serviceProviderMetadataUrl;
    private String serviceProviderMetadataType;
    private boolean serviceProviderMetadata;//has identity Provider Metadata
    private ServiceProvider remoteServiceProvider;
    /**
     * These properties as used when is a remoteIdentityProvider
     */
    private String entityId;
    private String ssoUrl;
    private String logoutUrl;
    private String artifactResolutionUrl;
    private String assertionSigningCertificate;



    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getSsoUrl() {
        return ssoUrl;
    }

    public void setSsoUrl(String ssoUrl) {
        this.ssoUrl = ssoUrl;
    }

    public String getLogoutUrl() {
        return logoutUrl;
    }

    public void setLogoutUrl(String logoutUrl) {
        this.logoutUrl = logoutUrl;
    }

    public String getArtifactResolutionUrl() {
        return artifactResolutionUrl;
    }

    public void setArtifactResolutionUrl(String artifactResolutionUrl) {
        this.artifactResolutionUrl = artifactResolutionUrl;
    }

    public String getAssertionSigningCertificate() {
        return assertionSigningCertificate;
    }

    public void setAssertionSigningCertificate(String assertionSigningCertificate) {
        this.assertionSigningCertificate = assertionSigningCertificate;
    }

    public boolean hasIncomingAssertionsSignature() {
        return incomingAssertionsSignature;
    }

    public void setIncomingAssertionsSignature(boolean incomingAssertionsSignature) {
        this.incomingAssertionsSignature = incomingAssertionsSignature;
    }


    public int getIdentityProviderId() {
        return identityProviderId;
    }

    public void setIdentityProviderId(int identityProviderId) {
        this.identityProviderId = identityProviderId;
    }

    public String getServiceProviderMetadataUrl() {
        return serviceProviderMetadataUrl;
    }

    public void setServiceProviderMetadataUrl(String serviceProviderMetadataUrl) {
        this.serviceProviderMetadataUrl = serviceProviderMetadataUrl;
    }

    public String getServiceProviderMetadataType() {
        return serviceProviderMetadataType;
    }

    public void setServiceProviderMetadataType(String serviceProviderMetadataType) {
        this.serviceProviderMetadataType = serviceProviderMetadataType;
    }

    public boolean hasServiceProviderMetadata() {
        return serviceProviderMetadata;
    }

    public void setServiceProviderMetadata(boolean serviceProviderMetadata) {
        this.serviceProviderMetadata = serviceProviderMetadata;
    }


    public ServiceProvider getRemoteServiceProvider() {
        return remoteServiceProvider;
    }

    public void setRemoteServiceProvider(ServiceProvider remoteServiceProvider) {
        this.remoteServiceProvider = remoteServiceProvider;
    }
}
