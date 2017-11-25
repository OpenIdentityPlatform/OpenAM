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
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: STSAgent.java,v 1.23 2009/11/16 21:52:58 mallas Exp $
 *
 */

package com.sun.identity.wss.provider.plugins;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.ArrayList;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.wss.provider.STSConfig;
import com.sun.identity.wss.security.PasswordCredential;
import com.sun.identity.wss.provider.ProviderException;
import com.sun.identity.wss.provider.ProviderUtils;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdRepoException;


public class STSAgent extends STSConfig {

    // Initialize the Attributes names set
    private static Set attrNames = new HashSet();;
    
    private static final String AGENT_PASSWORD_ATTR = "userpassword";
    private static final String AGENT_DEVICE_STATUS_ATTR = 
        "sunIdentityServerDeviceStatus";
    private static final String AGENT_TYPE_ATTR = "AgentType";
    private static final String ENDPOINT = "STSEndpoint";
    private static final String MEX_ENDPOINT = "STSMexEndpoint";
    private static final String SEC_MECH = "SecurityMech";
    private static final String RESPONSE_SIGN = "isResponseSign";
    private static final String RESPONSE_ENCRYPT = "isResponseEncrypt";
    private static final String REQUEST_SIGN = "isRequestSign";     
    private static final String REQUEST_ENCRYPT = "isRequestEncrypt";
    private static final String REQUEST_HEADER_ENCRYPT = 
                                "isRequestHeaderEncrypt";
    private static final String USER_NAME = "UserName";
    private static final String USER_PASSWORD = "UserPassword";
    private static final String USER_CREDENTIAL = "UserCredential";
    private static final String STS_CONFIG = "STS";
    private static final String PRIVATE_KEY_ALIAS = "privateKeyAlias";
    private static final String PUBLIC_KEY_ALIAS = "publicKeyAlias";
    private static final String KDC_SERVER = "KerberosDomainServer";
    private static final String KDC_DOMAIN = "KerberosDomain";
    private static final String KRB_SERVICE_PRINCIPAL = 
             "KerberosServicePrincipal";
    private static final String KRB_TICKET_CACHE_DIR = 
             "KerberosTicketCacheDir";
    private static final String ENCRYPTION_ALG = "EncryptionAlgorithm";
    private static final String ENCRYPTION_STRENGTH = "EncryptionStrength";
    private static final String SIGNING_REF_TYPE = "SigningRefType";
    private static final String PROTOCOL_VERSION = "WSTrustVersion";
    private static final String SAML_ATTRIBUTE_MAPPING = 
                                 "SAMLAttributeMapping";
    private static final String INCLUDE_MEMBERSHIPS = "includeMemberships";
    private static final String SAML_ATTRIBUTE_NS = "AttributeNamespace";
    private static final String NAMEID_MAPPER = "NameIDMapper";
    private static final String KEYTYPE = "KeyType";
    private static final String REQUESTED_CLAIMS = "RequestedClaims";
    private static final String DNS_CLAIM = "DnsClaim";
    private static final String SIGNED_ELEMENTS = "SignedElements";
     
    private static Debug debug = ProviderUtils.debug;
    
    private AMIdentityRepository idRepo;
    private boolean profilePresent = false;
    private SSOToken token = null;
    
    static {
        attrNames.add(ENDPOINT);
        attrNames.add(MEX_ENDPOINT);
        attrNames.add(SEC_MECH);
        attrNames.add(RESPONSE_SIGN);
        attrNames.add(RESPONSE_ENCRYPT);
        attrNames.add(REQUEST_SIGN);
        attrNames.add(REQUEST_ENCRYPT);
        attrNames.add(REQUEST_HEADER_ENCRYPT);
        attrNames.add(USER_CREDENTIAL);
        attrNames.add(STS_CONFIG);
        attrNames.add(PRIVATE_KEY_ALIAS);
        attrNames.add(PUBLIC_KEY_ALIAS);
        attrNames.add(KDC_SERVER);
        attrNames.add(KDC_DOMAIN);
        attrNames.add(KRB_SERVICE_PRINCIPAL);
        attrNames.add(KRB_TICKET_CACHE_DIR);
        attrNames.add(ENCRYPTION_ALG);
        attrNames.add(ENCRYPTION_STRENGTH);
        attrNames.add(SIGNING_REF_TYPE);
        attrNames.add(PROTOCOL_VERSION);
        attrNames.add(INCLUDE_MEMBERSHIPS);
        attrNames.add(SAML_ATTRIBUTE_MAPPING);
        attrNames.add(SAML_ATTRIBUTE_NS);
        attrNames.add(NAMEID_MAPPER);
        attrNames.add(KEYTYPE);
        attrNames.add(REQUESTED_CLAIMS);
        attrNames.add(DNS_CLAIM);
        attrNames.add(SIGNED_ELEMENTS);
    }

