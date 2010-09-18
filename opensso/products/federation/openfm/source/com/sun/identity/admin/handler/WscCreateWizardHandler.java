/**
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
 * $Id $
 */

package com.sun.identity.admin.handler;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import com.icesoft.faces.component.selectinputtext.SelectInputText;
import com.sun.identity.admin.Resources;
import com.sun.identity.admin.dao.WssProfileDao;
import com.sun.identity.admin.model.LinkBean;
import com.sun.identity.admin.model.NextPopupBean;
import com.sun.identity.admin.model.SecurityMechanism;
import com.sun.identity.admin.model.SecurityTokenServiceType;
import com.sun.identity.admin.model.StsClientProfileBean;
import com.sun.identity.admin.model.WscCreateWizardBean;
import com.sun.identity.admin.model.WscCreateWizardStep;
import com.sun.identity.admin.model.WscCreateWizardStep1Validator;
import com.sun.identity.admin.model.WscCreateWizardStep2Validator;
import com.sun.identity.admin.model.WscCreateWizardStep3Validator;
import com.sun.identity.admin.model.WscCreateWizardStep5Validator;
import com.sun.identity.admin.model.WscProfileBean;
import com.sun.identity.admin.model.WspProfileBean;
import com.sun.identity.wss.provider.ProviderException;
import com.sun.identity.wss.provider.plugins.AgentProvider;
import com.sun.identity.wss.provider.plugins.STSAgent;

