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
 * $Id: DiscoEntryHandlerImplUtils.java,v 1.4 2008/06/25 05:49:56 qcheng Exp $
 *
 */
/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.liberty.ws.disco.plugins;

import java.io.StringReader;
import java.io.StringWriter;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.stream.StreamSource;
import javax.xml.bind.JAXBException;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.liberty.ws.disco.common.DiscoConstants;
import com.sun.identity.liberty.ws.disco.common.DiscoServiceManager;
import com.sun.identity.liberty.ws.disco.common.DiscoUtils;
import com.sun.identity.liberty.ws.disco.jaxb.AuthenticateRequesterElement;
import com.sun.identity.liberty.ws.disco.jaxb.AuthorizeRequesterElement;
import com.sun.identity.liberty.ws.disco.jaxb.AuthenticateSessionContextElement;
import com.sun.identity.liberty.ws.disco.jaxb.EncryptResourceIDElement;
import com.sun.identity.liberty.ws.disco.jaxb.InsertEntryType;
import com.sun.identity.liberty.ws.disco.jaxb.RemoveEntryType;
import com.sun.identity.liberty.ws.disco.jaxb.ResourceIDType;
import com.sun.identity.liberty.ws.disco.jaxb.ResourceOfferingType;
import
    com.sun.identity.liberty.ws.disco.jaxb.QueryType.RequestedServiceTypeType;
import com.sun.identity.liberty.ws.disco.jaxb11.GenerateBearerTokenElement;
import com.sun.identity.liberty.ws.disco.plugins.jaxb.DiscoEntryElement;
import com.sun.identity.plugin.datastore.DataStoreProvider;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.liberty.ws.interfaces.ResourceIDMapper;
import com.sun.identity.shared.xml.XMLUtils;
import org.xml.sax.InputSource;

public class DiscoEntryHandlerImplUtils {

    protected static Debug debug = DiscoUtils.debug;

    /*
     * Retrieves discovery entries from an user entry.
     * Used by implementations of SPI <code>DiscoEntryHandler</code>:
     * <code>DynamicDiscoEntryHandler</code> and
     * <code>UserDynamicEntryHandler</code>.
     * @param store <code>DataStoreProvider</code> object.
     * @param userID user ID.
     * @param attrName name of the user attribute.
     * @param discoEntries The results are returned through Map of
     *  <code>entryId</code> to <code>DiscoEntryElement</code> object.
     * @return true if the results need to be stored; false otherwise.
     * @throws Exception if SDK errors occurred.
     */
    public static boolean getUserDiscoEntries(
        DataStoreProvider store,
        String userID,
        String attrName,
        Map discoEntries)
        throws Exception
    {
        boolean needStore = false;
        Set attr = store.getAttribute(userID, attrName);
        Iterator i = attr.iterator();
        DiscoEntryElement entry = null;
        String entryID = null;
        String entryStr = null;
        while (i.hasNext()) {
            entryStr = (String) i.next();
            try {
                entry = (DiscoEntryElement)
                    DiscoUtils.getDiscoUnmarshaller().unmarshal(
                        XMLUtils.createSAXSource(new InputSource(new StringReader(entryStr))));
                entryID = entry.getResourceOffering().getEntryID();
                if ((entryID == null) || (entryID.length() == 0)) {
                    entryID = SAMLUtils.generateID();
                    entry.getResourceOffering().setEntryID(entryID);
                    needStore = true;
                }
                discoEntries.put(entryID, entry);
            } catch (Exception e) {
                // this is to skip this miss configured entry
                // remove it from the store for predictable behavior
                debug.error(
                    "DiscoEntryHandlerImplUtils.getUserDiscoEntries: wrong "
                    + "format for entry. Removing it from store: " + entryStr);
                needStore = true;
                continue;
            }
        }
        return needStore;
    }

