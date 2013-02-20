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
 * $Id: DiscoEntryHandler.java,v 1.2 2008/06/25 05:47:12 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.disco.plugins;

import java.util.Map;
import java.util.List;

/**
 * The class <code>DiscoEntryHandler</code> is an interface that is 
 * used to get and set <code>DiscoEntries</code> for a user.
 * <p>
 * A default implementation will be provided for this discovery service.
 * If you want to handle <code>DiscoEntry</code> differently, implement this
 * interface and set the implementing class to
 * <code>DiscoEntryHandler Plugins Class</code> field in Discovery Service.
 *
 * @supported.all.api
 */
public interface DiscoEntryHandler {

    /**
     * Key used in method <code>modifyDiscoEntries()</code> return Map.
     * The value of this key is status code String such as "OK", "Failed", etc.
     */
    public static final String STATUS_CODE = "STATUS_CODE";

    /**
     * Key used in method <code>modifyDiscoEntries()</code> return Map.
     * The value of this key is a List of <code>entryIds</code> for the entries
     * that were added.
     */
    public static final String NEW_ENTRY_IDS = "newEntryIDs";

    /**
     * Finds all the discovery entries for a user.
     * @param userID The user whose discovery entries will be returned.
     * @param reqServiceTypes List of
     *  <code>com.sun.identity.liberty.ws.disco.jaxb.RequestedServiceType</code>
     *  objects from discovery query.
     * @return Map of <code>entryId</code> and
     *  <code>com.sun.identity.liberty.ws.disco.plugins.jaxb.DiscoEntryElement
     *  </code> objects for this user. For each <code>DiscoEntry</code> element
     *  in the List, the <code>entryId</code> attribute of
     *  <code>ResourceOffering</code> should be set.
     */
    public Map getDiscoEntries(String userID, List reqServiceTypes); 


    /**
     * Modifies discovery entries for a user.
     * @param userID The user whose discovery entries will be set.
     * @param removes List of
     *  <code>com.sun.identity.liberty.ws.disco.jaxb.RemoveEntryType</code>
     *  <code>jaxb</code> objects.
     * @param inserts List of
     *  <code>com.sun.identity.liberty.ws.disco.jaxb.InsertEntryType</code>
     *  <code>jaxb</code> objects.
     * @return Map which contains the following key value pairs:
     *  Key: <code>DiscoEntryHandler.STATUS_CODE</code>
     *  Value: status code String such as "OK", "Failed", etc.
     *  Key: <code>DiscoEntryHandler.NEW_ENTRY_IDS</code>
     *  Value: List of <code>entryId</code>s for the entries that were added.
     *  The second key/value pair will only exist when status code is
     *  "OK", and there are <code>InsertEntry</code> elements in the
     *  <code>Modify</code> request.
     *  When successful, all modification (removes and inserts) should
     *  be done. No partial changes should be done.
     */
    public Map modifyDiscoEntries(String userID,
                                        List removes, List inserts);
}
