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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.core.rest;

import static org.forgerock.http.routing.RoutingMode.EQUALS;
import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.json.resource.Resources.newHandler;
import static org.forgerock.json.resource.Resources.newSingleton;
import static org.forgerock.openam.audit.AuditConstants.Component.CONFIG;
import static org.forgerock.openam.audit.AuditConstants.Component.REALMS;
import static org.forgerock.openam.core.rest.sms.SmsServerPropertiesResource.SERVER_DEFAULT_NAME;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Router;
import org.forgerock.openam.core.rest.sms.SmsRequestHandlerFactory;
import org.forgerock.openam.core.rest.sms.SmsServerPropertiesResource;
import org.forgerock.openam.core.rest.sms.SmsServerPropertiesResourceFactory;
import org.forgerock.openam.rest.AbstractRestRouteProvider;
import org.forgerock.openam.rest.ResourceRouter;
import org.forgerock.openam.rest.RestRouteProvider;
import org.forgerock.openam.rest.authz.CrestPrivilegeAuthzModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.sm.SchemaType;

/**
 * A {@link RestRouteProvider} that add routes for all the SMS endpoints.
 *
 * @since 13.0.0
 */
public class SmsRestRouteProvider extends AbstractRestRouteProvider {
    private static final String AM_CONSOLE_CONFIG_XML = "amConsoleConfig.xml";
    private static final String DIRECTORY_CONFIGURATION_TAB_NAME = "directoryConfiguration";

    private final Logger logger = LoggerFactory.getLogger("frRest");

    private SmsRequestHandlerFactory smsRequestHandlerFactory;
    private SmsServerPropertiesResourceFactory smsServerPropertiesResourceFactory;

    @Override
    public void addResourceRoutes(ResourceRouter rootRouter, ResourceRouter realmRouter) {

        realmRouter.route("realms")
                .auditAs(REALMS)
                .authorizeWith(CrestPrivilegeAuthzModule.class)
                .toCollection(RealmResource.class);
        
        realmRouter.route("realm-config")
                .auditAs(CONFIG)
                .authorizeWith(CrestPrivilegeAuthzModule.class)
                .toRequestHandler(STARTS_WITH, smsRequestHandlerFactory.create(SchemaType.ORGANIZATION));

        rootRouter.route("global-config")
                .auditAs(CONFIG)
                .toRequestHandler(STARTS_WITH, smsRequestHandlerFactory.create(SchemaType.GLOBAL));

        for (String tab : getTabNames()) {
            rootRouter.route("global-config/servers/" + SERVER_DEFAULT_NAME + "/properties/" + tab)
                    .auditAs(CONFIG)
                    .authorizeWith(CrestPrivilegeAuthzModule.class)
                    .toRequestHandler(EQUALS, newHandler(smsServerPropertiesResourceFactory.create(tab, true)));
        }

        for (String tab : getTabNames()) {
            rootRouter.route("global-config/servers/{serverName}/properties/" + tab)
                    .auditAs(CONFIG)
                    .authorizeWith(CrestPrivilegeAuthzModule.class)
                    .toRequestHandler(EQUALS, newHandler(smsServerPropertiesResourceFactory.create(tab, false)));
        }
    }

    @Inject
    public void setSmsRequestHandlerFactory(SmsRequestHandlerFactory smsRequestHandlerFactory) {
        this.smsRequestHandlerFactory = smsRequestHandlerFactory;
    }

    @Inject
    public void setSmsServerPropertiesResourceFactory(SmsServerPropertiesResourceFactory factory) {
        this.smsServerPropertiesResourceFactory = factory;
    }

    private Set<String> getTabNames() {
        Set<String> tabNames = new HashSet<>();
        try {
            String result = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(AM_CONSOLE_CONFIG_XML));
            Matcher matcher = Pattern.compile(".*ServerEdit(.*)ViewBean.*").matcher(result);
            while (matcher.find()) {
                tabNames.add(matcher.group(1).toLowerCase());
            }

            // New tabs will not lead to the creation of a ViewBean, so will not be matched in this way.
            // Add additional tab names here so that suitable routes are constructed.
            tabNames.add(DIRECTORY_CONFIGURATION_TAB_NAME);

        } catch (IOException e) {
            logger.error("Error getting tab names", e);
        }
        return tabNames;
    }

}
