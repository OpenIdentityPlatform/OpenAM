/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2012 ForgeRock Inc.
 */
package org.forgerock.openam.dashboard;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.json.fluent.JsonValue;

import java.security.AccessController;
import java.util.*;


public final class Dashboard {

    public static JsonValue getDefinitions(SSOToken token) {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());

        JsonValue result = new JsonValue(new HashMap<String, Object>());
        try {
            ServiceConfigManager scm = new ServiceConfigManager("dashboardService", adminToken);
            ServiceConfig sc = scm.getGlobalConfig("default");
            Set<String> subNames = sc.getSubConfigNames();

            for (String s : subNames) {
                ServiceConfig sc1 = sc.getSubConfig(s);
                Map app = new LinkedHashMap<String, Object>();
                Map attrs = sc1.getAttributes();

                for (String s1 : (Set<String>) attrs.keySet()) {
                    List<String> sList = new ArrayList((Set<String>) (attrs.get(s1)));
                    app.put(s1.toLowerCase(), sList);
                }
                result.put(s, app);
            }

        } catch (SSOException ex) {
            // No need to do anything,  return empty object
        } catch (SMSException ex) {
            // No need to do anything,  return empty object
        }
        return result;
    }

    public static JsonValue getAllowedDashboard(SSOToken token) {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());

        JsonValue allApps = getDefinitions(token);
        JsonValue result = new JsonValue(new HashMap<String, Object>());
        try {
            ServiceConfigManager scm = new ServiceConfigManager("dashboardService", adminToken);
            ServiceConfig sc = scm.getOrganizationConfig(token.getProperty("Organization"), "default");
            Set<String> apps = (Set<String>) sc.getAttributes().get("assignedDashboard");

            for (String s : apps) {
                String sTemp = s.toLowerCase();
                JsonValue val = allApps.get(sTemp);
                if (val != null) {
                    result.put(sTemp, val.getObject());
                }
            }

        } catch (SSOException ex) {
            // No need to do anything,  return empty object
        } catch (SMSException ex) {
            // No need to do anything,  return empty object

        }
        return result;
    }

    public static JsonValue getAssignedDashboard(SSOToken token) {
        JsonValue allApps = getDefinitions(token);
        JsonValue result = new JsonValue(new HashMap<String, Object>());
        try {
            AMIdentity user = new AMIdentity(token);
            Set<String> apps = (Set<String>) user.getAttribute("assignedDashboard");

            for (String s : apps) {
                String sTemp = s.toLowerCase();
                JsonValue val = allApps.get(sTemp);
                if (val != null) {
                    result.put(sTemp, val.getObject());
                }
            }
        } catch (SSOException ex) {
            // No need to do anything,  return empty object
        } catch (IdRepoException ex) {
            // No need to do anything,  return empty object
        }
        return result;
    }
}

