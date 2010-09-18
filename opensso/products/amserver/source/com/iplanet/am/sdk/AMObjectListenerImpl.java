/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AMObjectListenerImpl.java,v 1.6 2009/01/28 05:34:47 ww203982 Exp $
 *
 */

package com.iplanet.am.sdk;

import com.sun.identity.shared.debug.Debug;
import java.util.Map;
import java.util.Set;
import com.sun.identity.shared.ldap.util.DN;

/**
 * This class provides the implementation for listening to change
 * events in Identity Repository.
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 */
public class AMObjectListenerImpl implements AMObjectListener {

    private Debug debug = Debug.getInstance("amProfileListener");

    AMObjectListenerImpl() {

    }

    public void objectChanged(String name, int eventType, Map configMap) {
        // Add a debug message
        if (debug.messageEnabled()) {
            debug.message("AMObjectListenerImpl.objectChanged(): name: " + name
                    + " type: " + eventType);
        }
        // Normalize the DN
        String normalizedDN = (new DN(name)).toRFCString().toLowerCase();
        AMStoreConnection.updateCache(normalizedDN, eventType);

        try {
            // TODO: See if this Org Cache can be eliminated
            if (AMCompliance.isComplianceUserDeletionEnabled()) {
                AMCompliance.cleanDeletedOrgCache(normalizedDN);
            }

            if (AMDCTree.isRequired()) { // TODO: Needs to use the generic Cache
                AMDCTree.cleanDomainMap(normalizedDN);
            }
        } catch (AMException ame) {
            if (debug.warningEnabled()) {
                debug.warning("AMObjectListenerImpl.objectChanged() "
                        + "AMException occured: ", ame);
            }
        }

        // Notify affected objects above the event
        AMObjectImpl.notifyEntryEvent(normalizedDN, eventType, false);

        // FIXME: Avoid calling AM SDK Repo plugin here
        AMSDKRepo.notifyObjectChangedEvent(normalizedDN, eventType);
    }

    public void objectsChanged(String parentName, int eventType, Set attrNames,
            Map configMap) {
        // Add a debug message
        if (debug.messageEnabled()) {
            debug.message("AMObjectListenerImpl.objectsChanged(): "
                    + "parentName: " + parentName + " type: " + eventType
                    + "\n config map= " + configMap);
        }
        // Normalize the DN
        String dn = (new DN(parentName)).toRFCString().toLowerCase();
        AMStoreConnection.updateCache(dn, eventType);

        try {
            // TODO: What is the Deleted OrgCache? See if this can be eliminated
            if (AMCompliance.isComplianceUserDeletionEnabled()) {
                AMCompliance.cleanDeletedOrgCache(dn);
            }

            if (AMDCTree.isRequired()) { // TODO: Needs to use the generic Cache
                AMDCTree.cleanDomainMap(dn);
            }
        } catch (AMException ame) {
            if (debug.warningEnabled()) {
                debug.warning("AMObjectListenerImpl.objectsChanged() "
                        + "AMException occured: ", ame);
            }
        }

        // Notify affected objects above the event
        AMObjectImpl.notifyEntryEvent(dn, eventType, true);

        // FIXME: Avoid calling AM SDK Repo plugin here
        AMSDKRepo.notifyAllObjectsChangedEvent();
    }

    public void permissionsChanged(String orgName, Map configMap) {
        // Add a debug message
        if (debug.messageEnabled()) {
            debug.message("AMObjectListenerImpl.permissionsChanged(): "
                    + "orgName: " + orgName);
        }
        // Normalize the DN
        String dn = (new DN(orgName)).toRFCString().toLowerCase();

        // Update AMStoreConnection cache
        AMStoreConnection.updateCache(dn, AMEvent.OBJECT_CHANGED);

        // Notify affected objects above the event
        AMObjectImpl.notifyACIChangeEvent(dn, AMEvent.OBJECT_CHANGED);

        // FIXME: Avoid calling AM SDK Repo plugin here
        AMSDKRepo.notifyAllObjectsChangedEvent();
    }

    public void allObjectsChanged() {
        debug.error("AMObjectListenerImpl: Received all objects changed "
                + "event from event service");

        AMEvent amEvent = new AMEvent(AMStoreConnection.getAMSdkBaseDN());
        AMObjectImpl.notifyAffectedDNs(AMStoreConnection.getAMSdkBaseDN(), 
            amEvent);

        // FIXME: Avoid calling AM SDK Repo plugin here
        AMSDKRepo.notifyAllObjectsChangedEvent();
    }


    public Map getConfigMap() {
        return null;
    }


    public void setConfigMap(Map cMap) {        
    }
}
