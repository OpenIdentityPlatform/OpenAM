/*
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
 * $Id: DSEvent.java,v 1.4 2009/01/28 05:34:49 ww203982 Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */

package com.iplanet.services.ldap.event;

import org.forgerock.openam.ldap.PersistentSearchChangeType;

/**
 * 
 */
public class DSEvent {

    private int _eventType;

    private String _searchId;

    private String _id;

    private String _className;

    public static final int OBJECT_ADDED = PersistentSearchChangeType.ADDED;

    public static final int OBJECT_REMOVED = PersistentSearchChangeType.REMOVED;

    public static final int OBJECT_RENAMED = PersistentSearchChangeType.RENAMED;

    public static final int OBJECT_CHANGED = PersistentSearchChangeType.MODIFIED;

    /**
     * Default constructor
     */
    DSEvent() {

    }

    /**
     * 
     */
    void setEventType(int eventType) {
        this._eventType = eventType;
    }

    /**
     * Returns the type of change event either add, modify, delete, etc.
     * 
     * 
     */
    public int getEventType() {
        return _eventType;
    }

    /**
     * 
     */
    void setID(String id) {
        this._id = id;
    }

    /**
     * Return the Directory Server ID assigned to the entry changed
     * 
     * 
     */
    public String getID() {
        return _id;
    }

    /**
     * 
     */
    void setClassName(String className) {
        this._className = className;
    }

    /**
     * Returns the Directory Server class name for the given entry
     * 
     * 
     */
    public String getClassName() {
        return _className;
    }

    /**
     * 
     */
    void setSearchID(String id) {
        this._searchId = id;
    }

    /**
     * Returns the Directory Server search ID that was assigned when the
     * persistent search was first submited. 
     * 
     */
    public String getSearchID() {
        return _searchId;
    }
}
