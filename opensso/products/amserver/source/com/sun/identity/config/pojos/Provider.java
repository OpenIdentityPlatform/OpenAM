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
 * $Id: Provider.java,v 1.2 2008/06/25 05:42:36 qcheng Exp $
 *
 */
package com.sun.identity.config.pojos;

/**
 * @author jperez
 */
public class Provider {


    private FederalProtocol federalProtocol;
    private String metadataUrl;
    private String metadataType;
    private boolean metadata; //has metadata
    private CircleTrust circleTrust;
    private String displayName;
    private String metaAlias;
    private boolean postBinding;
    private boolean artifactBinding;
    private boolean outgoingAssertionSignature;
    private String keyAlias;


    public FederalProtocol getFederalProtocol() {
        return federalProtocol;
    }

    public void setFederalProtocol(FederalProtocol federalProtocol) {
        this.federalProtocol = federalProtocol;
    }

    public String getMetadataUrl() {
        return metadataUrl;
    }

    public void setMetadataUrl(String metadataUrl) {
        this.metadataUrl = metadataUrl;
    }

    public CircleTrust getCircleTrust() {
        return circleTrust;
    }

    public void setCircleTrust(CircleTrust circleTrust) {
        this.circleTrust = circleTrust;
    }


    public String getMetadataType() {
        return metadataType;
    }

    public void setMetadataType(String metadataType) {
        this.metadataType = metadataType;
    }

    public boolean hasMetadata() {
        return metadata;
    }

    public void setMetadata(boolean hasMetadata) {
        this.metadata = hasMetadata;
    }


   

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getMetaAlias() {
        return metaAlias;
    }

    public void setMetaAlias(String metaAlias) {
        this.metaAlias = metaAlias;
    }

    public boolean isPostBinding() {
        return postBinding;
    }

    public void setPostBinding(boolean postBinding) {
        this.postBinding = postBinding;
    }

    public boolean isArtifactBinding() {
        return artifactBinding;
    }

    public void setArtifactBinding(boolean artifactBinding) {
        this.artifactBinding = artifactBinding;
    }

   

    public boolean hasOutgoingAssertionSignature() {
        return outgoingAssertionSignature;
    }

    public void setOutgoingAssertionSignature(boolean outgoingAssertionSignature) {
        this.outgoingAssertionSignature = outgoingAssertionSignature;
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }



}
