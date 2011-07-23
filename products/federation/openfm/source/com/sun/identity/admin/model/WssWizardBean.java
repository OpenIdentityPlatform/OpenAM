package com.sun.identity.admin.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.faces.model.SelectItem;

import com.sun.identity.admin.dao.SigningKeysDao;
import com.sun.identity.authentication.service.ConfiguredAuthServices;

public class WssWizardBean 
    extends WizardBean
    implements Serializable 
{
    protected static final String EMPTY_LIST_VALUE = "[Empty]";
    
    
    // Lists -------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public List<SelectItem> getAuthenticationChainList() {
        List<SelectItem> items = new ArrayList<SelectItem>();
        
        ConfiguredAuthServices authServices = new ConfiguredAuthServices();
        Set<String> authChains = authServices.getChoiceValues().keySet();
        Iterator<String> i = authChains.iterator();
        
        while( i.hasNext() ) {
            String authChain = i.next();
            items.add(new SelectItem(authChain));
        }
        
        return items;
    }

    public List<SelectItem> getEncryptionAlgorithmList() {
        List<SelectItem> items = new ArrayList<SelectItem>();
        
        items.add(new SelectItem(EncryptionAlgorithm.AES_128.toString(),
                                 EncryptionAlgorithm.AES_128.toLocaleString()));
        items.add(new SelectItem(EncryptionAlgorithm.AES_192.toString(),
                                 EncryptionAlgorithm.AES_192.toLocaleString()));
        items.add(new SelectItem(EncryptionAlgorithm.AES_256.toString(),
                                 EncryptionAlgorithm.AES_256.toLocaleString()));
        items.add(new SelectItem(EncryptionAlgorithm.TRIPLEDES_0.toString(),
                                 EncryptionAlgorithm.TRIPLEDES_0.toLocaleString()));
        items.add(new SelectItem(EncryptionAlgorithm.TRIPLEDES_112.toString(),
                                 EncryptionAlgorithm.TRIPLEDES_112.toLocaleString()));
        items.add(new SelectItem(EncryptionAlgorithm.TRIPLEDES_168.toString(),
                                 EncryptionAlgorithm.TRIPLEDES_168.toLocaleString()));
        
        return items;
    }    
    
    public List<SelectItem> getKeyAliasList() {
        List<SelectItem> items = new ArrayList<SelectItem>();

        SigningKeysDao signingKeysDao = new SigningKeysDao();
        List<SigningKeyBean> signingKeys = signingKeysDao.getSigningKeyBeans();
        for( SigningKeyBean bean : signingKeys ) {
            items.add(new SelectItem(bean.getTitle()));
        }

        return items;
    }

    public List<SelectItem> getSecurityMechanismList() {
        List<SelectItem> items = new ArrayList<SelectItem>();

        items.add(new SelectItem(SecurityMechanism.ANONYMOUS.toString(),
                                 SecurityMechanism.ANONYMOUS.toLocaleString()));
        items.add(new SelectItem(SecurityMechanism.KERBEROS_TOKEN.toString(),
                                 SecurityMechanism.KERBEROS_TOKEN.toLocaleString()));
        items.add(new SelectItem(SecurityMechanism.SAML_HOK.toString(),
                                 SecurityMechanism.SAML_HOK.toLocaleString()));
        items.add(new SelectItem(SecurityMechanism.SAML_SV.toString(),
                                 SecurityMechanism.SAML_SV.toLocaleString()));
        items.add(new SelectItem(SecurityMechanism.SAML2_HOK.toString(),
                                 SecurityMechanism.SAML2_HOK.toLocaleString()));
        items.add(new SelectItem(SecurityMechanism.SAML2_SV.toString(),
                                 SecurityMechanism.SAML2_SV.toLocaleString()));
        items.add(new SelectItem(SecurityMechanism.USERNAME_TOKEN.toString(),
                                 SecurityMechanism.USERNAME_TOKEN.toLocaleString()));
        items.add(new SelectItem(SecurityMechanism.USERNAME_TOKEN_PLAIN.toString(),
                                 SecurityMechanism.USERNAME_TOKEN_PLAIN.toLocaleString()));
        items.add(new SelectItem(SecurityMechanism.X509_TOKEN.toString(),
                                 SecurityMechanism.X509_TOKEN.toLocaleString()));

        return items;
    }
    
    public List<SelectItem> getStsTypeList() {
        List<SelectItem> items = new ArrayList<SelectItem>();

        items.add(new SelectItem(SecurityTokenServiceType.OPENSSO.toString(),
                                 SecurityTokenServiceType.OPENSSO.toLocaleString()));
        items.add(new SelectItem(SecurityTokenServiceType.OTHER.toString(),
                                 SecurityTokenServiceType.OTHER.toLocaleString()));
        items.add(new SelectItem(SecurityTokenServiceType.NONE.toString(),
                                 SecurityTokenServiceType.NONE.toLocaleString()));

        return items;
    }

    public List<SelectItem> getTokenConversionTypeList() {
        List<SelectItem> items = new ArrayList<SelectItem>();
        
        items.add(new SelectItem(TokenConversionType.SSO_TOKEN.toString(),
                                 TokenConversionType.SSO_TOKEN.toLocaleString()));
        items.add(new SelectItem(TokenConversionType.SAML_TOKEN.toString(),
                                 TokenConversionType.SAML_TOKEN.toLocaleString()));
        items.add(new SelectItem(TokenConversionType.SAML2_TOKEN.toString(),
                                 TokenConversionType.SAML2_TOKEN.toLocaleString()));
        
        return items;
    }
    
    public List<SelectItem> getX509SigningRefTypeList() {
        List<SelectItem> items = new ArrayList<SelectItem>();
        
        items.add(new SelectItem(X509SigningRefType.DIRECT.toString(),
                                 X509SigningRefType.DIRECT.toLocaleString()));
        items.add(new SelectItem(X509SigningRefType.KEY_IDENTIFIER.toString(),
                                 X509SigningRefType.KEY_IDENTIFIER.toLocaleString()));
        items.add(new SelectItem(X509SigningRefType.ISSUER_SERIAL.toString(),
                                 X509SigningRefType.ISSUER_SERIAL.toLocaleString()));
        
        return items;
    }
}
