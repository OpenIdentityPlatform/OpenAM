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
 * $Id: StsProfileBean.java,v 1.2 2009/10/21 16:46:03 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import java.io.Serializable;

import com.icesoft.faces.context.effects.Effect;

public class StsProfileBean extends WspProfileBean implements Serializable {

    private String issuer;
    private Effect issuerMessageEffect;
    private Effect issuerInputEffect;
    private int tokenLifetime;
    private Effect tokenLifetimeMessageEffect;
    private Effect tokenLifetimeInputEffect;
    private String certAlias;
    private String tokenPluginClass;
    private Effect tokenPluginClassMessageEffect;
    private Effect tokenPluginClassInputEffect;
    private EditableSelectOneBean trustedIssuers;
    private EditableSelectOneBean trustedAddresses;

    
    // getters / setters -------------------------------------------------------

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public Effect getIssuerMessageEffect() {
        return issuerMessageEffect;
    }

    public void setIssuerMessageEffect(Effect issuerMessageEffect) {
        this.issuerMessageEffect = issuerMessageEffect;
    }

    public Effect getIssuerInputEffect() {
        return issuerInputEffect;
    }

    public void setIssuerInputEffect(Effect issuerInputEffect) {
        this.issuerInputEffect = issuerInputEffect;
    }

    public int getTokenLifetime() {
        return tokenLifetime;
    }

    public void setTokenLifetime(int tokenLifetime) {
        this.tokenLifetime = tokenLifetime;
    }

    public Effect getTokenLifetimeMessageEffect() {
        return tokenLifetimeMessageEffect;
    }

    public void setTokenLifetimeMessageEffect(Effect tokenLifetimeMessageEffect) {
        this.tokenLifetimeMessageEffect = tokenLifetimeMessageEffect;
    }

    public Effect getTokenLifetimeInputEffect() {
        return tokenLifetimeInputEffect;
    }

    public void setTokenLifetimeInputEffect(Effect tokenLifetimeInputEffect) {
        this.tokenLifetimeInputEffect = tokenLifetimeInputEffect;
    }

    public String getCertAlias() {
        return certAlias;
    }

    public void setCertAlias(String certAlias) {
        this.certAlias = certAlias;
    }

    public String getTokenPluginClass() {
        return tokenPluginClass;
    }

    public void setTokenPluginClass(String tokenPluginClass) {
        this.tokenPluginClass = tokenPluginClass;
    }

    public Effect getTokenPluginClassMessageEffect() {
        return tokenPluginClassMessageEffect;
    }

    public void setTokenPluginClassMessageEffect(
            Effect tokenPluginClassMessageEffect) {
        this.tokenPluginClassMessageEffect = tokenPluginClassMessageEffect;
    }

    public Effect getTokenPluginClassInputEffect() {
        return tokenPluginClassInputEffect;
    }

    public void setTokenPluginClassInputEffect(Effect tokenPluginClassInputEffect) {
        this.tokenPluginClassInputEffect = tokenPluginClassInputEffect;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setTrustedIssuers(EditableSelectOneBean trustedIssuers) {
        this.trustedIssuers = trustedIssuers;
    }

    public EditableSelectOneBean getTrustedIssuers() {
        return trustedIssuers;
    }

    public void setTrustedAddresses(EditableSelectOneBean trustedAddresses) {
        this.trustedAddresses = trustedAddresses;
    }

    public EditableSelectOneBean getTrustedAddresses() {
        return trustedAddresses;
    }

}
