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
 * $Id: WssCreateWizardBean.java,v 1.3 2009/08/03 22:25:31 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import com.icesoft.faces.context.effects.Effect;
import com.sun.identity.admin.dao.SigningKeysDao;
import com.sun.identity.authentication.service.ConfiguredAuthServices;
import com.sun.identity.wss.provider.ProviderUtils;
import com.sun.identity.wss.provider.STSConfig;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.faces.model.SelectItem;

public class WssCreateWizardBean
        extends WizardBean
        implements Serializable
{

    private String wspServiceEndPoint;
    private Integer[] wspSecurityMechanisms;
    private String wspAuthenticationChain;

    private boolean responseSigned;
    private boolean responseEncrypted;
    private boolean requestSignatureVerified;
    private boolean requestHeaderDecrypted;
    private boolean requestDecrypted;
    private int encryptionAlgorithm;
    private String privateKeyAlias;
    private String publicKeyAlias;

    private String userNameTokenUserName;
    private String userNameTokenPassword;
    private int x509TokenSigningReferenceType;
    private String kerberosDomainServer;
    private String kerberosDomain;
    private String kerberosServicePrincipal;
    private String kerberosTicketCache;
    private String stsConfigurationName;

    private boolean configureWsc;
    private String wscServiceName;
    private int wscSecurityMechanism;

    private Effect wspServiceEndPointInputEffect;
    private Effect wspServiceEndPointMessageEffect;
    private Effect wscServiceNameInputEffect;
    private Effect wscServiceNameMessageEffect;

    private WssCreateWizardSummaryRealm summaryRealm;
    private WssCreateWizardSummaryServiceEndPoint summaryServiceEndPoint;
    private WssCreateWizardSummaryWspSecurity summaryWspSecurity;
    private WssCreateWizardSummarySignEncrypt summarySignEncrypt;
    private WssCreateWizardSummaryEncryptAlgorithm summaryEncryptAlgorithm;
    private WssCreateWizardSummaryKeyAliases summaryKeyAliases;
    private WssCreateWizardSummaryServiceName summaryServiceName;
    private WssCreateWizardSummaryWscSecurity summaryWscSecurity;



    public WssCreateWizardBean() {
        super();
        initialize();
    }

    @Override
    public void reset() {
        super.reset();
        initialize();
    }

    private void initialize() {

        this.setWspServiceEndPoint("http://<wsp-host-name:portnumber>/<webService>");
        this.setWspSecurityMechanisms( 
                    new Integer[] {
                        SecurityMechanism.SAML2_HOK.toInt(),
                        SecurityMechanism.SAML2_SV.toInt(),
                        SecurityMechanism.USERNAME_TOKEN.toInt(),
                        SecurityMechanism.X509_TOKEN.toInt()
                    }
                );
        this.setWspAuthenticationChain(null);

        this.setResponseSigned(false);
        this.setResponseEncrypted(false);
        this.setRequestSignatureVerified(true);
        this.setRequestHeaderDecrypted(false);
        this.setRequestDecrypted(false);
        
        this.setWscServiceName("WebServiceName");
        this.setWscSecurityMechanism(SecurityMechanism.STS_SECURITY.toInt());
        this.setConfigureWsc(false);

        this.setEncryptionAlgorithm(EncryptionAlgorithm.AES_256.toInt());
        this.setUserNameTokenUserName(null);
        this.setUserNameTokenPassword(null);
        this.setX509TokenSigningReferenceType(X509SigningRefType.DIRECT.toInt());
        this.setKerberosDomain(null);
        this.setKerberosDomainServer(null);
        this.setKerberosServicePrincipal(null);
        this.setKerberosTicketCache(null);

        List<SelectItem> stsConfigs = this.getStsConfigurationNameList();
        if( stsConfigs.size() == 1 ) {
            SelectItem stsConfig = stsConfigs.iterator().next();
            this.setStsConfigurationName((String) stsConfig.getValue());
        } else {
            this.setStsConfigurationName(null);
        }

        List<SelectItem> aliases = this.getPrivateKeyAliasList();
        if( aliases.size() == 1 ) {
            SelectItem alias = aliases.iterator().next();
            this.setPrivateKeyAlias(alias.getLabel());
        } else {
            this.setPrivateKeyAlias(null);
        }

        aliases = this.getPublicKeyAliasList();
        if( aliases.size() == 1 ) {
            SelectItem alias = aliases.iterator().next();
            this.setPublicKeyAlias(alias.getLabel());
        } else {
            this.setPublicKeyAlias(null);
        }

        this.setSummaryRealm(new WssCreateWizardSummaryRealm(this));
        this.setSummaryServiceEndPoint(new WssCreateWizardSummaryServiceEndPoint(this));
        this.setSummaryWspSecurity(new WssCreateWizardSummaryWspSecurity(this));
        this.setSummaryServiceName(new WssCreateWizardSummaryServiceName(this));
        this.setSummaryWscSecurity(new WssCreateWizardSummaryWscSecurity(this));
        this.setSummarySignEncrypt(new WssCreateWizardSummarySignEncrypt(this));
        this.setSummaryKeyAliases(new WssCreateWizardSummaryKeyAliases(this));
        this.setSummaryEncryptAlgorithm(new WssCreateWizardSummaryEncryptAlgorithm(this));
    }


    public List<SelectItem> getAuthenticationChainList() {
        List<SelectItem> items = new ArrayList<SelectItem>();

        ConfiguredAuthServices authServices = new ConfiguredAuthServices();
        Set authChains = authServices.getChoiceValues().keySet();
        Iterator i = authChains.iterator();

        while( i.hasNext() ) {
            String authChain = (String) i.next();
            items.add(new SelectItem(authChain));
        }

        return items;
    }

    public String getEncryptionAlgorithmLocaleString() {
        return EncryptionAlgorithm.valueOf(this.getEncryptionAlgorithm()).toLocaleString();
    }

    public List<SelectItem> getEncryptionAlgorithmList() {
        List<SelectItem> items = new ArrayList<SelectItem>();

        items.add(new SelectItem(EncryptionAlgorithm.AES_128.toInt(),
                                 EncryptionAlgorithm.AES_128.toLocaleString()));
        items.add(new SelectItem(EncryptionAlgorithm.AES_192.toInt(),
                                 EncryptionAlgorithm.AES_192.toLocaleString()));
        items.add(new SelectItem(EncryptionAlgorithm.AES_256.toInt(),
                                 EncryptionAlgorithm.AES_256.toLocaleString()));
        items.add(new SelectItem(EncryptionAlgorithm.TRIPLEDES_0.toInt(),
                                 EncryptionAlgorithm.TRIPLEDES_0.toLocaleString()));
        items.add(new SelectItem(EncryptionAlgorithm.TRIPLEDES_112.toInt(),
                                 EncryptionAlgorithm.TRIPLEDES_112.toLocaleString()));
        items.add(new SelectItem(EncryptionAlgorithm.TRIPLEDES_168.toInt(),
                                 EncryptionAlgorithm.TRIPLEDES_168.toLocaleString()));

        return items;
    }

    public List<SelectItem> getSecurityMechanismForWscList() {
        List<SelectItem> items = new ArrayList<SelectItem>();

        for(int i=0; i < getWspSecurityMechanisms().length; i++) {
            SecurityMechanism securityMechanism
                    = SecurityMechanism.valueOf(getWspSecurityMechanisms()[i]);
            items.add(new SelectItem(securityMechanism.toInt(),
                                     securityMechanism.toLocaleString()));

        }

        // STS Security is always an option for the WSC
        items.add(new SelectItem(SecurityMechanism.STS_SECURITY.toInt(),
                                 SecurityMechanism.STS_SECURITY.toLocaleString()));


        return items;
    }

    public List<SelectItem> getSecurityMechanismForWspList() {
        List<SelectItem> items = new ArrayList<SelectItem>();

        items.add(new SelectItem(SecurityMechanism.ANONYMOUS.toInt(),
                                 SecurityMechanism.ANONYMOUS.toLocaleString()));
        items.add(new SelectItem(SecurityMechanism.SAML_HOK.toInt(),
                                 SecurityMechanism.SAML_HOK.toLocaleString()));
        items.add(new SelectItem(SecurityMechanism.SAML_SV.toInt(),
                                 SecurityMechanism.SAML_SV.toLocaleString()));
        items.add(new SelectItem(SecurityMechanism.SAML2_HOK.toInt(),
                                 SecurityMechanism.SAML2_HOK.toLocaleString()));
        items.add(new SelectItem(SecurityMechanism.SAML2_SV.toInt(),
                                 SecurityMechanism.SAML2_SV.toLocaleString()));
        items.add(new SelectItem(SecurityMechanism.USERNAME_TOKEN.toInt(),
                                 SecurityMechanism.USERNAME_TOKEN.toLocaleString()));
        items.add(new SelectItem(SecurityMechanism.USERNAME_TOKEN_PLAIN.toInt(),
                                 SecurityMechanism.USERNAME_TOKEN_PLAIN.toLocaleString()));
        items.add(new SelectItem(SecurityMechanism.KERBEROS_TOKEN.toInt(),
                                 SecurityMechanism.KERBEROS_TOKEN.toLocaleString()));
        items.add(new SelectItem(SecurityMechanism.X509_TOKEN.toInt(),
                                 SecurityMechanism.X509_TOKEN.toLocaleString()));

        return items;
    }

    public List<SelectItem> getStsConfigurationNameList() {
        return StsConfiguration.getSelectItems();
    }


    public List<SelectItem> getPublicKeyAliasList() {
        List<SelectItem> items = new ArrayList<SelectItem>();

        SigningKeysDao signingKeysDao = new SigningKeysDao();
        List<SigningKeyBean> signingKeys = signingKeysDao.getSigningKeyBeans();
        for( SigningKeyBean bean : signingKeys ) {
            items.add(new SelectItem(bean.getTitle()));
        }

        return items;
    }

    public List<SelectItem> getPrivateKeyAliasList() {
        List<SelectItem> items = new ArrayList<SelectItem>();

        SigningKeysDao signingKeysDao = new SigningKeysDao();
        List<SigningKeyBean> signingKeys = signingKeysDao.getSigningKeyBeans();
        for( SigningKeyBean bean : signingKeys ) {
            items.add(new SelectItem(bean.getTitle()));
        }

        return items;
    }

    public List<SelectItem> getX509SigningReferenceTypeList() {
        List<SelectItem> items = new ArrayList<SelectItem>();

        items.add(new SelectItem(X509SigningRefType.DIRECT.toInt(),
                                 X509SigningRefType.DIRECT.toLocaleString()));
        items.add(new SelectItem(X509SigningRefType.KEY_IDENTIFIER.toInt(),
                                 X509SigningRefType.KEY_IDENTIFIER.toLocaleString()));
        items.add(new SelectItem(X509SigningRefType.ISSUER_SERIAL.toInt(),
                                 X509SigningRefType.ISSUER_SERIAL.toLocaleString()));

        return items;
    }

    /**
     * @return the wspServiceEndPoint
     */
    public String getWspServiceEndPoint() {
        return wspServiceEndPoint;
    }

    /**
     * @param wspServiceEndPoint the wspServiceEndPoint to set
     */
    public void setWspServiceEndPoint(String wspServiceEndPoint) {
        this.wspServiceEndPoint = wspServiceEndPoint;
    }

    /**
     * @return the wspSecurityMechanisms
     */
    public Integer[] getWspSecurityMechanisms() {
        return wspSecurityMechanisms;
    }

    /**
     * @param wspSecurityMechanisms the wspSecurityMechanisms to set
     */
    public void setWspSecurityMechanisms(Integer[] wspSecurityMechanisms) {
        this.wspSecurityMechanisms = wspSecurityMechanisms;
    }

    /**
     * @return the wspAuthenticationChain
     */
    public String getWspAuthenticationChain() {
        return wspAuthenticationChain;
    }

    /**
     * @param wspAuthenticationChain the wspAuthenticationChain to set
     */
    public void setWspAuthenticationChain(String wspAuthenticationChain) {
        this.wspAuthenticationChain = wspAuthenticationChain;
    }

    /**
     * @return the responseSigned
     */
    public boolean isResponseSigned() {
        return responseSigned;
    }

    /**
     * @param responseSigned the responseSigned to set
     */
    public void setResponseSigned(boolean responseSigned) {
        this.responseSigned = responseSigned;
    }

    /**
     * @return the responseEncrypted
     */
    public boolean isResponseEncrypted() {
        return responseEncrypted;
    }

    /**
     * @param responseEncrypted the responseEncrypted to set
     */
    public void setResponseEncrypted(boolean responseEncrypted) {
        this.responseEncrypted = responseEncrypted;
    }

    /**
     * @return the requestSignatureVerified
     */
    public boolean isRequestSignatureVerified() {
        return requestSignatureVerified;
    }

    /**
     * @param requestSignatureVerified the requestSignatureVerified to set
     */
    public void setRequestSignatureVerified(boolean requestSignatureVerified) {
        this.requestSignatureVerified = requestSignatureVerified;
    }

    /**
     * @return the requestHeaderDecrypted
     */
    public boolean isRequestHeaderDecrypted() {
        return requestHeaderDecrypted;
    }

    /**
     * @param requestHeaderDecrypted the requestHeaderDecrypted to set
     */
    public void setRequestHeaderDecrypted(boolean requestHeaderDecrypted) {
        this.requestHeaderDecrypted = requestHeaderDecrypted;
    }

    /**
     * @return the requestDecrypted
     */
    public boolean isRequestDecrypted() {
        return requestDecrypted;
    }

    /**
     * @param requestDecrypted the requestDecrypted to set
     */
    public void setRequestDecrypted(boolean requestDecrypted) {
        this.requestDecrypted = requestDecrypted;
    }

    /**
     * @return the encryptionAlgorithm
     */
    public int getEncryptionAlgorithm() {
        return encryptionAlgorithm;
    }

    /**
     * @param encryptionAlgorithm the encryptionAlgorithm to set
     */
    public void setEncryptionAlgorithm(int encryptionAlgorithm) {
        this.encryptionAlgorithm = encryptionAlgorithm;
    }

    /**
     * @return the privateKeyAlias
     */
    public String getPrivateKeyAlias() {
        return privateKeyAlias;
    }

    /**
     * @param privateKeyAlias the privateKeyAlias to set
     */
    public void setPrivateKeyAlias(String privateKeyAlias) {
        this.privateKeyAlias = privateKeyAlias;
    }

    /**
     * @return the publicKeyAlias
     */
    public String getPublicKeyAlias() {
        return publicKeyAlias;
    }

    /**
     * @param publicKeyAlias the publicKeyAlias to set
     */
    public void setPublicKeyAlias(String publicKeyAlias) {
        this.publicKeyAlias = publicKeyAlias;
    }

    /**
     * @return the userNameTokenUserName
     */
    public String getUserNameTokenUserName() {
        return userNameTokenUserName;
    }

    /**
     * @param userNameTokenUserName the userNameTokenUserName to set
     */
    public void setUserNameTokenUserName(String userNameTokenUserName) {
        this.userNameTokenUserName = userNameTokenUserName;
    }

    /**
     * @return the userNameTokenPassword
     */
    public String getUserNameTokenPassword() {
        return userNameTokenPassword;
    }

    /**
     * @param userNameTokenPassword the userNameTokenPassword to set
     */
    public void setUserNameTokenPassword(String userNameTokenPassword) {
        this.userNameTokenPassword = userNameTokenPassword;
    }

    /**
     * @return the x509TokenSigningReferenceType
     */
    public int getX509TokenSigningReferenceType() {
        return x509TokenSigningReferenceType;
    }

    /**
     * @param x509TokenSigningReferenceType the x509TokenSigningReferenceType to set
     */
    public void setX509TokenSigningReferenceType(int x509TokenSigningReferenceType) {
        this.x509TokenSigningReferenceType = x509TokenSigningReferenceType;
    }

    /**
     * @return the kerberosDomainServer
     */
    public String getKerberosDomainServer() {
        return kerberosDomainServer;
    }

    /**
     * @param kerberosDomainServer the kerberosDomainServer to set
     */
    public void setKerberosDomainServer(String kerberosDomainServer) {
        this.kerberosDomainServer = kerberosDomainServer;
    }

    /**
     * @return the kerberosDomain
     */
    public String getKerberosDomain() {
        return kerberosDomain;
    }

    /**
     * @param kerberosDomain the kerberosDomain to set
     */
    public void setKerberosDomain(String kerberosDomain) {
        this.kerberosDomain = kerberosDomain;
    }

    /**
     * @return the kerberosServicePrincipal
     */
    public String getKerberosServicePrincipal() {
        return kerberosServicePrincipal;
    }

    /**
     * @param kerberosServicePrincipal the kerberosServicePrincipal to set
     */
    public void setKerberosServicePrincipal(String kerberosServicePrincipal) {
        this.kerberosServicePrincipal = kerberosServicePrincipal;
    }

    /**
     * @return the kerberosTicketCache
     */
    public String getKerberosTicketCache() {
        return kerberosTicketCache;
    }

    /**
     * @param kerberosTicketCache the kerberosTicketCache to set
     */
    public void setKerberosTicketCache(String kerberosTicketCache) {
        this.kerberosTicketCache = kerberosTicketCache;
    }

    /**
     * @return the stsConfigurationName
     */
    public String getStsConfigurationName() {
        return stsConfigurationName;
    }

    /**
     * @param stsConfigurationName the stsConfigurationName to set
     */
    public void setStsConfigurationName(String stsConfigurationName) {
        this.stsConfigurationName = stsConfigurationName;
    }

    /**
     * @return the configureWsc
     */
    public boolean isConfigureWsc() {
        return configureWsc;
    }

    /**
     * @param configureWsc the configureWsc to set
     */
    public void setConfigureWsc(boolean configureWsc) {
        this.configureWsc = configureWsc;
    }

    /**
     * @return the wscServiceName
     */
    public String getWscServiceName() {
        return wscServiceName;
    }

    /**
     * @param wscServiceName the wscServiceName to set
     */
    public void setWscServiceName(String wscServiceName) {
        this.wscServiceName = wscServiceName;
    }

    /**
     * @return the wscSecurityMechanism
     */
    public int getWscSecurityMechanism() {
        return wscSecurityMechanism;
    }

    /**
     * @param wscSecurityMechanism the wscSecurityMechanism to set
     */
    public void setWscSecurityMechanism(int wscSecurityMechanism) {
        this.wscSecurityMechanism = wscSecurityMechanism;
    }

    /**
     * @return the wspServiceEndPointInputEffect
     */
    public Effect getWspServiceEndPointInputEffect() {
        return wspServiceEndPointInputEffect;
    }

    /**
     * @param wspServiceEndPointInputEffect the wspServiceEndPointInputEffect to set
     */
    public void setWspServiceEndPointInputEffect(Effect wspServiceEndPointInputEffect) {
        this.wspServiceEndPointInputEffect = wspServiceEndPointInputEffect;
    }

    /**
     * @return the wspServiceEndPointMessageEffect
     */
    public Effect getWspServiceEndPointMessageEffect() {
        return wspServiceEndPointMessageEffect;
    }

    /**
     * @param wspServiceEndPointMessageEffect the wspServiceEndPointMessageEffect to set
     */
    public void setWspServiceEndPointMessageEffect(Effect wspServiceEndPointMessageEffect) {
        this.wspServiceEndPointMessageEffect = wspServiceEndPointMessageEffect;
    }

    /**
     * @return the wscServiceNameInputEffect
     */
    public Effect getWscServiceNameInputEffect() {
        return wscServiceNameInputEffect;
    }

    /**
     * @param wscServiceNameInputEffect the wscServiceNameInputEffect to set
     */
    public void setWscServiceNameInputEffect(Effect wscServiceNameInputEffect) {
        this.wscServiceNameInputEffect = wscServiceNameInputEffect;
    }

    /**
     * @return the wscServiceNameMessageEffect
     */
    public Effect getWscServiceNameMessageEffect() {
        return wscServiceNameMessageEffect;
    }

    /**
     * @param wscServiceNameMessageEffect the wscServiceNameMessageEffect to set
     */
    public void setWscServiceNameMessageEffect(Effect wscServiceNameMessageEffect) {
        this.wscServiceNameMessageEffect = wscServiceNameMessageEffect;
    }

    /**
     * @return the summaryServiceEndPoint
     */
    public WssCreateWizardSummaryServiceEndPoint getSummaryServiceEndPoint() {
        return summaryServiceEndPoint;
    }

    /**
     * @param summaryServiceEndPoint the summaryServiceEndPoint to set
     */
    public void setSummaryServiceEndPoint(WssCreateWizardSummaryServiceEndPoint summaryServiceEndPoint) {
        this.summaryServiceEndPoint = summaryServiceEndPoint;
    }

    /**
     * @return the summaryWspSecurity
     */
    public WssCreateWizardSummaryWspSecurity getSummaryWspSecurity() {
        return summaryWspSecurity;
    }

    /**
     * @param summaryWspSecurity the summaryWspSecurity to set
     */
    public void setSummaryWspSecurity(WssCreateWizardSummaryWspSecurity summaryWspSecurity) {
        this.summaryWspSecurity = summaryWspSecurity;
    }

    /**
     * @return the summarySignEncrypt
     */
    public WssCreateWizardSummarySignEncrypt getSummarySignEncrypt() {
        return summarySignEncrypt;
    }

    /**
     * @param summarySignEncrypt the summarySignEncrypt to set
     */
    public void setSummarySignEncrypt(WssCreateWizardSummarySignEncrypt summarySignEncrypt) {
        this.summarySignEncrypt = summarySignEncrypt;
    }

    /**
     * @return the summaryEncryptAlgorithm
     */
    public WssCreateWizardSummaryEncryptAlgorithm getSummaryEncryptAlgorithm() {
        return summaryEncryptAlgorithm;
    }

    /**
     * @param summaryEncryptAlgorithm the summaryEncryptAlgorithm to set
     */
    public void setSummaryEncryptAlgorithm(WssCreateWizardSummaryEncryptAlgorithm summaryEncryptAlgorithm) {
        this.summaryEncryptAlgorithm = summaryEncryptAlgorithm;
    }

    /**
     * @return the summaryKeyAliases
     */
    public WssCreateWizardSummaryKeyAliases getSummaryKeyAliases() {
        return summaryKeyAliases;
    }

    /**
     * @param summaryKeyAliases the summaryKeyAliases to set
     */
    public void setSummaryKeyAliases(WssCreateWizardSummaryKeyAliases summaryKeyAliases) {
        this.summaryKeyAliases = summaryKeyAliases;
    }

    /**
     * @return the summaryServiceName
     */
    public WssCreateWizardSummaryServiceName getSummaryServiceName() {
        return summaryServiceName;
    }

    /**
     * @param summaryServiceName the summaryServiceName to set
     */
    public void setSummaryServiceName(WssCreateWizardSummaryServiceName summaryServiceName) {
        this.summaryServiceName = summaryServiceName;
    }

    /**
     * @return the summaryWscSecurity
     */
    public WssCreateWizardSummaryWscSecurity getSummaryWscSecurity() {
        return summaryWscSecurity;
    }

    /**
     * @param summaryWscSecurity the summaryWscSecurity to set
     */
    public void setSummaryWscSecurity(WssCreateWizardSummaryWscSecurity summaryWscSecurity) {
        this.summaryWscSecurity = summaryWscSecurity;
    }

    /**
     * @return the summaryRealm
     */
    public WssCreateWizardSummaryRealm getSummaryRealm() {
        return summaryRealm;
    }

    /**
     * @param summaryRealm the summaryRealm to set
     */
    public void setSummaryRealm(WssCreateWizardSummaryRealm summaryRealm) {
        this.summaryRealm = summaryRealm;
    }

}
