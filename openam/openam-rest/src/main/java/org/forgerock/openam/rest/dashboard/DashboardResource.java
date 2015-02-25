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
 * Copyright 2012-2014 ForgeRock Inc.
 */

package org.forgerock.openam.rest.dashboard;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import java.util.HashMap;
import javax.inject.Inject;
import javax.inject.Named;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PermanentException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.dashboard.Dashboard;
import org.forgerock.openam.forgerockrest.RestUtils;
import org.forgerock.openam.forgerockrest.utils.PrincipalRestUtils;
import org.forgerock.openam.rest.resource.SSOTokenContext;

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
    public void actionCollection(final ServerContext context, final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionInstance(final ServerContext context, final String resourceId, final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createInstance(final ServerContext context, final CreateRequest request,
            final ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteInstance(final ServerContext context, final String resourceId, final DeleteRequest request,
            final ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void patchInstance(final ServerContext context, final String resourceId, final PatchRequest request,
            final ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void queryCollection(final ServerContext context, final QueryRequest request,
            final QueryResultHandler handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readInstance(final ServerContext context, final String resourceId, final ReadRequest request,
            final ResultHandler<Resource> handler) {

        try {
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

            Resource resource = new Resource("0", String.valueOf(System.currentTimeMillis()), val);
            handler.handleResult(resource);
        } catch (SSOException ex) {
            debug.error("DashboardResource :: READ : SSOToken was not found.");
            handler.handleError(new PermanentException(401, "Unauthorized", null));
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateInstance(final ServerContext context, final String resourceId, final UpdateRequest request,
            final ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }
}