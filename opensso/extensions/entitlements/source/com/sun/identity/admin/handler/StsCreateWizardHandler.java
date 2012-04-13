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
 * $Id: StsCreateWizardHandler.java,v 1.2 2009/08/03 22:25:32 ggennaro Exp $
 */

package com.sun.identity.admin.handler;

import com.iplanet.sso.SSOToken;
import com.sun.identity.admin.Resources;
import com.sun.identity.admin.model.EncryptionAlgorithm;
import com.sun.identity.admin.model.LinkBean;
import com.sun.identity.admin.model.MessageBean;
import com.sun.identity.admin.model.MessagesBean;
import com.sun.identity.admin.model.NextPopupBean;
import com.sun.identity.admin.model.SecurityMechanism;
import com.sun.identity.admin.model.StsCreateWizardBean;
import com.sun.identity.admin.model.StsCreateWizardStep;
import com.sun.identity.admin.model.StsCreateWizardStep1Validator;
import com.sun.identity.admin.model.StsCreateWizardStep2Validator;
import com.sun.identity.admin.model.X509SigningRefType;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.wss.provider.ProviderException;
import com.sun.identity.wss.provider.ProviderUtils;
import com.sun.identity.wss.provider.STSConfig;
import com.sun.identity.wss.provider.TrustAuthorityConfig;
import com.sun.identity.wss.provider.plugins.STSAgent;
import com.sun.identity.wss.security.PasswordCredential;
import java.io.Serializable;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.faces.application.FacesMessage;
import javax.faces.component.UISelectOne;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;



