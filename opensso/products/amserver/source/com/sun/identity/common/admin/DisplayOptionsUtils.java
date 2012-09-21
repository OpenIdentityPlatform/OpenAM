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
 * $Id: DisplayOptionsUtils.java,v 1.3 2008/06/25 05:42:27 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.common.admin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchemaManager;

public class DisplayOptionsUtils {
    private static final String CONSOLE_SERVICE_NAME = 
        "iplanetamadminconsoleservice";

    private static final String ATTR_CONSOLE_ROLE_DISPLAY_OPTION =
        "iplanet-am-admin-console-role-display-options";

    private DisplayOptionsUtils() {
    }

    /**
     * Returns a set of default role display options of a given role definition.
     * 
     * @param ssoToken
     *            Single Sign On Token for get service configuration
     *            information.
     * @param roleDefn
     *            Name of role definition.
     * @return a set of default role display options of a given role definition.
     * @throws SMSException
     *             if there are problems reading service configuration
     *             information.
     * @throws SSOException
     *             if Single Sign On Token is invalid.
     */
    public static Set getDefaultDisplayOptions(SSOToken ssoToken,
            String roleDefn) throws SMSException, SSOException {
        Set displayOptions = Collections.EMPTY_SET;
        Map mapSvcConf = getServiceConfiguration(ssoToken,
                CONSOLE_SERVICE_NAME, SchemaType.GLOBAL);

        if (!mapSvcConf.isEmpty()) {
            Set allDisplayOptions = (Set) mapSvcConf
                    .get(ATTR_CONSOLE_ROLE_DISPLAY_OPTION);

            if ((allDisplayOptions != null) && !allDisplayOptions.isEmpty()) {
                displayOptions = getDefaultDisplayOptions(allDisplayOptions,
                        roleDefn);
            }
        }

        return displayOptions;
    }

    private static Set getDefaultDisplayOptions(Set allDisplayOptions,
            String roleDefn) {
        Set displayOptions = null;
        String prefix = roleDefn + "|";

        for (Iterator iter = allDisplayOptions.iterator(); iter.hasNext()
                && (displayOptions == null);) {
            String options = (String) iter.next();

            if (options.startsWith(prefix)) {
                StringTokenizer st = new StringTokenizer(options, "|");

                if (st.countTokens() == 3) {
                    st.nextToken(); // skip role definition token
                    st.nextToken(); // skip description token

                    StringTokenizer stz = new StringTokenizer(st.nextToken(),
                            " ");
                    displayOptions = new HashSet(stz.countTokens());

                    while (stz.hasMoreElements()) {
                        displayOptions.add(stz.nextToken());
                    }
                }
            }
        }

        return (displayOptions != null) ? displayOptions
                : Collections.EMPTY_SET;
    }

    /**
     * Returns a map of attribute name to its values of a service configuration.
     */
    private static Map getServiceConfiguration(SSOToken ssoToken,
            String serviceName, SchemaType type) throws SMSException,
            SSOException {
        Map attrMap = Collections.EMPTY_MAP;

        if (type != SchemaType.POLICY) {
            ServiceSchemaManager scm = new ServiceSchemaManager(serviceName,
                    ssoToken);
            attrMap = scm.getSchema(type).getAttributeDefaults();
        }

        return attrMap;
    }
}
