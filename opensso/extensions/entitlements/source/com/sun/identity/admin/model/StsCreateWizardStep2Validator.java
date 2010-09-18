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
 * $Id: StsCreateWizardStep2Validator.java,v 1.2 2009/08/03 22:25:31 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.Resources;
import javax.faces.application.FacesMessage;

public class StsCreateWizardStep2Validator extends WizardStepValidator {

    private static final int MAX_KERBEROS_FIELD_LENGTH = 255;
    private static final int MAX_USERNAME_TOKEN_FIELD_LENGTH = 50;

    public StsCreateWizardStep2Validator(WizardBean wizardBean) {
        super(wizardBean);
    }

    public boolean validate() {
        StsCreateWizardBean wizardBean = (StsCreateWizardBean) getWizardBean();
        SecurityMechanism securityMechanism
                = SecurityMechanism.valueOf(wizardBean.getSecurityMechanism());

        switch(securityMechanism) {
            case ANONYMOUS:
                break;
            case KERBEROS_TOKEN:
                if( !validKerberosSettings() ) {
                    return false;
                }
                break;
            case SAML2_HOK:
            case SAML2_SV:
            case SAML_HOK:
            case SAML_SV:
                break;
            case STS_SECURITY:
                if( !validStsSecuritySettings() ) {
                    return false;
                }
                break;
            case USERNAME_TOKEN:
            case USERNAME_TOKEN_PLAIN:
                if( !validUserNameTokenSettings() ) {
                    return false;
                }
                break;
            case X509_TOKEN:
                if( !validX509TokenSettings() ) {
                    return false;
                }
                break;
            default:
                noSecurityMechanism();
                return false;
        }

        return true;
    }

    private boolean validKerberosSettings() {
        StsCreateWizardBean wizardBean = (StsCreateWizardBean) getWizardBean();
        String domain = wizardBean.getKerberosDomain();
        String domainServer = wizardBean.getKerberosDomainServer();
        String serverPrincipal = wizardBean.getKerberosServicePrincipal();
        String ticketCache = wizardBean.getKerberosTicketCache();
        String inputPattern = "[\\w \\@\\.\\/\\&\\:]{0," + MAX_KERBEROS_FIELD_LENGTH + "}?";

        if( (domain == null || domain.matches(inputPattern)) &&
            (domainServer == null || domainServer.matches(inputPattern)) &&
            (serverPrincipal == null || serverPrincipal.matches(inputPattern)) &&
            (ticketCache == null || ticketCache.matches(inputPattern)) )
        {

            return true;
        }

        MessageBean mb = new MessageBean();
        Resources r = new Resources();
        mb.setSummary(r.getString(this, "invalidKerberosSettingsSummary"));
        mb.setDetail(r.getString(this, "invalidKerberosSettingsDetail"));
        mb.setSeverity(FacesMessage.SEVERITY_ERROR);

        getMessagesBean().addMessageBean(mb);
        wizardBean.gotoStep(StsCreateWizardStep.SECURITY.toInt());

        return false;
    }

    private boolean validStsSecuritySettings() {
        StsCreateWizardBean wizardBean = (StsCreateWizardBean) getWizardBean();
        String stsConfigName = wizardBean.getStsConfigurationName();

        if( stsConfigName != null )
        {
            return true;
        }

        MessageBean mb = new MessageBean();
        Resources r = new Resources();
        mb.setSummary(r.getString(this, "invalidStsConfigSettingsSummary"));
        mb.setDetail(r.getString(this, "invalidStsConfigSettingsDetail"));
        mb.setSeverity(FacesMessage.SEVERITY_ERROR);

        getMessagesBean().addMessageBean(mb);
        wizardBean.gotoStep(StsCreateWizardStep.SECURITY.toInt());

        return false;
    }

    private boolean validUserNameTokenSettings() {
        StsCreateWizardBean wizardBean = (StsCreateWizardBean) getWizardBean();
        String userName = wizardBean.getUserNameTokenUserName();
        String password = wizardBean.getUserNameTokenPassword();
        String inputPattern = "[\\w ]{0," + MAX_USERNAME_TOKEN_FIELD_LENGTH + "}?";

        if( (userName == null || userName.matches(inputPattern)) &&
            (password == null || password.matches(inputPattern)) )
        {

            return true;
        }

        MessageBean mb = new MessageBean();
        Resources r = new Resources();
        mb.setSummary(r.getString(this, "invalidUserNameTokenSettingsSummary"));
        mb.setDetail(r.getString(this, "invalidUserNameTokenSettingsDetail"));
        mb.setSeverity(FacesMessage.SEVERITY_ERROR);

        getMessagesBean().addMessageBean(mb);
        wizardBean.gotoStep(StsCreateWizardStep.SECURITY.toInt());

        return false;
    }

    private boolean validX509TokenSettings() {
        StsCreateWizardBean wizardBean = (StsCreateWizardBean) getWizardBean();
        X509SigningRefType signingRef
                = X509SigningRefType.valueOf(wizardBean.getX509TokenSigningReferenceType());

        if( signingRef != null ) {
            return true;
        }

        MessageBean mb = new MessageBean();
        Resources r = new Resources();
        mb.setSummary(r.getString(this, "invalidX509TokenSettingsSummary"));
        mb.setDetail(r.getString(this, "invalidX509TokenSettingsDetail"));
        mb.setSeverity(FacesMessage.SEVERITY_ERROR);

        getMessagesBean().addMessageBean(mb);
        wizardBean.gotoStep(StsCreateWizardStep.SECURITY.toInt());

        return false;
    }

    private void noSecurityMechanism() {
        WssCreateWizardBean wizardBean = (WssCreateWizardBean) getWizardBean();
        MessageBean mb = new MessageBean();
        Resources r = new Resources();
        mb.setSummary(r.getString(this, "invalidNoSecuritySettingsSummary"));
        mb.setDetail(r.getString(this, "invalidNoSecuritySettingsDetail"));
        mb.setSeverity(FacesMessage.SEVERITY_ERROR);

        getMessagesBean().addMessageBean(mb);
        wizardBean.gotoStep(StsCreateWizardStep.SECURITY.toInt());
    }

}