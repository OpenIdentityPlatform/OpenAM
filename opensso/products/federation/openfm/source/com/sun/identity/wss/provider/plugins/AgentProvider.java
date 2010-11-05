/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * "Portions Copyrighted [year] [name of copyght owner]"
 *
 * $Id: AgentProvider.java,v 1.41 2009/11/16 21:52:58 mallas Exp $
 *
 */

package com.sun.identity.wss.provider.plugins; 

import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Enumeration;

import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.wss.provider.ProviderConfig;
import com.sun.identity.wss.provider.ProviderUtils;
import com.sun.identity.wss.provider.TrustAuthorityConfig;
import com.sun.identity.wss.provider.ProviderException;
import com.sun.identity.wss.security.PasswordCredential;

import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchOpModifier;

/**
 * This class <code>AgentProvider</code> extends from 
 * <code>ProviderConfig</code> to manage the web services
 * server provider or the web services client configuration via Access
 * Manager Agents.
 */
public class AgentProvider extends ProviderConfig {

     // Initialize the Attributes names set
     private static Set attrNames = new HashSet();

     private static final String AGENT_PASSWORD_ATTR = "userpassword";
     private static final String AGENT_DEVICE_STATUS_ATTR = 
         "sunIdentityServerDeviceStatus";
     private static final String AGENT_TYPE_ATTR = "AgentType";
     private static final String SEC_MECH = "SecurityMech";
     private static final String WSP_ENDPOINT = "WSPEndpoint";
     private static final String WSS_PROXY_ENDPOINT = "WSPProxyEndpoint";
     private static final String KS_FILE = "KeyStoreFile";
     private static final String KS_PASSWD = "KeyStorePassword";
     private static final String KEY_PASSWD = "KeyPassword";
     private static final String RESPONSE_SIGN = "isResponseSign";
     private static final String RESPONSE_ENCRYPT = "isResponseEncrypt";
     private static final String REQUEST_SIGN = "isRequestSign";     
     private static final String REQUEST_ENCRYPT = "isRequestEncrypt";
     private static final String REQUEST_HEADER_ENCRYPT = 
         "isRequestHeaderEncrypt";
     private static final String KEY_ALIAS = "privateKeyAlias";
     private static final String KEY_TYPE = "privateKeyType";
     private static final String PUBLIC_KEY_ALIAS = "publicKeyAlias";
     private static final String STS_TRUST_AUTHORITY = "STS";
     private static final String DISCOVERY_TRUST_AUTHORITY = "Discovery";
     private static final String PROPERTY = "Property:";
     private static final String USER_NAME = "UserName";
     private static final String USER_PASSWORD = "UserPassword";
     private static final String USER_CREDENTIAL = "UserCredential";
     private static final String SERVICE_TYPE = "serviceType";
     private static final String USE_DEFAULT_KEYSTORE = "useDefaultStore";
     private static final String FORCE_AUTHENTICATION = "forceUserAuthn";
     private static final String KEEP_SECURITY_HEADERS = "keepSecurityHeaders";
     private static final String AUTHENTICATION_CHAIN = "authenticationChain";  
     private static final String SAML_ATTRIBUTE_MAPPING = 
                                 "SAMLAttributeMapping";
     private static final String INCLUDE_MEMBERSHIPS = "includeMemberships";
     private static final String SAML_ATTRIBUTE_NS = "AttributeNamespace";
     private static final String NAMEID_MAPPER = "NameIDMapper";
     private static final String KDC_SERVER = "KerberosDomainServer";
     private static final String KDC_DOMAIN = "KerberosDomain";
     private static final String KRB_SERVICE_PRINCIPAL = 
             "KerberosServicePrincipal";
     private static final String KRB_TICKET_CACHE_DIR = 
             "KerberosTicketCacheDir";
     private static final String KRB_KEYTAB_FILE = "KerberosKeyTabFile";     
     private static final String VERIFY_KRB_SIGNATURE = "isVerifyKrbSignature";
     private static final String USE_PASSTHROUGH_TOKEN = 
                                  "isPassThroughSecurityToken";
     private static final String TOKEN_CONVERSION_TYPE = "TokenConversionType";
     private static final String ENCRYPTION_ALG = "EncryptionAlgorithm";
     private static final String ENCRYPTION_STRENGTH = "EncryptionStrength";
     private static final String SIGNING_REF_TYPE = "SigningRefType";
     
