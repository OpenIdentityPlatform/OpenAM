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
 * $Id: WssProfileDao.java,v 1.7 2009/10/22 23:31:09 ggennaro Exp $
 */

package com.sun.identity.admin.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.admin.model.EditableSelectOneBean;
import com.sun.identity.admin.model.EncryptionAlgorithm;
import com.sun.identity.admin.model.SamlAttributeMapItem;
import com.sun.identity.admin.model.SamlAttributesTableBean;
import com.sun.identity.admin.model.SecurityMechanism;
import com.sun.identity.admin.model.SecurityMechanismPanelBean;
import com.sun.identity.admin.model.StsClientProfileBean;
import com.sun.identity.admin.model.StsProfileBean;
import com.sun.identity.admin.model.TokenConversionType;
import com.sun.identity.admin.model.UserCredentialItem;
import com.sun.identity.admin.model.UserCredentialsTableBean;
import com.sun.identity.admin.model.WscProfileBean;
import com.sun.identity.admin.model.WspProfileBean;
import com.sun.identity.admin.model.WssProfileBean;
import com.sun.identity.admin.model.X509SigningRefType;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchOpModifier;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.wss.provider.ProviderConfig;
import com.sun.identity.wss.provider.ProviderException;
import com.sun.identity.wss.provider.ProviderUtils;
import com.sun.identity.wss.provider.STSConfig;
import com.sun.identity.wss.provider.TrustAuthorityConfig;
import com.sun.identity.wss.provider.plugins.AgentProvider;
import com.sun.identity.wss.provider.plugins.STSAgent;
import com.sun.identity.wss.security.PasswordCredential;
import com.sun.identity.wss.security.WSSUtils;


public class WssProfileDao {

    // agent service schema
    private static final String SERVICE_NAME_AGENT = "AgentService";
    private static final String AGENT_TYPE_KEY = "AgentType";
    private static final String AGENT_TYPE_STS = "STSAgent";
    private static final String AGENT_TYPE_WSC = "WSCAgent";
    private static final String AGENT_TYPE_WSP = "WSPAgent";
    private static final String RESERVED_WSP = "wsp";

    // WSCAgent
    private static final String KERBEROS_TICKET_CACHE_DIR 
        = "KerberosTicketCacheDir";
    
    // WSPAgent
    private static final String KERBEROS_KEY_TAB_FILE = "KerberosKeyTabFile";
    private static final String AUTHENTICATION_CHAIN = "authenticationChain";
    private static final String TOKEN_CONVERSION_TYPE = "TokenConversionType";
    
    // STSAgent
    private static final String STS_ENDPOINT = "STSEndpoint";
    private static final String STS_MEX_ENDPOINT = "STSMexEndpoint";
    
    // hosted sts schema
    private static final String SERVICE_NAME_STS = "sunFAMSTSService";
    private static final String HOSTED_STS_ENDPOINT = "stsEndPoint";
    private static final String HOSTED_STS_AUTHENTICATION_CHAIN 
        = "AuthenticationChain";
    private static final String STS_ISSUER = "stsIssuer";
    private static final String STS_TOKEN_LIFETIME = "stsLifetime";
    private static final String STS_CERT_ALIAS = "stsCertAlias";
    private static final String STS_END_USER_TOKEN_PLUGIN_CLASS 
        = "com.sun.identity.wss.sts.clientusertoken";
    private static final String TRUSTED_ISSUERS = "trustedIssuers";
    private static final String TRUSTED_IP_ADDRESSES = "trustedIPAddresses";
    
    // common
    private static final String SECURITY_MECH = "SecurityMech";
    private static final String KERBEROS_DOMAIN = "KerberosDomain";
    private static final String KERBEROS_DOMAIN_SERVER = "KerberosDomainServer";
    private static final String KERBEROS_SERVICE_PRINCIPAL 
        = "KerberosServicePrincipal";
    private static final String SIGNING_REF_TYPE = "SigningRefType";
    private static final String STS_CLIENT_PROFILE_NAME = "STS";
    private static final String USERNAME_CREDENTIALS = "UserCredential";

    private static final String ENCRYPTION_STRENGTH = "EncryptionStrength";
    private static final String ENCRYPTION_ALGORITHM = "EncryptionAlgorithm";
    private static final String ENCRYPTION_ALGORITHM_AES = "AES";
    private static final String ENCRYPTION_ALGORITHM_DESEDE = "DESede";
    private static final String IS_REQUEST_SIGN = "isRequestSign";
    private static final String IS_REQUEST_HEADER_ENCRYPT 
        = "isRequestHeaderEncrypt";
    private static final String IS_REQUEST_ENCRYPT = "isRequestEncrypt";
    private static final String IS_RESPONSE_SIGN = "isResponseSign";
    private static final String IS_RESPONSE_ENCRYPT = "isResponseEncrypt";
    private static final String PRIVATE_KEY_ALIAS = "privateKeyAlias";
    private static final String PUBLIC_KEY_ALIAS = "publicKeyAlias";

    private static final String SAML_ATTRIBUTE_MAPPING = "SAMLAttributeMapping";
    private static final String NAME_ID_MAPPER = "NameIDMapper";
    private static final String ATTRIBUTE_NAMESPACE = "AttributeNamespace";
    private static final String INCLUDE_MEMBERSHIPS = "includeMemberships";

    private static final String WSP_ENDPOINT = "WSPEndpoint";
    
    private static final Comparator<WspProfileBean> WSP_PROFILE_COMPARATOR =
        new Comparator<WspProfileBean>() {
            public int compare(WspProfileBean a, WspProfileBean b) {
                
                if( a == b ) {
                    return 0;
                } else if( a == null ) {
                    return -1;
                } else if( b == null ) {
                    return 1;
                } else if( a.getEndPoint().compareToIgnoreCase(b.getEndPoint()) == 0 ) {
                    return a.getProfileName().compareTo(b.getProfileName());
                } else {
                    return a.getEndPoint().compareToIgnoreCase(b.getEndPoint());
                }
            }
        };

    
    //--------------------------------------------------------------------------

    WssProfileDao() {
    }

    // get/set from map for primitives -----------------------------------------
    
    @SuppressWarnings("unchecked")
    private static boolean getBooleanValue(Map map, String keyName) {
        String value = getStringValue(map, keyName);
        return Boolean.valueOf(value).booleanValue();
    }

