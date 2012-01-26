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
 * $Id: SiteIDValidator.java,v 1.3 2008/06/25 05:44:06 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.sm;

import java.security.AccessController;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.security.AdminTokenAction;

/**
 * The class <code>ServerIDValidator</code> is used to validate the correct
 * string format for the attribute iplanet-am-platform-server-list in
 * iPlanetAMPlatformService. e.g. proto://host.port|xx where xx is an unique
 * identifier and could any two bytes value
 */
public class SiteIDValidator implements ServiceAttributeValidator {

    private static final String PLATFORM_SERVER_LIST = 
        "iplanet-am-platform-server-list";

    private static Debug debug = Debug.getInstance("amSession");

    private Set serverAddrSet = new HashSet();

    private Set serverIdSet = new HashSet();

    /**
     * Default Constructor.
     */
    public SiteIDValidator() {
    }

    /**
     * Validates the values for the attribute iplanet-am-platform-server-list.
     * 
     * @param values
     *            the set of values to be validated
     * @return true if all of the values are valid; false otherwise
     */
    public boolean validate(Set values) {
        if (values.isEmpty()) {
            return true;
        }
        Set idSet = new HashSet();
        Set urlSet = new HashSet();
        Iterator it = values.iterator();
        boolean serverLookup = false;
        serverLookup = getServerDetails();
        while (it.hasNext()) {
            String value = (String) it.next();
            StringTokenizer tok = new StringTokenizer(value, "|");
            if (tok.countTokens() != 2) {
                return false;
            }
            String url = tok.nextToken();
            String id = tok.nextToken();
            int byteLength = id.getBytes().length;
            if (byteLength != 2 || idSet.contains(id)) {
                return false;
            } else {
                idSet.add(id);
            }
            if (urlSet.contains(url)) {
                return false;
            } else {
                urlSet.add(url);
            }
            if (serverLookup) {
                if (serverAddrSet.contains(url)) {
                    return false;
                }
                if (serverIdSet.contains(id)) {
                    return false;
                }
            } else {
                if (debug.messageEnabled()) {
                    debug.message("Unable to get server list information. "
                            + "Server validation cannot be performed");
                }
            }
        }
        return true;
    }

    /**
     * Internal method for getting the Server list
     * 
     * @return true if Server list is obtained, false otherwise
     */
    private boolean getServerDetails() {
        if (!serverAddrSet.isEmpty()) {
            return true;
        }
        try {
            SSOToken stoken = (SSOToken) AccessController
                    .doPrivileged(AdminTokenAction.getInstance());
            ServiceSchemaManager ssm = new ServiceSchemaManager(
                    ISAuthConstants.PLATFORM_SERVICE_NAME, stoken);
            if (ssm != null) {
                ServiceSchema ss = ssm.getGlobalSchema();
                if (ss != null) {
                    Map attrs = ss.getAttributeDefaults();
                    Set serverList = (Set) attrs.get(PLATFORM_SERVER_LIST);
                    if (serverList != null && !serverList.isEmpty()) {
                        Iterator serverIterator = serverList.iterator();
                        while (serverIterator.hasNext()) {
                            String serverVal = (String) serverIterator.next();
                            if (serverVal != null) {
                                StringTokenizer tk = new StringTokenizer(
                                        serverVal, "|");
                                String serverUrl = tk.nextToken();
                                String serverId = tk.nextToken();
                                serverAddrSet.add(serverUrl);
                                StringTokenizer sidtk = new StringTokenizer(
                                        serverId, "|");
                                serverIdSet.add(sidtk.nextToken());
                            }
                        }
                    }
                }
            }
            return true;
        } catch (SMSException se) {
            if (debug.messageEnabled()) {
                debug.message("Site List Validator. Unable to get global " +
                        "config: SMSException", se);
            }
        } catch (SSOException ssoe) {
            if (debug.messageEnabled()) {
                debug.message("Site List Validator. Unable to get global " +
                        "config: SSOException", ssoe);
            }
        }
        return false;
    }
}
