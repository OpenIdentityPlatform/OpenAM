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
 * $Id: DiscoveryAgent.java,v 1.14 2008/10/20 19:05:59 arviranga Exp $
 *
 */
package com.sun.identity.wss.provider.plugins; 

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Iterator;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Enumeration;

import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import com.iplanet.sso.SSOException;
import com.sun.identity.wss.provider.DiscoveryConfig;
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


public class DiscoveryAgent extends DiscoveryConfig {

     // Initialize the Attributes names set
     private static Set attrNames = new HashSet();;

     private static final String AGENT_PASSWORD_ATTR = "userpassword";
     private static final String AGENT_DEVICE_STATUS_ATTR = 
         "sunIdentityServerDeviceStatus";
     private static final String AGENT_TYPE_ATTR = "AgentType";
     private static final String ENDPOINT = "DiscoveryEndpoint";
     private static final String KEY_ALIAS = "privateKeyAlias";
     private static final String AUTHN_ENDPOINT = "AuthNServiceEndpoint";

     private AMIdentityRepository idRepo;
     private static Set agentConfigAttribute;
     private static Debug debug = ProviderUtils.debug;

     // Instance variables
     private SSOToken token;
     private boolean profilePresent;

     static {
         attrNames.add(ENDPOINT);
         attrNames.add(AUTHN_ENDPOINT);
         attrNames.add(KEY_ALIAS);
     }
     
     public DiscoveryAgent() {
         
     }
     
     public DiscoveryAgent(AMIdentity amIdentity) throws ProviderException {
        try {
            this.name = amIdentity.getName();
            this.type = amIdentity.getType().getName();
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

     public void init (String name, String type, SSOToken token) 
               throws ProviderException {

         this.name = name;
         this.type = type;                
         this.token = token;

         // Obtain the provider from Agent profile
         try {
             AMIdentity provider = 
                 new AMIdentity(token, name, IdType.AGENT, "/", null);
             if(!provider.isExists()) {
               if(debug.messageEnabled()) {
                  debug.message("DiscoveryAgent.init: provider " + name
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
             debug.error("DiscoveryAgent.init: Unable to get idRepo", ire);
             throw (new ProviderException("idRepo exception: "+
                     ire.getMessage()));
        } catch (Exception e) {
            debug.error("DiscoveryAgent.init: Unable to get idRepo", e);
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
 
        if (debug.messageEnabled()) {
            debug.message("Attribute name: " + attr + " Value: "+ value);
        }

        if (attr.equals(AUTHN_ENDPOINT)) {
           this.authServiceEndpoint = value; 
        } else if(attr.equals(ENDPOINT)) {
           this.endpoint = value;
        } else if(attr.equals(KEY_ALIAS)) {
           this.privateKeyAlias = value;
        } else {
           if(ProviderUtils.debug.messageEnabled()) {
              ProviderUtils.debug.message("DiscoveryAgent.setConfig: Invalid " +
              "Attribute configured." + attr);
           }
        }
    }

    public void store() throws ProviderException {

        Map config = new HashMap();

        config.put(AGENT_TYPE_ATTR, type);
        config.put(AGENT_PASSWORD_ATTR, name);
        config.put(AGENT_DEVICE_STATUS_ATTR, "Active");
        
        if(authServiceEndpoint != null) { 
           config.put(AUTHN_ENDPOINT, authServiceEndpoint);
        }

        if(endpoint != null) {
           config.put(ENDPOINT, endpoint);
        }

        if(privateKeyAlias != null) {
           config.put(KEY_ALIAS, privateKeyAlias);
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
            debug.error("DiscoveryAgent.store: Unable to get idRepo", e);
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
            AMIdentity id = new AMIdentity(token, name,
                            IdType.AGENT, "/", null);
            Set identities = new HashSet();
            identities.add(id);
            idRepo.deleteIdentities(identities);
        } catch (Exception e) {
            debug.error("DiscoveryAgent.delete: Unable to get idRepo", e);
            throw (new ProviderException("idRepo exception: "+ e.getMessage()));
        }
    }

    private String getKeyValue(String key, String value) {
        return key + "=" + value;
    }
   

}
