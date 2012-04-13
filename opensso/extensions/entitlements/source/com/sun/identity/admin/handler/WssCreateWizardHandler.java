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
 * $Id: WssCreateWizardHandler.java,v 1.3 2009/08/03 22:25:32 ggennaro Exp $
 */

package com.sun.identity.admin.handler;

import com.iplanet.sso.SSOToken;
import com.sun.identity.admin.Resources;
import com.sun.identity.admin.model.EncryptionAlgorithm;
import com.sun.identity.admin.model.SecurityMechanism;
import com.sun.identity.admin.model.LinkBean;
import com.sun.identity.admin.model.MessageBean;
import com.sun.identity.admin.model.MessagesBean;
import com.sun.identity.admin.model.NextPopupBean;
import com.sun.identity.admin.model.WssCreateWizardBean;
import com.sun.identity.admin.model.WssCreateWizardStep;
import com.sun.identity.admin.model.WssCreateWizardStep1Validator;
import com.sun.identity.admin.model.WssCreateWizardStep3Validator;
import com.sun.identity.admin.model.X509SigningRefType;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.wss.provider.ProviderConfig;
import com.sun.identity.wss.provider.ProviderException;
import com.sun.identity.wss.provider.ProviderUtils;
import com.sun.identity.wss.provider.STSConfig;
import com.sun.identity.wss.provider.TrustAuthorityConfig;
import com.sun.identity.wss.provider.plugins.AgentProvider;
import com.sun.identity.wss.security.PasswordCredential;
import java.io.Serializable;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.faces.component.UISelectMany;
import javax.faces.component.UISelectOne;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;

