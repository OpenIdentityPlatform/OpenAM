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
 * $Id: AMObjectClassManager.java,v 1.5 2008/06/25 05:41:21 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import com.iplanet.am.sdk.common.IDirectoryServices;
import com.sun.identity.shared.debug.Debug;
import java.util.HashMap;
import java.util.Map;

/**
 * A Class to manage the object class information.
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 */
public class AMObjectClassManager {
    // Debug object
    static Debug debug = AMCommonUtils.debug;

    // Map storing the object types and corresponding object classes
    public static Map objectClassMap = new HashMap();

    // Map to store the objectclass with the matching object type (reverse of
    // above)
    public static Map objectTypeMap = new HashMap();

    public static String getObjectClass(int objectType) {
        String type = Integer.toString(objectType);
        String oc = (String) objectClassMap.get(type);
        if (oc == null) {
            oc = getObjectClassFromDS(objectType);
            if (oc.length() != 0) {
                objectClassMap.put(type, oc);
            } else {
                // The right thing to do is to throw an exception here.
            }
        }
        return oc;
    }

    private static String getObjectClassFromDS(int objectType) {
        IDirectoryServices dsServices = AMDirectoryAccessFactory
                .getDirectoryServices();
        return dsServices.getObjectClass(objectType);
    }

    public static int getObjectType(String objectClass) {
        String typeS = (String) objectTypeMap.get(objectClass);
        if (typeS != null) {
            return (Integer.parseInt(typeS));
        }
        if (objectClass.equalsIgnoreCase(getObjectClass(AMObject.USER))) {
            return AMObject.USER;
        } else if (objectClass.equalsIgnoreCase(
                getObjectClass(AMObject.ROLE))) {
            return AMObject.ROLE;
        } else if (objectClass
                .equalsIgnoreCase(getObjectClass(AMObject.FILTERED_ROLE))) {
            return AMObject.FILTERED_ROLE;
        } else if (objectClass
                .equalsIgnoreCase(getObjectClass(AMObject.ORGANIZATION))) {
            return AMObject.ORGANIZATION;
        } else if (objectClass.equalsIgnoreCase(
                getObjectClass(AMObject.GROUP))) {
            return AMObject.GROUP;
        } else if (objectClass
                .equalsIgnoreCase(
                        getObjectClass(AMObject.ASSIGNABLE_DYNAMIC_GROUP))) {
            return AMObject.ASSIGNABLE_DYNAMIC_GROUP;
        } else if (objectClass
                .equalsIgnoreCase(getObjectClass(AMObject.DYNAMIC_GROUP))) {
            return AMObject.DYNAMIC_GROUP;
        } else if (objectClass
                .equalsIgnoreCase(getObjectClass(AMObject.PEOPLE_CONTAINER))) {
            return AMObject.PEOPLE_CONTAINER;
        } else if (objectClass
                .equalsIgnoreCase(getObjectClass(AMObject.GROUP_CONTAINER))) {
            return AMObject.GROUP_CONTAINER;
        } else if (objectClass
                .equalsIgnoreCase(
                        getObjectClass(AMObject.ORGANIZATIONAL_UNIT))) {
            return AMObject.ORGANIZATIONAL_UNIT;
        } else if (objectClass
                .equalsIgnoreCase(getObjectClass(AMObject.RESOURCE))) {
            return AMObject.RESOURCE;
        } else {
            return AMObject.UNKNOWN_OBJECT_TYPE;
        }
    }

}
