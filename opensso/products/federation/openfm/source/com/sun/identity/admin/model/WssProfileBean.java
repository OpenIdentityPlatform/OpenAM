/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: WssProfileBean.java,v 1.2 2009/10/19 22:51:24 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import java.io.Serializable;

import com.icesoft.faces.context.effects.Effect;

public class WssProfileBean implements Serializable {

    private String profileName;
    private Effect profileNameInputEffect;
    private Effect profileNameMessageEffect;
    
    private String endPoint;
    private Effect endPointInputEffect;
    private Effect endPointMessageEffect;
    
    private boolean isUsingMexEndPoint;
    private String mexEndPoint;
    private Effect mexEndPointInputEffect;
    private Effect mexEndPointMessageEffect;

    private String kerberosDomain;
    private Effect kerberosDomainInputEffect;
    private Effect kerberosDomainMessageEffect;
    private String kerberosDomainServer;
    private Effect kerberosDomainServerInputEffect;
    private Effect kerberosDomainServerMessageEffect;
    private String kerberosServicePrincipal;
    private Effect kerberosServicePrincipalInputEffect;
    private Effect kerberosServicePrincipalMessageEffect;
    private String x509SigningRefType;
    
    private boolean requestSigned;
    private boolean requestHeaderEncrypted;
    private boolean requestEncrypted;
    private boolean responseSignatureVerified;
    private boolean responseDecrypted;
    private String encryptionAlgorithm;
    private String privateKeyAlias;
    private String publicKeyAlias;
    
    private String nameIdMapper;
    private Effect nameIdMapperInputEffect;
    private Effect nameIdMapperMessageEffect;
    
    private String attributeNamespace;
    private Effect attributeNamespaceInputEffect;
    private Effect attributeNamespaceMessageEffect;
    
    private boolean includeMemberships;
    
    private SamlAttributesTableBean samlAttributesTable;

    

    // getters / setters -------------------------------------------------------
    
    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getProfileName() {
        return profileName;
    }

    public Effect getProfileNameInputEffect() {
        return profileNameInputEffect;
    }

    public void setProfileNameInputEffect(Effect profileNameInputEffect) {
        this.profileNameInputEffect = profileNameInputEffect;
    }

    public Effect getProfileNameMessageEffect() {
        return profileNameMessageEffect;
    }

