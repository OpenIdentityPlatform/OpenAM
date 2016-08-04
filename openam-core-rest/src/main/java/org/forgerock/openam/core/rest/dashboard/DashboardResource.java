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
 * Copyright 2012-2015 ForgeRock AS.
 */

package org.forgerock.openam.core.rest.dashboard;

import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PermanentException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.dashboard.Dashboard;
import org.forgerock.openam.forgerockrest.utils.PrincipalRestUtils;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.util.promise.Promise;

/**
 * JSON REST interface to return specific information from the Dashboard service.
 *
 * This endpoint only supports the READ operation - and then only for specific
 * values (referred to as the resourceId).
 */
public final class DashboardResource implements CollectionResourceProvider {

    private final Debug debug;

    @Inject
    public DashboardResource(@Named("frRest") Debug debug) {
        this.debug = debug;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String resourceId,
            ActionRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(Context context, CreateRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context context, String resourceId,
            DeleteRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String resourceId,
            PatchRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        return RestUtils.generateUnsupportedOperation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, String resourceId,
            UpdateRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }
}