public class WscCreateWizardHandler 
        extends WssWizardHandler 
        implements Serializable
{
    
    @Override
    public void initWizardStepValidators() {
        getWizardStepValidators()[WscCreateWizardStep.WSC_PROFILE.toInt()] = new WscCreateWizardStep1Validator(getWizardBean());
        getWizardStepValidators()[WscCreateWizardStep.WSC_USING_STS.toInt()] = new WscCreateWizardStep2Validator(getWizardBean());
        getWizardStepValidators()[WscCreateWizardStep.WSC_SECURITY.toInt()] = new WscCreateWizardStep3Validator(getWizardBean());
        getWizardStepValidators()[WscCreateWizardStep.WSC_SAML.toInt()] = new WscCreateWizardStep5Validator(getWizardBean());
    }

    @Override
    public void cancelListener(ActionEvent event) {
        getWizardBean().reset();
        doCancelNext();
    }

    public void doCancelNext() {
        NextPopupBean npb = NextPopupBean.getInstance();
        npb.setVisible(true);
        Resources r = new Resources();
        npb.setTitle(r.getString(this, "cancelTitle"));
        npb.setMessage(r.getString(this, "cancelMessage"));
        npb.setLinkBeans(getCancelLinkBeans());
    }

    private List<LinkBean> getCancelLinkBeans() {
        List<LinkBean> lbs = new ArrayList<LinkBean>();
        lbs.add(LinkBean.HOME);
        lbs.add(LinkBean.WSS);
        lbs.add(LinkBean.WSC_CREATE);
        return lbs;
    }


    @Override
    public void finishListener(ActionEvent event) {
        if (!validateFinish(event)) {
            return;
        }

        if( save() ) {
            doFinishNext();
            getWizardBean().reset();
        }
    }

    public void doFinishNext() {
        NextPopupBean npb = NextPopupBean.getInstance();
        npb.setVisible(true);
        Resources r = new Resources();
        npb.setTitle(r.getString(this, "finishTitle"));
        npb.setMessage(r.getString(this, "finishMessage"));
        npb.setLinkBeans(getFinishLinkBeans());
    }

    private List<LinkBean> getFinishLinkBeans() {
        List<LinkBean> lbs = new ArrayList<LinkBean>();
        lbs.add(LinkBean.HOME);
        lbs.add(LinkBean.WSS);
        lbs.add(LinkBean.WSC_CREATE);
        return lbs;
    }


    public void wspEndPointListener(ValueChangeEvent event) {

        if (event.getComponent() instanceof SelectInputText) {
            WscCreateWizardBean wizardBean 
                = (WscCreateWizardBean) getWizardBean();
            WscProfileBean wscProfile = wizardBean.getWscProfileBean();
            SelectInputText autoComplete 
                = (SelectInputText) event.getComponent();
            String newEndPoint = (String) event.getNewValue();
            
            ArrayList<WspProfileBean> matches
                = WssProfileDao.getMatchingWspProfiles(newEndPoint);

            ArrayList<SelectItem> suggestions = new ArrayList<SelectItem>();
            for( WspProfileBean match : matches ) {
                suggestions.add(new SelectItem(match, match.getEndPoint()));
                if( suggestions.size() > autoComplete.getRows() ) {
                    break;
                }
            }
            wizardBean.setWspProfileSuggestions(suggestions);
            
            
            WspProfileBean chosenWspProfile = null;
            if( autoComplete.getSelectedItem() != null ) {
                chosenWspProfile 
                    = (WspProfileBean) autoComplete.getSelectedItem().getValue();
            } else if( newEndPoint != null ) {
                chosenWspProfile 
                    = WssProfileDao.getWspProfileBeanByEndPoint(newEndPoint);
            }
            
            if( chosenWspProfile != null ) {
                wizardBean.setUsingWsp(true);
                wizardBean.setChosenWspProfileBean(chosenWspProfile);
                wscProfile.setUsingMexEndPoint(false);
            } else {
                wizardBean.setUsingWsp(false);
                wizardBean.setChosenWspProfileBean(null);
                wscProfile.setUsingMexEndPoint(false);
            }
            
            // update based on changes
            wizardBean.updateWscProfileWithPresets();
            wizardBean.updateSecurityMechanism();

            // reset wizard state to ensure user revisits steps in case of changes
            wizardBean.getWizardStepBeans()[WscCreateWizardStep.WSC_SECURITY.toInt()].setEnabled(false);
            wizardBean.getWizardStepBeans()[WscCreateWizardStep.WSC_SIGN_ENCRYPT.toInt()].setEnabled(false);
            wizardBean.getWizardStepBeans()[WscCreateWizardStep.WSC_SAML.toInt()].setEnabled(false);
            wizardBean.getWizardStepBeans()[WscCreateWizardStep.SUMMARY.toInt()].setEnabled(false);
        }        
    }

    public void stsTypeListener(ValueChangeEvent event) {

        String oldValue = (String)event.getOldValue();
        String newValue = (String)event.getNewValue();

        if( !oldValue.equalsIgnoreCase(newValue) ) {
            WscCreateWizardBean wizardBean
                = (WscCreateWizardBean) getWizardBean();

            SecurityTokenServiceType stsType
                    = SecurityTokenServiceType.valueOf(newValue);

            switch(stsType) {
                case OPENSSO:
                    wizardBean.setUsingSts(true);
                    wizardBean.setUsingOurSts(true);
                    break;
                case OTHER:
                    wizardBean.setUsingSts(true);
                    wizardBean.setUsingOurSts(false);
                    break;
                default:
                    wizardBean.setUsingSts(false);
                    wizardBean.setUsingOurSts(false);
                    break;
            }

            // update based on changes
            wizardBean.updateStsClientProfileWithPresets();
            wizardBean.updateSecurityMechanism();

            // reset wizard state to ensure user revisits steps in case of changes
            wizardBean.getWizardStepBeans()[WscCreateWizardStep.WSC_SIGN_ENCRYPT.toInt()].setEnabled(false);
            wizardBean.getWizardStepBeans()[WscCreateWizardStep.WSC_SAML.toInt()].setEnabled(false);
            wizardBean.getWizardStepBeans()[WscCreateWizardStep.SUMMARY.toInt()].setEnabled(false);
        }
    }

    public void usingMexEndPointListener(ValueChangeEvent event) {

        WscCreateWizardBean wizardBean
                = (WscCreateWizardBean) getWizardBean();
        WscProfileBean wscProfileBean = wizardBean.getWscProfileBean();

        if( wscProfileBean.isUsingMexEndPoint()
                && wscProfileBean.getEndPoint() != null 
                && wscProfileBean.getEndPoint().length() > 0 )
        {
            wscProfileBean.setMexEndPoint(wscProfileBean.getEndPoint() + "/mex");
        } else {
            wscProfileBean.setMexEndPoint(null);
        }

        // reset wizard state to ensure user revisits steps in case of changes
        wizardBean.getWizardStepBeans()[WscCreateWizardStep.WSC_SIGN_ENCRYPT.toInt()].setEnabled(false);
        wizardBean.getWizardStepBeans()[WscCreateWizardStep.SUMMARY.toInt()].setEnabled(false);
    }

    
    private boolean save() {
        WscCreateWizardBean wizardBean = (WscCreateWizardBean) getWizardBean();
        WscProfileBean wscProfileBean = wizardBean.getWscProfileBean();
        StsClientProfileBean stsClientProfileBean = wizardBean.getStsClientProfileBean();
        AgentProvider wsc = null;
        STSAgent stsClient = null;

        // initialize the sts client profile
        if( wizardBean.isUsingSts() ) {
            String profileName = "STS Client - " + wscProfileBean.getProfileName();
            stsClientProfileBean.setProfileName(profileName);
            
            if( WssProfileDao.stsAgentExists(profileName) ) {
                showErrorMessage("saveErrorSummary", "saveErrorDetailExists");
                getWizardBean().gotoStep(WscCreateWizardStep.WSC_PROFILE.toInt());
                return false;
            }
            
            try {
                stsClient = WssProfileDao.getStsAgent(stsClientProfileBean);
            } catch (ProviderException e) {
                // problem with initialization
                showErrorMessage("saveErrorSummary", "saveErrorDetailInit");
                getWizardBean().gotoStep(WscCreateWizardStep.SUMMARY.toInt());
                Logger.getLogger(WscCreateWizardHandler.class.getName()).log(Level.SEVERE, null, e);
                return false;
            }
        }
        
        // initialize the wsc profile
        try {
            if( wizardBean.isUsingSts() ) {
                wscProfileBean.setStsClientProfileName(stsClientProfileBean.getProfileName());
                wscProfileBean.setSecurityMechanism(SecurityMechanism.STS_SECURITY.toString());
            }
            
            wsc = WssProfileDao.getAgentProvider(wscProfileBean);
        } catch (ProviderException e) {
            // problem with initialization
            showErrorMessage("saveErrorSummary", "saveErrorDetailInit");
            getWizardBean().gotoStep(WscCreateWizardStep.SUMMARY.toInt());
            Logger.getLogger(WscCreateWizardHandler.class.getName()).log(Level.SEVERE, null, e);
            return false;
        }
        
        if( wsc != null && wsc.isExists() ) {
            showErrorMessage("saveErrorSummary", "saveErrorDetailExists");
            getWizardBean().gotoStep(WscCreateWizardStep.WSC_PROFILE.toInt());
            return false;
        }

        // Store if no issues in initializing above...
        try {
            if( wizardBean.isUsingSts() ) {
                stsClient.store();
            }
            
            wsc.store();
        } catch (ProviderException e) {
            // problem with persistence
            showErrorMessage("saveErrorSummary", "saveErrorDetailStore");
            getWizardBean().gotoStep(WscCreateWizardStep.SUMMARY.toInt());
            Logger.getLogger(WscCreateWizardHandler.class.getName()).log(Level.SEVERE, null, e);
            return false;
        }

        return true;
    }

}