    /*
     * Sets discovery entries to user entry.
     * Used by implementations of SPI <code>DiscoEntryHandler:
     * <code>UserDiscoEntryHandler</code> and
     * <code>UserDynamicEntryHandler</code>.
     * @param store <code>DataStoreProvider</code> object.
     * @param userID user ID.
     * @param attrName name of the user attribute to set to.
     * @param entries <code>Collection</code> of <code>DiscoEntryElement</code>
     *  to be set.
     * @return true if the operation is successful.
     */
    public static boolean setUserDiscoEntries(
        DataStoreProvider store,
        String userID,
        String attrName,
        Collection entries)
    {
        debug.message("in DiscoEntryHandlerImplUtils.setUserDiscoEntries");
        try {
            Iterator i = entries.iterator();
            Set xmlStrings = new HashSet();
            StringWriter sw = null;
            while (i.hasNext()) {
                sw = new StringWriter(1000);
                DiscoUtils.getDiscoMarshaller().marshal(
                    ((DiscoEntryElement) i.next()), sw);
                xmlStrings.add(sw.getBuffer().toString());
            }
            Map map = new HashMap();
            map.put(attrName, xmlStrings);
            store.setAttributes(userID, map);
            return true;
        } catch (Exception e) {
            debug.error(
                "DiscoEntryHandlerImplUtils.setUserDiscoEntries: Exception", e);
            return false;
        }
    }

    /*
     * Finds the matching resource offering according to RequestedServiceType.
     * Used by <code>DiscoEntryHandler</code>s.
     *
     * @param discoEntries all discovery entries
     * @param reqServiceTypes List of requested service types
     * @return Map of matching discovery entries. In this map,
     *  key is <code>entryId</code>, value is <code>DiscoEntryElement</code>.
     */
    public static Map getQueryResults(
        Map discoEntries,
        List reqServiceTypes)
    {
        Map results = null;
        if ((reqServiceTypes == null) || (reqServiceTypes.size() == 0)) {
            if (debug.messageEnabled()) {
                debug.message("DiscoEntryHandlerImplUtils.getQueryResults: "
                    + "no reqServiceTypes");
            }
            results = discoEntries;
        } else {
            results = new HashMap();
            Iterator i = discoEntries.keySet().iterator();
            while (i.hasNext()) {
                String curKey = (String) i.next();
                DiscoEntryElement cur =
                    (DiscoEntryElement) discoEntries.get(curKey);
                ResourceOfferingType offering = cur.getResourceOffering();
                String serviceType =
                    offering.getServiceInstance().getServiceType();
                List options = null;
                if (offering.getOptions() != null) {
                    options = offering.getOptions().getOption();
                }

                Iterator j = reqServiceTypes.iterator();
                while (j.hasNext()) {
                    RequestedServiceTypeType curReqType =
                        (RequestedServiceTypeType)j.next();
                    String requestedServiceType = curReqType.getServiceType();
                    if (!requestedServiceType.equals(serviceType)) {
                        continue;
                    }

                    List queryOptions = null;
                    if (curReqType.getOptions() != null) {
                        queryOptions = curReqType.getOptions().getOption();
                    }

                    if (evaluateOptionsRules(queryOptions, options)) {
                        /* code for proxy support
                        if (proxyServiceTypes.contains(serviceType)) {
                            if (this cur is that proxy) {
                                results.add(cur);
                            } else if (requester is the provider) {
                                results.add(cur);
                            }
                        } else {
                            results.add(cur);
                        }
                        */
                        results.put(curKey, cur);
                        break;
                    }
                }
            }
        }
        return results;
    }

    /**
     * Performs options matching for queries.  The match succeeds
     * if either list is null or if there is any intersection
     * between the lists.
     * @param reqOptions the requested options
     * @param regOptions the registered options
     * @return true if the options pass the matching rules
     */
    private static boolean evaluateOptionsRules(
        List reqOptions,
        List regOptions
    ) {
        if (reqOptions == null || regOptions == null ||
            (reqOptions.size() == 0))
        {
            return true;
        }

        Iterator i = reqOptions.iterator();
        while (i.hasNext()) {
            String option = (String) i.next();
            if (regOptions.contains(option)) {
                return true;
            }
        }

        return false;
    }

    /*
     * Removes discovery entries.
     * Used by implementations of SPI <code>DiscoEntryHandler</code>:
     * <code>UserDiscoEntryHandler</code> and
     * <code>UserDynamicEntryHandler</code>.
     *
     * @param discoEntriesMap Discovery Entries Map.
     * @param removes List of entries to be removed.
     * @return true if the operation is successful.
     */
    public static boolean handleRemoves(Map discoEntriesMap, List removes) {
        Iterator i = removes.iterator();
        RemoveEntryType remove = null;
        while (i.hasNext()) {
            remove = (RemoveEntryType) i.next();
            if (!discoEntriesMap.containsKey(remove.getEntryID())) {
                if (debug.messageEnabled()) {
                    debug.message("DiscoEntryHandlerImplUtils.handleRemoves: "
                        + "can not remove entry: " + remove.getEntryID());
                }
                return false;
            }
            discoEntriesMap.remove(remove.getEntryID());
        }
        return true;
    }

