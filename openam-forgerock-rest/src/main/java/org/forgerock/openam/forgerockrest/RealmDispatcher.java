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
 * Copyright 2012 ForgeRock AS.
 */
package org.forgerock.openam.forgerockrest;

import static org.forgerock.json.resource.Context.newRootContext;

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
import org.forgerock.json.resource.Connections;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.exception.ResourceException;
import org.forgerock.json.resource.provider.RequestHandler;
import org.forgerock.json.resource.provider.Router;
import org.forgerock.json.resource.provider.UriTemplateRoutingStrategy;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOTokenManager;
import java.security.AccessController;
import com.sun.identity.security.AdminTokenAction;



import com.sun.identity.sm.OrganizationConfigManager;

/**
 * A simple {@code Map} based collection resource provider.
 */
public final class RealmDispatcher  {


    private RealmDispatcher() {

    }

    static private void initRealmEndpoints(OrganizationConfigManager ocm, UriTemplateRoutingStrategy routes) {

        try {
            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                    AdminTokenAction.getInstance());
            String rName = ocm.getOrganizationName();
            if (rName.length() > 1) rName = rName + "/";

            routes.register(rName + "users", new IdentityResource("user", rName));                // Just a simply READ to make sure dispatching works
            routes.register(rName + "agents", new IdentityResource("agent", rName));              // Just a simply READ to make sure dispatching works
            routes.register(rName + "groups", new IdentityResource("group", rName));              // Just a simply READ to make sure dispatching works
            Set subOrgs = ocm.getSubOrganizationNames();
            routes.register(rName + "realms", new RealmResource(subOrgs));

            for (Object theRealm : subOrgs) {
                String realm = rName + (String) theRealm;
                initRealmEndpoints(new OrganizationConfigManager(adminToken, realm), routes);
            }
        } catch (Exception e) {

        }
    }

    static public void initDispatcher(UriTemplateRoutingStrategy routes) {
        try {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction. getInstance());
        OrganizationConfigManager ocm = new OrganizationConfigManager(adminToken,"/");
        initRealmEndpoints(ocm, routes);
        } catch (Exception e) {

        }
    }
}