     private static final String USER_TOKEN_DETECT_REPLAY = 
             "DetectUserTokenReplay";
     private static final String MESSAGE_REPLAY_DETECTION =
                                 "DetectMessageReplay";
     private static final String DNS_CLAIM = "DnsClaim";
     private static final String SIGNED_ELEMENTS = "SignedElements";
     private AMIdentityRepository idRepo;
     private static Set agentConfigAttribute;
     private static Debug debug = ProviderUtils.debug;

     // Instance variables
     private SSOToken token;
     private boolean profilePresent;

     static {
         attrNames.add(SEC_MECH);
         attrNames.add(WSP_ENDPOINT);
         attrNames.add(WSS_PROXY_ENDPOINT);
         attrNames.add(KS_FILE);
         attrNames.add(KS_PASSWD);
         attrNames.add(KEY_PASSWD);
         attrNames.add(RESPONSE_SIGN);
         attrNames.add(RESPONSE_ENCRYPT);
         attrNames.add(REQUEST_HEADER_ENCRYPT);
         attrNames.add(REQUEST_SIGN);
         attrNames.add(REQUEST_ENCRYPT);
         attrNames.add(KEY_ALIAS);
         attrNames.add(KEY_TYPE);
         attrNames.add(PUBLIC_KEY_ALIAS);
         attrNames.add(STS_TRUST_AUTHORITY);
         attrNames.add(DISCOVERY_TRUST_AUTHORITY);
         attrNames.add(USER_CREDENTIAL);
         attrNames.add(SERVICE_TYPE);
         attrNames.add(USE_DEFAULT_KEYSTORE);
         attrNames.add(FORCE_AUTHENTICATION);
         attrNames.add(KEEP_SECURITY_HEADERS);
         attrNames.add(AUTHENTICATION_CHAIN);
         attrNames.add(INCLUDE_MEMBERSHIPS);
         attrNames.add(SAML_ATTRIBUTE_MAPPING);
         attrNames.add(SAML_ATTRIBUTE_NS);
         attrNames.add(NAMEID_MAPPER);
         attrNames.add(KDC_DOMAIN);
         attrNames.add(KDC_SERVER);
         attrNames.add(KRB_SERVICE_PRINCIPAL);
         attrNames.add(KRB_TICKET_CACHE_DIR);         
         attrNames.add(KRB_KEYTAB_FILE);
         attrNames.add(VERIFY_KRB_SIGNATURE);
         attrNames.add(USE_PASSTHROUGH_TOKEN);
         attrNames.add(TOKEN_CONVERSION_TYPE);
         attrNames.add(ENCRYPTION_ALG);
         attrNames.add(ENCRYPTION_STRENGTH);
         attrNames.add(SIGNING_REF_TYPE);         
         attrNames.add(USER_TOKEN_DETECT_REPLAY);
         attrNames.add(MESSAGE_REPLAY_DETECTION);
         attrNames.add(DNS_CLAIM);
         attrNames.add(SIGNED_ELEMENTS);
     }