    /*
     * Adds discovery entries.
     * Used by implementations of SPI <code>DiscoEntryHandler</code>:
     * <code>UserDiscoEntryHandler</code> and
     * <code>UserDynamicEntryHandler</code>.
     *
     * @param discoEntriesMap Discovery Entries Map.
     * @param removes List of entries to be added.
     * @return true if the operation is successful; false otherwise.
     */
    public static Map handleInserts(Set discoEntries, List inserts) {

        /*
         * if support proxy:
         * look through discoEntries and find all the serviceTypes that have
         *  proxy proxyServiceTypes
         */

        Map insertResults = new HashMap();
        insertResults.put(DiscoEntryHandler.STATUS_CODE,
	    DiscoConstants.STATUS_FAILED);
        Set supportedDirectives = DiscoServiceManager.getSupportedDirectives();
        if (debug.messageEnabled()) {
            debug.message("DiscoEntryHandlerImplUtils.handleInserts: "
                + "size of supportedDirective is "
                + supportedDirectives.size());
        }

        Iterator i = inserts.iterator();
        InsertEntryType insertEntry = null;
        DiscoEntryElement de = null;
        ResourceOfferingType resOff = null;
        List newEntryIDs = new LinkedList();
        while (i.hasNext()) {
            insertEntry = (InsertEntryType) i.next();
            try {
                de = DiscoUtils.getDiscoEntryFactory().
                    createDiscoEntryElement();
            } catch (JAXBException je) {
                debug.error(
                    "DiscoEntryHandlerImplUtils.handleInserts: couldn't "
                    + "create DiscoEntry: ", je);
                return insertResults;
            }
            resOff = insertEntry.getResourceOffering();
            String newEntryID = SAMLUtils.generateID();
            if (debug.messageEnabled()) {
                debug.message(
                    "DiscoEntryHandlerImplUtils: newEntryID=" + newEntryID);
            }
            resOff.setEntryID(newEntryID);
            newEntryIDs.add(newEntryID);
            de.setResourceOffering(resOff);

            List dirs = insertEntry.getAny();
            if ((dirs != null) && !dirs.isEmpty()) {
                Iterator j = dirs.iterator();
                while (j.hasNext()) {
                    Object dir = j.next();
                    if (dir instanceof AuthenticateRequesterElement) {
                        if (!supportedDirectives.contains(
                            DiscoConstants.AUTHN_DIRECTIVE)
                        ) {
                            debug.error("Directive AuthenticateRequester is "
                                + "not supported.");
                            return insertResults;
                        }
                    } else if (dir instanceof AuthorizeRequesterElement) {
                        if (!supportedDirectives.contains(
                            DiscoConstants.AUTHZ_DIRECTIVE)
                        ) {
                            debug.error("Directive AuthorizeRequester is "
                                + "not supported.");
                            return insertResults;
                        }
                    } else if (dir instanceof AuthenticateSessionContextElement) 
                    {
                        if (!supportedDirectives.contains(
                            DiscoConstants.SESSION_DIRECTIVE)
                        ) {
                            debug.error("Directive AuthenticateSessionContext "
                                + "is not supported.");
                            return insertResults;
                        }
                    } else if (dir instanceof EncryptResourceIDElement) {
                        if (!supportedDirectives.contains(
                                        DiscoConstants.ENCRYPT_DIRECTIVE))
                        {
                            debug.error("Directive EncryptResourceID "
                                + "is not supported.");
                            return insertResults;
                        }
                    } else if (dir instanceof GenerateBearerTokenElement) {
                        if (!supportedDirectives.contains(
                            DiscoConstants.BEARER_DIRECTIVE)
                        ) {
                            debug.error("Directive GenerateBearerToken "
                                + "is not supported.");
                            return insertResults;
                        }
                    } else {
                        debug.error("Directive " + dir + " is not supported.");
                        return insertResults;
                    }
                }
                de.getAny().addAll(dirs);
            }

            if (!discoEntries.add(de)) {
                debug.error(
                    "DiscoEntryHandlerImplUtils.handleInserts: couldn't " +
                        "add DiscoEntry to Set.");
                return insertResults;
            }
        }
        insertResults.put(DiscoEntryHandler.STATUS_CODE,
            DiscoConstants.STATUS_OK);
        insertResults.put(DiscoEntryHandler.NEW_ENTRY_IDS, newEntryIDs);
        return insertResults;
    }