    @SuppressWarnings("unchecked")
    private static void setBooleanValue(Map map, String keyName, boolean value) {
        setStringValue(map, keyName, String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    private static void setSetValue(Map map, String keyName, Set set) {
        
        if( map != null && keyName != null ) {
            map.put(keyName, set);
        }
    }
    
    @SuppressWarnings("unchecked")
    private static int getIntValue(Map map, String keyName) {
        String value = getStringValue(map, keyName);
        return value == null ? -1 : Integer.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private static void setIntValue(Map map, String keyName, int value) {
        setStringValue(map, keyName, String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    private static ArrayList<String> getListValue(Map map, String keyName) {
        ArrayList<String> a = new ArrayList<String>();
        if( map != null && map.get(keyName) instanceof Set ) {

            Set<String> values = (Set<String>) map.get(keyName);
            if( values != null ) {
                a.addAll(values);
            }
        }
        return a;
    }
   
    @SuppressWarnings("unchecked")
    private static void setListValue(Map map, String keyName, List list) {

        if( map != null && keyName != null ) {
            HashSet<String> values = new HashSet<String>();
            
            if( list != null ) {
                values.addAll(list);
            }
            
            map.put(keyName, values);
        }
    }
    
    @SuppressWarnings("unchecked")
    private static String getStringValue(Map map, String keyName) {

        if( map != null && map.get(keyName) instanceof Set ) {

            Set<String> values = (Set<String>) map.get(keyName);
            if( values != null && !values.isEmpty() )
                return (String)values.iterator().next();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static void setStringValue(Map map, String keyName, String value) {

        if( map != null && keyName != null ) {

            HashSet<String> values = new HashSet<String>();
            
            if( value != null ) {
                values.add(value);
            }
            
            map.put(keyName, values);
        }
    }

    // get from map for complex types ------------------------------------------

    @SuppressWarnings("unchecked")
    private static String getEncryptionAlgorithm(Map map) {
        String algorithm = getStringValue(map, ENCRYPTION_ALGORITHM);
        int strength = getIntValue(map, ENCRYPTION_STRENGTH);
        EncryptionAlgorithm ea;

        if(algorithm.equals(ENCRYPTION_ALGORITHM_DESEDE)) {
            switch(strength) {
                case 168:
                    ea = EncryptionAlgorithm.TRIPLEDES_168;
                    break;
                case 112:
                    ea = EncryptionAlgorithm.TRIPLEDES_112;
                    break;
                default:
                    ea = EncryptionAlgorithm.TRIPLEDES_0;
                    break;
            }
        } else {
            switch(strength) {
                case 256:
                    ea = EncryptionAlgorithm.AES_256;
                    break;
                case 192:
                    ea = EncryptionAlgorithm.AES_192;
                    break;
                default:
                    ea = EncryptionAlgorithm.AES_128;
                    break;
            }
        }

        return ea.toString();
    }
    
    @SuppressWarnings("unchecked")
    private static PasswordCredential getPasswordCredential(Map map) {
        ArrayList<PasswordCredential> pcList = getPasswordCredentials(map);
        if( pcList.size() > 0 ) {
            PasswordCredential pc = pcList.get(0);
            return pc;
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private static ArrayList<PasswordCredential> getPasswordCredentials(Map map) {
        ArrayList<PasswordCredential> a = new ArrayList<PasswordCredential>();

        if(map != null && map.get(USERNAME_CREDENTIALS) != null ) {
            Set<String> values = (Set<String>)map.get(USERNAME_CREDENTIALS);
            Pattern p = Pattern.compile("UserName:(.+?)\\|UserPassword:(.+?)");

            for(String v : values) {
                StringTokenizer st = new StringTokenizer(v, ",");
                while( st.hasMoreTokens() ) {
                    String creds = st.nextToken();
                    Matcher m = p.matcher(creds);
                    if( m.matches() ) {
                        String username = m.group(1);
                        String password = m.group(2);
                        if( username != null && password != null ) {
                            PasswordCredential pc
                                = new PasswordCredential(username, password);
                            a.add(pc);
                        }
                    }
                }
            }
        }
        return a;
    }
    
    @SuppressWarnings("unchecked")
    private static SamlAttributesTableBean getSamlAttributesTable(Map map) {
        ArrayList<String> mapPairs = getListValue(map, SAML_ATTRIBUTE_MAPPING);
        Hashtable<String, String> configValues = new Hashtable<String, String>();

        if( mapPairs != null ) {
            for(String s : mapPairs) {
                if( s != null && s.contains("=") ) {
                    String assertionAttrName = s.substring(0, s.indexOf("="));
                    String localAttrName = s.substring(s.indexOf("=") + 1);

                    if( assertionAttrName.length() > 0
                            && localAttrName.length() > 0 ) {
                        configValues.put(localAttrName, assertionAttrName);
                    }
                }
            }
        }

        ArrayList<String> defaultValues = new ArrayList<String>();
        defaultValues.add("cn");
        defaultValues.add("employeenumber");
        defaultValues.add("givenname");
        defaultValues.add("mail");
        defaultValues.add("manager");
        defaultValues.add("postaladdress");
        defaultValues.add("sn");
        defaultValues.add("telephonenumber");
        defaultValues.add("uid");

        ArrayList<SamlAttributeMapItem> attributeMapItems
            = new ArrayList<SamlAttributeMapItem>();

        for(String s : defaultValues) {
            SamlAttributeMapItem item = new SamlAttributeMapItem();
            item.setCustom(false);
            item.setLocalAttributeName(s);

            if( configValues.containsKey(s) ) {
                item.setAssertionAttributeName(configValues.get(s));
                configValues.remove(s);
            } else {
                item.setAssertionAttributeName(null);
            }
            attributeMapItems.add(item);
        }

        for(String s : configValues.keySet()) {
            SamlAttributeMapItem item = new SamlAttributeMapItem();
            item.setCustom(true);
            item.setLocalAttributeName(s);
            item.setAssertionAttributeName(configValues.get(s));
            attributeMapItems.add(item);
        }

        SamlAttributesTableBean table = new SamlAttributesTableBean();
        table.setAttributeMapItems(attributeMapItems);

        return table;
    }
    
    @SuppressWarnings("unchecked")
    private static String getSecurityMechanism(Map map) {
        ArrayList<String> smList = getListValue(map, SECURITY_MECH);
        String value = null;

        if( smList.size() > 0 ) {
            SecurityMechanism sm = SecurityMechanism.valueOfConfig(smList.get(0));
            if( sm != null ) {
                value = sm.toString();
            }
        }

        return value;
    }
    
    @SuppressWarnings("unchecked")
    private static ArrayList<SecurityMechanismPanelBean> getSecurityMechanismPanels(Map map)
    {
        ArrayList<SecurityMechanismPanelBean> panels
             = new ArrayList<SecurityMechanismPanelBean>();

        if( map != null && map.get(SECURITY_MECH) != null ) {
            ArrayList<String> configValues = getListValue(map, SECURITY_MECH);
            ArrayList<SecurityMechanism> supported
                = new ArrayList<SecurityMechanism>();

            for(SecurityMechanism sm : SecurityMechanism.values()) {
                if( sm != SecurityMechanism.STS_SECURITY ) {
                    supported.add(sm);
                }
            }

            for(SecurityMechanism sm : supported) {
                SecurityMechanismPanelBean panel
                    = new SecurityMechanismPanelBean();

                panel.setExpanded(false);
                panel.setSecurityMechanism(sm);

                if( configValues != null &&
                        configValues.contains(sm.toConfigString()) ) {
                    panel.setChecked(true);
                } else {
                    panel.setChecked(false);
                }

                panels.add(panel);
            }

        }

        return panels;
    }
    
    @SuppressWarnings("unchecked")
    private static String getSigningRefType(Map map) {
        String configValue = getStringValue(map, SIGNING_REF_TYPE);
        String value = null;
        
        if( configValue != null ) {
            X509SigningRefType signingRefType 
                = X509SigningRefType.valueOfConfig(configValue);
            value = signingRefType.toString();
        }
        
        return value;
    }

    @SuppressWarnings("unchecked")
    private static String getTokenConversionType(Map map) {
        String configValue = getStringValue(map, TOKEN_CONVERSION_TYPE);
        String value = null;

        if( configValue != null ) {
            TokenConversionType tct
                = TokenConversionType.valueOfConfig(configValue);
            if( tct != null ) {
                value = tct.toString();
            }
        }

        return value;
    }
    
    @SuppressWarnings("unchecked")
    private static EditableSelectOneBean getTrustedAddresses(Map map) {
        EditableSelectOneBean esmb = new EditableSelectOneBean();

        esmb = new EditableSelectOneBean();
        esmb.setItems(getListValue(map, TRUSTED_IP_ADDRESSES));
        esmb.setValidPattern("((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)");

        return esmb;
    }

    @SuppressWarnings("unchecked")
    private static EditableSelectOneBean getTrustedIssuers(Map map) {
        EditableSelectOneBean esmb = new EditableSelectOneBean();
        esmb.setItems(getListValue(map, TRUSTED_ISSUERS));
        return esmb;
    }
    
    @SuppressWarnings("unchecked")
    private static UserCredentialsTableBean getUserCredentialsTable(Map map) {
        ArrayList<PasswordCredential> configValues
            = getPasswordCredentials(map);
        UserCredentialsTableBean table = new UserCredentialsTableBean();

        if( configValues.size() > 0 ) {
            ArrayList<UserCredentialItem> items
                = new ArrayList<UserCredentialItem>();

            for(PasswordCredential pc : configValues) {
                UserCredentialItem item = new UserCredentialItem();
                item.setUserName(pc.getUserName());
                item.setPassword(pc.getPassword());
                item.setEditing(false);
                item.setNewUserName(null);
                item.setNewPassword(null);

                items.add(item);
            }

            table.setUserCredentialItems(items);
        }

        return table;
    }

    // get from bean for complex types -----------------------------------------
    
    private static String getAlgorithm(WssProfileBean bean) {
        String value = null;

        if( bean != null ) {
            String encryptionAlgorithm = bean.getEncryptionAlgorithm();
            
            if( encryptionAlgorithm != null ) {
                EncryptionAlgorithm ea
                    = EncryptionAlgorithm.valueOf(encryptionAlgorithm);
                
                switch(ea) {
                    case AES_128:
                    case AES_192:
                    case AES_256:
                        value = ENCRYPTION_ALGORITHM_AES;
                        break;
                    case TRIPLEDES_0:
                    case TRIPLEDES_112:
                    case TRIPLEDES_168:
                        value = ENCRYPTION_ALGORITHM_DESEDE;
                        break;
                }
            }
        }

        return value;
    }
    
    private static int getStrength(WssProfileBean bean) {
        int value = -1;

        if( bean != null ) {
            String encryptionAlgorithm = bean.getEncryptionAlgorithm();
            
            if( encryptionAlgorithm != null ) {
                EncryptionAlgorithm ea
                    = EncryptionAlgorithm.valueOf(encryptionAlgorithm);

                switch(ea) {
                    case AES_128:
                        value = 128;
                        break;
                    case AES_192:
                        value = 192;
                        break;
                    case AES_256:
                        value = 256;
                        break;
                    case TRIPLEDES_0:
                        value = 0;
                        break;
                    case TRIPLEDES_112:
                        value = 112;
                        break;
                    case TRIPLEDES_168:
                        value = 168;
                        break;
                }
            }
        }

        return value;
    }
    
    private static HashSet<String> getSamlAttributes(WssProfileBean bean) {
        HashSet<String> attributeMap = new HashSet<String>();

        if( bean != null ) {
            SamlAttributesTableBean table = bean.getSamlAttributesTable();
            
            if( table != null ) {
                for(SamlAttributeMapItem item : table.getAttributeMapItems()) {
                    String assertionName = item.getAssertionAttributeName();
                    String localName = item.getLocalAttributeName();

                    if( assertionName != null && localName != null ) {
                        String entry = assertionName + "=" + localName;
                        attributeMap.add(entry);
                    }
                }
            }
            
        }

        return attributeMap;
    }
    
    private static String getTokenConversion(WspProfileBean bean) {
        String value = null;
        
        if( bean != null ) {
            String enumValue = bean.getTokenConversionType();
            
            if( enumValue != null ) {
                TokenConversionType tokenConversionType 
                    = TokenConversionType.valueOf(enumValue);
                
                if( tokenConversionType != null ) {
                    value = tokenConversionType.toConfigString();
                }
            }
            
        }
        
        return value;
    }
    
    @SuppressWarnings("unchecked")
    private static STSConfig getTrustAuthorityConfig(WscProfileBean bean) {
        STSConfig stsConfig = null;

        if( bean != null ) {

            List stsConfigs = ProviderUtils.getAllSTSConfig();
            Iterator i = stsConfigs.iterator();
            while( i.hasNext() ) {
                stsConfig = (STSConfig) i.next();

                // Needed to workaround issue with
                // ProviderUtils.getAllSTSConfig not having type set
                // appropriately.
                stsConfig.setType(TrustAuthorityConfig.STS_TRUST_AUTHORITY);
                if( stsConfig.getName().equals(bean.getStsClientProfileName()) ) {
                    break;
                }
            }
        }

        return stsConfig;
    }
    
    private static List<PasswordCredential> getUserCred(WscProfileBean bean) {
        ArrayList<PasswordCredential> a = new ArrayList<PasswordCredential>();

        if( bean != null ) {
            String uname = bean.getUserNameTokenUserName();
            String pword = bean.getUserNameTokenPassword();

            if( uname != null && pword != null ) {
                PasswordCredential pc = new PasswordCredential(uname, pword);
                a.add(pc);
            }
        }

        return a;
    }
    
    private static List<PasswordCredential> getUserCreds(WspProfileBean bean) {
        ArrayList<PasswordCredential> a = new ArrayList<PasswordCredential>();

        if( bean != null ) {
            UserCredentialsTableBean table = bean.getUserCredentialsTable();
            
            if( table != null ) {
                ArrayList<UserCredentialItem> items 
                    = table.getUserCredentialItems();
                
                for(UserCredentialItem item : items) {
                    String uname = item.getUserName();
                    String pword = item.getPassword();

                    if( uname != null && pword != null ) {
                        PasswordCredential pc = new PasswordCredential(uname, pword);
                        a.add(pc);
                    }
                }
            }
        }

        return a;

    }
    
    private static HashSet<String> getUserCreds(UserCredentialsTableBean table) {
        HashSet<String> set = new HashSet<String>();
        
        if( table != null && table.getUserCredentialItems() != null ) {
            Iterator<UserCredentialItem> i
                = table.getUserCredentialItems().iterator();
            StringBuilder userCreds = new StringBuilder();

            while( i.hasNext() ) {
                UserCredentialItem item = i.next();
                userCreds
                    .append("UserName:")
                    .append(item.getUserName())
                    .append("|")
                    .append("UserPassword:")
                    .append(item.getPassword());

                if( !i.hasNext() ) {
                    break;
                }

                userCreds.append(",");
            }

            if( userCreds.length() > 0 ) {
                HashSet<String> values = new HashSet<String>();
                values.add(userCreds.toString());
                set.add(userCreds.toString());
            }
        }
        
        return set;
    }
    
    private static String getSigningRefType(WssProfileBean bean) {
        String value = null;
        
        if( bean != null ) {
            String enumValue = bean.getX509SigningRefType();
            
            if( enumValue != null ) {
                X509SigningRefType signingRefType 
                    = X509SigningRefType.valueOf(enumValue);
                
                if( signingRefType != null ) {
                    value = signingRefType.toConfigString();
                }
            }
            
        }
        
        return value;
    }
    
    //--------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static Map getGlobalServiceAttributeDefaults(String serviceName) {
        
        SSOToken adminToken = WSSUtils.getAdminToken();

        try {
            
            ServiceSchemaManager scm 
                        = new ServiceSchemaManager(serviceName, adminToken);
            ServiceSchema globalSchema = scm.getGlobalSchema();
            
            return globalSchema.getAttributeDefaults();
            
        } catch (SSOException ssoEx) {
            throw new RuntimeException(ssoEx);
        } catch (SMSException smsEx) {
            throw new RuntimeException(smsEx);
        }
    }
    
    
    @SuppressWarnings("unchecked")
    private static Map getServiceAttributeDefaults(String subSchema) {

        SSOToken adminToken = WSSUtils.getAdminToken();
        
        try {
            
            ServiceSchemaManager scm 
                = new ServiceSchemaManager(SERVICE_NAME_AGENT, adminToken);
            ServiceSchema orgSchema = scm.getOrganizationSchema();
            ServiceSchema subOrgSchema = orgSchema.getSubSchema(subSchema);

            return subOrgSchema.getAttributeDefaults();
            
        } catch (SSOException ssoEx) {
            throw new RuntimeException(ssoEx);
        } catch (SMSException smsEx) {
            throw new RuntimeException(smsEx);
        }
    
    }
    

    @SuppressWarnings("unchecked")
    private static WscProfileBean getWscProfileBeanFromMap(Map map) {
        WscProfileBean bean = new WscProfileBean();
        
        bean.setProfileName(null);
        bean.setEndPoint(getStringValue(map, WSP_ENDPOINT));
        bean.setUsingMexEndPoint(false);
        bean.setMexEndPoint(null);
        bean.setSecurityMechanism(getSecurityMechanism(map));
        bean.setStsClientProfileName(getStringValue(map, STS_CLIENT_PROFILE_NAME));
        bean.setX509SigningRefType(getSigningRefType(map));
        
        PasswordCredential pc = getPasswordCredential(map);
        if( pc != null ) {
            bean.setUserNameTokenUserName(pc.getUserName());
            bean.setUserNameTokenPassword(pc.getPassword());
        }
        
        bean.setKerberosDomain(getStringValue(map, KERBEROS_DOMAIN));
        bean.setKerberosDomainServer(getStringValue(map, KERBEROS_DOMAIN_SERVER));
        bean.setKerberosServicePrincipal(getStringValue(map, KERBEROS_SERVICE_PRINCIPAL));
        bean.setKerberosTicketCache(getStringValue(map, KERBEROS_TICKET_CACHE_DIR));
        bean.setRequestSigned(getBooleanValue(map, IS_REQUEST_SIGN));
        bean.setRequestHeaderEncrypted(getBooleanValue(map, IS_REQUEST_HEADER_ENCRYPT));
        bean.setRequestEncrypted(getBooleanValue(map, IS_REQUEST_ENCRYPT));
        bean.setResponseSignatureVerified(getBooleanValue(map, IS_RESPONSE_SIGN));
        bean.setResponseDecrypted(getBooleanValue(map, IS_RESPONSE_ENCRYPT));
        bean.setEncryptionAlgorithm(getEncryptionAlgorithm(map));
        bean.setPublicKeyAlias(getStringValue(map, PUBLIC_KEY_ALIAS));
        bean.setPrivateKeyAlias(getStringValue(map, PRIVATE_KEY_ALIAS));
        bean.setAttributeNamespace(getStringValue(map, ATTRIBUTE_NAMESPACE));
        bean.setNameIdMapper(getStringValue(map, NAME_ID_MAPPER));
        bean.setIncludeMemberships(getBooleanValue(map, INCLUDE_MEMBERSHIPS));
        bean.setSamlAttributesTable(getSamlAttributesTable(map));
        return bean;
    }
    
    @SuppressWarnings("unchecked")
    private static WspProfileBean getWspProfileBeanFromMap(Map map) {
        WspProfileBean bean = new WspProfileBean();
        
        bean.setProfileName(null);
        bean.setEndPoint(getStringValue(map, WSP_ENDPOINT));
        bean.setSecurityMechanismPanels(getSecurityMechanismPanels(map));
        bean.setKerberosDomain(getStringValue(map, KERBEROS_DOMAIN));
        bean.setKerberosDomainServer(getStringValue(map, KERBEROS_DOMAIN_SERVER));
        bean.setKerberosServicePrincipal(getStringValue(map, KERBEROS_SERVICE_PRINCIPAL));
        bean.setKerberosKeyTabFile(getStringValue(map, KERBEROS_KEY_TAB_FILE));
        bean.setUserCredentialsTable(getUserCredentialsTable(map));
        bean.setX509SigningRefType(getSigningRefType(map));
        bean.setAuthenticationChain(getStringValue(map, AUTHENTICATION_CHAIN));
        bean.setTokenConversionType(getTokenConversionType(map));
        bean.setRequestSigned(getBooleanValue(map, IS_REQUEST_SIGN));
        bean.setRequestHeaderEncrypted(getBooleanValue(map, IS_REQUEST_HEADER_ENCRYPT));
        bean.setRequestEncrypted(getBooleanValue(map, IS_REQUEST_ENCRYPT));
        bean.setResponseSignatureVerified(getBooleanValue(map, IS_RESPONSE_SIGN));
        bean.setResponseDecrypted(getBooleanValue(map, IS_RESPONSE_ENCRYPT));
        bean.setEncryptionAlgorithm(getEncryptionAlgorithm(map));
        bean.setPrivateKeyAlias(getStringValue(map, PRIVATE_KEY_ALIAS));
        bean.setPublicKeyAlias(getStringValue(map, PUBLIC_KEY_ALIAS));
        bean.setAttributeNamespace(getStringValue(map, ATTRIBUTE_NAMESPACE));
        bean.setNameIdMapper(getStringValue(map, NAME_ID_MAPPER));
        bean.setIncludeMemberships(getBooleanValue(map, INCLUDE_MEMBERSHIPS));
        bean.setSamlAttributesTable(getSamlAttributesTable(map));
        
        return bean;
    }

    @SuppressWarnings("unchecked")
    private static StsClientProfileBean getStsClientProfileBeanFromMap(Map map) {
        StsClientProfileBean bean = new StsClientProfileBean();

        bean.setProfileName(null);
        bean.setEndPoint(getStringValue(map, STS_ENDPOINT));
        bean.setMexEndPoint(getStringValue(map, STS_MEX_ENDPOINT));
        if(bean.getMexEndPoint() != null && bean.getMexEndPoint().length() > 0) {
            bean.setUsingMexEndPoint(true);
        } else {
            bean.setUsingMexEndPoint(false);
        }
        bean.setSecurityMechanism(getSecurityMechanism(map));
        bean.setStsClientProfileName(getStringValue(map, STS_CLIENT_PROFILE_NAME));
        bean.setX509SigningRefType(getSigningRefType(map));

        PasswordCredential pc = getPasswordCredential(map);
        if( pc != null ) {
            bean.setUserNameTokenUserName(pc.getUserName());
            bean.setUserNameTokenPassword(pc.getPassword());
        }

        bean.setKerberosDomain(getStringValue(map, KERBEROS_DOMAIN));
        bean.setKerberosDomainServer(getStringValue(map, KERBEROS_DOMAIN_SERVER));
        bean.setKerberosServicePrincipal(getStringValue(map, KERBEROS_SERVICE_PRINCIPAL));
        bean.setKerberosTicketCache(getStringValue(map, KERBEROS_TICKET_CACHE_DIR));
        bean.setRequestSigned(getBooleanValue(map, IS_REQUEST_SIGN));
        bean.setRequestHeaderEncrypted(getBooleanValue(map, IS_REQUEST_HEADER_ENCRYPT));
        bean.setRequestEncrypted(getBooleanValue(map, IS_REQUEST_ENCRYPT));
        bean.setResponseSignatureVerified(getBooleanValue(map, IS_RESPONSE_SIGN));
        bean.setResponseDecrypted(getBooleanValue(map, IS_RESPONSE_ENCRYPT));
        bean.setEncryptionAlgorithm(getEncryptionAlgorithm(map));
        bean.setPublicKeyAlias(getStringValue(map, PUBLIC_KEY_ALIAS));
        bean.setPrivateKeyAlias(getStringValue(map, PRIVATE_KEY_ALIAS));
        bean.setAttributeNamespace(getStringValue(map, ATTRIBUTE_NAMESPACE));
        bean.setNameIdMapper(getStringValue(map, NAME_ID_MAPPER));
        bean.setIncludeMemberships(getBooleanValue(map, INCLUDE_MEMBERSHIPS));
        bean.setSamlAttributesTable(getSamlAttributesTable(map));
        
        return bean;
    }    
    
    @SuppressWarnings("unchecked")
    private static StsProfileBean getStsProfileBeanFromMap(Map map) {
        StsProfileBean bean = new StsProfileBean();

        bean.setProfileName(null);
        bean.setEndPoint(getStringValue(map, HOSTED_STS_ENDPOINT));
        bean.setMexEndPoint(getStringValue(map, STS_MEX_ENDPOINT));

        bean.setIssuer(getStringValue(map, STS_ISSUER));
        
        // for the ui, use seconds instead of milliseconds
        int millis = getIntValue(map, STS_TOKEN_LIFETIME);
        bean.setTokenLifetime(millis / 1000);

        bean.setCertAlias(getStringValue(map, STS_CERT_ALIAS));
        bean.setTokenPluginClass(getStringValue(map, STS_END_USER_TOKEN_PLUGIN_CLASS));

        bean.setSecurityMechanismPanels(getSecurityMechanismPanels(map));
        bean.setKerberosDomain(getStringValue(map, KERBEROS_DOMAIN));
        bean.setKerberosDomainServer(getStringValue(map, KERBEROS_DOMAIN_SERVER));
        bean.setKerberosServicePrincipal(getStringValue(map, KERBEROS_SERVICE_PRINCIPAL));
        bean.setKerberosKeyTabFile(getStringValue(map, KERBEROS_KEY_TAB_FILE));
        bean.setUserCredentialsTable(getUserCredentialsTable(map));
        bean.setX509SigningRefType(getSigningRefType(map));
        bean.setAuthenticationChain(getStringValue(map, HOSTED_STS_AUTHENTICATION_CHAIN));

        bean.setRequestSigned(getBooleanValue(map, IS_REQUEST_SIGN));
        bean.setRequestHeaderEncrypted(getBooleanValue(map, IS_REQUEST_HEADER_ENCRYPT));
        bean.setRequestEncrypted(getBooleanValue(map, IS_REQUEST_ENCRYPT));
        bean.setResponseSignatureVerified(getBooleanValue(map, IS_RESPONSE_SIGN));
        bean.setResponseDecrypted(getBooleanValue(map, IS_RESPONSE_ENCRYPT));
        bean.setEncryptionAlgorithm(getEncryptionAlgorithm(map));
        bean.setPrivateKeyAlias(getStringValue(map, PRIVATE_KEY_ALIAS));
        bean.setPublicKeyAlias(getStringValue(map, PUBLIC_KEY_ALIAS));

        bean.setAttributeNamespace(getStringValue(map, ATTRIBUTE_NAMESPACE));
        bean.setNameIdMapper(getStringValue(map, NAME_ID_MAPPER));
        bean.setIncludeMemberships(getBooleanValue(map, INCLUDE_MEMBERSHIPS));
        bean.setSamlAttributesTable(getSamlAttributesTable(map));

        bean.setTrustedAddresses(getTrustedAddresses(map));
        bean.setTrustedIssuers(getTrustedIssuers(map));
        
        return bean;
    }
    
    //--------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public static StsClientProfileBean getDefaultStsClientProfileBean() {
        Map defaults = getServiceAttributeDefaults(AGENT_TYPE_STS);
        StsClientProfileBean bean = getStsClientProfileBeanFromMap(defaults);
        return bean;
    }
    
    
    @SuppressWarnings("unchecked")
    public static WscProfileBean getDefaultWscProfileBean() {
        Map defaults = getServiceAttributeDefaults(AGENT_TYPE_WSC);
        WscProfileBean wscProfileBean = getWscProfileBeanFromMap(defaults);
        
        // for ease of use, preset saml attributes
        SamlAttributesTableBean table = wscProfileBean.getSamlAttributesTable();
        if( table != null ) {
            for(SamlAttributeMapItem item : table.getAttributeMapItems()) {
                if( !item.isCustom() 
                        && item.getAssertionAttributeName() == null) {
                   item.setAssertionAttributeName(item.getLocalAttributeName());
                }
            }
        }
        
        return wscProfileBean;
    }
    
    
    @SuppressWarnings("unchecked")
    public static WspProfileBean getDefaultWspProfileBean() {
        Map defaults = getServiceAttributeDefaults(AGENT_TYPE_WSP);
        WspProfileBean wspProfileBean = getWspProfileBeanFromMap(defaults);
        
        // for ease of use, preset saml attributes
        SamlAttributesTableBean table = wspProfileBean.getSamlAttributesTable();
        if( table != null ) {
            for(SamlAttributeMapItem item : table.getAttributeMapItems()) {
                if( !item.isCustom() 
                        && item.getAssertionAttributeName() == null) {
                   item.setAssertionAttributeName(item.getLocalAttributeName());
                }
            }
        }
        
        return wspProfileBean;
    }
    
    
    @SuppressWarnings("unchecked")
    public static StsProfileBean getHostedStsProfileBean() {
        Map stsConfigMap = getGlobalServiceAttributeDefaults(SERVICE_NAME_STS);
        StsProfileBean bean = getStsProfileBeanFromMap(stsConfigMap);

        // TODO: Schema doesn't have mex end point defined
        if( bean.getMexEndPoint() == null ) {
            bean.setMexEndPoint(bean.getEndPoint() + "/mex");
        }
        
        return bean;
    }
    

    @SuppressWarnings("unchecked")
    public static WspProfileBean getWspProfileBeanByEndPoint(String endPoint) {
        WspProfileBean wspProfileBean = null;
        SSOToken adminToken = WSSUtils.getAdminToken();

        try {
            
            // TODO: parameterize realm?
            AMIdentityRepository idRepo 
                = new AMIdentityRepository(adminToken, "/");
            
            HashSet<String> set;
            HashMap<String, Set<String>> searchMap 
                = new HashMap<String, Set<String>>();

            set = new HashSet<String>();
            set.add(AGENT_TYPE_WSP);
            searchMap.put(AGENT_TYPE_KEY, set);

            set = new HashSet<String>();
            set.add(endPoint);
            searchMap.put(WSP_ENDPOINT, set);
            
            IdSearchControl searchControl = new IdSearchControl();
            searchControl.setAllReturnAttributes(true);
            searchControl.setTimeOut(0);
            searchControl.setSearchModifiers(IdSearchOpModifier.OR, searchMap);
            
            IdSearchResults searchResults
                = idRepo.searchIdentities(IdType.AGENT, "*", searchControl);
            
            Set<AMIdentity> wspAgents = searchResults.getSearchResults();
            for(AMIdentity wspAgent : wspAgents) {
                // Old console didn't enforce uniqueness of end points
                if( !wspAgent.getName().equalsIgnoreCase(RESERVED_WSP) ) {
                    Map map = wspAgent.getAttributes();
                    wspProfileBean = getWspProfileBeanFromMap(map);
                    wspProfileBean.setProfileName(wspAgent.getName());
                    break;
                }
            }
            
        } catch (SSOException ssoEx) {
            throw new RuntimeException(ssoEx);
        } catch (IdRepoException irEx) {
            throw new RuntimeException(irEx);
        }
        
        return wspProfileBean;    
    }
    
    
    /**
     * Performs a partial match, based on provided end point, of WSP profiles.
     * @param ep - End point
     * @return ArrayList of WspProfileBean objects.
     */
    @SuppressWarnings("unchecked")
    public static ArrayList<WspProfileBean> getMatchingWspProfiles(String ep) {
        
       ArrayList<WspProfileBean> matches = new ArrayList<WspProfileBean>();
       SSOToken adminToken = WSSUtils.getAdminToken();

       try {
           // TODO: parameterize realm?
           AMIdentityRepository idRepo 
               = new AMIdentityRepository(adminToken, "/");
   
           HashSet<String> set = new HashSet<String>();
           set.add(AGENT_TYPE_WSP);

           HashMap<String, Set<String>> searchMap 
               = new HashMap<String, Set<String>>();
           searchMap.put(AGENT_TYPE_KEY, set);
   
           IdSearchControl searchControl = new IdSearchControl();
           searchControl.setAllReturnAttributes(true);
           searchControl.setTimeOut(0);
           searchControl.setSearchModifiers(IdSearchOpModifier.OR, searchMap);
   
           IdSearchResults searchResults
               = idRepo.searchIdentities(IdType.AGENT, "*", searchControl);
       
           Set<AMIdentity> wspAgents = searchResults.getSearchResults();
           for( AMIdentity wspAgent : wspAgents ) {
               
               Map map = wspAgent.getAttributes();
               WspProfileBean wspProfile = getWspProfileBeanFromMap(map);
               
               if( wspProfile.getEndPoint() != null
                       && wspProfile.getEndPoint().startsWith(ep)
                       && !wspAgent.getName().equalsIgnoreCase(RESERVED_WSP)) {
                   
                   wspProfile.setProfileName(wspAgent.getName());
                   matches.add(wspProfile);
               }
           }
       
           Collections.sort(matches, WSP_PROFILE_COMPARATOR);
       
       } catch (SSOException ssoEx) {
           throw new RuntimeException(ssoEx);
       } catch (IdRepoException irEx) {
           throw new RuntimeException(irEx);
       }
   
       return matches;
    }
    

    @SuppressWarnings("unchecked")
    public static boolean stsAgentExists(String stsAgentName) {

        List stsConfigs = ProviderUtils.getAllSTSConfig();
        Iterator i = stsConfigs.iterator();
        while( i.hasNext() ) {
            STSConfig stsConfig = (STSConfig) i.next();
            if( stsConfig.getName().equalsIgnoreCase(stsAgentName) ) {
                return true;
            }
        }

        return false;
    }
    
    
    public static STSAgent getStsAgent(StsClientProfileBean bean) 
        throws ProviderException 
    {
        STSAgent stsClient = null;
        
        if( bean != null ) {
            stsClient = new STSAgent();
            SSOToken adminToken = WSSUtils.getAdminToken();

            stsClient.init(bean.getProfileName(), TrustAuthorityConfig.STS_TRUST_AUTHORITY, adminToken);
            
            stsClient.setEndpoint(bean.getEndPoint());
            stsClient.setMexEndpoint(bean.getMexEndPoint());
            
            if( bean.getSecurityMechanism() != null ) {
                SecurityMechanism securityMechanism = SecurityMechanism.valueOf(bean.getSecurityMechanism());

                ArrayList<String> secMech = new ArrayList<String>();
                secMech.add(securityMechanism.toConfigString());
                stsClient.setSecurityMechs(secMech);
                
                switch(securityMechanism) {
                    case KERBEROS_TOKEN:
                        stsClient.setKDCDomain(bean.getKerberosDomain());
                        stsClient.setKDCServer(bean.getKerberosDomainServer());
                        stsClient.setKerberosServicePrincipal(bean.getKerberosServicePrincipal());
                        stsClient.setKerberosTicketCacheDir(bean.getKerberosTicketCache());
                        break;
                    case STS_SECURITY:
                        stsClient.setSTSConfigName(bean.getStsClientProfileName());
                        break;
                    case USERNAME_TOKEN:
                    case USERNAME_TOKEN_PLAIN:
                        stsClient.setUsers(getUserCred(bean));
                        break;
                    case X509_TOKEN:
                        stsClient.setSigningRefType(getSigningRefType(bean));
                        break;
                }
            }
            
            stsClient.setRequestSignEnabled(bean.isRequestSigned());
            stsClient.setRequestHeaderEncryptEnabled(bean.isRequestHeaderEncrypted());
            stsClient.setRequestEncryptEnabled(bean.isRequestEncrypted());
            stsClient.setResponseSignEnabled(bean.isResponseSignatureVerified());
            stsClient.setResponseEncryptEnabled(bean.isResponseDecrypted());
            stsClient.setKeyAlias(bean.getPrivateKeyAlias());
            stsClient.setPublicKeyAlias(bean.getPublicKeyAlias());
            stsClient.setEncryptionAlgorithm(getAlgorithm(bean));
            stsClient.setEncryptionStrength(getStrength(bean));
            
            stsClient.setSAMLAttributeMapping(getSamlAttributes(bean));
            stsClient.setNameIDMapper(bean.getNameIdMapper());
            stsClient.setSAMLAttributeNamespace(bean.getAttributeNamespace());
            stsClient.setIncludeMemberships(bean.isIncludeMemberships());
        }

        return stsClient;
    }

    public static AgentProvider getAgentProvider(WscProfileBean bean) 
        throws ProviderException 
    {
        AgentProvider wsc = null;
        
        if( bean != null ) {
            wsc = new AgentProvider();
            SSOToken adminToken = WSSUtils.getAdminToken();

            wsc.init(bean.getProfileName(), ProviderConfig.WSC, adminToken, false);
            
            // TODO: what about mex end point?
            wsc.setWSPEndpoint(bean.getEndPoint());
            
            if( bean.getSecurityMechanism() != null ) {
                SecurityMechanism securityMechanism = SecurityMechanism.valueOf(bean.getSecurityMechanism());

                ArrayList<String> secMech = new ArrayList<String>();
                secMech.add(securityMechanism.toConfigString());
                wsc.setSecurityMechanisms(secMech);
                
                switch(securityMechanism) {
                    case KERBEROS_TOKEN:
                        wsc.setKDCDomain(bean.getKerberosDomain());
                        wsc.setKDCServer(bean.getKerberosDomainServer());
                        wsc.setKerberosServicePrincipal(bean.getKerberosServicePrincipal());
                        wsc.setKerberosTicketCacheDir(bean.getKerberosTicketCache());
                        break;
                    case STS_SECURITY:
                        wsc.setTrustAuthorityConfig(getTrustAuthorityConfig(bean));
                        break;
                    case USERNAME_TOKEN:
                    case USERNAME_TOKEN_PLAIN:
                        wsc.setUsers(getUserCred(bean));
                        break;
                    case X509_TOKEN:
                        wsc.setSigningRefType(getSigningRefType(bean));
                        break;
                }
            }
            
            wsc.setRequestSignEnabled(bean.isRequestSigned());
            wsc.setRequestHeaderEncryptEnabled(bean.isRequestHeaderEncrypted());
            wsc.setRequestEncryptEnabled(bean.isRequestEncrypted());
            wsc.setResponseSignEnabled(bean.isResponseSignatureVerified());
            wsc.setResponseEncryptEnabled(bean.isResponseDecrypted());
            wsc.setDefaultKeyStore(true);
            wsc.setKeyAlias(bean.getPrivateKeyAlias());
            wsc.setPublicKeyAlias(bean.getPublicKeyAlias());
            wsc.setEncryptionAlgorithm(getAlgorithm(bean));
            wsc.setEncryptionStrength(getStrength(bean));
            
            wsc.setSAMLAttributeMapping(getSamlAttributes(bean));
            wsc.setNameIDMapper(bean.getNameIdMapper());
            wsc.setSAMLAttributeNamespace(bean.getAttributeNamespace());
            wsc.setIncludeMemberships(bean.isIncludeMemberships());
        }
        
        return wsc;
    }

    public static AgentProvider getAgentProvider(WspProfileBean bean) 
        throws ProviderException 
    {
        AgentProvider wsp = null;
        
        if( bean != null ) {
            wsp = new AgentProvider();
            SSOToken adminToken = WSSUtils.getAdminToken();
            
            wsp.init(bean.getProfileName(), ProviderConfig.WSP, adminToken, false);

            // TODO: what about mex end point here as well?
            wsp.setWSPEndpoint(bean.getEndPoint());
            
            if( bean.getSecurityMechanismPanels() != null ) {
                
                ArrayList<String> secMech = new ArrayList<String>();
                ArrayList<SecurityMechanismPanelBean> panels  = bean.getSecurityMechanismPanels();
                
                for(SecurityMechanismPanelBean panel : panels ) {
                    SecurityMechanism securityMechanism  = panel.getSecurityMechanism();

                    if( panel.isChecked() && securityMechanism != null ) {
                        
                        secMech.add(securityMechanism.toConfigString());
                        
                        switch(securityMechanism) {
                            case KERBEROS_TOKEN:
                                wsp.setKDCDomain(bean.getKerberosDomain());
                                wsp.setKDCServer(bean.getKerberosDomainServer());
                                wsp.setKerberosServicePrincipal(bean.getKerberosServicePrincipal());
                                wsp.setKeyTabFile(bean.getKerberosKeyTabFile());
                                break;
                            case USERNAME_TOKEN:
                            case USERNAME_TOKEN_PLAIN:
                                wsp.setUsers(getUserCreds(bean));
                                break;
                            case X509_TOKEN:
                                wsp.setSigningRefType(getSigningRefType(bean));
                                break;
                        }
                    }
                }
                wsp.setSecurityMechanisms(secMech);
                wsp.setAuthenticationChain(bean.getAuthenticationChain());
                wsp.setTokenConversionType(getTokenConversion(bean));
                
                wsp.setRequestSignEnabled(bean.isRequestSigned());
                wsp.setRequestHeaderEncryptEnabled(bean.isRequestHeaderEncrypted());
                wsp.setRequestEncryptEnabled(bean.isRequestEncrypted());
                wsp.setResponseSignEnabled(bean.isResponseSignatureVerified());
                wsp.setResponseEncryptEnabled(bean.isResponseDecrypted());
                wsp.setDefaultKeyStore(true);
                wsp.setKeyAlias(bean.getPrivateKeyAlias());
                wsp.setPublicKeyAlias(bean.getPublicKeyAlias());
                wsp.setEncryptionAlgorithm(getAlgorithm(bean));
                wsp.setEncryptionStrength(getStrength(bean));
                
                wsp.setSAMLAttributeMapping(getSamlAttributes(bean));
                wsp.setNameIDMapper(bean.getNameIdMapper());
                wsp.setSAMLAttributeNamespace(bean.getAttributeNamespace());
                wsp.setIncludeMemberships(bean.isIncludeMemberships());
            }
        }
        
        return wsp;
    }

    public static void updateHostedSts(StsProfileBean bean) 
        throws SSOException, SMSException 
    {

        if( bean == null ) {
            return;
        }

        SSOToken adminToken = WSSUtils.getAdminToken();
        ServiceSchemaManager scm = new ServiceSchemaManager(SERVICE_NAME_STS, adminToken);
        ServiceSchema globalSchema = scm.getGlobalSchema();
        HashMap<String, HashSet<Object>> attrs = new HashMap<String, HashSet<Object>>();
        
        setStringValue(attrs, STS_ISSUER, bean.getIssuer());
        setIntValue(attrs, STS_TOKEN_LIFETIME, bean.getTokenLifetime() * 1000);
        setStringValue(attrs, STS_CERT_ALIAS, bean.getCertAlias());
        setStringValue(attrs, STS_END_USER_TOKEN_PLUGIN_CLASS, bean.getTokenPluginClass());
        
        if( bean.getSecurityMechanismPanels() != null ) {
            ArrayList<String> secMechs = new ArrayList<String>();
            ArrayList<SecurityMechanismPanelBean> panels = bean.getSecurityMechanismPanels();
            
            for(SecurityMechanismPanelBean panel : panels) {
                if( panel.isChecked() ) {
                    SecurityMechanism sm = panel.getSecurityMechanism();
                    secMechs.add(sm.toConfigString());
                }
            }

            setListValue(attrs, SECURITY_MECH, secMechs);
            setStringValue(attrs, KERBEROS_DOMAIN, bean.getKerberosDomain());
            setStringValue(attrs, KERBEROS_DOMAIN_SERVER, bean.getKerberosDomainServer());
            setStringValue(attrs, KERBEROS_SERVICE_PRINCIPAL, bean.getKerberosServicePrincipal());
            setStringValue(attrs, KERBEROS_KEY_TAB_FILE, bean.getKerberosKeyTabFile());
            
            setSetValue(attrs, USERNAME_CREDENTIALS, getUserCreds(bean.getUserCredentialsTable()));
            setStringValue(attrs, SIGNING_REF_TYPE, getSigningRefType(bean));
        }
        
        setStringValue(attrs, HOSTED_STS_AUTHENTICATION_CHAIN,  bean.getAuthenticationChain());
        setBooleanValue(attrs, IS_REQUEST_SIGN, bean.isRequestSigned());
        setBooleanValue(attrs, IS_REQUEST_HEADER_ENCRYPT, bean.isRequestHeaderEncrypted());
        setBooleanValue(attrs, IS_REQUEST_ENCRYPT, bean.isRequestEncrypted());
        setBooleanValue(attrs, IS_RESPONSE_SIGN, bean.isResponseSignatureVerified());
        setBooleanValue(attrs, IS_RESPONSE_ENCRYPT, bean.isResponseDecrypted());
        setStringValue(attrs, ENCRYPTION_ALGORITHM, getAlgorithm(bean));
        setIntValue(attrs, ENCRYPTION_STRENGTH, getStrength(bean));
        setStringValue(attrs, PRIVATE_KEY_ALIAS, bean.getPrivateKeyAlias());
        setStringValue(attrs, PUBLIC_KEY_ALIAS, bean.getPublicKeyAlias());
        
        setSetValue(attrs, SAML_ATTRIBUTE_MAPPING, getSamlAttributes(bean));
        setStringValue(attrs, NAME_ID_MAPPER, bean.getNameIdMapper());
        setStringValue(attrs, ATTRIBUTE_NAMESPACE, bean.getAttributeNamespace());
        setBooleanValue(attrs, INCLUDE_MEMBERSHIPS, bean.isIncludeMemberships());
        
        setListValue(attrs, TRUSTED_IP_ADDRESSES, bean.getTrustedAddresses().getItems());
        setListValue(attrs, TRUSTED_ISSUERS,  bean.getTrustedIssuers().getItems());
        
        globalSchema.setAttributeDefaults(attrs);
    }
    
}
