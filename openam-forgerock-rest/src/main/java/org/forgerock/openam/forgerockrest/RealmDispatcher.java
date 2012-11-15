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
package org.forgerock.openam.forgerockrest;


import java.lang.Exception;
import java.lang.String;
import java.util.Arrays;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;


import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Router;

import static org.forgerock.json.resource.RoutingMode.EQUALS;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOTokenManager;

import java.security.AccessController;

import com.sun.identity.security.AdminTokenAction;


import com.sun.identity.sm.OrganizationConfigManager;

/**
 * A simple {@code Map} based collection resource provider.
 */
public final class RealmDispatcher {


    private RealmDispatcher() {

    }

    static private void initRealmEndpoints(OrganizationConfigManager ocm, Router router) {

        try {
            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
            String rName = ocm.getOrganizationName();
            if (rName.length() > 1) rName = rName + "/";


            router.addRoute(EQUALS, rName + "users", new IdentityResource("user", rName));
            router.addRoute(EQUALS, rName + "agents", new IdentityResource("agent", rName));
            router.addRoute(EQUALS, rName + "groups", new IdentityResource("group", rName));

            Set subOrgs = ocm.getSubOrganizationNames();           //grab subrealms
            router.addRoute(EQUALS, "/realms", new RealmResource(subOrgs));
            //Recursively calls on each realm registring users agents groups for each subrealm
            for (Object theRealm : subOrgs) {
                String realm = rName + (String) theRealm;
                initRealmEndpoints(new OrganizationConfigManager(adminToken, realm), router);
            }
        } catch (Exception e) {

        }
    }

    static public void initDispatcher(Router router) {
        try {
            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                    AdminTokenAction.getInstance());
            OrganizationConfigManager ocm = new OrganizationConfigManager(adminToken, "/");
            initRealmEndpoints(ocm, router);
        } catch (Exception e) {

        }
    }
}