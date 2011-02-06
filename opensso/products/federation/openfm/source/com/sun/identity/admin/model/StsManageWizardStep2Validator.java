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
 * $Id: StsManageWizardStep2Validator.java,v 1.4 2009/10/21 16:46:02 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import java.util.ArrayList;

import com.sun.identity.admin.effect.InputFieldErrorEffect;
import com.sun.identity.admin.effect.MessageErrorEffect;



public class StsManageWizardStep2Validator 
        extends StsManageWizardStepValidator
{
    
    public StsManageWizardStep2Validator(WizardBean wb) {
        super(wb);
    }

    @Override
    public boolean validate() {
        
        if( !validSecurityTokenType() ) {
            return false;
        }
        
        return true;
    }

    private boolean validSecurityTokenType() {
        StsProfileBean profileBean 
            = getStsManageWizardBean().getStsProfileBean();
        ArrayList<SecurityMechanismPanelBean> panelBeans 
            = profileBean.getSecurityMechanismPanels();
        SecurityMechanismPanelBean invalidPanel = null;
        boolean atLeastOne = false;
        
        for(SecurityMechanismPanelBean panel : panelBeans) {
            
            if( panel.isChecked() ) {
                atLeastOne = true;
                SecurityMechanism sm = panel.getSecurityMechanism();
                
                switch(sm) {
                    case KERBEROS_TOKEN:
                        if( !validKerberosSettings() ) {
                            invalidPanel = panel;
                        }
                        break;
                    case USERNAME_TOKEN:
                    case USERNAME_TOKEN_PLAIN:
                        if( !validUserNameSettings() )
                        {
                            invalidPanel = panel;
                        }
                        break;
                    case X509_TOKEN:
                        if( !validX509Settings() ) {
                            invalidPanel = panel;
                        }
                        break;
                    default:
                        break;
                }

                if( invalidPanel != null ) {
                    break;
                }
            }
        }
        
        // make the invalid panel expanded
        if( invalidPanel != null ) {
            
            for(SecurityMechanismPanelBean panel : panelBeans) {
                panel.setExpanded(false);
            }
            invalidPanel.setExpanded(true);
            
            return false;
        } else if( !atLeastOne ) {

            showErrorMessage("noSecurityTokenTypeSummary", 
                             "noSecurityTokenTypeDetail");
            return false;
        } else {
            return true;
        }
    }

    private boolean validKerberosSettings() {
        StsManageWizardBean wizardBean = getStsManageWizardBean();
        StsProfileBean profileBean = wizardBean.getStsProfileBean();
        
        String domain = profileBean.getKerberosDomain();
        String domainServer = profileBean.getKerberosDomainServer();
        String serverPrincipal = profileBean.getKerberosServicePrincipal();
        String keyTabFile = profileBean.getKerberosKeyTabFile();
        String pattern = "[\\w \\@\\.\\/\\&\\:]{0,255}?";
        String summaryKey = null;
        String detailKey = null;
        
        if(domain != null && !domain.matches(pattern)) {
            summaryKey = "invalidKerberosDomainSummary";
            detailKey = "invalidKerberosDomainDetail";
            profileBean.setKerberosDomainInputEffect(new InputFieldErrorEffect());
            profileBean.setKerberosDomainMessageEffect(new MessageErrorEffect());
        } else if(domainServer != null && !domainServer.matches(pattern)) {
            summaryKey = "invalidKerberosDomainServerSummary";
            detailKey = "invalidKerberosDomainServerDetail";
            profileBean.setKerberosDomainServerInputEffect(new InputFieldErrorEffect());
            profileBean.setKerberosDomainServerMessageEffect(new MessageErrorEffect());
        } else if(serverPrincipal != null && !serverPrincipal.matches(pattern)) {
            summaryKey = "invalidKerberosServicePrincipalSummary";
            detailKey = "invalidKerberosServicePrincipalDetail";
            profileBean.setKerberosServicePrincipalInputEffect(new InputFieldErrorEffect());
            profileBean.setKerberosServicePrincipalMessageEffect(new MessageErrorEffect());
        } else if(keyTabFile != null && !keyTabFile.matches(pattern)) {
            summaryKey = "invalidKerberosKeyTabFileSummary";
            detailKey = "invalidKerberosKeyTabFileDetail";
            profileBean.setKerberosKeyTabFileInputEffect(new InputFieldErrorEffect());
            profileBean.setKerberosKeyTabFileMessageEffect(new MessageErrorEffect());
        }
        
        if( summaryKey != null ) {
            showErrorMessage(summaryKey, detailKey);
            return false;
        }
        
        return true;
    }

    private boolean validUserNameSettings() {
        StsManageWizardBean wizardBean = getStsManageWizardBean();
        StsProfileBean profileBean = wizardBean.getStsProfileBean();
        UserCredentialsTableBean uctb = profileBean.getUserCredentialsTable();
        
        if( uctb == null 
                || uctb.getUserCredentialItems() == null 
                || uctb.getUserCredentialItems().size() == 0 ) {
            
            showErrorMessage("invalidUserNameSummary",
                             "invalidUserNameDetail");
            return false;
        }

        return true;
    }
        
    private boolean validX509Settings() {
        StsManageWizardBean wizardBean = getStsManageWizardBean();
        StsProfileBean profileBean = wizardBean.getStsProfileBean();
        X509SigningRefType signingRef 
            = X509SigningRefType.valueOf(profileBean.getX509SigningRefType());
        
        if( signingRef != null ) {
            return true;
        }
        
        showErrorMessage("invalidX509TokenSettingsSummary",
                         "invalidX509TokenSettingsDetail");
        return false;
    }
    
}
