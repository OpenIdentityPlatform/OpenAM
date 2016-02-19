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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.openam.entitlement.rest;

import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.utils.Time.*;
import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.inject.Inject;
import javax.inject.Named;
import javax.security.auth.Subject;
import java.util.Set;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.SubjectAttributesManager;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CountPolicy;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.forgerockrest.utils.PrincipalRestUtils;
import org.forgerock.openam.rest.RealmAwareResource;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.util.promise.Promise;

/**
 * REST endpoint implementation to query the list of subject attributes appropriate to
 * users in a given realm (taken from the requesting user's realm context).
 *
 * This resource only supports the QUERY operation, and is intended for use by the
 * XUI to populate fields.
 */
public class SubjectAttributesResourceV1 extends RealmAwareResource {

    private final static String JSON_OBJ_NAME = "SubjectAttributes";

    private final Debug debug;

    @Inject
    public SubjectAttributesResourceV1(@Named("frRest") Debug debug) {
        this.debug = debug;
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String resourceId,
            ActionRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(Context context, CreateRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context context, String resourceId,
            DeleteRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String resourceId,
            PatchRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        final Subject mySubject = getContextSubject(context);
        if (mySubject == null) {
            debug.error("SubjectAttributesResource :: QUERY : Unknown Subject");
            return new BadRequestException().asPromise();
        }
        final String principalName = PrincipalRestUtils.getPrincipalNameFromSubject(mySubject);
        final SubjectAttributesManager manager = getSubjectAttributesManager(mySubject, getRealm(context));
        final Set<String> attributes;
        try {
            attributes = manager.getAvailableSubjectAttributeNames();
        } catch (EntitlementException e) {
            debug.error("SubjectAttributesResource :: QUERY by " + principalName + " : Unable to query available " +
                    "subject attribute names.");
            return new InternalServerErrorException().asPromise();
        }
        for (String attr : attributes) {
            handler.handleResource(newResourceResponse(attr, Long.toString(currentTimeMillis()), JsonValue.json(attr)));
        }
        return newResultPromise(newQueryResponse(null, CountPolicy.EXACT, 0));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId,
            ReadRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, String resourceId,
            UpdateRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    SubjectAttributesManager getSubjectAttributesManager(Subject mySubject, String realm) {
        return SubjectAttributesManager.getInstance(mySubject, realm);
    }
}