    /**
     * This is used by the global disocvery service handler to retrieve
     * the resource offerings registered at the realm, org, role etc.
     */
    public static void getGlobalDiscoEntries(AMIdentity amIdentity,
        String attrName, Map discoEntries, String userID) throws Exception
    {
        Map map = amIdentity.getServiceAttributes(
                      "sunIdentityServerDiscoveryService");

        Set attr = (Set)map.get(attrName);                
        if (attr == null || attr.isEmpty()) {
            debug.error("DiscoEntryHandlerImplUtils.getServiceDiscoEntries: " +
            "The resource offerings are not available");
            return;
        }

        if(debug.messageEnabled()) {
           debug.message("DiscoEntryHandlerImplUtils.getServiceDiscoEntries: " +
                         attr);
        }

        Iterator j = attr.iterator();
        String entryStr = null;
        String resIDValue = null;
        DiscoEntryElement entry = null;
        ResourceIDType resID = null;
        ResourceOfferingType resOff = null;
        String entryID = null;
        String providerID = null;
        while (j.hasNext()) {
            entryStr = (String) j.next();
            try {
                entry = (DiscoEntryElement)
                         DiscoUtils.getDiscoUnmarshaller().unmarshal(
                        XMLUtils.createSAXSource(new InputSource(new StringReader(entryStr))));
                resOff = entry.getResourceOffering();
                entryID = resOff.getEntryID();
                if(entryID == null) {
                   entryID = SAMLUtils.generateID();
                   resOff.setEntryID(entryID);
                }
                ResourceIDType rid = resOff.getResourceID();
                if((rid == null) || (rid.getValue() == null) || 
                              (rid.getValue().equals(""))) {
                   com.sun.identity.liberty.ws.disco.jaxb.ObjectFactory 
                                       discoFac =
                   new com.sun.identity.liberty.ws.disco.jaxb.ObjectFactory();
                   resID = discoFac.createResourceIDType();
                   resID.setValue(DiscoConstants.IMPLIED_RESOURCE);
                   resOff.setResourceID(resID);
                }
                entry.setResourceOffering(resOff);
                discoEntries.put(entryID, entry);
            } catch (Exception e) {
                debug.error("DiscoEntryHandlerImplUtils.getServiceDiscoEntries:"
                    + " Exception for getting entry: " + entryStr + ":", e);
                continue;
            }
        }
    }

    /**
     * Registers the discovery service resource offerings to the AMIdentity
     *  
     * This is used by the global disocvery service handler to register
     * the resource offerings to the realm, org, role etc.
     * @param amIdentity the idrepo object that the resource offerings are 
     *                   being set.
     * @param attrName the discovery service attribute name where the disco
     *                 entries are being stored.
     * @param entries the list of discovery services that needs to be set. 
     * @return true if successfully set the entries.
     */
    public static boolean setGlobalDiscoEntries(
          AMIdentity amIdentity, String attrName, Collection entries) {
        try {
            Iterator i = entries.iterator();
            Set xmlStrings = new HashSet();
            String entryId = null;
            StringWriter sw = null;
            while (i.hasNext()) {
                sw = new StringWriter(1000);
                DiscoUtils.getDiscoMarshaller().marshal(
                        ((DiscoEntryElement)i.next()),
                                              sw);
                xmlStrings.add(sw.getBuffer().toString());
            }
            Map map = new HashMap();
            map.put(attrName, xmlStrings);
            amIdentity.modifyService("sunIdentityServerDiscoveryService", map);
            amIdentity.store();
            return true;
        } catch (Exception e) {
            debug.error("DiscoEntryHandlerImplUtils.setServiceDiscoEntries:"
                        + " Exception", e);
            return false;
        }
    }
}
