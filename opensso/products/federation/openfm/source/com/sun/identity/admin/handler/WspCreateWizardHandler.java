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

import com.sun.identity.admin.Resources;
import com.sun.identity.admin.dao.WssProfileDao;
import com.sun.identity.admin.model.LinkBean;
import com.sun.identity.admin.model.NextPopupBean;
import com.sun.identity.admin.model.SecurityMechanismPanelBean;
import com.sun.identity.admin.model.WscCreateWizardStep;
import com.sun.identity.admin.model.WspCreateWizardBean;
import com.sun.identity.admin.model.WspCreateWizardStep;
import com.sun.identity.admin.model.WspCreateWizardStep1Validator;
import com.sun.identity.admin.model.WspCreateWizardStep2Validator;
import com.sun.identity.admin.model.WspCreateWizardStep4Validator;
import com.sun.identity.admin.model.WspProfileBean;
import com.sun.identity.wss.provider.ProviderException;
import com.sun.identity.wss.provider.plugins.AgentProvider;

public class WspCreateWizardHandler 
        extends WssWizardHandler 
        implements Serializable
{
    @Override
    public void initWizardStepValidators() {
        getWizardStepValidators()[WspCreateWizardStep.WSP_PROFILE.toInt()] = new WspCreateWizardStep1Validator(getWizardBean());
        getWizardStepValidators()[WspCreateWizardStep.WSP_SECURITY.toInt()] = new WspCreateWizardStep2Validator(getWizardBean());
        getWizardStepValidators()[WspCreateWizardStep.WSP_SAML.toInt()] = new WspCreateWizardStep4Validator(getWizardBean());
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
        lbs.add(LinkBean.WSP_CREATE);
        return lbs;
    }


    @Override
    public void finishListener(ActionEvent event) {
        resetWidgets(event);

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
        lbs.add(LinkBean.WSP_CREATE);
        lbs.add(LinkBean.WSC_CREATE);
        return lbs;
    }

    @Override
    public void expandStepListener(ActionEvent event) {
        resetWidgets(event);
        super.expandStepListener(event);
    }
    
    @Override
    public void nextListener(ActionEvent event) {
        resetWidgets(event);
        super.nextListener(event);
    }
    
    @Override
    public void previousListener(ActionEvent event) {
        resetWidgets(event);
        super.previousListener(event);
    }
    
    private void resetWidgets(ActionEvent event) {
        int step = getStep(event);
        WspCreateWizardStep wizardStep = WspCreateWizardStep.valueOf(step);
        WspCreateWizardBean wizardBean = (WspCreateWizardBean) getWizardBean();
        WspProfileBean profileBean = wizardBean.getWspProfileBean();
        boolean resetSecurityWidgets = false;
        boolean resetSamlWidgets = false;

        switch(wizardStep) {
            case WSP_SAML:
                resetSamlWidgets = true;
                break;
            case WSP_SECURITY:
                resetSecurityWidgets = true;
                break;
            case SUMMARY:
                resetSecurityWidgets = true;
                resetSamlWidgets = true;
                break;
            default:
                break;
        }
        
        if( resetSecurityWidgets 
                && profileBean.getUserCredentialsTable() != null ) {
            profileBean.getUserCredentialsTable().resetInterface();
        }
        
        if( resetSamlWidgets && profileBean.getSamlAttributesTable() != null ) {
            profileBean.getSamlAttributesTable().resetInterface();
        }
    }

    
    private boolean save() {
        AgentProvider wsp = null;
        WspCreateWizardBean wizardBean = (WspCreateWizardBean) getWizardBean();
        WspProfileBean wspProfileBean = wizardBean.getWspProfileBean();
        
        try {
            
            wsp = WssProfileDao.getAgentProvider(wspProfileBean);

        } catch (ProviderException e) {
            // problem with initialization
            showErrorMessage("saveErrorSummary", "saveErrorDetailInit");
            getWizardBean().gotoStep(WscCreateWizardStep.SUMMARY.toInt());
            Logger.getLogger(WspCreateWizardHandler.class.getName()).log(Level.SEVERE, null, e);
            return false;
        }
        
        if( wsp != null && wsp.isExists() ) {
            showErrorMessage("saveErrorSummary", "saveErrorDetailExists");
            getWizardBean().gotoStep(WscCreateWizardStep.WSC_PROFILE.toInt());
            return false;
        }
        
        try {
            wsp.store();
        } catch (ProviderException e) {
            // problem with persistence
            showErrorMessage("saveErrorSummary", "saveErrorDetailStore");
            getWizardBean().gotoStep(WscCreateWizardStep.SUMMARY.toInt());
            Logger.getLogger(WspCreateWizardHandler.class.getName()).log(Level.SEVERE, null, e);
            return false;
        }
        
        return true;
    }
    
    public void usingMexEndPointListener(ValueChangeEvent event) {
        WspCreateWizardBean wizardBean = (WspCreateWizardBean) getWizardBean();
        WspProfileBean profileBean = wizardBean.getWspProfileBean();
        
        if( profileBean.isUsingMexEndPoint()
                && profileBean.getEndPoint() != null 
                && profileBean.getEndPoint().length() > 0 ) {
            
            profileBean.setMexEndPoint(profileBean.getEndPoint() + "/mex");
        } else {
            profileBean.setMexEndPoint(null);
        }

        // reset wizard state to ensure user revisits steps in case of changes
        wizardBean.getWizardStepBeans()[WspCreateWizardStep.SUMMARY.toInt()].setEnabled(false);
    }
    
    
    // listeners for the security mechanism panels -----------------------------
    
    public void securityMechanismPanelChangeListener(ValueChangeEvent event) {
        WspCreateWizardBean wizardBean = (WspCreateWizardBean) getWizardBean();
        WspProfileBean profileBean = wizardBean.getWspProfileBean();
        ArrayList<SecurityMechanismPanelBean> panelBeans
            = profileBean.getSecurityMechanismPanels();
        Object attributeValue 
            = event.getComponent().getAttributes().get("panelBean");

        if( attributeValue instanceof SecurityMechanismPanelBean ) {
            SecurityMechanismPanelBean activePanelBean
                = (SecurityMechanismPanelBean) attributeValue;
            
            if( activePanelBean.isChecked() ) {
                activePanelBean.setExpanded(true);
            } else {
                activePanelBean.setExpanded(false);
            }
            
            for(SecurityMechanismPanelBean panelBean : panelBeans) {
                if( panelBean != activePanelBean 
                        && activePanelBean.isCollapsible() ) {

                        panelBean.setExpanded(false);
                }
            }
        }   
    }
    
    public void securityMechanismPanelActionListener(ActionEvent event) {
        WspCreateWizardBean wizardBean = (WspCreateWizardBean) getWizardBean();
        WspProfileBean profileBean = wizardBean.getWspProfileBean();
        ArrayList<SecurityMechanismPanelBean> panelBeans
            = profileBean.getSecurityMechanismPanels();
        Object attributeValue 
            = event.getComponent().getAttributes().get("panelBean");
        
        if( attributeValue instanceof SecurityMechanismPanelBean ) {
            SecurityMechanismPanelBean activePanelBean 
                = (SecurityMechanismPanelBean) attributeValue;
            
            // only one is actively shown
            for(SecurityMechanismPanelBean panelBean : panelBeans) {
                if( activePanelBean == panelBean 
                        && panelBean.isChecked() 
                        && !panelBean.isExpanded() ) {
                    panelBean.setExpanded(true);
                } else {
                    panelBean.setExpanded(false);
                }
            }
        }   
    }
    

}
