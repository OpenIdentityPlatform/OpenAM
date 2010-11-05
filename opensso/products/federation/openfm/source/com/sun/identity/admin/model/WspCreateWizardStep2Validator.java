/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
 * $Id: WspCreateWizardStep2Validator.java,v 1.1 2009/10/19 22:51:22 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import java.util.ArrayList;

import com.sun.identity.admin.effect.InputFieldErrorEffect;
import com.sun.identity.admin.effect.MessageErrorEffect;


public class WspCreateWizardStep2Validator 
        extends WspCreateWizardStepValidator
{
    public WspCreateWizardStep2Validator(WizardBean wb) {
        super(wb);
    }

    @Override
    public boolean validate() {
        WspCreateWizardBean wizardBean = getWspCreateWizardBean();
        WspProfileBean profileBean = wizardBean.getWspProfileBean();
        
        ArrayList<SecurityMechanismPanelBean> panels
            = profileBean.getSecurityMechanismPanels();
        boolean atLeastOne = false;
        
        for(SecurityMechanismPanelBean panel : panels) {
            SecurityMechanism sm = panel.getSecurityMechanism();
            
            if( sm != null && panel.isChecked() ) {
                atLeastOne = true;

                switch(sm) {
                    case USERNAME_TOKEN:
                    case USERNAME_TOKEN_PLAIN:
                        if( !validUserNameTokenSettings(profileBean) ) {
                            return false;
                        }
                        break;
                    case KERBEROS_TOKEN:
                        if( !validKerberosSettings(profileBean) ) {
                            return false;
                        }
                        break;
                    case X509_TOKEN:
                        if( !validX509TokenSettings(profileBean) ) {
                            return false;
                        }
                        break;
                }
            }
        }
                
        if( !atLeastOne ) {
            noSecurityMechanism();
            return false;
        }
        
        return true;
    }

    private void noSecurityMechanism() {
        showErrorMessage("invalidNoSecuritySettingsSummary", 
                         "invalidNoSecuritySettingsDetail");
    }

    private boolean validUserNameTokenSettings(WspProfileBean pb) {
        UserCredentialsTableBean table = pb.getUserCredentialsTable();
        ArrayList<UserCredentialItem> items = null;
        String pattern = "[\\w ]{1,50}?";

        if( table != null ) {
            items = table.getUserCredentialItems();
            
            if( items != null && items.size() == 0 ) {
                showErrorMessage("noUserNameTokenSettingsSummary", 
                                 "noUserNameTokenSettingsDetail");
                return false;
            } else {
                
                boolean valid = true;
                
                for(UserCredentialItem item : items) {
                    String uname = item.getUserName();
                    String pword = item.getPassword();
                    
                    if( (uname != null && !uname.matches(pattern)) ||
                        (pword != null && !pword.matches(pattern)) ) {
                        valid = false;
                        break;
                    }
                }

                if( !valid ) {
                    showErrorMessage("invalidUserNameTokenSettingsSummary", 
                                     "invalidUserNameTokenSettingsDetail");
                    return false;
                }
            }
        }
        
        return true;
    }

    private boolean validKerberosSettings(WspProfileBean pb) {
        String domain = pb.getKerberosDomain();
        String domainServer = pb.getKerberosDomainServer();
        String serverPrincipal = pb.getKerberosServicePrincipal();
        String keyTabFile = pb.getKerberosKeyTabFile();
        String pattern = "[\\w \\@\\.\\/\\&\\:]{0,255}?";
        String summaryKey = null;
        String detailKey = null;

        if(domain != null && !domain.matches(pattern)) {
            summaryKey = "invalidKerberosDomainSummary";
            detailKey = "invalidKerberosDomainDetail";
            pb.setKerberosDomainInputEffect(new InputFieldErrorEffect());
            pb.setKerberosDomainMessageEffect(new MessageErrorEffect());

        } else if(domainServer != null && !domainServer.matches(pattern)) {
            summaryKey = "invalidKerberosDomainServerSummary";
            detailKey = "invalidKerberosDomainServerDetail";
            pb.setKerberosDomainServerInputEffect(new InputFieldErrorEffect());
            pb.setKerberosDomainServerMessageEffect(new MessageErrorEffect());

        } else if(serverPrincipal != null && !serverPrincipal.matches(pattern)) {
            summaryKey = "invalidKerberosServicePrincipalSummary";
            detailKey = "invalidKerberosServicePrincipalDetail";
            pb.setKerberosServicePrincipalInputEffect(new InputFieldErrorEffect());
            pb.setKerberosServicePrincipalMessageEffect(new MessageErrorEffect());

        } else if(keyTabFile != null && !keyTabFile.matches(pattern)) {
            summaryKey = "invalidKerberosKeyTabFileSummary";
            detailKey = "invalidKerberosKeyTabFileDetail";
            pb.setKerberosKeyTabFileInputEffect(new InputFieldErrorEffect());
            pb.setKerberosKeyTabFileMessageEffect(new MessageErrorEffect());

        }

        if( summaryKey != null ) {
            showErrorMessage(summaryKey, detailKey);
            return false;
        }

        return true;
    }
    
    private boolean validX509TokenSettings(WspProfileBean pb) {
        X509SigningRefType signingRef 
                = X509SigningRefType.valueOf(pb.getX509SigningRefType());

        if( signingRef != null ) {
            return true;
        }

        showErrorMessage("invalidX509TokenSettingsSummary", 
                         "invalidX509TokenSettingsDetail");
        return false;
    }
}