public class WssCreateWizardHandler 
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
    private String selectedSecurityPanel = PANEL_STS_CONFIGURATION_SETTINGS;



    @Override
    public void initWizardStepValidators() {
        getWizardStepValidators()[WssCreateWizardStep.WSP_ENDPOINT_SECURITY.toInt()] = new WssCreateWizardStep1Validator(getWizardBean());
        getWizardStepValidators()[WssCreateWizardStep.WSC_SERVICENAME_SECURITY.toInt()] = new WssCreateWizardStep3Validator(getWizardBean());
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
        WssCreateWizardBean wizardBean = (WssCreateWizardBean) getWizardBean();

        setSelectedSecurityPanel(PANEL_STS_CONFIGURATION_SETTINGS);

        NextPopupBean npb = NextPopupBean.getInstance();
        npb.setVisible(true);
        Resources r = new Resources();
        npb.setTitle(r.getString(this, "finishTitle"));
        
        if( wizardBean.isConfigureWsc() ) {
            npb.setMessage(r.getString(this, "finishMessageBoth"));
        } else {
            npb.setMessage(r.getString(this, "finishMessageWsp"));
        }

        npb.setLinkBeans(getFinishLinkBeans());
    }

    public void doCancelNext() {
        setSelectedSecurityPanel(PANEL_STS_CONFIGURATION_SETTINGS);

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

    public void selectedWscSecurityMechanismListener(ValueChangeEvent event) {
        UISelectOne uiSelection = (UISelectOne) event.getSource();
        Integer value = (Integer)uiSelection.getValue();
        SecurityMechanism securityMechanism = SecurityMechanism.valueOf(value);

        setSelectedSecurityPanel(securityMechanismPanels.get(securityMechanism));
    }

    public void selectedWspSecurityMechanismListener(ValueChangeEvent event) {
        WssCreateWizardBean wizardBean = (WssCreateWizardBean)getWizardBean();
        UISelectMany uiSelection = (UISelectMany) event.getSource();
        Integer values[] = (Integer[]) uiSelection.getSelectedValues();

        ArrayList<SecurityMechanism> available = new ArrayList<SecurityMechanism>();
        for(int i=0; i < values.length; i++) {
            available.add(SecurityMechanism.valueOf(values[i]));
        }

        SecurityMechanism wscSecurityMechanism
                = SecurityMechanism.valueOf(wizardBean.getWscSecurityMechanism());
        if( !available.contains(wscSecurityMechanism) ) {
            wscSecurityMechanism = SecurityMechanism.STS_SECURITY;
            wizardBean.setWscSecurityMechanism(wscSecurityMechanism.toInt());

            // removed the user's choice, have them revisit step 3.
            if( wizardBean.isConfigureWsc() ) {
                wizardBean.getWizardStepBeans()[WssCreateWizardStep.SUMMARY.toInt()].setEnabled(false);
            }
        }

        setSelectedSecurityPanel(securityMechanismPanels.get(wscSecurityMechanism));
    }

    private boolean save() {
        Resources r = new Resources();
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
        AgentProvider wsp = null;
        AgentProvider wsc = null;

        try {
            wsp = initWspProvider(adminToken);
        } catch (ProviderException ex) {
            showSaveErrorPopup(r.getString(this, "saveErrorSummary"),
                               r.getString(this, "saveErrorDetailWspExists"));
            getWizardBean().gotoStep(WssCreateWizardStep.WSP_ENDPOINT_SECURITY.toInt());
            Logger.getLogger(WssCreateWizardHandler.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        try {
            wsc = initWscProvider(adminToken, wsp);
        } catch (ProviderException ex) {
            showSaveErrorPopup(r.getString(this, "saveErrorSummary"),
                               r.getString(this, "saveErrorDetailWscExists"));
            getWizardBean().gotoStep(WssCreateWizardStep.WSC_SERVICENAME_SECURITY.toInt());
            Logger.getLogger(WssCreateWizardHandler.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        try {
            wsp.store();
            if( wsc != null ) {
                wsc.store();
            }
        } catch(ProviderException ex) {
            showSaveErrorPopup(r.getString(this, "saveErrorSummary"),
                               r.getString(this, "saveErrorDetail"));
            getWizardBean().gotoStep(WssCreateWizardStep.SUMMARY.toInt());
            Logger.getLogger(WssCreateWizardHandler.class.getName()).log(Level.SEVERE, null, ex);
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

    private AgentProvider initWscProvider(SSOToken adminToken, AgentProvider wsp) 
            throws ProviderException
    {
        WssCreateWizardBean wizardBean = (WssCreateWizardBean)getWizardBean();

        if( !wizardBean.isConfigureWsc() ) {
            return null;
        }

        // the name will be the service name
        AgentProvider wsc = new AgentProvider();
        wsc.init(wizardBean.getWscServiceName(), ProviderConfig.WSC, adminToken, false);

        // throw exception if it already exists...
        if( wsc.isExists() ) {
            throw new ProviderException();
        }

        // need to ensure the WSC has the same endpoint as the WSP
        wsc.setWSPEndpoint(wsp.getWSPEndpoint());

        ArrayList securityMechs = new ArrayList();
        SecurityMechanism wscSecurityMech = SecurityMechanism.valueOf(wizardBean.getWscSecurityMechanism());
        securityMechs.add( wscSecurityMech.toConfigString() );
        wsc.setSecurityMechanisms(securityMechs);

        wsc.setRequestSignEnabled(wizardBean.isRequestSignatureVerified());
        wsc.setRequestEncryptEnabled(wizardBean.isRequestDecrypted());
        wsc.setRequestHeaderEncryptEnabled(wizardBean.isRequestHeaderDecrypted());
        wsc.setResponseSignEnabled(wizardBean.isResponseSigned());
        wsc.setResponseEncryptEnabled(wizardBean.isResponseEncrypted());

        wsc.setEncryptionAlgorithm(wsp.getEncryptionAlgorithm());
        wsc.setEncryptionStrength(wsp.getEncryptionStrength());

        // The Public Key Alias of Web Service Provider will be used to encrypt
        // requests and the Private Key Alias of Web Service Client will be used
        // to sign requests, from the web service client
        wsc.setKeyAlias( wsp.getPublicKeyAlias() );
        wsc.setPublicKeyAlias( wsp.getKeyAlias() );

        switch(wscSecurityMech) {
            case ANONYMOUS:
            case SAML2_HOK:
            case SAML2_SV:
            case SAML_HOK:
            case SAML_SV:
                // no additional settings needed
                break;
            case KERBEROS_TOKEN:
                wsc.setKDCServer(wizardBean.getKerberosDomainServer());
                wsc.setKDCDomain(wizardBean.getKerberosDomain());
                wsc.setKerberosServicePrincipal(wizardBean.getKerberosServicePrincipal());
                wsc.setKerberosTicketCacheDir(wizardBean.getKerberosTicketCache());

                wsp.setKDCServer(wizardBean.getKerberosDomainServer());
                wsp.setKDCDomain(wizardBean.getKerberosDomain());
                wsp.setKerberosServicePrincipal(wizardBean.getKerberosServicePrincipal());
                // schema issue not lining up with the api
                // wsp.setKerberosTicketCacheDir(wizardBean.getKerberosTicketCache());
                break;
            case STS_SECURITY:
                List stsConfigs = ProviderUtils.getAllSTSConfig();
                Iterator i = stsConfigs.iterator();
                while( i.hasNext() ) {
                    STSConfig stsConfig = (STSConfig) i.next();
                    // Needed to workaround issue with
                    // ProviderUtils.getAllSTSConfig not having type set
                    // appropriately.
                    stsConfig.setType(TrustAuthorityConfig.STS_TRUST_AUTHORITY);
                    if( stsConfig.getName().equals(wizardBean.getStsConfigurationName()) ) {
                        wsc.setTrustAuthorityConfig(stsConfig);
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
                wsc.setUsers(userCreds);
                if( wsp.getAuthenticationChain().equalsIgnoreCase("[EMPTY]") ) {
                    wsp.setUsers(userCreds);
                }
                break;
            case X509_TOKEN:
                wsc.setSigningRefType(X509SigningRefType.valueOf(wizardBean.getX509TokenSigningReferenceType()).toConfigString());
                wsp.setSigningRefType(X509SigningRefType.valueOf(wizardBean.getX509TokenSigningReferenceType()).toConfigString());
                break;
        }

        return wsc;
    }

    private AgentProvider initWspProvider(SSOToken adminToken) 
            throws ProviderException
    {
        WssCreateWizardBean wizardBean = (WssCreateWizardBean)getWizardBean();
        AgentProvider wsp = new AgentProvider();

        // the name will be the service end point
        wsp = new AgentProvider();
        wsp.init(wizardBean.getWspServiceEndPoint(), ProviderConfig.WSP, adminToken, true);

        // throw exception if it already exists...
        if( wsp.isExists() ) {
            throw new ProviderException();
        }


        wsp.setWSPEndpoint(wizardBean.getWspServiceEndPoint());

        ArrayList securityMechs = new ArrayList();
        for( int i : wizardBean.getWspSecurityMechanisms() ) {
            SecurityMechanism mechanisms = SecurityMechanism.valueOf(i);
            securityMechs.add(mechanisms.toConfigString());
        }
        wsp.setSecurityMechanisms(securityMechs);

        wsp.setAuthenticationChain(wizardBean.getWspAuthenticationChain());
        wsp.setResponseSignEnabled(wizardBean.isResponseSigned());
        wsp.setResponseEncryptEnabled(wizardBean.isResponseEncrypted());
        wsp.setRequestSignEnabled(wizardBean.isRequestSignatureVerified());
        wsp.setRequestHeaderEncryptEnabled(wizardBean.isRequestHeaderDecrypted());
        wsp.setRequestEncryptEnabled(wizardBean.isRequestDecrypted());

        switch( EncryptionAlgorithm.valueOf(wizardBean.getEncryptionAlgorithm()) ) {
            case AES_128:
                wsp.setEncryptionAlgorithm("AES");
                wsp.setEncryptionStrength(128);
                break;
            case AES_192:
                wsp.setEncryptionAlgorithm("AES");
                wsp.setEncryptionStrength(192);
                break;
            case AES_256:
                wsp.setEncryptionAlgorithm("AES");
                wsp.setEncryptionStrength(256);
                break;
            case TRIPLEDES_0:
                wsp.setEncryptionAlgorithm("DESede");
                wsp.setEncryptionStrength(0);
                break;
            case TRIPLEDES_112:
                wsp.setEncryptionAlgorithm("DESede");
                wsp.setEncryptionStrength(112);
                break;
            case TRIPLEDES_168:
                wsp.setEncryptionAlgorithm("DESede");
                wsp.setEncryptionStrength(168);
                break;
        }

        // The Public Key Alias of Web Service Client will be used to
        // encrypt responses and the Private Key Alias of Web Service Provider
        // will be used to sign responses, from the web service provider
        wsp.setKeyAlias(wizardBean.getPrivateKeyAlias());
        wsp.setPublicKeyAlias(wizardBean.getPublicKeyAlias());

        return wsp;
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