    /** Creates a new instance of STSAgent */
    public STSAgent() {
    }
    
    public STSAgent(AMIdentity amIdentity) throws ProviderException {
        try {
            this.name = amIdentity.getName();
            this.type = amIdentity.getType().getName();
            if(debug.messageEnabled()) {
               debug.message("STSAgent: name = " + name + "type = " + type);
            }
            Map attributes = (Map) amIdentity.getAttributes(attrNames);
            parseAgentKeyValues(attributes);
        } catch (IdRepoException ire) {
            debug.error("STSAgent.constructor: Idrepo exception", ire);
            throw new ProviderException(ire.getMessage());            
        } catch (SSOException se) {
            debug.error("STSAgent.constructor: SSO exception", se);
            throw new ProviderException(se.getMessage());            
        }
    }
    
    public void init(String name, String type, SSOToken token) 
        throws ProviderException {
        
        this.name = name;
        this.type = type;                
        this.token = token;
        if(debug.messageEnabled()) {
           debug.message("STSAgent: name = " + name + "type = " + type); 
        }

        // Obtain the provider from Agent profile
        try {
            AMIdentity provider = 
                new AMIdentity(token, name, IdType.AGENT, "/", null);
            if(!provider.isExists()) {
               if(debug.messageEnabled()) {
                  debug.message("STSAgent.init: provider " + name
                          + "does not exist");
               }
               return; 
            }
            Map attributes = (Map) provider.getAttributes(attrNames);
            profilePresent = true;
            parseAgentKeyValues(attributes);
        } catch (IdRepoException ire) {
            if(ire.getErrorCode().equals("402")) {
               //permission denied
               profilePresent = false;
               return;
            }
            debug.error("STSAgent.init: Unable to get idRepo", ire);
            throw (new ProviderException("idRepo exception: "+ ire.getMessage()));
        } catch (Exception e) {
            debug.error("STSAgent.init: Unable to get idRepo", e);
            throw (new ProviderException("idRepo exception: "+ e.getMessage()));
        }        
         
    }
    
