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
 * $Id: ServiceProvider.java,v 1.2 2008/06/25 05:42:36 qcheng Exp $
 *
 */
package com.sun.identity.config.pojos;

/**
 * @author jperez
 */
public class ServiceProvider extends Provider {

    private IdentityProvider remoteIdentityProvider;
    private int serviceProviderId;
    private String identityProviderMetadataUrl;
    private String identityProviderMetadataType;
    private boolean identityProviderMetadata;//has identity Provider Metadata


    /**
     * These properties as used when is a remoteServiceProvider
     */
    private String entityId;
    private String assertionConsumerUrl;
    private String assertionSigningCertificate;


    public int getServiceProviderId() {
        return serviceProviderId;
    }

    public void setServiceProviderId(int serviceProviderId) {
        this.serviceProviderId = serviceProviderId;
    }


    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }


    public String getAssertionConsumerUrl() {
        return assertionConsumerUrl;
    }

    public void setAssertionConsumerUrl(String assertionConsumerUrl) {
        this.assertionConsumerUrl = assertionConsumerUrl;
    }

    public String getAssertionSigningCertificate() {
        return assertionSigningCertificate;
    }

    public void setAssertionSigningCertificate(String assertionSigningCertificate) {
        this.assertionSigningCertificate = assertionSigningCertificate;
    }

    public IdentityProvider getRemoteIdentityProvider() {
        return remoteIdentityProvider;
    }

    public void setRemoteIdentityProvider(IdentityProvider remoteIdentityProvider) {
        this.remoteIdentityProvider = remoteIdentityProvider;
    }

    public String getIdentityProviderMetadataUrl() {
        return identityProviderMetadataUrl;
    }

    public void setIdentityProviderMetadataUrl(String identityProviderMetadataUrl) {
        this.identityProviderMetadataUrl = identityProviderMetadataUrl;
    }

    public String getIdentityProviderMetadataType() {
        return identityProviderMetadataType;
    }

    public void setIdentityProviderMetadataType(String identityProviderMetadataType) {
        this.identityProviderMetadataType = identityProviderMetadataType;
    }

    public boolean hasIdentityProviderMetadata() {
        return identityProviderMetadata;
    }

    public void setIdentityProviderMetadata(boolean identityProviderMetadata) {
        this.identityProviderMetadata = identityProviderMetadata;
    }


}
