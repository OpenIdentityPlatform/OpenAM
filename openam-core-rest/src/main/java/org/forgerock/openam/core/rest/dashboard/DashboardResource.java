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
 * Copyright 2012-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.core.rest.dashboard;

import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DASHBOARD_RESOURCE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.PATH_PARAM;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.READ_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.TITLE;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.HashMap;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Parameter;
import org.forgerock.api.annotations.Read;
import org.forgerock.api.annotations.Schema;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.dashboard.Dashboard;
import org.forgerock.openam.forgerockrest.utils.PrincipalRestUtils;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;

/**
 * JSON REST interface to return specific information from the Dashboard service.
 *
 * This endpoint only supports the READ operation - and then only for specific
 * values (referred to as the resourceId).
 */
@CollectionProvider(
        details = @Handler(
                title = DASHBOARD_RESOURCE + TITLE,
                description = DASHBOARD_RESOURCE + DESCRIPTION,
                resourceSchema = @Schema(
                        schemaResource = "Dashboard.resource.schema.json"),
                mvccSupported = false),
        pathParam = @Parameter(name = "resourceId", type = "string", description = DASHBOARD_RESOURCE +
                PATH_PARAM + DESCRIPTION))
public final class DashboardResource {

    private final Debug debug;

    @Inject
    public DashboardResource(@Named("frRest") Debug debug) {
        this.debug = debug;
    }

    /**
     * {@inheritDoc}
     */
    @Read(operationDescription = @Operation(
            description = DASHBOARD_RESOURCE + READ_DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId,
            ReadRequest request) {
        SSOTokenContext tokenContext = context.asContext(SSOTokenContext.class);
        SSOToken token = tokenContext.getCallerSSOToken();

        final String principalName = PrincipalRestUtils.getPrincipalNameFromServerContext(context);

        JsonValue val = new JsonValue(new HashMap<String, Object>());

        if (resourceId.equals("defined")) {
            if (debug.messageEnabled()) {
                debug.message("DashboardResource :: READ by " + principalName +
                        ": Locating definitions from DashboardService.");
            }
            val = Dashboard.getDefinitions(token);
        } else if (resourceId.equals("available")) {
            if (debug.messageEnabled()) {
                debug.message("DashboardResource :: READ by " + principalName +
                    ": Locating allowed apps from DashboardService.");
            }
            val = Dashboard.getAllowedDashboard(token);
        } else if (resourceId.equals("assigned")) {
            if (debug.messageEnabled()) {
                debug.message("DashboardResource :: READ by " + principalName +
                        ": Locating assigned apps from DashboardService.");
            }
            val = Dashboard.getAssignedDashboard(token);
        }

        ResourceResponse resource = newResourceResponse("0", String.valueOf(val.getObject().hashCode()), val);
        return newResultPromise(resource);
    }

}