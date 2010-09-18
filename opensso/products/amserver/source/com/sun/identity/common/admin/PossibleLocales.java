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
 * $Id: PossibleLocales.java,v 1.5 2008/08/06 16:43:24 veiming Exp $
 *
 */

package com.sun.identity.common.admin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.ChoiceValues;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.security.AccessController;

/**
 * This class provides a set of possible locales.
 */
public class PossibleLocales extends ChoiceValues {
    private static SSOToken ssoToken = getAdminSSOToken();

    private static final String G11N_SERIVCE_LOCALE_CHARSET_MAPPING = 
        "sun-identity-g11n-settings-locale-charset-mapping";

    private static final String G11N_SERVICE_NAME = "iPlanetG11NSettings";

    private static final String SEPARATOR = "|";

    private static final String LOCALE_ATTR = "locale";

    private static Set DEFAULT_LOCALES;

    private static Debug debug = Debug.getInstance("PossibleLocales");

    static {
        DEFAULT_LOCALES = new HashSet();
        DEFAULT_LOCALES.add("locale=en|charset=UTF-8;ISO-8859-1");
        DEFAULT_LOCALES.add("locale=ja|charset=UTF-8;Shift_JIS;EUC-JP");
        DEFAULT_LOCALES.add("locale=fr|charset=UTF-8;ISO-8859-15");
        DEFAULT_LOCALES.add("locale=de|charset=UTF-8;ISO-8859-15");
        DEFAULT_LOCALES.add("locale=es|charset=UTF-8;ISO-8859-15");
        DEFAULT_LOCALES.add("locale=ko|charset=UTF-8;EUC-KR");
        DEFAULT_LOCALES.add("locale=zh|charset=UTF-8;GB2312");
        DEFAULT_LOCALES.add("locale=ar|charset=UTF-8;ISO-8859-6");
        DEFAULT_LOCALES.add("locale=th|charset=UTF-8;TIS-620");
        DEFAULT_LOCALES.add("locale=zh_TW|charset=UTF-8;BIG5");
    }

    /**
     * Returns a map of locales to its localized name.
     * 
     * @return a map of locales to its localized name.
     */
    public Map getChoiceValues() {
        Map map = new HashMap();

        /*
         * provides a blank value because preferred locale attribute value can
         * be blanked.
         */
        map.put("", "-");

        ServiceSchemaManager mgr = getG11NServiceSchemaManager();
        Set values = DEFAULT_LOCALES;

        if (mgr != null) {
            AttributeSchema attributeSchema =
                getLocaleCharsetMappingAttributeSchema(mgr);

            if (attributeSchema != null) {
                values = attributeSchema.getDefaultValues();
            }
        }

        if ((values != null) && !values.isEmpty()) {
            for (Iterator iter = values.iterator(); iter.hasNext();) {
                String locale = parseLocaleCharsetValue((String)iter.next());
                if ((locale != null) && (locale.length() > 0)) {
                    map.put(locale, locale);
                }
            }
        }

        return map;
    }

    private static String parseLocaleCharsetValue(String localeCharset) {
        String locale = null;
        StringTokenizer st = new StringTokenizer(localeCharset, SEPARATOR);

        while (st.hasMoreTokens() && (locale == null)) {
            locale = getLocale(st.nextToken());
        }

        return locale;
    }

    private static String getLocale(String str) {
        String locale = null;

        StringTokenizer st = new StringTokenizer(str, "=");

        if (st.countTokens() == 2) {
            String attr = st.nextToken();

            if (attr.equals(LOCALE_ATTR)) {
                locale = st.nextToken();
            }
        }

        return locale;
    }

    private AttributeSchema getLocaleCharsetMappingAttributeSchema(
            ServiceSchemaManager mgr) {
        AttributeSchema attributeSchema = null;

        try {
            ServiceSchema schema = mgr.getSchema(SchemaType.GLOBAL);

            if (schema != null) {
                attributeSchema = schema.getAttributeSchema(
                        G11N_SERIVCE_LOCALE_CHARSET_MAPPING);
            }
        } catch (SMSException smse) {
            debug.error(
                    "PossibleLocales.getLocaleCharsetMappingAttributeSchema",
                    smse);
        }

        return attributeSchema;
    }

    private ServiceSchemaManager getG11NServiceSchemaManager() {
        ServiceSchemaManager mgr = null;

        if (ssoToken != null) {
            try {
                mgr = new ServiceSchemaManager(G11N_SERVICE_NAME, ssoToken);
            } catch (SSOException ssoe) {
                debug.error("PossibleLocales.getG11NServiceSchemaManager",
                     ssoe);
            } catch (SMSException smse) {
                String installTime = SystemProperties.get(
                    Constants.SYS_PROPERTY_INSTALL_TIME, "false");
                if (installTime.equals("false")) {
                    debug.error("PossibleLocales.getG11NServiceSchemaManager",
                        smse);
                }
            }
        }

        return mgr;
    }

    private static SSOToken getAdminSSOToken() {
        return (SSOToken)AccessController.doPrivileged(
            AdminTokenAction.getInstance());
    }
}
