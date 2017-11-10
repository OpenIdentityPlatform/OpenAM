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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.entitlement.service;

import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.forgerock.openam.utils.StringUtils.isEmpty;

import java.security.AccessController;

import javax.security.auth.Subject;

import com.google.common.collect.ImmutableMap;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.entitlement.PolicyConstants;
import org.forgerock.openam.entitlement.ResourceType;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.setup.SetupListener;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.sm.ServiceManager;

/**
 * This class is responsible for creating the default Resource Type (URL resource type) for the realm.
 * It is added to the ServiceConfigManager as a listener so gets called automatically if the organization config is
 * changed.
 */
public final class DefaultUrlResourceTypeGenerator implements ServiceListener, SetupListener {

    private static final ResourceType DEFAULT_RESOURCE_TYPE = ResourceType.builder()
            .addPatterns(asSet("*://*:*/*", "*://*:*/*?*"))
            .addActions(new ImmutableMap.Builder<String, Boolean>().put("GET", true).put("POST", true)
                    .put("PUT", true).put("DELETE", true).put("HEAD", true).put("OPTIONS", true)
                    .put("PATCH", true).build())
            .setName("URL")
            .setUUID("UrlResourceType")
            .build();

    @Override
    public void schemaChanged(String serviceName, String version) {
        //This method is not used.
    }

    @Override
    public void globalConfigChanged(String serviceName, String version, String groupName, String serviceComponent,
                                    int type) {
        //This method is not used.
    }

    @Override
    public void organizationConfigChanged(String serviceName, String version, String orgName, String groupName,
                                          String serviceComponent, int type) {
        if (ServiceListener.ADDED == type && ServiceManager.REALM_SERVICE.equals(serviceName) && isEmpty(groupName)) {
            loadDefaultResourceType(orgName);
        }
    }

    private void loadDefaultResourceType(String orgName) {
        ResourceTypeService resourceTypeService = InjectorHolder.getInstance(ResourceTypeService.class);
        try {
            Subject adminSubject = SubjectUtils.createSuperAdminSubject();
            resourceTypeService.saveResourceType(adminSubject, orgName, DEFAULT_RESOURCE_TYPE);
        } catch (EntitlementException ssoe) {
            PolicyConstants.DEBUG.error(
                   "DefaultUrlResourceTypeGenerator.loadDefaultServices. Exception in loading default services ", ssoe);
        }
    }

    @Override
    public void setupComplete() {
        registerListener();
    }

    private static void registerListener() {
        SSOToken adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
        try {
            ServiceConfigManager scm = new ServiceConfigManager(ServiceManager.REALM_SERVICE, adminToken);
            scm.addListener(new DefaultUrlResourceTypeGenerator());
        } catch (SSOException | SMSException e) {
            PolicyConstants.DEBUG.error("DefaultUrlResourceTypeGenerator.setupComplete", e);
        }
    }

}
