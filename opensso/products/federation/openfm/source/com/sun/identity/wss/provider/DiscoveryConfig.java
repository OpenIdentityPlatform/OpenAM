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
 * $Id: DiscoveryConfig.java,v 1.6 2009/02/28 00:59:42 mrudul_uchil Exp $
 *
 */
package com.sun.identity.wss.provider;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import com.sun.identity.liberty.ws.disco.DiscoveryClient;
import com.sun.identity.liberty.ws.disco.DiscoveryException;
import com.sun.identity.liberty.ws.disco.Description;
import com.sun.identity.liberty.ws.disco.Directive;
import com.sun.identity.liberty.ws.disco.InsertEntry;
import com.sun.identity.liberty.ws.disco.RemoveEntry;
import com.sun.identity.liberty.ws.disco.Modify;
import com.sun.identity.liberty.ws.disco.ModifyResponse;
import com.sun.identity.liberty.ws.disco.ResourceOffering;
import com.sun.identity.liberty.ws.disco.ResourceID;
import com.sun.identity.liberty.ws.disco.ServiceInstance;
import com.sun.identity.liberty.ws.disco.QueryResponse;
import com.sun.identity.liberty.ws.disco.common.DiscoConstants;
import com.sun.identity.liberty.ws.common.Status;
import com.sun.identity.wss.security.SecurityMechanism;

/**
 * This abstract class <code>DiscoveryConfig</code> represents the 
 * configuration of a Discovery client entity. It extends 
 * <code>TrustAuthorityConfig</code>.
 * 
 * <p>This class can be extended to define the trust authority config
 * which is Discovery client configuration.
 * 
 * <p>Pluggable implementation of this abstract class can choose to store this 
 * configuration in desired configuration store. This pluggable implementation
 * class can be configured in client's AMConfig.properties as value of 
 * "com.sun.identity.wss.discovery.config.plugin" property 
 * for Discovery client configuration.
 * 
 * <p>This class also provides methods for registering and un-registering
 * with the discovery service. All the static methods in this class are for 
 * the persistent operations.
 * @supported.all.api
 */

public abstract class DiscoveryConfig extends TrustAuthorityConfig {

      protected String authServiceEndpoint = null;

      /** Creates a new instance of DiscoveryConfig */
      public DiscoveryConfig() {}


      /**
       * Returns Authentication Web Service End point.
       * @return Authentication Web Service End point
       */
      public String getAuthServiceEndPoint() {
          return authServiceEndpoint;
      }         

      /**
       * Sets Authentication Web Service End point.
       * @param authServiceEndpoint Authentication Web Service End point
       *
       */
      public void setAuthServiceEndPoint(String authServiceEndpoint) {
          this.authServiceEndpoint = authServiceEndpoint;
      }        

      /**
       * Registers the Discovery client configuration with trusted authority.
       *
       * @param config the configuration of the Discovery client.
       *
       * @param serviceURI the <code>URI</code> of the web services provider.
       *
       * @exception ProviderException if any failure.
       */
      public void registerProviderWithTA(ProviderConfig config,
               String serviceURI) throws ProviderException {

          registerProviderWithTA(config, serviceURI, false);
      }
       

      /**
       * Registers the Discovery client configuration with trusted authority.
       *
       * @param config the configuration of the Discovery client.
       *
       * @param serviceURI the <code>URI</code> of the web services provider.
       *
       * @param unregister if true unregisters the service offering with
       *        trusted authority before registration.
       *        
       * @exception ProviderException if any failure.
       */
      public void registerProviderWithTA(ProviderConfig config, 
               String serviceURI, boolean unregister) throws ProviderException {

          try {
              if(unregister) {
                 unregisterProviderWithTA(serviceURI);
              }
              DiscoveryClient discoClient = new DiscoveryClient(endpoint, null);
              Modify modifyReq = getDiscoveryModify(config, serviceURI);
              ModifyResponse response = discoClient.modify(modifyReq);
              
              Status status = response.getStatus();
              if(status != null) {
                 if(status.getCode().getLocalPart().equalsIgnoreCase
                            (DiscoConstants.STATUS_OK)) {
                    return;
                 } else {
                    throw new ProviderException(
                        ProviderUtils.bundle.getString("registrationFailed"));
                 }
              }
              throw new ProviderException(
                  ProviderUtils.bundle.getString("registrationFailed"));
          } catch (DiscoveryException de) {
             ProviderUtils.debug.error("DiscoveryConfig.registerProviderWith"+
                 "TA: is failed", de);
              throw new ProviderException(
                  ProviderUtils.bundle.getString("registrationFailed"));
          }
           
      }

