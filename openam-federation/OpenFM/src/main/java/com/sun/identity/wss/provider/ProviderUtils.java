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
 * $Id: ProviderUtils.java,v 1.10 2008/10/20 19:06:00 arviranga Exp $
 *
 */

package com.sun.identity.wss.provider;

import java.util.ResourceBundle;

//import com.iplanet.am.util.Locale;
import com.sun.identity.shared.locale.Locale;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.wss.security.WSSUtils;
import com.sun.identity.wss.provider.plugins.STSAgent;
import com.sun.identity.wss.provider.plugins.DiscoveryAgent;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchOpModifier;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.shared.Constants;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

public class ProviderUtils {

     static ResourceBundle bundle = null;
     private static final String AGENT_CONFIG_ATTR = 
             "AgentType";
     public static Debug debug = Debug.getInstance("WebServicesSecurity");
     
     static {
            bundle = Locale.getInstallResourceBundle("fmWSSProvider");
     }
     
     public static List getAllSTSConfig() {
         List stsConfigs = new ArrayList();
         //stsConfigs.add(getLocalSTSConfig());
         //Set agentConfigAttribute = new HashSet();
         //agentConfigAttribute.add(AGENT_CONFIG_ATTR);
         try {
             SSOToken adminToken = WSSUtils.getAdminToken();
             AMIdentityRepository idRepo = 
                new AMIdentityRepository(adminToken, "/");

             IdSearchControl control = new IdSearchControl();
             //control.setReturnAttributes(agentConfigAttribute);
             control.setAllReturnAttributes(true);
             control.setTimeOut(0);
             //control.setMaxResults(2);
             Map kvPairMap = new HashMap();
             Set set = new HashSet();
             set.add("STSAgent");            
             kvPairMap.put(AGENT_CONFIG_ATTR, set);

             control.setSearchModifiers(IdSearchOpModifier.OR, kvPairMap);

             IdSearchResults results = idRepo.searchIdentities(IdType.AGENT,
                "*", control);
             Set agents = results.getSearchResults();                          
             Iterator iter = agents.iterator();
             //Map attrs = (Map) results.getResultAttributes();
             while (iter.hasNext()) {
                 AMIdentity provider = (AMIdentity) iter.next();
                 //Map attributes = (Map) attrs.get(provider);
                 STSConfig stsConfig = new STSAgent(provider);
                 stsConfigs.add(stsConfig);                
             }             
        } catch (Exception e) {
            debug.error("ProviderUtils.getAllSTSConfig:ERROR: ",e);
        }
         return stsConfigs;
     }

     public static List getAllDiscoveryConfig() {
         List discoConfigs = new ArrayList();         
         //Set agentConfigAttribute = new HashSet();
         //agentConfigAttribute.add(AGENT_CONFIG_ATTR);

         try {
             SSOToken adminToken = WSSUtils.getAdminToken();
             AMIdentityRepository idRepo = 
                 new AMIdentityRepository(adminToken, "/");

             IdSearchControl control = new IdSearchControl();
             //control.setReturnAttributes(agentConfigAttribute);
             control.setAllReturnAttributes(true);
             control.setTimeOut(0);       
             Map kvPairMap = new HashMap();
             Set set = new HashSet();
             set.add("DiscoveryAgent");            
             kvPairMap.put(AGENT_CONFIG_ATTR, set);

             control.setSearchModifiers(IdSearchOpModifier.OR, kvPairMap);

             IdSearchResults results = idRepo.searchIdentities(IdType.AGENT,
                 "*", control);
             Set agents = results.getSearchResults();
             Iterator iter = agents.iterator();
             //Map attrs = (Map) results.getResultAttributes();
             while (iter.hasNext()) {
                 AMIdentity provider = (AMIdentity) iter.next();
                 //Map attributes = (Map) attrs.get(provider);
                 DiscoveryConfig discoConfig = new DiscoveryAgent(provider);
                 discoConfigs.add(discoConfig);                
             }
         } catch (Exception e) {
             debug.error("ProviderUtils.getAllDiscoSConfig:ERROR: ", e);                        
         }
         return discoConfigs;
         
     }
     
     public static STSConfig getLocalSTSConfig() {
         STSConfig stsConfig = new STSAgent();
         stsConfig.setName("defaultSTS");
         stsConfig.setType("STSAgent");
         stsConfig.setEndpoint(getLocalSTSEndpoint());
         stsConfig.setMexEndpoint(getLocalSTSMexEndpoint());
         List secMech = new ArrayList();
         secMech.add("urn:sun:wss:security:null:X509Token");
         stsConfig.setSecurityMechs(secMech);
         return stsConfig;
     }
     
     public static DiscoveryConfig getLocalDiscoveryConfig() {
         return null;
     }
     
     private static String getLocalSTSEndpoint() {
        String protocol =  SystemConfigurationUtil.getProperty(
                           Constants.AM_SERVER_PROTOCOL);        
        String host = SystemConfigurationUtil.getProperty(
                       Constants.AM_SERVER_HOST);
        String port = SystemConfigurationUtil.getProperty(
                       Constants.AM_SERVER_PORT);
        String deployuri = SystemConfigurationUtil.getProperty(
                       Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
        return protocol + "://" + host + ":" + port + deployuri + "/sts";
    }

    private static String getLocalSTSMexEndpoint() {
        return getLocalSTSEndpoint() + "/mex";
    }
}