    public void setProfileNameMessageEffect(Effect profileNameMessageEffect) {
        this.profileNameMessageEffect = profileNameMessageEffect;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    public Effect getEndPointInputEffect() {
        return endPointInputEffect;
    }

    public void setEndPointInputEffect(Effect endPointInputEffect) {
        this.endPointInputEffect = endPointInputEffect;
    }

    public Effect getEndPointMessageEffect() {
        return endPointMessageEffect;
    }

    public void setEndPointMessageEffect(Effect endPointMessageEffect) {
        this.endPointMessageEffect = endPointMessageEffect;
    }

    public boolean isUsingMexEndPoint() {
        return isUsingMexEndPoint;
    }

    public void setUsingMexEndPoint(boolean isUsingMexEndPoint) {
        this.isUsingMexEndPoint = isUsingMexEndPoint;
    }

    public String getMexEndPoint() {
        return mexEndPoint;
    }

    public void setMexEndPoint(String mexEndPoint) {
        this.mexEndPoint = mexEndPoint;
    }

    public Effect getMexEndPointInputEffect() {
        return mexEndPointInputEffect;
    }

    public void setMexEndPointInputEffect(Effect mexEndPointInputEffect) {
        this.mexEndPointInputEffect = mexEndPointInputEffect;
    }

    public Effect getMexEndPointMessageEffect() {
        return mexEndPointMessageEffect;
    }

    public void setMexEndPointMessageEffect(Effect mexEndPointMessageEffect) {
        this.mexEndPointMessageEffect = mexEndPointMessageEffect;
    }

    public String getNameIdMapper() {
        return nameIdMapper;
    }

    public void setNameIdMapper(String nameIdMapper) {
        this.nameIdMapper = nameIdMapper;
    }

    public Effect getNameIdMapperInputEffect() {
        return nameIdMapperInputEffect;
    }

    public void setNameIdMapperInputEffect(Effect nameIdMapperInputEffect) {
        this.nameIdMapperInputEffect = nameIdMapperInputEffect;
    }

    public Effect getNameIdMapperMessageEffect() {
        return nameIdMapperMessageEffect;
    }

    public void setNameIdMapperMessageEffect(Effect nameIdMapperMessageEffect) {
        this.nameIdMapperMessageEffect = nameIdMapperMessageEffect;
    }

    public String getAttributeNamespace() {
        return attributeNamespace;
    }

    public void setAttributeNamespace(String attributeNamespace) {
        this.attributeNamespace = attributeNamespace;
    }

    public Effect getAttributeNamespaceInputEffect() {
        return attributeNamespaceInputEffect;
    }

    public void setAttributeNamespaceInputEffect(
            Effect attributeNamespaceInputEffect) {
        this.attributeNamespaceInputEffect = attributeNamespaceInputEffect;
    }

    public Effect getAttributeNamespaceMessageEffect() {
        return attributeNamespaceMessageEffect;
    }

    public void setAttributeNamespaceMessageEffect(
            Effect attributeNamespaceMessageEffect) {
        this.attributeNamespaceMessageEffect = attributeNamespaceMessageEffect;
    }

    public boolean isIncludeMemberships() {
        return includeMemberships;
    }

    public void setIncludeMemberships(boolean includeMemberships) {
        this.includeMemberships = includeMemberships;
    }

    public SamlAttributesTableBean getSamlAttributesTable() {
        return samlAttributesTable;
    }

    public void setSamlAttributesTable(SamlAttributesTableBean samlAttributesTable) {
        this.samlAttributesTable = samlAttributesTable;
    }

    public boolean isRequestSigned() {
        return requestSigned;
    }

    public void setRequestSigned(boolean requestSigned) {
        this.requestSigned = requestSigned;
    }

    public boolean isRequestHeaderEncrypted() {
        return requestHeaderEncrypted;
    }

    public void setRequestHeaderEncrypted(boolean requestHeaderEncrypted) {
        this.requestHeaderEncrypted = requestHeaderEncrypted;
    }

    public boolean isRequestEncrypted() {
        return requestEncrypted;
    }

    public void setRequestEncrypted(boolean requestEncrypted) {
        this.requestEncrypted = requestEncrypted;
    }

    public boolean isResponseSignatureVerified() {
        return responseSignatureVerified;
    }

    public void setResponseSignatureVerified(boolean responseSignatureVerified) {
        this.responseSignatureVerified = responseSignatureVerified;
    }

    public boolean isResponseDecrypted() {
        return responseDecrypted;
    }

    public void setResponseDecrypted(boolean responseDecrypted) {
        this.responseDecrypted = responseDecrypted;
    }

    public String getEncryptionAlgorithm() {
        return encryptionAlgorithm;
    }

    public void setEncryptionAlgorithm(String encryptionAlgorithm) {
        this.encryptionAlgorithm = encryptionAlgorithm;
    }

    public String getPrivateKeyAlias() {
        return privateKeyAlias;
    }

    public void setPrivateKeyAlias(String privateKeyAlias) {
        this.privateKeyAlias = privateKeyAlias;
    }

    public String getPublicKeyAlias() {
        return publicKeyAlias;
    }

    public void setPublicKeyAlias(String publicKeyAlias) {
        this.publicKeyAlias = publicKeyAlias;
    }

    public String getKerberosDomain() {
        return kerberosDomain;
    }

    public void setKerberosDomain(String kerberosDomain) {
        this.kerberosDomain = kerberosDomain;
    }

    public Effect getKerberosDomainInputEffect() {
        return kerberosDomainInputEffect;
    }

    public void setKerberosDomainInputEffect(Effect kerberosDomainInputEffect) {
        this.kerberosDomainInputEffect = kerberosDomainInputEffect;
    }

    public Effect getKerberosDomainMessageEffect() {
        return kerberosDomainMessageEffect;
    }

    public void setKerberosDomainMessageEffect(Effect kerberosDomainMessageEffect) {
        this.kerberosDomainMessageEffect = kerberosDomainMessageEffect;
    }

    public String getKerberosDomainServer() {
        return kerberosDomainServer;
    }

    public void setKerberosDomainServer(String kerberosDomainServer) {
        this.kerberosDomainServer = kerberosDomainServer;
    }

    public Effect getKerberosDomainServerInputEffect() {
        return kerberosDomainServerInputEffect;
    }

    public void setKerberosDomainServerInputEffect(
            Effect kerberosDomainServerInputEffect) {
        this.kerberosDomainServerInputEffect = kerberosDomainServerInputEffect;
    }

    public Effect getKerberosDomainServerMessageEffect() {
        return kerberosDomainServerMessageEffect;
    }

    public void setKerberosDomainServerMessageEffect(
            Effect kerberosDomainServerMessageEffect) {
        this.kerberosDomainServerMessageEffect = kerberosDomainServerMessageEffect;
    }

    public String getKerberosServicePrincipal() {
        return kerberosServicePrincipal;
    }

    public void setKerberosServicePrincipal(String kerberosServicePrincipal) {
        this.kerberosServicePrincipal = kerberosServicePrincipal;
    }

    public Effect getKerberosServicePrincipalInputEffect() {
        return kerberosServicePrincipalInputEffect;
    }

    public void setKerberosServicePrincipalInputEffect(
            Effect kerberosServicePrincipalInputEffect) {
        this.kerberosServicePrincipalInputEffect = kerberosServicePrincipalInputEffect;
    }

    public Effect getKerberosServicePrincipalMessageEffect() {
        return kerberosServicePrincipalMessageEffect;
    }

    public void setKerberosServicePrincipalMessageEffect(
            Effect kerberosServicePrincipalMessageEffect) {
        this.kerberosServicePrincipalMessageEffect = kerberosServicePrincipalMessageEffect;
    }

    public void setX509SigningRefType(String x509SigningRefType) {
        this.x509SigningRefType = x509SigningRefType;
    }

    public String getX509SigningRefType() {
        return x509SigningRefType;
    }

    
}
