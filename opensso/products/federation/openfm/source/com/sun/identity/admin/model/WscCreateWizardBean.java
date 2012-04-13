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
 * $Id: WscCreateWizardBean.java,v 1.4 2009/11/13 17:16:57 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import com.sun.identity.admin.dao.WssProfileDao;

public class WscCreateWizardBean
        extends WssWizardBean
        implements Serializable
{
    private WscProfileBean wscProfileBean;
    private StsClientProfileBean stsClientProfileBean;
    
    private WscProfileBean defaultWscProfileBean;
    private StsClientProfileBean defaultStsClientProfileBean;

    private ArrayList<SelectItem> wspProfileSuggestions;
    private boolean usingWsp;
    private WspProfileBean chosenWspProfileBean;

    private String stsType;
    private boolean usingSts;
    private boolean usingOurSts;
    private StsProfileBean hostedStsProfileBean;
    
    
    private RealmSummary realmSummary;
    private WscCreateProfileNameSummary profileNameSummary;
    private WscCreateUseStsSummary useStsSummary;
    private WscCreateServiceSecuritySummary serviceSecuritySummary;
    private WscCreateSignEncryptSummary signEncryptSummary;
    private WscCreateSamlSummary samlSummary;


    public WscCreateWizardBean() {
        super();
        initialize();
    }

    @Override
    public void reset() {
        super.reset();
        initialize();
    }

    private void initialize() {
        
        this.setDefaultWscProfileBean(WssProfileDao.getDefaultWscProfileBean());
        this.setDefaultStsClientProfileBean(WssProfileDao.getDefaultStsClientProfileBean());
        
        this.setWspProfileSuggestions(null);
        this.setUsingWsp(false);
        this.setChosenWspProfileBean(null);
        
        this.setStsType(SecurityTokenServiceType.OPENSSO.toString());
        this.setUsingSts(true);
        this.setUsingOurSts(true);
        this.setHostedStsProfileBean(WssProfileDao.getHostedStsProfileBean());
        
        this.setWscProfileBean(WssProfileDao.getDefaultWscProfileBean());
        this.setStsClientProfileBean(WssProfileDao.getDefaultStsClientProfileBean());
        updateStsClientProfileWithPresets();
        updateSecurityMechanism();

        this.setRealmSummary(new RealmSummary());
        this.setProfileNameSummary(new WscCreateProfileNameSummary(this));
        this.setUseStsSummary(new WscCreateUseStsSummary(this));
        this.setServiceSecuritySummary(new WscCreateServiceSecuritySummary(this));
        this.setSignEncryptSummary(new WscCreateSignEncryptSummary(this));
        this.setSamlSummary(new WscCreateSamlSummary(this));
    }

    // convenience methods -----------------------------------------------------
    
    public void updateStsClientProfileWithPresets() {
        StsClientProfileBean stsClientProfile 
            = this.getStsClientProfileBean();
        StsClientProfileBean defaultStsClientProfile
            = this.getDefaultStsClientProfileBean();
        StsProfileBean hostedSts 
            = this.getHostedStsProfileBean();
        
        if( this.isUsingOurSts() ) {
            stsClientProfile.setEndPoint(hostedSts.getEndPoint());
            stsClientProfile.setMexEndPoint(hostedSts.getMexEndPoint());
            stsClientProfile.setRequestSigned(hostedSts.isRequestSigned());
            stsClientProfile.setRequestHeaderEncrypted(hostedSts.isRequestHeaderEncrypted());
            stsClientProfile.setRequestEncrypted(hostedSts.isRequestEncrypted());
            stsClientProfile.setResponseSignatureVerified(hostedSts.isResponseSignatureVerified());
            stsClientProfile.setResponseDecrypted(hostedSts.isResponseDecrypted());
            stsClientProfile.setEncryptionAlgorithm(hostedSts.getEncryptionAlgorithm());
            stsClientProfile.setPublicKeyAlias(hostedSts.getPublicKeyAlias());
        } else if( this.isUsingSts() ) {
            stsClientProfile.setEndPoint(defaultStsClientProfile.getEndPoint());
            stsClientProfile.setMexEndPoint(defaultStsClientProfile.getMexEndPoint());
            stsClientProfile.setRequestSigned(defaultStsClientProfile.isRequestSigned());
            stsClientProfile.setRequestHeaderEncrypted(defaultStsClientProfile.isRequestHeaderEncrypted());
            stsClientProfile.setRequestEncrypted(defaultStsClientProfile.isRequestEncrypted());
            stsClientProfile.setResponseSignatureVerified(defaultStsClientProfile.isResponseSignatureVerified());
            stsClientProfile.setResponseDecrypted(defaultStsClientProfile.isResponseDecrypted());
            stsClientProfile.setEncryptionAlgorithm(defaultStsClientProfile.getEncryptionAlgorithm());
            stsClientProfile.setPublicKeyAlias(defaultStsClientProfile.getPublicKeyAlias());
        } 
    }
    
    public void updateWscProfileWithPresets() {
        WspProfileBean chosenWspProfile = this.getChosenWspProfileBean();
        WscProfileBean defaultWscProfile = this.getDefaultWscProfileBean();
        WscProfileBean wscProfile = this.getWscProfileBean();
        
        if( this.usingWsp && chosenWspProfile != null ) {
            wscProfile.setRequestSigned(chosenWspProfile.isRequestSigned());
            wscProfile.setRequestHeaderEncrypted(chosenWspProfile.isRequestHeaderEncrypted());
            wscProfile.setRequestEncrypted(chosenWspProfile.isRequestEncrypted());
            wscProfile.setResponseSignatureVerified(chosenWspProfile.isResponseSignatureVerified());
            wscProfile.setResponseDecrypted(chosenWspProfile.isResponseDecrypted());
            wscProfile.setEncryptionAlgorithm(chosenWspProfile.getEncryptionAlgorithm());
            wscProfile.setPublicKeyAlias(chosenWspProfile.getPublicKeyAlias());
            
        } else {
            wscProfile.setRequestSigned(defaultWscProfile.isRequestSigned());
            wscProfile.setRequestHeaderEncrypted(defaultWscProfile.isRequestHeaderEncrypted());
            wscProfile.setRequestEncrypted(defaultWscProfile.isRequestEncrypted());
            wscProfile.setResponseSignatureVerified(defaultWscProfile.isResponseSignatureVerified());
            wscProfile.setResponseDecrypted(defaultWscProfile.isResponseDecrypted());
            wscProfile.setEncryptionAlgorithm(defaultWscProfile.getEncryptionAlgorithm());
            wscProfile.setPublicKeyAlias(defaultWscProfile.getPublicKeyAlias());
        }        
    }
    
    public void updateSecurityMechanism() {
        WscProfileBean wscOrSts;
        String securityMechanism;
        boolean found = false;
        
        if( this.usingSts ) {
            wscOrSts = this.getStsClientProfileBean();
        } else {
            wscOrSts = this.getWscProfileBean();
        } 
        
        securityMechanism = wscOrSts.getSecurityMechanism();
        for(SelectItem item : getSecurityMechanismList()) {
            String itemValue = (String) item.getValue();
            if( itemValue.equalsIgnoreCase(securityMechanism)) {
                found = true;
                break;
            }
        }
        
        if( !found && getSecurityMechanismList().size() > 0 ) {
            SelectItem firstItem = getSecurityMechanismList().get(0);
            String firstItemValue = (String) firstItem.getValue();
            securityMechanism = firstItemValue;
        }
        
        wscOrSts.setSecurityMechanism(securityMechanism);
    }
    
    public String getSecurityMechanismPanel() {
        String panelId;
        SecurityMechanism sm;
        
        if( this.usingSts ) {
            StsClientProfileBean stsProfile = this.getStsClientProfileBean();
            sm = SecurityMechanism.valueOf(stsProfile.getSecurityMechanism());
        } else {
            WscProfileBean wscProfile = this.getWscProfileBean();
            sm = SecurityMechanism.valueOf(wscProfile.getSecurityMechanism());
        }
        
        switch(sm) {
            case USERNAME_TOKEN:
            case USERNAME_TOKEN_PLAIN:
                panelId = "userNameTokenSettingsPanel";
                break;
            case KERBEROS_TOKEN:
                panelId = "kerberosSettingsPanel";
                break;
            case X509_TOKEN:
                panelId = "x509TokenSettingsPanel";
                break;
            default:
                panelId = "noSettingsPanel";
                break;
        }

        return panelId;
    }

    public boolean isSamlAttributeMappingAvailable() {
        boolean value = false;
        SecurityMechanism sm;
        
        if( this.usingSts ) {
            StsClientProfileBean stsProfile = this.getStsClientProfileBean();
            sm = SecurityMechanism.valueOf(stsProfile.getSecurityMechanism());
        } else {
            WscProfileBean wscProfile = this.getWscProfileBean();
            sm = SecurityMechanism.valueOf(wscProfile.getSecurityMechanism());
        }
        
        switch(sm) {
            case SAML2_HOK:
            case SAML2_SV:
            case SAML_HOK:
            case SAML_SV:
                value = true;
                break;
        }

        return value;
    }    
    
    // Lists -------------------------------------------------------------------

    public List<SelectItem> getSecurityMechanismList() {
        WspProfileBean wspOrStsProfile = null;
        
        if( isUsingOurSts() && getHostedStsProfileBean() != null ) {
            wspOrStsProfile = getHostedStsProfileBean();
        } else if( isUsingWsp() && getChosenWspProfileBean() != null ) {
            wspOrStsProfile = getChosenWspProfileBean();
        }
        
        if( wspOrStsProfile != null ) {
            ArrayList<SelectItem> newItems = new ArrayList<SelectItem>();
            
            for(SecurityMechanismPanelBean panel 
                    : wspOrStsProfile.getSecurityMechanismPanels()) {
                
                if( panel.isChecked() ) {
                    SelectItem item = new SelectItem(
                            panel.getSecurityMechanism().toString(), 
                            panel.getSecurityMechanism().toLocaleString());
                    newItems.add(item);
                }
            }
            
            return newItems;
        } else {
            return super.getSecurityMechanismList();
        }
    }

    
    // Getters / Setters -------------------------------------------------------

    public WscProfileBean getWscProfileBean() {
        return wscProfileBean;
    }

    public void setWscProfileBean(WscProfileBean wscProfileBean) {
        this.wscProfileBean = wscProfileBean;
    }

    public StsClientProfileBean getStsClientProfileBean() {
        return stsClientProfileBean;
    }

    public void setStsClientProfileBean(StsClientProfileBean stsClientProfileBean) {
        this.stsClientProfileBean = stsClientProfileBean;
    }

    public ArrayList<SelectItem> getWspProfileSuggestions() {
        return wspProfileSuggestions;
    }

    public void setWspProfileSuggestions(ArrayList<SelectItem> wspProfileSuggestions) {
        this.wspProfileSuggestions = wspProfileSuggestions;
    }

    public boolean isUsingWsp() {
        return usingWsp;
    }

    public void setUsingWsp(boolean usingWsp) {
        this.usingWsp = usingWsp;
    }

    public String getStsType() {
        return stsType;
    }

    public void setStsType(String stsType) {
        this.stsType = stsType;
    }

    public boolean isUsingSts() {
        return usingSts;
    }

    public void setUsingSts(boolean usingSts) {
        this.usingSts = usingSts;
    }

    public boolean isUsingOurSts() {
        return usingOurSts;
    }

    public void setUsingOurSts(boolean usingOurSts) {
        this.usingOurSts = usingOurSts;
    }

    public RealmSummary getRealmSummary() {
        return realmSummary;
    }

    public void setRealmSummary(RealmSummary realmSummary) {
        this.realmSummary = realmSummary;
    }

    public WscCreateProfileNameSummary getProfileNameSummary() {
        return profileNameSummary;
    }

    public void setProfileNameSummary(WscCreateProfileNameSummary profileNameSummary) {
        this.profileNameSummary = profileNameSummary;
    }

    public WscCreateUseStsSummary getUseStsSummary() {
        return useStsSummary;
    }

    public void setUseStsSummary(WscCreateUseStsSummary useStsSummary) {
        this.useStsSummary = useStsSummary;
    }

    public WscCreateServiceSecuritySummary getServiceSecuritySummary() {
        return serviceSecuritySummary;
    }

    public void setServiceSecuritySummary(
            WscCreateServiceSecuritySummary serviceSecuritySummary) {
        this.serviceSecuritySummary = serviceSecuritySummary;
    }

    public WscCreateSignEncryptSummary getSignEncryptSummary() {
        return signEncryptSummary;
    }

    public void setSignEncryptSummary(WscCreateSignEncryptSummary signEncryptSummary) {
        this.signEncryptSummary = signEncryptSummary;
    }

    public WscCreateSamlSummary getSamlSummary() {
        return samlSummary;
    }

    public void setSamlSummary(WscCreateSamlSummary samlSummary) {
        this.samlSummary = samlSummary;
    }

    public void setChosenWspProfileBean(WspProfileBean chosenWspProfileBean) {
        this.chosenWspProfileBean = chosenWspProfileBean;
    }

    public WspProfileBean getChosenWspProfileBean() {
        return chosenWspProfileBean;
    }

    public void setDefaultWscProfileBean(WscProfileBean defaultWscProfileBean) {
        this.defaultWscProfileBean = defaultWscProfileBean;
    }

    public WscProfileBean getDefaultWscProfileBean() {
        return defaultWscProfileBean;
    }

    public void setHostedStsProfileBean(StsProfileBean hostedStsProfileBean) {
        this.hostedStsProfileBean = hostedStsProfileBean;
    }

    public StsProfileBean getHostedStsProfileBean() {
        return hostedStsProfileBean;
    }

    public void setDefaultStsClientProfileBean(
            StsClientProfileBean defaultStsClientProfileBean) {
        this.defaultStsClientProfileBean = defaultStsClientProfileBean;
    }

    public StsClientProfileBean getDefaultStsClientProfileBean() {
        return defaultStsClientProfileBean;
    }
}