    private void parseAgentKeyValues(Map attributes) throws ProviderException {
        if(attributes == null || attributes.isEmpty()) {
           return;
        }
        if(debug.messageEnabled()) {
           debug.message("STSAgent.parseAgentKeyValues::" + attributes);
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
                
        if(attr.equals(ENDPOINT)) {
            this.endpoint = value;
        } else if(attr.equals(MEX_ENDPOINT)) {
            this.mexEndpoint = value;
        } else if(attr.equals(SEC_MECH)) {
           if (secMech == null) {
               secMech = new ArrayList();
           }
           StringTokenizer st = new StringTokenizer(value, ","); 
           while(st.hasMoreTokens()) {
               secMech.add(st.nextToken());
           }
        } else if(attr.equals(RESPONSE_SIGN)) {
           this.isResponseSigned = Boolean.valueOf(value).booleanValue();
        } else if(attr.equals(RESPONSE_ENCRYPT)) {
           this.isResponseEncrypted = Boolean.valueOf(value).booleanValue();
        } else if(attr.equals(REQUEST_SIGN)) {
           this.isRequestSigned = Boolean.valueOf(value).booleanValue();
        } else if(attr.equals(REQUEST_ENCRYPT)) {
           this.isRequestEncrypted = Boolean.valueOf(value).booleanValue();
        } else if(attr.equals(REQUEST_HEADER_ENCRYPT)) {
           this.isRequestHeaderEncrypted = Boolean.valueOf(value).booleanValue();
        } else if(attr.equals(PRIVATE_KEY_ALIAS)) {
           this.privateKeyAlias = value;
        } else if(attr.equals(STS_CONFIG)) {
           this.stsConfigName = value;
        } else if(attr.equals(PUBLIC_KEY_ALIAS)) {
           this.publicKeyAlias = value;
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
        } else if(attr.equals(KDC_DOMAIN)) {
            this.kdcDomain = value;
        } else if(attr.equals(KRB_SERVICE_PRINCIPAL)) {
            this.servicePrincipal = value;        
        } else if(attr.equals(KRB_TICKET_CACHE_DIR)) {
            this.ticketCacheDir = value;
        } else if(attr.equals(KDC_SERVER)) {
            this.kdcServer = value;        
        } else if(attr.equals(SIGNING_REF_TYPE)) {
            if(value != null && value.length() !=0) {
               this.signingRefType = value;
            }
        } else if (attr.equals(ENCRYPTION_ALG)) {
            if(value != null && value.length() !=0) {
               this.encryptionAlgorithm = value;
            }
        } else if (attr.equals(ENCRYPTION_STRENGTH)) {
            if(value != null && value.length() != 0) {
               this.encryptionStrength = Integer.parseInt(value);
            }
        } else if (attr.equals(PROTOCOL_VERSION)) {
            if(value != null && value.length() != 0) {
               this.protocolVersion = value;
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
        } else if(attr.equals(KEYTYPE)) {
           if(value != null) {
              this.keyType = value;
           }
        } else if(attr.equals(REQUESTED_CLAIMS)) {
            if(requestedClaims == null) {
               requestedClaims = new ArrayList();
            }
            if(value == null) {
               return;
            }
            StringTokenizer st = new StringTokenizer(value, ","); 
            while(st.hasMoreTokens()) {
               requestedClaims.add(st.nextToken());
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
            AMIdentity id = new AMIdentity(token, name,
                            IdType.AGENT, "/", null);
            Set identities = new HashSet();
            identities.add(id);
            idRepo.deleteIdentities(identities);
        } catch (Exception e) {
            debug.error("STSAgent.delete: Unable to get idRepo", e);
            throw (new ProviderException("idRepo exception: "+ e.getMessage()));
        }
        
    }
    
    public void store() throws ProviderException {
        
        Map config = new HashMap(); 

        config.put(AGENT_TYPE_ATTR, type);
        config.put(AGENT_PASSWORD_ATTR, name);
        config.put(AGENT_DEVICE_STATUS_ATTR, "Active");
        
        if(endpoint != null) {
           config.put(ENDPOINT, endpoint);
        }        

        if(mexEndpoint != null) {
           config.put(MEX_ENDPOINT, mexEndpoint);
        }

        if(privateKeyAlias != null) {
           config.put(PRIVATE_KEY_ALIAS, privateKeyAlias);
        }

        if(publicKeyAlias != null) {
           config.put(PUBLIC_KEY_ALIAS, publicKeyAlias);
        }

        if(stsConfigName != null) {
           config.put(STS_CONFIG, stsConfigName);
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
        if(signingRefType != null) {
           config.put(SIGNING_REF_TYPE, signingRefType); 
        }
        
        if(encryptionAlgorithm != null) {
           config.put(ENCRYPTION_ALG, encryptionAlgorithm);
        }
        
        config.put(ENCRYPTION_STRENGTH, 
                new Integer(encryptionStrength).toString());
        
        if(protocolVersion != null) {
           config.put(PROTOCOL_VERSION, protocolVersion); 
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
        
        if(keyType != null) {
           config.put(KEYTYPE, keyType);
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
            if (secMechSet != null) {
                attributes.put(SEC_MECH, secMechSet);
            }

            if(samlAttributes != null && !samlAttributes.isEmpty()) {
               attributes.put(SAML_ATTRIBUTE_MAPPING,samlAttributes); 
            }
            
            if(requestedClaims != null && !requestedClaims.isEmpty()) {
               Set claims = new HashSet();
               claims.addAll(requestedClaims);
               attributes.put(REQUESTED_CLAIMS, claims); 
            }

            if (signedElementSet != null && !signedElementSet.isEmpty()) {
                attributes.put(SIGNED_ELEMENTS, signedElementSet);
            }
            
            if (profilePresent) {
                attributes.remove(AGENT_TYPE_ATTR);
                // Construct AMIdentity object and save
                AMIdentity id = new AMIdentity(token,
                    name, IdType.AGENT, "/", null);
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
                idRepo.createIdentity(IdType.AGENT, name, attributes);
            }
        } catch (Exception e) {
            debug.error("STSAgent.store: Unable to get idRepo", e);
            throw (new ProviderException("idRepo exception: "+ e.getMessage()));
        }
        
    }
    
    private String getKeyValue(String key, String value) {
        return key + "=" + value;
    }

}
