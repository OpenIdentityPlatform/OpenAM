/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: GlobalDiscoEntryHandler.java,v 1.2 2008/06/25 05:49:56 qcheng Exp $
 *
 */
package com.sun.identity.liberty.ws.disco.plugins;

import java.security.AccessController;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.liberty.ws.disco.common.DiscoConstants;
import com.sun.identity.liberty.ws.disco.common.DiscoUtils;
import com.sun.identity.liberty.ws.disco.DiscoveryException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.common.SystemConfigurationUtil;

/*
 * The class <code>GlobalDiscoEntryHandler</code> provides an
 * implementation for handling service registations in B2E case. 
 * <p>
 */
public class GlobalDiscoEntryHandler implements DiscoEntryHandler {

    private static final String DYNAMIC_ATTR_NAME =
					"sunIdentityServerDynamicDiscoEntries";
    private static final String DISCO_SERVICE =
                                        "sunIdentityServerDiscoveryService";

    /**
     * Default Constructor
     */
    public GlobalDiscoEntryHandler() {
	DiscoUtils.debug.message("in GlobalDiscoEntryHandler.constructor");
    }

    /**
     * Returns the default realm identity object.
     */
    private synchronized static AMIdentity getRealmIdentity() 
          throws DiscoveryException
    {
        try {
            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                     AdminTokenAction.getInstance());

            AMIdentityRepository idRepo = 
                 new AMIdentityRepository(adminToken, "/");
            return idRepo.getRealmIdentity();

        } catch (IdRepoException ire) {
            DiscoUtils.debug.error("GlobalDiscoEntryHandler.getRealm" +
            "Identity: Initialization failed", ire);
            throw new DiscoveryException(ire.getMessage());
            
        } catch (SSOException se) {
            DiscoUtils.debug.error("GlobalDiscoEntryHandler.getRealm" +
            "Identity: SSOException ", se);
            throw new DiscoveryException(se.getMessage());
        }
    }

    /**
     * This method registers the Discovery Service to the root realm if
     * the service is not already registered.
     */
    private static void registerDiscoveryService() 
                       throws DiscoveryException {
        try {
            AMIdentity amId = getRealmIdentity();
            Set assignedServices = amId.getAssignedServices(); 
            if(assignedServices != null && 
                   assignedServices.contains(DISCO_SERVICE)) {
               return; 
            }
            amId.assignService(DISCO_SERVICE, null);
        } catch(IdRepoException ire) {
            DiscoUtils.debug.error("GlobalDiscoEntryHandler.register" +
            "DiscoveryService: Exception", ire);
            throw new DiscoveryException(ire.getMessage());

        } catch(SSOException se) {
            DiscoUtils.debug.error("GlobalDiscoEntryHandler.register" +
            "DiscoveryService: Exception", se);
            throw new DiscoveryException(se.getMessage());
        }
    }

    /**
     * Returns DiscoEntries for a user under userEntry.
     * @param userID The user whose DiscoEntries will be returned.
     * @param reqServiceTypes List of
     *          com.sun.identity.liberty.ws.disco.jaxb.RequestedServiceType 
     *          objects from discovery Query.
     * @return Map of entryId and 
     * 		com.sun.identity.liberty.ws.disco.plugins.jaxb.DiscoEntryElement
     *		for this user. For each DiscoEntry element in the List,
     *		the entryId attribute of ResourceOffering need to be set.
     */
    public Map getDiscoEntries(String userID, List reqServiceTypes) {
	DiscoUtils.debug.message("in GlobalDiscoEntryHandler.getDiscoEntries");
	Map results = new HashMap();
	try {
	    DiscoEntryHandlerImplUtils.getGlobalDiscoEntries(
                     getRealmIdentity(), DYNAMIC_ATTR_NAME, results, userID);
	    results = DiscoEntryHandlerImplUtils.getQueryResults(
                      results, reqServiceTypes);
	} catch (Exception e) {
	    DiscoUtils.debug.error("GlobalDiscoEntryHandler.getDiscoEntries: "
		+ "Exception:", e);
	}
	    
	return results;
    }

    /**
     * Modify DiscoEntries for the default organization.
     * @param userID This is not used in this implementation.
     *
     * @param removes List of
     *          com.sun.identity.liberty.ws.disco.jaxb.RemoveEntryType jaxb
     *          objects.
     * @param inserts List of
     *          com.sun.identity.liberty.ws.disco.jaxb.InsertEntryType jaxb
     *          objects.
     * @return Map which contains the following key value pairs:
     *          Key: <code>DiscoEntryHandler.STATUS_CODE</code>
     *          Value: status code String such as "OK", "Failed", etc.
     *          Key: <code>DiscoEntryHandler.NEW_ENTRY_IDS</code>
     *          Value: List of entryIds for the entries that were added.
     *          The second key/value pair will only exist when status code is
     *          "OK", and there are InsertEntry elements in the Modify request.
     *          When successful, all modification (removes and inserts) should
     *          be done. No partial changes should be done.
     */
    public Map modifyDiscoEntries(String userID, List removes, List inserts)
    {
	if (DiscoUtils.debug.messageEnabled()) {
	    DiscoUtils.debug.message("GlobalDiscoEntryHandler.modifyDisco"
		+ "Entries: init ");
	}
      
        Map result = new HashMap();
        result.put(STATUS_CODE, DiscoConstants.STATUS_FAILED);

        Map discoEntries = new HashMap();
        try {
            // Try to register discovery service if not already registered
            registerDiscoveryService();

            AMIdentity amId = getRealmIdentity();
            DiscoEntryHandlerImplUtils.getGlobalDiscoEntries(
                  getRealmIdentity(), DYNAMIC_ATTR_NAME, discoEntries, userID);

            if((removes != null) && (removes.size() != 0)) {
               if (!DiscoEntryHandlerImplUtils.handleRemoves(
                                       discoEntries, removes)) {
                   return result;
               }
            }
        
            Set entries = new HashSet();
            entries.addAll(discoEntries.values());
            List newEntryIDs = null;

            if ((inserts != null) && (inserts.size() != 0)) {
                Map insertResults = DiscoEntryHandlerImplUtils.handleInserts(
                                    entries, inserts);
                if (!((String) insertResults.get(STATUS_CODE)).
                       equals(DiscoConstants.STATUS_OK)) {
                     return result;
                }
                newEntryIDs = (List) insertResults.get(NEW_ENTRY_IDS);
            }

            if(!DiscoEntryHandlerImplUtils.setGlobalDiscoEntries(amId, 
                       DYNAMIC_ATTR_NAME, entries)) {
               return result;
            } else {
               result.put(STATUS_CODE, DiscoConstants.STATUS_OK);
               if ((newEntryIDs != null) && (newEntryIDs.size() != 0)) {
                   result.put(NEW_ENTRY_IDS, newEntryIDs);
               }
               return result;
            }
	} catch (DiscoveryException de) {
            DiscoUtils.debug.error("GlobalDiscoEntryHandler.modify" +
            "DiscoEntries: Exception", de);
            return result;
        } catch (Exception ex) {
            DiscoUtils.debug.error("GlobalDiscoEntryHandler.modify" +
            "DiscoEntries: Exception", ex);
            return result;
        }
    }


}
