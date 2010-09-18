/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: EntityUtils.java,v 1.2 2008/06/25 05:52:26 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMEntity;
import com.iplanet.am.sdk.AMEntityType;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.util.AMResourceBundleCache;
import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOException;
import com.sun.identity.common.ISLocaleContext;
import com.sun.identity.shared.locale.Locale;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;


/**
 * The class provides common entity helper methods.
 */
class EntityUtils {
    private final static String USER_SERVICE = "iPlanetAMUserService";
    private ISLocaleContext localeContext = new ISLocaleContext(); 

    /**
     * Prints entity information on line.
     *
     * @param prnUtl Print writer.
     * @param entity <code>AMEntity</code> object.
     * @param connection Store connection object.
     * @param isDNsOnly true to print DNs information only.
     */
    static void printEntityInformation(
        PrintUtils prnUtl,
        AMEntity entity,
        AMStoreConnection connection,
        boolean isDNsOnly
    ) throws AdminException
    {
        try {
            AdminReq.writer.println("  " + entity.getDN());

            if (!isDNsOnly) {
                prnUtl.printAVPairs(entity.getAttributes(), 2);
            }
        } catch (AMException ame) {
            throw new AdminException(ame.toString());
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe.toString());
        }
    }

    /**
     * Returns a map of entity names and <code>AMEntityType</code> object.
     */
    public Map getEntityTypesMap(AMStoreConnection dpConnection) {
        Map entityTypes= Collections.EMPTY_MAP;
        Set set = dpConnection.getEntityTypes();

        if (set != null && !set.isEmpty()) {
            entityTypes = new HashMap(set.size() * 2);
            Iterator iter = set.iterator();
            while (iter.hasNext()) {
                AMEntityType type = (AMEntityType)iter.next();
                String svcName = type.getServiceName();
                if (svcName != null && !svcName.equals(USER_SERVICE)) {
                    entityTypes.put(type.getName(), type);
                }
            }
        } else {
            entityTypes = Collections.EMPTY_MAP;
        }
        return entityTypes;
    }

    /**
     * Returns the localized string of an attribute from the i18n key. The i18n
     * key will be looked up in the properties file for the specified service
     * and the display string corresponding to the key will be returned. If
     * key does not exist in the properties file the key will be returned.
     *
     * @param serviceName name of service.
     * @param key i18n key for the attribute.
     * @return localized string of an attribute in a service.
     */
    public String getL10NAttributeName(
        AMStoreConnection dpConnection,
        String serviceName, 
        String key) {
        String i18nName = key;

        try {
            String name = dpConnection.getI18NPropertiesFileName(serviceName);

            if (name != null) {
                ResourceBundle rb = getBundle(name, localeContext.getLocale());
                i18nName = Locale.getString(rb, key, AdminReq.debug);
            }
        } catch (AMException ame) {
            AdminReq.debug.warning ("EntityUtils.getL10NAttributeName",ame);
        } catch (MissingResourceException mre) {
            AdminReq.debug.warning ("EntityUtils.getL10NAttributeName",mre);
        }

        return i18nName;
    }

    /**
     * Returns resource bundle.
     *
     * @param name of bundle.
     * @param locale of bundl.e
     * @return resource bundle.
     */
    public static ResourceBundle getBundle(
        String name, 
        java.util.Locale locale) 
    {
        AMResourceBundleCache cache = AMResourceBundleCache.getInstance();
        ResourceBundle rb = cache.getResBundle(name, locale);

        if (rb == null) {
            rb = cache.getResBundle("amAdminCLI", locale);
        }

        return rb;
    }
}