     public void init (String providerName, 
         String providerType, SSOToken token, boolean isEndPoint) 
         throws ProviderException {

        this.providerName = providerName;
        this.providerType = providerType;
        this.token = token;
        if(debug.messageEnabled()) {
           debug.message("AgentProvider: name = " + providerName + 
                   "type = " + providerType);
        }

        if ((providerType.equals(ProviderConfig.WSP)) && (isEndPoint)) {
            // Obtain the WSP Agent profile given its end point
            try {
                if (idRepo == null) {
                    idRepo = new AMIdentityRepository(token, "/");
                }
                IdSearchControl control = new IdSearchControl();
                control.setAllReturnAttributes(true);
                control.setTimeOut(0);

                Map kvPairMap = new HashMap();
                Set set = new HashSet();
                set.add(providerType);
                kvPairMap.put(AGENT_TYPE_ATTR, set);

                set = new HashSet();
                set.add(providerName);
                kvPairMap.put(WSP_ENDPOINT, set);

                control.setSearchModifiers(IdSearchOpModifier.OR, kvPairMap);

                IdSearchResults results = 
                    idRepo.searchIdentities(IdType.AGENT,
                    "*", control);
                Set agents = results.getSearchResults();
                if (!agents.isEmpty()) {
                    //Map attrs = (Map) results.getResultAttributes();
                    AMIdentity provider = (AMIdentity) agents.iterator().next();
                    Map attributes = null;
                    if(attrNames != null) {
                       attributes = (Map) provider.getAttributes(attrNames);
                    } else {
                       attributes = (Map) provider.getAttributes();
                    }
                    if (debug.messageEnabled()) {
                        debug.message("AgentProvider.init "
                            + "Provider Configuration using end point : " 
                            + attributes);
                    }
                    profilePresent = true;
                    parseAgentKeyValues(attributes);                    
                }

            } catch (Exception ex) {
                debug.error("AgentProvider.init: Unable to get idRepo", ex);
                throw (new ProviderException("idRepo exception: "
                    + ex.getMessage()));
            }
            return;
        }
        
        // Obtain the provider from Agent profile based on ProviderName
        try {
            AMIdentity provider = 
                new AMIdentity(token, providerName, IdType.AGENT, "/", null);
            if(!provider.isExists()) {
               if(debug.messageEnabled()) {
                  debug.message("AgentProvider.init: provider " + providerName
                          + "does not exist");
               }
               return; 
            }
            Map attributes = (Map) provider.getAttributes(attrNames);
            profilePresent = true;
            if (debug.messageEnabled()) {
                debug.message("AgentProvider.init "
                    + "Provider configuration: "
                    + attributes);
            }
            parseAgentKeyValues(attributes);
        } catch (IdRepoException ire) {            
            if(ire.getErrorCode().equals("402")) {
               //permission denied
               profilePresent = false;
               return;
            }
            debug.error("AgentProvider.init: Unable to get idRepo", ire);
            throw (new ProviderException("idRepo exception: "
                    + ire.getMessage()));
        } catch (Exception e) {
            debug.error("AgentProvider.init: Unable to get idRepo", e);
            throw (new ProviderException("idRepo exception: "+ e.getMessage()));
        }
    }

     private void parseAgentKeyValues(Map attributes) throws ProviderException {
        if(attributes == null || attributes.isEmpty()) {
           return;
        }

        for (Iterator i = attributes.keySet().iterator(); i.hasNext(); ) {
            String key = (String)i.next();
            Set valSet = (Set)attributes.get(key);
            String value = null;
            if ((valSet != null) && (valSet.size() > 0)) {
                Iterator iter = valSet.iterator();
                StringBuffer sb =  new StringBuffer(100);
                while(iter.hasNext()) {
                   sb.append((String)iter.next()).append(",");
                }
                sb = sb.deleteCharAt(sb.length() - 1);
                value = sb.toString();
            }
            setConfig(key, value);
        }

    }

