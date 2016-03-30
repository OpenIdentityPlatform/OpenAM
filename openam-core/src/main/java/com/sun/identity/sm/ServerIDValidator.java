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
 * $Id: ServerIDValidator.java,v 1.3 2008/06/25 05:44:05 qcheng Exp $
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
public class ServerIDValidator implements ServiceAttributeValidator {

    private static final String PLATFORM_SITE_LIST = 
        "iplanet-am-platform-site-list";

    private static Debug debug = Debug.getInstance("amSession");

    private Set siteIdSet = new HashSet();

    private Set siteAddrSet = new HashSet();

    /**
     * Default Constructor.
     */
    public ServerIDValidator() {
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
        boolean siteLookup = false;
        siteLookup = getSiteDetails();
        while (it.hasNext()) {
            String value = (String) it.next();
            StringTokenizer tok = new StringTokenizer(value, "|");
            String url = tok.nextToken();
            StringBuilder buff = new StringBuilder();
            int count = 0;
            while (tok.hasMoreTokens()) {
                String lbid = tok.nextToken();
                if (lbid.getBytes().length != 2) {
                    return false;
                }
                switch (count) {
                case 0:
                    if (siteLookup) {
                        if (siteIdSet.contains(lbid)) {
                            return false;
                        }
                    }
                    break;
                default:
                    if (count > 0) {
                        int idx = (buff.toString()).indexOf(lbid);
                        if (idx > -1) {
                            return false;
                        }
                        if (siteLookup) {
                            if (validateID(lbid) == false) {
                                return false;
                            }
                        } else {
                            // Just log the message for validation
                            if (debug.messageEnabled()) {
                                debug.message("Unable to get site list " +
                                        "information. Site validation cannot " +
                                        "be performed");
                            }
                        }
                    } else {
                        return false;
                    }
                    break;
                }

                buff.append(lbid);
                if (tok.hasMoreTokens()) {
                    buff.append("|");
                    count++;
                }
                if (idSet.contains(lbid) && count < 1) {
                    return false;
                } else {
                    idSet.add(lbid);
                }
            }
            if (urlSet.contains(url)) {
                return false;
            } else {
                urlSet.add(url);
            }
            if (siteAddrSet.contains(url)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Internal method for checking if ServerID exits in SiteID list
     * 
     * @param sId
     *            serverID
     * @return true if serverID exits in Site list, false otherwise
     */
    private boolean validateID(String sId) {
        if (siteIdSet.isEmpty()) {
            return false;
        }
        if (!siteIdSet.contains(sId)) {
            return false;
        }
        return true;
    }

    /**
     * Internal method for getting the Site list
     * 
     * @return true if Site list is obtained, false otherwise
     */
    private boolean getSiteDetails() {
        if (!siteAddrSet.isEmpty()) {
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
                    Set siteList = (Set) attrs.get(PLATFORM_SITE_LIST);
                    if (siteList != null && !siteList.isEmpty()) {
                        Iterator siteIterator = siteList.iterator();
                        while (siteIterator.hasNext()) {
                            String siteVal = (String) siteIterator.next();
                            if (siteVal != null) {
                                StringTokenizer tk = new StringTokenizer(
                                        siteVal, "|");
                                String siteUrl = tk.nextToken();
                                String siteID = tk.nextToken();
                                siteAddrSet.add(siteUrl);
                                siteIdSet.add(siteID);
                            }
                        }
                    }
                }
            }
            return true;
        } catch (SMSException se) {
            if (debug.messageEnabled()) {
                debug.message("Server List validator. Unable to get global " +
                        "config: SMSException", se);
            }
        } catch (SSOException ssoe) {
            if (debug.messageEnabled()) {
                debug.message("Server List validator. Unable to get global " +
                        "config: SSOException", ssoe);
            }
        }
        return false;
    }
}
