/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: GatewayServletUtils.java,v 1.5 2008/06/25 05:43:54 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy.util;

import com.sun.identity.authentication.config.AMAuthConfigUtils;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class GatewayServletUtils implements ServiceListener {
    
    private static Debug debug = Debug.getInstance("amGateway");
    private static String moduleName = null;
    private static ServiceConfigManager sConfigMgr = null;
    private static String CERT_PORT_ATTR = null;
    private static HashMap AuthConfigMap = new HashMap();

    // constructor
    public GatewayServletUtils (ServiceConfigManager scm, String module) {
        sConfigMgr = scm;
        CERT_PORT_ATTR = "iplanet-am-auth-" +  module.toLowerCase() + 
            "-port-number";
        moduleName = module;
    }

    /**
     * This method is used to receive notifications if schema changes.
     * @param serviceName the name of the service.
     * @param version the version of the service.
     */
    public void schemaChanged (String serviceName, String version) {
        //No op.
    }
    
    /**
     * This method for implementing ServiceListener.
     * As this object listens for changes in schema of amConsoleService.
     * this method is No-op.
     * @param serviceName name of the service
     * @param version version of the service
     * @param groupName name of the group
     * @param serviceComponent service component
     * @param type type of modification
     */
    public void globalConfigChanged (
        String serviceName,
        String version,
        String groupName,
        String serviceComponent,
        int type
    ) {
        //No op.
    }
    
    /**
     * This method for implementing ServiveListener.
     * As this object listens for changes in schema of amConsoleService.
     * this method is No-op.
     */
    
    public void organizationConfigChanged () {
       organizationConfigChanged (null, null, null, null, null, 0);
    }               
                
    /**
     * This method for implementing ServiveListener.
     * As this object listens for changes in schema of amConsoleService.
     * this method is No-op.
     */
    public void organizationConfigChanged (String orgName) {
       organizationConfigChanged (null, null, orgName, null, null, 0);
    }               
                
    /**
     * This method for implementing ServiveListener.
     * As this object listens for changes in schema of amConsoleService.
     * this method is No-op.
     * @param serviceName name of the service
     * @param version version of the service
     * @param orgName name of the org
     * @param groupName name of the group
     * @param serviceComponent service component
     * @param type type of modification
     */
    public void organizationConfigChanged (
        String serviceName,
        String version,
        String orgName,
        String groupName,
        String serviceComponent,
        int type
    ) {
       String certModulePortNumber = null;
       String certModuleLevel = null;

       if (orgName == null) {
          orgName = SMSEntry.getRootSuffix();
       }
       // Get the port number for Cert module
       try {
          ServiceConfig config = 
              sConfigMgr.getOrganizationConfig(orgName, null);
          Map configAttrs = config.getAttributes();
          if (debug.messageEnabled()) {
              debug.message("GatewayServlet:configAttrs :  " 
                  + configAttrs.toString());
          }

          // Get the cert port number
          Set attrs = (Set) configAttrs.get(CERT_PORT_ATTR);
          if (attrs == null) {
              return;
          } 

          Iterator values = attrs.iterator();
          if (values.hasNext()) {
              certModulePortNumber = (String) values.next();
          }

          // Get cert auth level
          attrs = (Set) configAttrs.get(
              AMAuthConfigUtils.getAuthLevelAttribute(configAttrs, moduleName));
          if (attrs == null) {
              return;
          } 

          values = attrs.iterator();
          if (values.hasNext()) {
              certModuleLevel = (String) values.next();
          }
                        
          AuthServiceConfigInfo info = 
               new AuthServiceConfigInfo(orgName, moduleName, 
                                       certModulePortNumber, certModuleLevel);
          AuthConfigMap.put(orgName, info);
      } catch (Exception e) {
          debug.error("GatewayServletUtils : "+ 
                      "Unable to get Cert Module Level and/or Port Number", e);
      }
    } 
    
    /*
     *  This method is used to retrieve port number of Cert auth module.
     */
    public String getPortNumber(String orgName) {
       AuthServiceConfigInfo info = getAuthConfigInfo(orgName);
       return info.getPortNumber();
    }
    
    /*
     *  This method is used to retrieve Auth Level of Cert auth module.
     */
    public String getAuthLevel(String orgName) {
       AuthServiceConfigInfo info = getAuthConfigInfo(orgName);
       return info.getAuthLevel();
    }

    /*
     *  This method is used to retrieve Auth Level of Cert auth module.
     */
    public AuthServiceConfigInfo getAuthConfigInfo(String orgName) {
       AuthServiceConfigInfo info = 
                   (AuthServiceConfigInfo)AuthConfigMap.get(orgName);
       if (info == null) {
           organizationConfigChanged(orgName);
           info = (AuthServiceConfigInfo)AuthConfigMap.get(orgName);
       }
       return info;
    }
}
