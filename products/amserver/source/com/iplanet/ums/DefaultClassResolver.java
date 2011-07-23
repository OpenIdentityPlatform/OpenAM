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
 * $Id: DefaultClassResolver.java,v 1.3 2008/06/25 05:41:44 qcheng Exp $
 *
 */

package com.iplanet.ums;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.services.ldap.Attr;
import com.iplanet.services.ldap.AttrSet;

/**
 * Default IClassResolver implementation that can resolve the Java class to
 * instantiate for a specific collection of attributes.
 * 
 * @see com.iplanet.ums.TemplateManager
 * @see com.iplanet.ums.IClassResolver
 */

public class DefaultClassResolver implements java.io.Serializable,
        IClassResolver {

    private static Debug debug;
    static {
        debug = Debug.getInstance(IUMSConstants.UMS_DEBUG);
    }

    /**
     * Default constructor
     */
    public DefaultClassResolver() {
    }

    /**
     * Resolves a set of attributes to a subclass of PersistentObject and
     * returns the class. This implementation compares the object classes in the
     * set to a table of default mappings.
     * 
     * @param id
     *            ID of the entry
     * @param set
     *            a set of attributes of an object
     * @return a class for a corresponding object, or <code>null</code> if no
     *         class could be resolved
     */
    public Class resolve(String id, AttrSet set) {
        String[][] OC_JC_MAP = null;
        try {
            OC_JC_MAP = ConfigManagerUMS.getConfigManager().getClassResolver();
        } catch (ConfigManagerException e) {
            debug.error("ConfigManager.getClassResolver(): " + e);
        }
        Class javaClass = com.iplanet.ums.PersistentObject.class;
        if (OC_JC_MAP == null) {
            debug.warning("DefaultClassResolver.resolve: OC_JC_MAP is null");
            // default the object to com.iplanet.ums.PersistentObject
            return javaClass;
        }
        Attr attr = set.getAttribute("objectclass");
        String[] objectClasses = null;
        if (attr != null) {
            objectClasses = attr.getStringValues();
        }
        if (objectClasses != null) {
            int ocLength = objectClasses.length;
            int ocJcLength = OC_JC_MAP.length;
            outerLoop: for (int k = 0; k < ocJcLength; k++) {
                for (int i = 0; i < ocLength; i++) {
                    if (objectClasses[i].equalsIgnoreCase(OC_JC_MAP[k][0])) {
                        try {
                            javaClass = Class.forName(OC_JC_MAP[k][1]);
                        } catch (Exception e) {
                            debug.error(
                                    "Exception while trying Class.forName for: "
                                    + OC_JC_MAP[k][1] + " : "+ e.getMessage() 
                                    + " - Defaulting to PersistentObject");
                        }
                        break outerLoop;
                    }
                }
            }
        }
        return javaClass;
    }
}
