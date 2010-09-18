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
 * $Id: WspCreateWizardBean.java,v 1.10 2009/11/13 17:16:57 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import java.io.Serializable;
import com.sun.identity.admin.dao.WssProfileDao;

public class WspCreateWizardBean
        extends WssWizardBean
        implements Serializable
{
    private WspProfileBean wspProfileBean;
    
    private RealmSummary realmSummary;
    private WspCreateProfileNameSummary profileNameSummary;
    private WspCreateServiceSecuritySummary serviceSecuritySummary;
    private WspCreateSignEncryptSummary signEncryptSummary;
    private WspCreateSamlSummary samlSummary;

    
    public WspCreateWizardBean() {
        super();
        initialize();
    }

    @Override
    public void reset() {
        super.reset();
        initialize();
    }

    private void initialize() {
        
        this.setWspProfileBean(WssProfileDao.getDefaultWspProfileBean());
        
        this.setRealmSummary(new RealmSummary());
        this.setProfileNameSummary(new WspCreateProfileNameSummary(this));
        this.setServiceSecuritySummary(new WspCreateServiceSecuritySummary(this));
        this.setSignEncryptSummary(new WspCreateSignEncryptSummary(this));
        this.setSamlSummary(new WspCreateSamlSummary(this));
    }
    
    // Convenience methods -----------------------------------------------------
    
    public boolean isSamlAttributeMappingAvailable() {
        WspProfileBean profileBean = this.getWspProfileBean();
        return profileBean.isUsingAnySamlSecurityMechanism();
    }
    
    public boolean isTokenConversionAvailable() {
        WspProfileBean profileBean = this.getWspProfileBean();
        boolean isUsingAnySaml = profileBean.isUsingAnySamlSecurityMechanism();
        boolean isAuthChainEmpty = true;
        
        if( profileBean.getAuthenticationChain() != null ) {
            isAuthChainEmpty = 
                profileBean.getAuthenticationChain().equals(EMPTY_LIST_VALUE);
        }
        
        return !isAuthChainEmpty && isUsingAnySaml;
    }
    
    // Getters / Setters -------------------------------------------------------

    public void setRealmSummary(RealmSummary realmSummary) {
        this.realmSummary = realmSummary;
    }

    public RealmSummary getRealmSummary() {
        return realmSummary;
    }

    public void setWspProfileBean(WspProfileBean wspProfileBean) {
        this.wspProfileBean = wspProfileBean;
    }

    public WspProfileBean getWspProfileBean() {
        return wspProfileBean;
    }

    public void setProfileNameSummary(WspCreateProfileNameSummary profileNameSummary) {
        this.profileNameSummary = profileNameSummary;
    }

    public WspCreateProfileNameSummary getProfileNameSummary() {
        return profileNameSummary;
    }

    public void setServiceSecuritySummary(WspCreateServiceSecuritySummary serviceSecuritySummary) {
        this.serviceSecuritySummary = serviceSecuritySummary;
    }

    public WspCreateServiceSecuritySummary getServiceSecuritySummary() {
        return serviceSecuritySummary;
    }

    public void setSignEncryptSummary(WspCreateSignEncryptSummary signEncryptSummary) {
        this.signEncryptSummary = signEncryptSummary;
    }

    public WspCreateSignEncryptSummary getSignEncryptSummary() {
        return signEncryptSummary;
    }

    public void setSamlSummary(WspCreateSamlSummary samlSummary) {
        this.samlSummary = samlSummary;
    }

    public WspCreateSamlSummary getSamlSummary() {
        return samlSummary;
    }

}
