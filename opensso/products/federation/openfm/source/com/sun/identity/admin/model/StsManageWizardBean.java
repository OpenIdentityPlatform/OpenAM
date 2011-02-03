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
 * $Id: StsManageWizardBean.java,v 1.7 2009/11/13 17:16:57 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import java.io.Serializable;

import com.sun.identity.admin.Resources;
import com.sun.identity.admin.dao.WssProfileDao;

public class StsManageWizardBean
        extends WssWizardBean
        implements Serializable
{
    private StsProfileBean stsProfileBean;
    private StsManageTokenIssuanceSummary tokenIssuanceSummary;
    private StsManageServiceSecuritySummary serviceSecuritySummary;
    private StsManageSignEncryptSummary signEncryptSummary;
    private StsManageSamlSummary samlSummary;
    private StsManageTokenValidationSummary tokenValidationSummary;
        
    public StsManageWizardBean() {
        super();
        initialize();
    }

    @Override
    public void reset() {
        super.reset();
        initialize();
        setAllEnabled(true);
        this.gotoStep(StsManageWizardStep.SUMMARY.toInt());
    }

    
    private void initialize() {
        StsProfileBean stsProfileBean = WssProfileDao.getHostedStsProfileBean();
        
        // set the error message for invalid addresses
        Resources r = new Resources();
        String invalidMsg 
            = r.getString(this, "trustedAddresses.invalidMessageDetail");
        stsProfileBean.getTrustedAddresses().setInvalidMessageDetail(invalidMsg);
        
        this.setStsProfileBean(stsProfileBean);
        
        this.setTokenIssuanceSummary(new StsManageTokenIssuanceSummary(this));
        this.setServiceSecuritySummary(new StsManageServiceSecuritySummary(this));
        this.setSignEncryptSummary(new StsManageSignEncryptSummary(this));
        this.setSamlSummary(new StsManageSamlSummary(this));
        this.setTokenValidationSummary(new StsManageTokenValidationSummary(this));
    }

    // Convenience methods -----------------------------------------------------
    
    public boolean isSamlAttributeMappingAvailable() {
        StsProfileBean profileBean = this.getStsProfileBean();
        return profileBean.isUsingAnySamlSecurityMechanism();
    }
    
    // Getters / Setters -------------------------------------------------------

    public void setStsProfileBean(StsProfileBean stsProfileBean) {
        this.stsProfileBean = stsProfileBean;
    }

    public StsProfileBean getStsProfileBean() {
        return stsProfileBean;
    }

    public void setTokenIssuanceSummary(StsManageTokenIssuanceSummary tokenIssuanceSummary) {
        this.tokenIssuanceSummary = tokenIssuanceSummary;
    }

    public StsManageTokenIssuanceSummary getTokenIssuanceSummary() {
        return tokenIssuanceSummary;
    }

    public void setServiceSecuritySummary(StsManageServiceSecuritySummary serviceSecuritySummary) {
        this.serviceSecuritySummary = serviceSecuritySummary;
    }

    public StsManageServiceSecuritySummary getServiceSecuritySummary() {
        return serviceSecuritySummary;
    }

    public void setSignEncryptSummary(StsManageSignEncryptSummary signEncryptSummary) {
        this.signEncryptSummary = signEncryptSummary;
    }

    public StsManageSignEncryptSummary getSignEncryptSummary() {
        return signEncryptSummary;
    }

    public void setSamlSummary(StsManageSamlSummary samlSummary) {
        this.samlSummary = samlSummary;
    }

    public StsManageSamlSummary getSamlSummary() {
        return samlSummary;
    }

    public void setTokenValidationSummary(StsManageTokenValidationSummary tokenValidationSummary) {
        this.tokenValidationSummary = tokenValidationSummary;
    }

    public StsManageTokenValidationSummary getTokenValidationSummary() {
        return tokenValidationSummary;
    }
    
}