     /**
      * Unregisters the provider with trusted authority.
      *
      * @param serviceURI the service <code>URI</code> of the 
      *            web services provider.
      * @exception ProviderException if any failure.
      */
     public void unregisterProviderWithTA(String serviceURI) 
                     throws ProviderException {
         try {
             DiscoveryClient client = new DiscoveryClient(endpoint, null);
             client.setResourceID(DiscoConstants.IMPLIED_RESOURCE);
             List types = new ArrayList();
             types.add(serviceURI);
             QueryResponse result = client.getResourceOffering(types);
             List offerings = result.getResourceOffering();
             if((offerings == null) || (offerings.isEmpty())) {
                if(ProviderUtils.debug.messageEnabled()) {
                   ProviderUtils.debug.message("DiscoveryConfig." +
                   "unregisterProviderWithTA:: " +
                   "There are no resource offerings");
                }
                return;
             }
 
             ResourceOffering offering = (ResourceOffering)offerings.get(0);
             String entryID = offering.getEntryID();
             RemoveEntry remove = new RemoveEntry(entryID);
             List removes = new ArrayList();
             removes.add(remove);

             Modify modify = new Modify();
             ResourceID resourceID = 
                    new ResourceID(DiscoConstants.IMPLIED_RESOURCE); 
             modify.setResourceID(resourceID);
             modify.setRemoveEntry(removes);
             client.modify(modify);
         } catch (DiscoveryException de) {
             ProviderUtils.debug.error("DiscoveryConfig.unregisterProviderWith"+
                 "TA: is failed", de);
              throw new ProviderException(
                  ProviderUtils.bundle.getString("unregisterFailed"));
         }
     }

    private Modify getDiscoveryModify(ProviderConfig config, 
               String serviceURI) throws ProviderException {

         // Register with discovery only if there are liberty security mechs.
         List securityMech = config.getSecurityMechanisms();
         List libertyMech = new ArrayList();
         Iterator iter = securityMech.iterator();
         while(iter.hasNext()) {
             String secMech = (String)iter.next();
             if(isLibertySecurityMechanism(secMech)) {
                libertyMech.add(secMech);
             }
         }

         if(libertyMech.isEmpty()) {
            throw new ProviderException(
                  ProviderUtils.bundle.getString("noLibertyMechanisms"));
         }

         List directives = new ArrayList();
         iter = libertyMech.iterator();
         while(iter.hasNext()) {
            String mech = (String)iter.next();
            getDirectives(mech, directives); 
         }
         ResourceID resourceID = 
                    new ResourceID(DiscoConstants.IMPLIED_RESOURCE); 
         Description description = 
                   new Description(config.getSecurityMechanisms(), 
                   null, config.getWSPEndpoint());
         ArrayList descriptions = new ArrayList();
         descriptions.add(description);

         ServiceInstance serviceInstance = 
                  new ServiceInstance(serviceURI, 
                  config.getProviderName(), descriptions);
         ResourceOffering offering = 
                    new ResourceOffering(resourceID, serviceInstance);
         InsertEntry insertEntry = new InsertEntry(offering, null);
         if(!directives.isEmpty()) {
            insertEntry.setAny(directives);
         }
         ArrayList insertEntries = new ArrayList();
         insertEntries.add(insertEntry);
         return new Modify(resourceID, insertEntries, null);
    }

    private boolean isLibertySecurityMechanism(String mechanism) {
        if(mechanism == null) {
           return false;
        }

        return SecurityMechanism.getLibertySecurityMechanismURIs().
               contains(mechanism);
    }

    private void getDirectives(String mechanism, List directives) {
        if(mechanism == null) {
           return;
        }
        if(SecurityMechanism.LIB_NULL_SAML_TOKEN_URI.equals(mechanism) ||
           SecurityMechanism.LIB_TLS_SAML_TOKEN_URI.equals(mechanism) ||
           SecurityMechanism.LIB_CLIENT_TLS_SAML_TOKEN_URI.equals(mechanism)) {
           Directive dir1 = new Directive(Directive.AUTHENTICATE_REQUESTER); 
           directives.add(dir1);
        } else if(
           SecurityMechanism.LIB_NULL_SAML_BEARER_TOKEN_URI.equals(mechanism) ||
           SecurityMechanism.LIB_TLS_SAML_BEARER_TOKEN_URI.equals(mechanism) ||
           SecurityMechanism.LIB_CLIENT_TLS_SAML_BEARER_TOKEN_URI.
                      equals(mechanism)) {
           Directive dir1 = new Directive(Directive.AUTHENTICATE_REQUESTER); 
           directives.add(dir1);

           Directive dir2 = new Directive(Directive.GENERATE_BEARER_TOKEN);
           directives.add(dir2);
        }
    }

}
