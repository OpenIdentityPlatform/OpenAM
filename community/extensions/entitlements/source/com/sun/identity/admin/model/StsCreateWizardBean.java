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
 * $Id: StsCreateWizardBean.java,v 1.2 2009/08/03 22:25:31 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import com.icesoft.faces.context.effects.Effect;
import com.sun.identity.admin.dao.SigningKeysDao;
import com.sun.identity.wss.provider.ProviderUtils;
import com.sun.identity.wss.provider.STSConfig;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.faces.model.SelectItem;

public class StsCreateWizardBean
        extends WizardBean
        implements Serializable
{
    private String serviceName;
    private String serviceEndPoint;
    private String mexEndPoint;

    private int securityMechanism;
    private String userNameTokenUserName;
    private String userNameTokenPassword;
    private int x509TokenSigningReferenceType;
    private String kerberosDomainServer;
    private String kerberosDomain;
    private String kerberosServicePrincipal;
    private String kerberosTicketCache;
    private String stsConfigurationName;

    private boolean requestSigned;
    private boolean requestHeaderEncrypted;
    private boolean requestEncrypted;
    private boolean responseSignatureVerified;
    private boolean responseDecrypted;

    private int encryptionAlgorithm;
    private String privateKeyAlias;
    private String publicKeyAlias;

    private Effect serviceNameInputEffect;
    private Effect serviceNameMessageEffect;
    private Effect serviceEndPointInputEffect;
    private Effect serviceEndPointMessageEffect;
    private Effect mexEndPointInputEffect;
    private Effect mexEndPointMessageEffect;

    private StsCreateWizardSummaryServiceName summaryServiceName;
    private StsCreateWizardSummaryServiceEndPoint summaryServiceEndPoint;
    private StsCreateWizardSummaryMexEndPoint summaryMexEndPoint;
    private StsCreateWizardSummarySecurity summarySecurity;
    private StsCreateWizardSummarySignEncrypt summarySignEncrypt;
    private StsCreateWizardSummaryEncryptAlgorithm summaryEncryptAlgorithm;
    private StsCreateWizardSummaryKeyAliases summaryKeyAliases;


    public StsCreateWizardBean() {
        super();
        initialize();
    }

    @Override
    public void reset() {
        super.reset();
        initialize();
    }

    private void initialize() {

        this.setServiceName("StsServiceName");
        this.setServiceEndPoint("http://<sts-host-name:portnumber>/<deployuri>/sts");
        this.setMexEndPoint("http://<sts-host-name:portnumber>/<deployuri>/sts/mex");

        this.setSecurityMechanism(SecurityMechanism.ANONYMOUS.toInt());

        this.setRequestSigned(true);
        this.setRequestHeaderEncrypted(false);
        this.setRequestEncrypted(false);
        this.setResponseSignatureVerified(false);
        this.setResponseDecrypted(false);

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

        this.setSummaryServiceName(new StsCreateWizardSummaryServiceName(this));
        this.setSummaryServiceEndPoint(new StsCreateWizardSummaryServiceEndPoint(this));
        this.setSummaryMexEndPoint(new StsCreateWizardSummaryMexEndPoint(this));
        this.setSummarySecurity(new StsCreateWizardSummarySecurity(this));
        this.setSummarySignEncrypt(new StsCreateWizardSummarySignEncrypt(this));
        this.setSummaryEncryptAlgorithm(new StsCreateWizardSummaryEncryptAlgorithm(this));
        this.setSummaryKeyAliases(new StsCreateWizardSummaryKeyAliases(this));
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

    public List<SelectItem> getSecurityMechanismList() {
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
        items.add(new SelectItem(SecurityMechanism.STS_SECURITY.toInt(),
                                 SecurityMechanism.STS_SECURITY.toLocaleString()));
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
     * @return the serviceName
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * @param serviceName the serviceName to set
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * @return the serviceEndPoint
     */
    public String getServiceEndPoint() {
        return serviceEndPoint;
    }

    /**
     * @param serviceEndPoint the serviceEndPoint to set
     */
    public void setServiceEndPoint(String serviceEndPoint) {
        this.serviceEndPoint = serviceEndPoint;
    }

    /**
     * @return the mexEndPoint
     */
    public String getMexEndPoint() {
        return mexEndPoint;
    }

    /**
     * @param mexEndPoint the mexEndPoint to set
     */
    public void setMexEndPoint(String mexEndPoint) {
        this.mexEndPoint = mexEndPoint;
    }

    /**
     * @return the securityMechanism
     */
    public int getSecurityMechanism() {
        return securityMechanism;
    }

    /**
     * @param securityMechanism the securityMechanism to set
     */
    public void setSecurityMechanism(int securityMechanism) {
        this.securityMechanism = securityMechanism;
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
     * @return the requestSigned
     */
    public boolean isRequestSigned() {
        return requestSigned;
    }

    /**
     * @param requestSigned the requestSigned to set
     */
    public void setRequestSigned(boolean requestSigned) {
        this.requestSigned = requestSigned;
    }

    /**
     * @return the requestHeaderEncrypted
     */
    public boolean isRequestHeaderEncrypted() {
        return requestHeaderEncrypted;
    }

    /**
     * @param requestHeaderEncrypted the requestHeaderEncrypted to set
     */
    public void setRequestHeaderEncrypted(boolean requestHeaderEncrypted) {
        this.requestHeaderEncrypted = requestHeaderEncrypted;
    }

    /**
     * @return the requestEncrypted
     */
    public boolean isRequestEncrypted() {
        return requestEncrypted;
    }

    /**
     * @param requestEncrypted the requestEncrypted to set
     */
    public void setRequestEncrypted(boolean requestEncrypted) {
        this.requestEncrypted = requestEncrypted;
    }

    /**
     * @return the responseSignatureVerified
     */
    public boolean isResponseSignatureVerified() {
        return responseSignatureVerified;
    }

    /**
     * @param responseSignatureVerified the responseSignatureVerified to set
     */
    public void setResponseSignatureVerified(boolean responseSignatureVerified) {
        this.responseSignatureVerified = responseSignatureVerified;
    }

    /**
     * @return the responseDecrypted
     */
    public boolean isResponseDecrypted() {
        return responseDecrypted;
    }

    /**
     * @param responseDecrypted the responseDecrypted to set
     */
    public void setResponseDecrypted(boolean responseDecrypted) {
        this.responseDecrypted = responseDecrypted;
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
     * @return the serviceNameInputEffect
     */
    public Effect getServiceNameInputEffect() {
        return serviceNameInputEffect;
    }

    /**
     * @param serviceNameInputEffect the serviceNameInputEffect to set
     */
    public void setServiceNameInputEffect(Effect serviceNameInputEffect) {
        this.serviceNameInputEffect = serviceNameInputEffect;
    }

    /**
     * @return the serviceNameMessageEffect
     */
    public Effect getServiceNameMessageEffect() {
        return serviceNameMessageEffect;
    }

    /**
     * @param serviceNameMessageEffect the serviceNameMessageEffect to set
     */
    public void setServiceNameMessageEffect(Effect serviceNameMessageEffect) {
        this.serviceNameMessageEffect = serviceNameMessageEffect;
    }

    /**
     * @return the serviceEndPointInputEffect
     */
    public Effect getServiceEndPointInputEffect() {
        return serviceEndPointInputEffect;
    }

    /**
     * @param serviceEndPointInputEffect the serviceEndPointInputEffect to set
     */
    public void setServiceEndPointInputEffect(Effect serviceEndPointInputEffect) {
        this.serviceEndPointInputEffect = serviceEndPointInputEffect;
    }

    /**
     * @return the serviceEndPointMessageEffect
     */
    public Effect getServiceEndPointMessageEffect() {
        return serviceEndPointMessageEffect;
    }

    /**
     * @param serviceEndPointMessageEffect the serviceEndPointMessageEffect to set
     */
    public void setServiceEndPointMessageEffect(Effect serviceEndPointMessageEffect) {
        this.serviceEndPointMessageEffect = serviceEndPointMessageEffect;
    }

    /**
     * @return the mexEndPointInputEffect
     */
    public Effect getMexEndPointInputEffect() {
        return mexEndPointInputEffect;
    }

    /**
     * @param mexEndPointInputEffect the mexEndPointInputEffect to set
     */
    public void setMexEndPointInputEffect(Effect mexEndPointInputEffect) {
        this.mexEndPointInputEffect = mexEndPointInputEffect;
    }

    /**
     * @return the mexEndPointMessageEffect
     */
    public Effect getMexEndPointMessageEffect() {
        return mexEndPointMessageEffect;
    }

    /**
     * @param mexEndPointMessageEffect the mexEndPointMessageEffect to set
     */
    public void setMexEndPointMessageEffect(Effect mexEndPointMessageEffect) {
        this.mexEndPointMessageEffect = mexEndPointMessageEffect;
    }

    /**
     * @return the summaryServiceName
     */
    public StsCreateWizardSummaryServiceName getSummaryServiceName() {
        return summaryServiceName;
    }

    /**
     * @param summaryServiceName the summaryServiceName to set
     */
    public void setSummaryServiceName(StsCreateWizardSummaryServiceName summaryServiceName) {
        this.summaryServiceName = summaryServiceName;
    }

    /**
     * @return the summaryServiceEndPoint
     */
    public StsCreateWizardSummaryServiceEndPoint getSummaryServiceEndPoint() {
        return summaryServiceEndPoint;
    }

    /**
     * @param summaryServiceEndPoint the summaryServiceEndPoint to set
     */
    public void setSummaryServiceEndPoint(StsCreateWizardSummaryServiceEndPoint summaryServiceEndPoint) {
        this.summaryServiceEndPoint = summaryServiceEndPoint;
    }

    /**
     * @return the summaryMexEndPoint
     */
    public StsCreateWizardSummaryMexEndPoint getSummaryMexEndPoint() {
        return summaryMexEndPoint;
    }

    /**
     * @param summaryMexEndPoint the summaryMexEndPoint to set
     */
    public void setSummaryMexEndPoint(StsCreateWizardSummaryMexEndPoint summaryMexEndPoint) {
        this.summaryMexEndPoint = summaryMexEndPoint;
    }

    /**
     * @return the summarySecurity
     */
    public StsCreateWizardSummarySecurity getSummarySecurity() {
        return summarySecurity;
    }

    /**
     * @param summarySecurity the summarySecurity to set
     */
    public void setSummarySecurity(StsCreateWizardSummarySecurity summarySecurity) {
        this.summarySecurity = summarySecurity;
    }

    /**
     * @return the summarySignEncrypt
     */
    public StsCreateWizardSummarySignEncrypt getSummarySignEncrypt() {
        return summarySignEncrypt;
    }

    /**
     * @param summarySignEncrypt the summarySignEncrypt to set
     */
    public void setSummarySignEncrypt(StsCreateWizardSummarySignEncrypt summarySignEncrypt) {
        this.summarySignEncrypt = summarySignEncrypt;
    }

    /**
     * @return the summaryEncryptAlgorithm
     */
    public StsCreateWizardSummaryEncryptAlgorithm getSummaryEncryptAlgorithm() {
        return summaryEncryptAlgorithm;
    }

    /**
     * @param summaryEncryptAlgorithm the summaryEncryptAlgorithm to set
     */
    public void setSummaryEncryptAlgorithm(StsCreateWizardSummaryEncryptAlgorithm summaryEncryptAlgorithm) {
        this.summaryEncryptAlgorithm = summaryEncryptAlgorithm;
    }

    /**
     * @return the summaryKeyAliases
     */
    public StsCreateWizardSummaryKeyAliases getSummaryKeyAliases() {
        return summaryKeyAliases;
    }

    /**
     * @param summaryKeyAliases the summaryKeyAliases to set
     */
    public void setSummaryKeyAliases(StsCreateWizardSummaryKeyAliases summaryKeyAliases) {
        this.summaryKeyAliases = summaryKeyAliases;
    }


    

}
