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
 * $Id: AMEvent.java,v 1.8 2009/01/28 05:34:47 ww203982 Exp $
 *
 */

package com.iplanet.am.sdk;

import java.util.EventObject;

import com.sun.identity.shared.ldap.controls.LDAPPersistSearchControl;

import com.iplanet.services.ldap.event.DSEvent;

/** <p>Represents an event fired by Sun Java System Access Manager SDK.</p>
 *
 * <p><code>AMEvent</code>'s state consists of the following:
 * <ul>
 * <li>The event source: The underlying object that caused the event.
 * <li>The event source DN: DN of the underlying object that caused the 
 *     event.
 * <li>The event type
 * <li>The source object type: Type of the underlying object that caused
 *     event.
 * </ul>
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 * @supported.all.api
 */
public class AMEvent extends EventObject {

    private static final long serialVersionUID = 6448554078141700417L;

    /**
     * Represents an object addition event type.
     */
    public static final int OBJECT_ADDED = LDAPPersistSearchControl.ADD;

    /**
     * Represents an object change event type.
     */
    public static final int OBJECT_CHANGED = LDAPPersistSearchControl.MODIFY;

    /**
     * Represents an object removal event type.
     */
    public static final int OBJECT_REMOVED = LDAPPersistSearchControl.DELETE;

    /** Represents an object expiration event type. Occurs when the TTL for the
     *  object data is over. */
    public static final int OBJECT_EXPIRED = 9;

    /**
     * Represents an object renaming event type.
     */
    public static final int OBJECT_RENAMED = LDAPPersistSearchControl.MODDN;

    /* The above constants OBJECT_ADDED, OBJECT_CHANGED, OBJECT_REMOVED, 
     OBJECT_RENAMED should be kept in synch with the corresponding
     constants defined in com.iplanet.services.ldap.event.IDSEventListener

     OBJECT_ADDED=IDSEventListener.CHANGE_ADD
     OBJECT_CHANGED=IDSEventListener.CHANGE_MODIFY
     OBJECT_REMOVED=IDSEventListener.CHANGE_DELETE
     OBJECT_RENAMED=IDSEventListener.CHANGE_MOD_LOCATION
     */

    private int eventType;

    private String sourceDN;

    private int sourceType;

    /**
     * Constructs an event object.
     * This constructor accepts the event source object and passes it onto the
     * base class constructor.
     *
     * @param source The source object that caused the event. The source object
     *        could be User, Role, Group, Organization, etc., from this
     *        SDK. The source could also be a String 
     *        representing the DN (distinguished name) of the source 
     *        object.
     * @param eventType type of event.
     */
    public AMEvent(Object source, int eventType) {
        super(source);
        if (source instanceof DSEvent) {
            DSEvent dsEvent = (DSEvent) source;
            this.sourceDN = dsEvent.getID();
            this.sourceType = determineSourceType(dsEvent);
        } else if (source instanceof AMEvent) {
            this.sourceDN = ((AMEvent) source).getSourceDN();
            this.sourceType = AMObject.UNKNOWN_OBJECT_TYPE;                     
        } else {
            this.sourceDN = null;
            this.sourceType = AMObject.UNKNOWN_OBJECT_TYPE;
        }
        this.eventType = eventType;
    }

    protected AMEvent(Object source, int eventType, String sourceDN,
            int sourceType) {
        super(source);
        this.eventType = eventType;
        this.sourceDN = sourceDN;
        this.sourceType = sourceType;
    }

    /**
     * Protected constructor for package use only
     */
    protected AMEvent(String dn) {
        super(new String(""));
        this.eventType = OBJECT_CHANGED;
        sourceDN = dn;
    }

    /**
     * Returns the distinguished name of the source object in a String format.
     * Use this method if no searching or parsing operations need to be
     * performed on the distinguished name.
     *
     * @return distinguished name of the source object.
     */
    public String getSourceDN() {
        return sourceDN;
    }

