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
 * $Id: AMNamingAttrManager.java,v 1.6 2009/01/28 05:34:47 ww203982 Exp $
 *
 */

package com.iplanet.am.sdk;

import java.util.HashMap;
import java.util.Map;
import com.sun.identity.shared.ldap.util.DN;

import com.iplanet.am.sdk.common.IDirectoryServices;
import com.sun.identity.shared.debug.Debug;

/**
 * A class to manage the naming attribute related information. This class stores
 * the naming attribute information in the in its cache.
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 */
public class AMNamingAttrManager {

    // Debug object
    static Debug debug = AMCommonUtils.debug;

    static Map namingAttrMap = new HashMap();

    public static String getNamingAttr(int objectType) {
        return getNamingAttr(objectType, null);
    }

    /**
     * Gets the naming attribute after reading it from the corresponding
     * creation template. If not found, a default value will be used
     */
    public static String getNamingAttr(int objectType, String orgDN) {
        String cacheKey = (new Integer(objectType)).toString() + ":"
                + (new DN(orgDN)).toRFCString().toLowerCase();
        if (namingAttrMap.containsKey(cacheKey)) {
            return ((String) namingAttrMap.get(cacheKey));
        } else {
            IDirectoryServices dsServices = AMDirectoryAccessFactory
                    .getDirectoryServices();

            String nAttr = dsServices.getNamingAttribute(objectType, orgDN);
            if (nAttr != null) {
                namingAttrMap.put(cacheKey, nAttr);
            }
            return nAttr;
        }
    }

}
