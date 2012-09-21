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
 * $Id: UserDiscoEntryHandler.java,v 1.2 2008/06/25 05:49:56 qcheng Exp $
 *
 */

package com.sun.identity.liberty.ws.disco.plugins;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.sun.identity.liberty.ws.disco.common.DiscoConstants;
import com.sun.identity.plugin.datastore.DataStoreProvider;
import com.sun.identity.plugin.datastore.DataStoreProviderManager;

/*
 * The class <code>UserDiscoEntryHandler</code> provides a default
 * implementation for interface <code>DiscoEntryHandler</code>.
 * <p>
 * This implementation gets/modifies discovery entries stored at the user's
 * entry in attribute named "sunIdentityServerDiscoEntries".
 */
public class UserDiscoEntryHandler implements DiscoEntryHandler {
    private static final String USER_ATTR_NAME =
        "sunIdentityServerDiscoEntries";
    private static final String DISCO = "disco";

    /**
     * Default Constructor.
     */
    public UserDiscoEntryHandler() {
        DiscoEntryHandlerImplUtils.debug.message(
            "in UserDiscoEntryHandler.constructor");
    }

    /**
     * Finds discovery entries for a user under user entry.
     * @param userID The user whose discovery entries will be returned.
     * @param reqServiceTypes List of
     *  <code>com.sun.identity.liberty.ws.disco.jaxb.RequestedServiceType</code>
     *  objects from discovery query.
     * @return Map of <code>entryId</code> and 
     *  <code>com.sun.identity.liberty.ws.disco.plugins.jaxb.DiscoEntryElement
     *  </code> for this user. For each <code>DiscoEntry</code> element in the
     *  List, the <code>entryId</code> attribute of ResourceOffering need to
     *  be set.
     */
    public Map getDiscoEntries(String userID, List reqServiceTypes) {
        DiscoEntryHandlerImplUtils.debug.message(
            "in UserDiscoEntryHandler.getDiscoEntries");
        Map results = new HashMap();
        try {
            DataStoreProvider store = DataStoreProviderManager.getInstance().
                getDataStoreProvider(DISCO);

            if (DiscoEntryHandlerImplUtils.getUserDiscoEntries(
                store, userID, USER_ATTR_NAME,results))
            {
                // this is the case when the DiscoEntry is set through console
                // or amadmin, and entryID was not set
                if (!DiscoEntryHandlerImplUtils.setUserDiscoEntries(
                    store, userID, USER_ATTR_NAME, results.values()))
                {
                    DiscoEntryHandlerImplUtils.debug.error(
                        "UserDiscoEntryHandler.getDiscoEntries: " +
                            "couldn't set missing entryID to entry.");
                }
            }
            results = DiscoEntryHandlerImplUtils.getQueryResults(
                results, reqServiceTypes);
        } catch (Exception e) {
            DiscoEntryHandlerImplUtils.debug.error(
                "UserDiscoEntryHandler.getDiscoEntries:", e);
        }

        return results;
    }

    /**
     * Modifies discovery entries for a user.
     * @param userID The user whose discovery entries will be set.
     * @param removes List of
     *  <code>com.sun.identity.liberty.ws.disco.jaxb.RemoveEntryType</code>
     *  jaxb objects.
     * @param inserts List of
     *  <code>com.sun.identity.liberty.ws.disco.jaxb.InsertEntryType</code>
     *  jaxb objects.
     * @return Map which contains the following key value pairs:
     *  Key: <code>DiscoEntryHandler.STATUS_CODE</code>
     *  Value: status code String such as "OK", "Failed", etc.
     *  Key: <code>DiscoEntryHandler.NEW_ENTRY_IDS</code>
     *  Value: List of <code>entryIds</code> for the entries that were added.
     *  The second key/value pair will only exist when status code is
     *  "OK", and there are <code>InsertEntry</code> elements in the modify
     *  request. When successful, all modification (removes and inserts) should
     *  be done. No partial changes should be done.
     */
    public Map modifyDiscoEntries(String userID, List removes, List inserts) {
        DiscoEntryHandlerImplUtils.debug.message(
            "in UserDiscoEntryHandler.modifyDiscoEntries");
        Map result = new HashMap();
        result.put(STATUS_CODE, DiscoConstants.STATUS_FAILED);
        Map discoEntries = new HashMap();
        DataStoreProvider store = null;
        try {
            store = DataStoreProviderManager.getInstance().
                getDataStoreProvider(DISCO);
            DiscoEntryHandlerImplUtils.getUserDiscoEntries(
                store, userID, USER_ATTR_NAME, discoEntries);
        } catch (Exception e) {
            DiscoEntryHandlerImplUtils.debug.error(
                "UserDiscoEntryHandler.modifyDiscoEntries: Exception:", e);
            return result;
        }

        if ((removes != null) && !removes.isEmpty()) {
            if (DiscoEntryHandlerImplUtils.debug.messageEnabled()) {
                DiscoEntryHandlerImplUtils.debug.message(
                    "UserDiscoEntryHandler.modifyDiscoEntries: handling "
                    + removes.size() + " removes.");
            }
            if (!DiscoEntryHandlerImplUtils.handleRemoves(
                discoEntries, removes)
            ) {
                return result;
            }
        }

        Set results = new HashSet();
        results.addAll(discoEntries.values());
        List newEntryIDs = null;
        if ((inserts != null) && (inserts.size() != 0)) {
            if (DiscoEntryHandlerImplUtils.debug.messageEnabled()) {
                DiscoEntryHandlerImplUtils.debug.message(
                    "UserDiscoEntryHandler.modifyDiscoEntries: handling " +
                        inserts.size() + " inserts.");
            }
            Map insertResults = DiscoEntryHandlerImplUtils.handleInserts(
                                    results, inserts);
            if (!((String)insertResults.get(STATUS_CODE)).
                equals(DiscoConstants.STATUS_OK)
            ) {
                return result;
            }
            newEntryIDs = (List) insertResults.get(NEW_ENTRY_IDS);
        }

        // so far everything is successful
        if (!DiscoEntryHandlerImplUtils.setUserDiscoEntries(
                store, userID, USER_ATTR_NAME, results))
        {
            DiscoEntryHandlerImplUtils.debug.error(
                "UserDiscoEntryHandler.modifyDiscoEntries: "
                + "couldn't set DiscoEntries through DiscoEntryHandler.");
            return result;
        } else {
            if (DiscoEntryHandlerImplUtils.debug.messageEnabled()) {
                DiscoEntryHandlerImplUtils.debug.message(
                    "UserDiscoEntryHandler.modifyDisco"
                    + "Entries: set DiscoEntries through DiscoEntryHandler "
                    + "successfully.");
            }
            result.put(STATUS_CODE, DiscoConstants.STATUS_OK);
            if ((newEntryIDs != null) && (newEntryIDs.size() != 0)) {
                result.put(NEW_ENTRY_IDS, newEntryIDs);
            }
            return result;
        }
    }
}