    /**
     * Returns the type of the event.
     *
     * @return Returns one of the
     * following possible values:
     * <ul>
     * <li><code>AMEvent.OBJECT_ADDED</code>
     * <li><code>AMEvent.OBJECT_CHANGED</code>
     * <li><code>AMEvent.OBJECT_REMOVED</code>
     * <li><code>AMEvent.OBJECT_RENAMED</code>
     * <li><code>AMEvent.OBJECT_EXPIRED</code> </ul>
     */
    public int getEventType() {
        return eventType;
    }

    /**
     * Returns the type of the source object that caused the event.
     * <p>
     * @return Returns one of the following possible values:
     * <ul>
     * <li> <code>AMObject.USER</code>
     * <li> <code>AMObject.ROLE</code>
     * <li> <code>AMObject.FILTERED_ROLE</code>
     * <li> <code>AMObject.GROUP</code>
     * <li> <code>AMObject.DYNAMIC_GROUP</code>
     * <li> <code>AMObject.ASSIGNABLE_DYNAMIC_GROUP</code>
     * <li> <code>AMObject.ORGANIZATION</code>
     * <li> <code>AMObject.PEOPLE_CONTAINER</code>
     * <li> <code>AMObject.GROUP_CONTAINER</code>
     * <li> <code>AMObject.ORGINATIONAL_UNIT</code>
     * <li> <code>AMObject.UNKNOWN_OBJECT_TYPE</code> if source unknown
     * </ul>
     */
    public int getSourceType() {
        return sourceType;
    }

    private int determineSourceType(DSEvent source) {
        // getClassName() returns all the object classes as a comma separated
        // String
        if (source == null) {
            return AMObject.UNKNOWN_OBJECT_TYPE;
        }

        String objectClasses = source.getClassName().toLowerCase();
        if (objectClasses.indexOf(AMObjectClassManager
                .getObjectClass(AMObject.USER)) != -1) {
            return AMObject.USER;
        } else if (objectClasses.indexOf(AMObjectClassManager
                .getObjectClass(AMObject.ROLE)) != -1) {
            return AMObject.ROLE;
        } else if (objectClasses.indexOf(AMObjectClassManager
                .getObjectClass(AMObject.FILTERED_ROLE)) != -1) {
            return AMObject.FILTERED_ROLE;
        } else if (objectClasses.indexOf(AMObjectClassManager
                .getObjectClass(AMObject.ORGANIZATION)) != -1) {
            return AMObject.ORGANIZATION;
        } else if (objectClasses.indexOf(AMObjectClassManager
                .getObjectClass(AMObject.GROUP)) != -1) {
            return AMObject.GROUP;
        } else if (objectClasses.indexOf(AMObjectClassManager
                .getObjectClass(AMObject.ASSIGNABLE_DYNAMIC_GROUP)) != -1) {
            return AMObject.ASSIGNABLE_DYNAMIC_GROUP;
        } else if (objectClasses.indexOf(AMObjectClassManager
                .getObjectClass(AMObject.DYNAMIC_GROUP)) != -1) {
            return AMObject.DYNAMIC_GROUP;
        } else if (objectClasses.indexOf(AMObjectClassManager
                .getObjectClass(AMObject.PEOPLE_CONTAINER)) != -1) {
            return AMObject.PEOPLE_CONTAINER;
        } else if (objectClasses.indexOf(AMObjectClassManager
                .getObjectClass(AMObject.GROUP_CONTAINER)) != -1) {
            return AMObject.GROUP_CONTAINER;
        } else if (objectClasses.indexOf(AMObjectClassManager
                .getObjectClass(AMObject.ORGANIZATIONAL_UNIT)) != -1) {
            return AMObject.ORGANIZATIONAL_UNIT;
        } else if (objectClasses.indexOf(AMObjectClassManager
                .getObjectClass(AMObject.RESOURCE)) != -1) {
            return AMObject.RESOURCE;
        }
        return AMObject.UNKNOWN_OBJECT_TYPE;
    }
}
