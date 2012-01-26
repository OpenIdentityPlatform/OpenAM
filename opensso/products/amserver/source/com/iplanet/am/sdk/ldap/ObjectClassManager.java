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
 * $Id: ObjectClassManager.java,v 1.3 2008/06/25 05:41:26 qcheng Exp $
 *
 */

package com.iplanet.am.sdk.ldap;

import com.iplanet.am.sdk.AMObject;
import com.sun.identity.shared.debug.Debug;
import java.util.HashMap;
import java.util.Map;

/**
 * A Class to manage the object class information.
 */
class ObjectClassManager {
    // Debug object
    static Debug debug = CommonUtils.getDebugInstance();

    // Default Object Classes
    private static final String DEFAULT_USER_OBJECT_CLASS = 
        "inetorgperson";

    private static final String DEFAULT_RESOURCE_OBJECT_CLASS = 
        "inetcalresource";

    private static final String DEFAULT_ROLE_OBJECT_CLASS = 
        "nsmanagedroledefinition";

    private static final String DEFAULT_FILTERED_ROLE_OBJECT_CLASS = 
        "nsfilteredroledefinition";

    private static final String DEFAULT_ORGANIZATION_OBJECT_CLASS = 
        "organization";

    private static final String DEFAULT_ORGANIZATIONAL_UNIT_OBJECT_CLASS = 
        "organizationalunit";

    private static final String DEFAULT_GROUP_OBJECT_CLASS = 
        "iplanet-am-managed-group";

    private static final String DEFAULT_DYNAMIC_GROUP_OBJECT_CLASS = 
        "groupofurls";

    private static final String DEFAULT_ASSIGNABLE_DYNAMIC_GROUP_OBJECT_CLASS = 
        "iplanet-am-managed-assignable-group";

    private static final String DEFAULT_GROUP_CONTAINER_OBJECT_CLASS = 
        "iplanet-am-managed-group-container";

    private static final String DEFAULT_PEOPLE_CONTAINER_OBJECT_CLASS = 
        "nsManagedPeopleContainer";

    // Map storing the object types and corresponding object classes
    public static Map objectClassMap = new HashMap();

    // Map to store the objectclass with the matching object type
    // (reverse of above)
    public static Map objectTypeMap = new HashMap();

    public static String getObjectClass(int objectType) {
        String type = Integer.toString(objectType);
        String oc = (String) objectClassMap.get(type);
        if (oc == null) {
            oc = getObjectClassFromDS(objectType);
            if (oc.length() != 0) {
                objectClassMap.put(type, oc);
            } else {
                // FIXME:
                // The right thing to do is to throw an exception here.
            }
        }
        return oc;
    }

    private static String getObjectClassFromDS(int objectType) {
        // TODO: Try to obtain the Object Class from Creation Templates
        // If not found, try to extract it from search filter

        // Obtain the Object Class from the global search filter for this object
        // If not found, return the default
        String searchFilter = SearchFilterManager
                .getGlobalSearchFilter(objectType); // The search filter is
                                                    // already in lower case

        // Parse the Search filter to obtain the object class
        // Note: The object class that is picked up is the value of the first
        // object class specified in the filter.
        String pattern = "objectclass="; // Pattern to look for
        int index = searchFilter.indexOf(pattern);
        String objectClass = null;
        if (index != -1) {
            int startIndex = index + pattern.length();
            int endIndex = searchFilter.indexOf(')', startIndex);
            if (endIndex != -1) {
                objectClass = searchFilter.substring(startIndex, endIndex);
            }
        } else {
            objectClass = getDefaultObjectClass(objectType);
        }
        if (debug.messageEnabled()) {
            debug.message("ObjectClassManager.getObjectClassFromDS()- "
                    + "objectType: " + objectType + " objectclass: "
                    + objectClass.toLowerCase());
        }
        return objectClass.toLowerCase();
    }

    public static int getObjectType(String objectClass) {
        String typeS = (String) objectTypeMap.get(objectClass.toLowerCase());
        if (typeS != null) {
            return (Integer.parseInt(typeS));
        }
        if (objectClass.equalsIgnoreCase(getObjectClass(AMObject.USER))) {
            return AMObject.USER;
        } else if (objectClass.equalsIgnoreCase(
                getObjectClass(AMObject.ROLE))) {
            return AMObject.ROLE;
        } else if (objectClass.equalsIgnoreCase(
                getObjectClass(AMObject.FILTERED_ROLE))) {
            return AMObject.FILTERED_ROLE;
        } else if (objectClass.equalsIgnoreCase(
                getObjectClass(AMObject.ORGANIZATION))) {
            return AMObject.ORGANIZATION;
        } else if (objectClass.equalsIgnoreCase(
                getObjectClass(AMObject.GROUP))) {
            return AMObject.GROUP;
        } else if (objectClass.equalsIgnoreCase(
                getObjectClass(AMObject.ASSIGNABLE_DYNAMIC_GROUP))) {
            return AMObject.ASSIGNABLE_DYNAMIC_GROUP;
        } else if (objectClass.equalsIgnoreCase(
                getObjectClass(AMObject.DYNAMIC_GROUP))) {
            return AMObject.DYNAMIC_GROUP;
        } else if (objectClass
                .equalsIgnoreCase(getObjectClass(AMObject.PEOPLE_CONTAINER))) {
            return AMObject.PEOPLE_CONTAINER;
        } else if (objectClass.equalsIgnoreCase(
                getObjectClass(AMObject.GROUP_CONTAINER))) {
            return AMObject.GROUP_CONTAINER;
        } else if (objectClass.equalsIgnoreCase(
                getObjectClass(AMObject.ORGANIZATIONAL_UNIT))) {
            return AMObject.ORGANIZATIONAL_UNIT;
        } else if (objectClass.equalsIgnoreCase(
                getObjectClass(AMObject.RESOURCE))) {
            return AMObject.RESOURCE;
        } else {
            return AMObject.UNKNOWN_OBJECT_TYPE;
        }
    }

    /**
     * Gets the default object type corresponding to an object type
     */
    private static String getDefaultObjectClass(int objectType) {
        switch (objectType) {
        case AMObject.USER:
            return DEFAULT_USER_OBJECT_CLASS;
        case AMObject.ROLE:
            return DEFAULT_ROLE_OBJECT_CLASS;
        case AMObject.FILTERED_ROLE:
            return DEFAULT_FILTERED_ROLE_OBJECT_CLASS;
        case AMObject.GROUP:
            return DEFAULT_GROUP_OBJECT_CLASS;
        case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
            return DEFAULT_ASSIGNABLE_DYNAMIC_GROUP_OBJECT_CLASS;
        case AMObject.DYNAMIC_GROUP:
            return DEFAULT_DYNAMIC_GROUP_OBJECT_CLASS;
        case AMObject.ORGANIZATION:
            return DEFAULT_ORGANIZATION_OBJECT_CLASS;
        case AMObject.PEOPLE_CONTAINER:
            return DEFAULT_PEOPLE_CONTAINER_OBJECT_CLASS;
        case AMObject.ORGANIZATIONAL_UNIT:
            return DEFAULT_ORGANIZATIONAL_UNIT_OBJECT_CLASS;
        case AMObject.GROUP_CONTAINER:
            return DEFAULT_GROUP_CONTAINER_OBJECT_CLASS;
        case AMObject.RESOURCE:
            return DEFAULT_RESOURCE_OBJECT_CLASS;
        default:
            return ""; // This should not occur. Throw an exception here.
        }
    }

}
