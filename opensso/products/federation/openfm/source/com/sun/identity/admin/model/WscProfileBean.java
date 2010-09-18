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
 * $Id: WscProfileBean.java,v 1.3 2009/10/19 22:51:22 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import java.io.Serializable;

import com.icesoft.faces.context.effects.Effect;

public class WscProfileBean extends WssProfileBean implements Serializable {

    private String securityMechanism;
    private String stsClientProfileName;
    private String userNameTokenUserName;
    private Effect userNameTokenUserNameInputEffect;
    private Effect userNameTokenUserNameMessageEffect;
    private String userNameTokenPassword;
    private Effect userNameTokenPasswordInputEffect;
    private Effect userNameTokenPasswordMessageEffect;
    private String kerberosTicketCache;
    private Effect kerberosTicketCacheInputEffect;
    private Effect kerberosTicketCacheMessageEffect;
    
    
    // getters / setters -------------------------------------------------------
    
    public void setSecurityMechanism(String securityMechanism) {
        this.securityMechanism = securityMechanism;
    }

    public String getSecurityMechanism() {
        return securityMechanism;
    }

    public String getUserNameTokenUserName() {
        return userNameTokenUserName;
    }

    public void setUserNameTokenUserName(String userNameTokenUserName) {
        this.userNameTokenUserName = userNameTokenUserName;
    }

    public Effect getUserNameTokenUserNameInputEffect() {
        return userNameTokenUserNameInputEffect;
    }

    public void setUserNameTokenUserNameInputEffect(
            Effect userNameTokenUserNameInputEffect) {
        this.userNameTokenUserNameInputEffect = userNameTokenUserNameInputEffect;
    }

    public Effect getUserNameTokenUserNameMessageEffect() {
        return userNameTokenUserNameMessageEffect;
    }

    public void setUserNameTokenUserNameMessageEffect(
            Effect userNameTokenUserNameMessageEffect) {
        this.userNameTokenUserNameMessageEffect = userNameTokenUserNameMessageEffect;
    }

    public String getUserNameTokenPassword() {
        return userNameTokenPassword;
    }

    public void setUserNameTokenPassword(String userNameTokenPassword) {
        this.userNameTokenPassword = userNameTokenPassword;
    }

    public Effect getUserNameTokenPasswordInputEffect() {
        return userNameTokenPasswordInputEffect;
    }

    public void setUserNameTokenPasswordInputEffect(
            Effect userNameTokenPasswordInputEffect) {
        this.userNameTokenPasswordInputEffect = userNameTokenPasswordInputEffect;
    }

    public Effect getUserNameTokenPasswordMessageEffect() {
        return userNameTokenPasswordMessageEffect;
    }

    public void setUserNameTokenPasswordMessageEffect(
            Effect userNameTokenPasswordMessageEffect) {
        this.userNameTokenPasswordMessageEffect = userNameTokenPasswordMessageEffect;
    }

    public String getKerberosTicketCache() {
        return kerberosTicketCache;
    }

    public void setKerberosTicketCache(String kerberosTicketCache) {
        this.kerberosTicketCache = kerberosTicketCache;
    }

    public Effect getKerberosTicketCacheInputEffect() {
        return kerberosTicketCacheInputEffect;
    }

    public void setKerberosTicketCacheInputEffect(
            Effect kerberosTicketCacheInputEffect) {
        this.kerberosTicketCacheInputEffect = kerberosTicketCacheInputEffect;
    }

    public Effect getKerberosTicketCacheMessageEffect() {
        return kerberosTicketCacheMessageEffect;
    }

    public void setKerberosTicketCacheMessageEffect(
            Effect kerberosTicketCacheMessageEffect) {
        this.kerberosTicketCacheMessageEffect = kerberosTicketCacheMessageEffect;
    }

    public void setStsClientProfileName(String stsClientProfileName) {
        this.stsClientProfileName = stsClientProfileName;
    }

    public String getStsClientProfileName() {
        return stsClientProfileName;
    }

}