public class StsCreateWizardHandler 
        extends WizardHandler 
        implements Serializable
{
    public static final String PANEL_NO_SETTINGS = "noSettingsPanel";
    public static final String PANEL_X509_TOKEN_SETTINGS = "x509TokenSettingsPanel";
    public static final String PANEL_USER_NAME_TOKEN_SETTINGS = "userNameTokenSettingsPanel";
    public static final String PANEL_KERBEROS_SETTINGS = "kerberosSettingsPanel";
    public static final String PANEL_STS_CONFIGURATION_SETTINGS = "stsConfigSettingsPanel";

    private static final Map<SecurityMechanism, String> securityMechanismPanels =
            new HashMap<SecurityMechanism, String>() {
                {
                    put(SecurityMechanism.ANONYMOUS, PANEL_NO_SETTINGS);
                    put(SecurityMechanism.KERBEROS_TOKEN, PANEL_KERBEROS_SETTINGS);
                    put(SecurityMechanism.SAML2_HOK, PANEL_NO_SETTINGS);
                    put(SecurityMechanism.SAML2_SV, PANEL_NO_SETTINGS);
                    put(SecurityMechanism.SAML_HOK, PANEL_NO_SETTINGS);
                    put(SecurityMechanism.SAML_SV, PANEL_NO_SETTINGS);
                    put(SecurityMechanism.STS_SECURITY, PANEL_STS_CONFIGURATION_SETTINGS);
                    put(SecurityMechanism.USERNAME_TOKEN, PANEL_USER_NAME_TOKEN_SETTINGS);
                    put(SecurityMechanism.USERNAME_TOKEN_PLAIN, PANEL_USER_NAME_TOKEN_SETTINGS);
                    put(SecurityMechanism.X509_TOKEN, PANEL_X509_TOKEN_SETTINGS);
                }
            };

    private MessagesBean messagesBean;
    private String selectedSecurityPanel;



    @Override
    public void initWizardStepValidators() {
        getWizardStepValidators()[StsCreateWizardStep.SERVICENAME_ENDPOINT.toInt()] = new StsCreateWizardStep1Validator(getWizardBean());
        getWizardStepValidators()[StsCreateWizardStep.SECURITY.toInt()] = new StsCreateWizardStep2Validator(getWizardBean());
    }


    @Override
    public void cancelListener(ActionEvent event) {
        doCancelNext();
        getWizardBean().reset();
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
        setSelectedSecurityPanel(PANEL_NO_SETTINGS);

        NextPopupBean npb = NextPopupBean.getInstance();
        npb.setVisible(true);
        Resources r = new Resources();
        npb.setTitle(r.getString(this, "finishTitle"));
        npb.setMessage(r.getString(this, "finishMessage"));
        npb.setLinkBeans(getFinishLinkBeans());
    }

    public void doCancelNext() {
        setSelectedSecurityPanel(PANEL_NO_SETTINGS);

        NextPopupBean npb = NextPopupBean.getInstance();
        npb.setVisible(true);
        Resources r = new Resources();
        npb.setTitle(r.getString(this, "cancelTitle"));
        npb.setMessage(r.getString(this, "cancelMessage"));
        npb.setLinkBeans(getCancelLinkBeans());
    }

    private List<LinkBean> getFinishLinkBeans() {
        List<LinkBean> lbs = new ArrayList<LinkBean>();
        lbs.add(LinkBean.HOME);
        return lbs;
    }

    private List<LinkBean> getCancelLinkBeans() {
        List<LinkBean> lbs = new ArrayList<LinkBean>();
        lbs.add(LinkBean.HOME);
        return lbs;
    }

    public void selectedSecurityMechanismListener(ValueChangeEvent event) {
        UISelectOne uiSelection = (UISelectOne) event.getSource();
        Integer value = (Integer)uiSelection.getValue();
        SecurityMechanism securityMechanism = SecurityMechanism.valueOf(value);

        setSelectedSecurityPanel(securityMechanismPanels.get(securityMechanism));
    }

    private boolean save() {
        Resources r = new Resources();
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
        STSAgent sts = null;

        if( stsAgentExists() ) {
            showSaveErrorPopup(r.getString(this, "saveErrorSummary"),
                               r.getString(this, "saveErrorDetailStsExists"));
            getWizardBean().gotoStep(StsCreateWizardStep.SERVICENAME_ENDPOINT.toInt());
            return false;
        }

        // initialize
        try {

            sts = initStsAgentProvider(adminToken);
            sts.store();

        } catch(ProviderException ex) {
            showSaveErrorPopup(r.getString(this, "saveErrorSummary"),
                               r.getString(this, "saveErrorDetail"));
            getWizardBean().gotoStep(StsCreateWizardStep.SUMMARY.toInt());
            Logger.getLogger(StsCreateWizardHandler.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        return true;
    }

    private void showSaveErrorPopup(String summary, String detail) {
        MessageBean mb = new MessageBean();
        mb.setSummary(summary);
        mb.setDetail(detail);
        mb.setSeverity(FacesMessage.SEVERITY_ERROR);

        getMessagesBean().addMessageBean(mb);
    }

    private boolean stsAgentExists() {
        StsCreateWizardBean wizardBean = (StsCreateWizardBean) getWizardBean();

        List stsConfigs = ProviderUtils.getAllSTSConfig();
        Iterator i = stsConfigs.iterator();
        while( i.hasNext() ) {
            STSConfig stsConfig = (STSConfig) i.next();
            if( stsConfig.getName().equalsIgnoreCase(wizardBean.getServiceName()) ) {
                return true;
            }
        }

        return false;
    }



    private STSAgent initStsAgentProvider(SSOToken adminToken)
            throws ProviderException
    {
        StsCreateWizardBean wizardBean = (StsCreateWizardBean) getWizardBean();
        STSAgent sts = new STSAgent();

        // the name will be the service name
        sts.init(wizardBean.getServiceName(), 
                TrustAuthorityConfig.STS_TRUST_AUTHORITY,
                adminToken);


        sts.setEndpoint(wizardBean.getServiceEndPoint());
        sts.setMexEndpoint(wizardBean.getMexEndPoint());

        ArrayList securityMechs = new ArrayList();
        SecurityMechanism securityMech
                = SecurityMechanism.valueOf(wizardBean.getSecurityMechanism());
        securityMechs.add( securityMech.toConfigString() );
        sts.setSecurityMechs(securityMechs);

        switch(securityMech) {
            case ANONYMOUS:
            case SAML2_HOK:
            case SAML2_SV:
            case SAML_HOK:
            case SAML_SV:
                // no additional settings needed
                break;
            case KERBEROS_TOKEN:
                sts.setKDCDomain(wizardBean.getKerberosDomain());
                sts.setKDCServer(wizardBean.getKerberosDomainServer());
                sts.setKerberosServicePrincipal(wizardBean.getKerberosServicePrincipal());
                sts.setKerberosTicketCacheDir(wizardBean.getKerberosTicketCache());
                break;
            case STS_SECURITY:
                List stsConfigs = ProviderUtils.getAllSTSConfig();
                Iterator i = stsConfigs.iterator();
                while( i.hasNext() ) {
                    STSConfig stsConfig = (STSConfig) i.next();
                    if( stsConfig.getName().equalsIgnoreCase(wizardBean.getStsConfigurationName()) ) {
                        sts.setSTSConfigName(stsConfig.getName());
                        break;
                    }
                }
                break;
            case USERNAME_TOKEN:
            case USERNAME_TOKEN_PLAIN:
                PasswordCredential userCred
                        = new PasswordCredential(wizardBean.getUserNameTokenUserName(),
                                                 wizardBean.getUserNameTokenPassword());
                ArrayList userCreds = new ArrayList();
                userCreds.add(userCred);
                sts.setUsers(userCreds);
                break;
            case X509_TOKEN:
                sts.setSigningRefType(X509SigningRefType.valueOf(wizardBean.getX509TokenSigningReferenceType()).toConfigString());
                break;
        }

        sts.setRequestSignEnabled(wizardBean.isRequestSigned());
        sts.setRequestHeaderEncryptEnabled(wizardBean.isRequestHeaderEncrypted());
        sts.setRequestEncryptEnabled(wizardBean.isRequestEncrypted());
        sts.setResponseSignEnabled(wizardBean.isResponseSignatureVerified());
        sts.setResponseEncryptEnabled(wizardBean.isResponseDecrypted());
        
        switch( EncryptionAlgorithm.valueOf(wizardBean.getEncryptionAlgorithm()) ) {
            case AES_128:
                sts.setEncryptionAlgorithm("AES");
                sts.setEncryptionStrength(128);
                break;
            case AES_192:
                sts.setEncryptionAlgorithm("AES");
                sts.setEncryptionStrength(192);
                break;
            case AES_256:
                sts.setEncryptionAlgorithm("AES");
                sts.setEncryptionStrength(256);
                break;
            case TRIPLEDES_0:
                sts.setEncryptionAlgorithm("DESede");
                sts.setEncryptionStrength(0);
                break;
            case TRIPLEDES_112:
                sts.setEncryptionAlgorithm("DESede");
                sts.setEncryptionStrength(112);
                break;
            case TRIPLEDES_168:
                sts.setEncryptionAlgorithm("DESede");
                sts.setEncryptionStrength(168);
                break;
        }

        sts.setKeyAlias(wizardBean.getPrivateKeyAlias());
        sts.setPublicKeyAlias(wizardBean.getPublicKeyAlias());

        return sts;
    }


    // Getters / Setters -------------------------------------------------------

    public void setMessagesBean(MessagesBean messagesBean) {
        this.messagesBean = messagesBean;
    }

    public MessagesBean getMessagesBean() {
        return messagesBean;
    }

    /**
     * @return the selectedSecurityPanel
     */
    public String getSelectedSecurityPanel() {
        return selectedSecurityPanel;
    }

    /**
     * @param selectedSecurityPanel the selectedSecurityPanel to set
     */
    public void setSelectedSecurityPanel(String selectedSecurityPanel) {
        this.selectedSecurityPanel = selectedSecurityPanel;
    }


}