    private void setConfig(String attr, String value) {
         
        if (attr.equals(SEC_MECH)) {
           if (secMech == null) {
               secMech = new ArrayList();
           }

           if(value == null) {
              return; 
           }
           StringTokenizer st = new StringTokenizer(value, ","); 
           while(st.hasMoreTokens()) {
               secMech.add(st.nextToken());
           }
        } else if(attr.equals(WSP_ENDPOINT)) {
           this.wspEndpoint = value;
        } else if(attr.equals(WSS_PROXY_ENDPOINT)) {
           this.wssProxyEndpoint = value;
        } else if(attr.equals(KS_FILE)) {
           this.ksFile = value;
        } else if(attr.equals(KS_PASSWD)) {
           this.ksPasswd = value;
        } else if(attr.equals(KEY_PASSWD)) {
           this.keyPasswd = value;
        } else if(attr.equals(RESPONSE_SIGN)) {
           this.isResponseSigned = Boolean.valueOf(value).booleanValue();
        } else if(attr.equals(RESPONSE_ENCRYPT)) {
           this.isResponseEncrypted = Boolean.valueOf(value).booleanValue();
        } else if(attr.equals(REQUEST_SIGN)) {
           this.isRequestSigned = Boolean.valueOf(value).booleanValue();
        } else if(attr.equals(REQUEST_ENCRYPT)) {
           this.isRequestEncrypted = Boolean.valueOf(value).booleanValue();
        } else if(attr.equals(REQUEST_HEADER_ENCRYPT)) {
           this.isRequestHeaderEncrypted = 
                       Boolean.valueOf(value).booleanValue();
        } else if(attr.equals(KEY_ALIAS)) {
           this.privateKeyAlias = value;
        } else if(attr.equals(PUBLIC_KEY_ALIAS)) {
           this.publicKeyAlias = value;
        } else if(attr.equals(KEY_TYPE)) {
           this.privateKeyType = value;
        } else if(attr.equals(SERVICE_TYPE)) {
           this.serviceType = value;
        } else if(attr.equals(USE_DEFAULT_KEYSTORE)) {
           this.isDefaultKeyStore = Boolean.valueOf(value).booleanValue();
        } else if(attr.equals(DISCOVERY_TRUST_AUTHORITY)) {
            if ((value != null) && (value.length() != 0) 
                && (!value.equals("[Empty]"))) {
                try {
                    taconfig = TrustAuthorityConfig.getConfig(value, 
                        TrustAuthorityConfig.DISCOVERY_TRUST_AUTHORITY);                                              
                } catch (ProviderException pe) {
                    ProviderUtils.debug.error("AgentProvider.setAttribute: " +
                         "error",pe);
                }
            }
        } else if (attr.equals(STS_TRUST_AUTHORITY)) {
            if ((value != null) && (value.length() != 0) 
                && (!value.equals("[Empty]"))) {
                try {
                    taconfig = TrustAuthorityConfig.getConfig(value, 
                        TrustAuthorityConfig.STS_TRUST_AUTHORITY);
           
                } catch (ProviderException pe) {
                    ProviderUtils.debug.error("AgentProvider.setAttribute: " +
                          "error",pe);
                }
            }
        } else if(attr.startsWith(PROPERTY)) {
            properties.put(attr.substring(PROPERTY.length()), value);

        } else if(attr.equals(USER_CREDENTIAL)) {
            if ((value != null) && (value.length() != 0)) {
                if(usercredentials == null) {
                    usercredentials = new ArrayList();
                }
                StringTokenizer stVal = new StringTokenizer(value, ","); 
                while(stVal.hasMoreTokens()) {
                    String tmpVal = (String)stVal.nextToken();
                    int index = tmpVal.indexOf("|");
                    if(index == -1) {
                        return;
                    }
                    String usertmp = tmpVal.substring(0, index);
                    String passwordtmp = tmpVal.substring(index+1, 
                        tmpVal.length()); 

                    String user = null;
                    String password = null;
                    StringTokenizer st = new StringTokenizer(usertmp, ":"); 
                    if(USER_NAME.equals(st.nextToken())) {
                        if(st.hasMoreTokens()) {
                            user = st.nextToken();
                        }               
                    }
                    StringTokenizer st1 = new StringTokenizer(passwordtmp, ":"); 
                    if(USER_PASSWORD.equals(st1.nextToken())) {
                        if(st1.hasMoreTokens()) {
                            password = st1.nextToken();
                        }              
                    }

                    if((user != null) && (password != null)) {
                        PasswordCredential credential = 
                            new PasswordCredential(user, password);
                        usercredentials.add(credential);
                    }
                }
            }
        } else if(attr.equals(FORCE_AUTHENTICATION)) {
            if ((value != null) && (value.length() != 0)) {
                this.forceAuthn = Boolean.valueOf(value).booleanValue();
            }
        } else if(attr.equals(KEEP_SECURITY_HEADERS)) {
           this.preserveSecHeaders = Boolean.valueOf(value).booleanValue();
        } else if(attr.equals(AUTHENTICATION_CHAIN)) {
            if ((value != null) && (value.length() != 0) 
                && (!value.equals("[Empty]"))) {
                this.authenticationChain = value;
            }
        } else if(attr.equals(SAML_ATTRIBUTE_MAPPING)) {
            if(samlAttributes == null) {
               samlAttributes = new HashSet();
            }
            if(value == null) {
               return;
            }
            StringTokenizer st = new StringTokenizer(value, ","); 
            while(st.hasMoreTokens()) {
               samlAttributes.add(st.nextToken());
            }
            
        } else if(attr.equals(INCLUDE_MEMBERSHIPS)) {
            if ((value != null) && (value.length() != 0)) {
                this.includeMemberships = Boolean.valueOf(value).booleanValue();
            }
        } else if(attr.equals(SAML_ATTRIBUTE_NS)) {
           this.attributeNS = value;
        } else if(attr.equals(NAMEID_MAPPER)) {
           this.nameIDMapper = value;
        } else if(attr.equals(KDC_DOMAIN)) {
            this.kdcDomain = value;
        } else if(attr.equals(KRB_SERVICE_PRINCIPAL)) {
            this.servicePrincipal = value;
        } else if(attr.equals(KRB_KEYTAB_FILE)) {
            this.keytabFile = value;
        } else if(attr.equals(KRB_TICKET_CACHE_DIR)) {
            this.ticketCacheDir = value;
        } else if(attr.equals(KDC_SERVER)) {
            this.kdcServer = value;
        } else if(attr.equals(VERIFY_KRB_SIGNATURE)) {
            if ((value != null) && (value.length() != 0)) {
                this.verifyKrbSignature = Boolean.valueOf(value).booleanValue();
            }            
        } else if(attr.equals(USE_PASSTHROUGH_TOKEN)) {
            if ((value != null) && (value.length() != 0)) {
                this.usePassThroughToken = 
                        Boolean.valueOf(value).booleanValue();
            }
        } else if(attr.equals(TOKEN_CONVERSION_TYPE)) {
            this.tokenConversionType = value;
        } else if(attr.equals(SIGNING_REF_TYPE)) {
            if(value != null && value.length() != 0) {
               this.signingRefType = value;
            }
        } else if (attr.equals(ENCRYPTION_ALG)) {
            if(value != null && value.length() != 0) {
               this.encryptionAlgorithm = value;               
            }
        } else if (attr.equals(ENCRYPTION_STRENGTH)) {
            if(value != null && value.length() != 0) {
               this.encryptionStrength = Integer.parseInt(value);
            }       
        } else if(attr.equals(USER_TOKEN_DETECT_REPLAY)) {
            if ((value != null) && (value.length() != 0)) {
                this.detectUserTokenReplay = 
                        Boolean.valueOf(value).booleanValue();
            }
        } else if(attr.equals(MESSAGE_REPLAY_DETECTION)) {
            if ((value != null) && (value.length() != 0)) {
                this.detectMessageReplay = 
                        Boolean.valueOf(value).booleanValue();
            }
        } else if (attr.equals(DNS_CLAIM)) {
            if ((value != null) && (value.length() != 0)) {
                this.dnsClaim = value;
            }
        } else if (attr.equals(SIGNED_ELEMENTS)) {
           if (signedElements == null) {
               signedElements = new ArrayList();
           }

           if(value == null) {
              return;
           }
           StringTokenizer st = new StringTokenizer(value, ",");
           while(st.hasMoreTokens()) {
               signedElements.add(st.nextToken());
           }
        } else {
           if(ProviderUtils.debug.messageEnabled()) {
              ProviderUtils.debug.message("AgentProvider.setConfig: Invalid " +
              "Attribute configured." + attr);
           }
        }
    }

    public void store() throws ProviderException {

        Map config = new HashMap();

        config.put(AGENT_TYPE_ATTR, providerType);
        config.put(AGENT_PASSWORD_ATTR, providerName);
        config.put(AGENT_DEVICE_STATUS_ATTR, "Active");
        
        if(wspEndpoint != null) {
           config.put(WSP_ENDPOINT, wspEndpoint);
        }

        if(wssProxyEndpoint != null) {
           config.put(WSS_PROXY_ENDPOINT, wssProxyEndpoint);
        }

        if(ksFile != null) {
           config.put(KS_FILE, ksFile);
        }

        if(ksPasswd != null) {
           config.put(KS_PASSWD, ksPasswd);
        }

        if(keyPasswd != null) {
           config.put(KEY_PASSWD, keyPasswd);
        }

        if(serviceType != null) {
           config.put(SERVICE_TYPE, serviceType);
        }

        Set secMechSet = new HashSet();
        if(secMech != null) {
           Iterator iter = secMech.iterator();
           while(iter.hasNext()) {
               secMechSet.add((String)iter.next());
           }
        }

        config.put(RESPONSE_SIGN, 
                            Boolean.toString(isResponseSigned));
        config.put(RESPONSE_ENCRYPT, 
                            Boolean.toString(isResponseEncrypted));
        config.put(REQUEST_SIGN, 
                            Boolean.toString(isRequestSigned));
        config.put(REQUEST_ENCRYPT, 
                            Boolean.toString(isRequestEncrypted));
        config.put(REQUEST_HEADER_ENCRYPT,
                            Boolean.toString(isRequestHeaderEncrypted));
        config.put(USE_DEFAULT_KEYSTORE, 
                       Boolean.toString(isDefaultKeyStore));
        if((providerType != null) && (providerType.equals(ProviderConfig.WSC))) {
            config.put(FORCE_AUTHENTICATION,Boolean.toString(forceAuthn));
        }
        config.put(KEEP_SECURITY_HEADERS,
                       Boolean.toString(preserveSecHeaders));
        if(authenticationChain != null) {
           config.put(AUTHENTICATION_CHAIN, authenticationChain);
        }
        
        if(privateKeyAlias != null) {
           config.put(KEY_ALIAS, privateKeyAlias);
        }
        
        if(privateKeyType != null) {
           config.put(KEY_TYPE, privateKeyType);
        }

        if(publicKeyAlias != null) {
           config.put(PUBLIC_KEY_ALIAS, publicKeyAlias);
        }

        Enumeration props = properties.propertyNames();
        while(props.hasMoreElements()) {
           String propertyName = (String)props.nextElement();
           String propertyValue = properties.getProperty(propertyName);
           config.put(PROPERTY + propertyName, propertyValue);
        }

        if(usercredentials != null) {
           Iterator iter = usercredentials.iterator();
           StringBuffer sb =  new StringBuffer(100);
           while(iter.hasNext()) {
              PasswordCredential cred = (PasswordCredential)iter.next();
              String user = cred.getUserName();
              String password = cred.getPassword();
              if(user == null || password == null) {
                 continue;
              }
              
              sb.append(USER_NAME).append(":").append(user)
                .append("|").append(USER_PASSWORD).append(":").append(password).append(",");
           }
           sb = sb.deleteCharAt(sb.length() - 1);
           config.put(USER_CREDENTIAL, sb.toString());
        }

        String stsTA = null;
        String discoTA = null;
        if(taconfig != null) {
           if(taconfig.getType().startsWith(STS_TRUST_AUTHORITY)) {
              stsTA = taconfig.getName();                  
           }
           
           if(taconfig.getType().startsWith(DISCOVERY_TRUST_AUTHORITY)) {
              discoTA = taconfig.getName();                  
           } 
        }
                
        if(stsTA != null) {
           config.put(STS_TRUST_AUTHORITY, stsTA);
        }
        if(discoTA != null) {
           config.put(DISCOVERY_TRUST_AUTHORITY, discoTA); 
        }
        
        if(attributeNS != null) {
           config.put(SAML_ATTRIBUTE_NS, attributeNS); 
        }
        
        if(nameIDMapper != null) {
           config.put(NAMEID_MAPPER, nameIDMapper);
        }
        
        if(includeMemberships) {
           config.put(INCLUDE_MEMBERSHIPS,
                       Boolean.toString(includeMemberships));
        }
                
        if(verifyKrbSignature) {
           config.put(VERIFY_KRB_SIGNATURE,
                       Boolean.toString(verifyKrbSignature));                    
         }
        
        if(kdcServer != null) {
           config.put(KDC_SERVER, kdcServer); 
        }
        
        if(kdcDomain != null) {
           config.put(KDC_DOMAIN, kdcDomain); 
        }
        
        if(servicePrincipal != null) {
           config.put(KRB_SERVICE_PRINCIPAL, servicePrincipal);
        }
        
        if(ticketCacheDir != null) {
           config.put(KRB_TICKET_CACHE_DIR, ticketCacheDir); 
        }
        
        if(keytabFile != null) {
           config.put(KRB_KEYTAB_FILE, keytabFile);  
        }
                
        if(usePassThroughToken) {
           config.put(USE_PASSTHROUGH_TOKEN, 
                Boolean.toString(usePassThroughToken));
        }
                
        if(tokenConversionType != null) {
           config.put(TOKEN_CONVERSION_TYPE, tokenConversionType); 
        }
        
        if(signingRefType != null) {
           config.put(SIGNING_REF_TYPE, signingRefType); 
        }
        
        if(encryptionAlgorithm != null) {
           config.put(ENCRYPTION_ALG, encryptionAlgorithm);
        }
        
        config.put(ENCRYPTION_STRENGTH, 
                new Integer(encryptionStrength).toString());
        
        if(providerType.equals(WSP)) {
           config.put(USER_TOKEN_DETECT_REPLAY, 
                Boolean.toString(detectUserTokenReplay));
           config.put(MESSAGE_REPLAY_DETECTION, 
                Boolean.toString(detectMessageReplay));
        }

        if(dnsClaim != null) {
           config.put(DNS_CLAIM, dnsClaim);
        }

        Set signedElementSet = new HashSet();
        if(signedElements != null) {
           Iterator iter = signedElements.iterator();
           while(iter.hasNext()) {
               signedElementSet.add((String)iter.next());
           }
        }
        // Save the entry in Agent's profile
        try {
            Map attributes = new HashMap();
            Set values = null ;

            for (Iterator i = config.keySet().iterator(); i.hasNext(); ) {
                String key = (String)i.next();
                String value = (String)config.get(key);
                values = new HashSet();
                values.add(value);
                attributes.put(key, values);
            }
            if (secMechSet != null && !secMechSet.isEmpty()) {
                attributes.put(SEC_MECH, secMechSet);
            }

            if (signedElementSet != null && !signedElementSet.isEmpty()) {
                attributes.put(SIGNED_ELEMENTS, signedElementSet);
            }
            
            if(samlAttributes != null && !samlAttributes.isEmpty()) {
               attributes.put(SAML_ATTRIBUTE_MAPPING,samlAttributes); 
            }                      

            if (profilePresent) {
                attributes.remove(AGENT_TYPE_ATTR);
                // Construct AMIdentity object and save
                AMIdentity id = new AMIdentity(token,
                    providerName, IdType.AGENT, "/", null);
                if (debug.messageEnabled()) {
                    debug.message("Attributes to be stored: " + attributes);
                }
                id.setAttributes(attributes);
                id.store();
            } else {
                // Create a new Agent profile
                if (idRepo == null) {
                    idRepo = new AMIdentityRepository(token, "/");
                }
                if (debug.messageEnabled()) {
                    debug.message("New provider - Attributes to be stored: " 
                        + attributes);
                }
                idRepo.createIdentity(IdType.AGENT,
                    providerName, attributes);
            }
        } catch (Exception e) {
            debug.error("AgentProvider.store: Unable to get idRepo", e);
            throw (new ProviderException("idRepo exception: "+ e.getMessage()));
        }
    }

    public void delete() throws ProviderException {
        if (!profilePresent) {
            return;
        }

        // Delete the agent profile
        try {
            if (idRepo == null) {
                idRepo = new AMIdentityRepository(token, "/");
            }
            // Construct AMIdentity object to delete
            AMIdentity id = new AMIdentity(token,
                providerName, IdType.AGENT, "/", null);
            Set identities = new HashSet();
            identities.add(id);
            idRepo.deleteIdentities(identities);
        } catch (Exception e) {
            debug.error("AgentProvider.delete: Unable to get idRepo", e);
            throw (new ProviderException("idRepo exception: "+ e.getMessage()));
        }
    }

    private String getKeyValue(String key, String value) {
        return key + "=" + value;
    }

    /**
     * Checks if the agent profile exists for this provider.
     * @return true if the profile exists.
     */
    public boolean isExists() {
        return profilePresent;
    }

}
