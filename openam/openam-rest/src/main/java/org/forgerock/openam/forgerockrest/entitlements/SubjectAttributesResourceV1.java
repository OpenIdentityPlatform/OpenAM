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
* Copyright 2014 ForgeRock AS.
*/
package org.forgerock.openam.forgerockrest.entitlements;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.SubjectAttributesManager;
import com.sun.identity.shared.debug.Debug;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.security.auth.Subject;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.forgerockrest.RestUtils;
import org.forgerock.openam.forgerockrest.utils.PrincipalRestUtils;

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
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request, ResultHandler<JsonValue> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    @Override
    public void createInstance(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    @Override
    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request, ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request, ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    @Override
    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        final Subject mySubject = getContextSubject(context);

        if (mySubject == null) {
            debug.error("SubjectAttributesResource :: QUERY : Unknown Subject");
            handler.handleError(ResourceException.getException(ResourceException.BAD_REQUEST));
            return;
        }

        final String principalName = PrincipalRestUtils.getPrincipalNameFromSubject(mySubject);
        final SubjectAttributesManager manager = getSubjectAttributesManager(mySubject, getRealm(context));

        final Set<String> attributes;
        try {
            attributes = manager.getAvailableSubjectAttributeNames();
        } catch (EntitlementException e) {
            debug.error("SubjectAttributesResource :: QUERY by " + principalName + " : Unable to query available " +
                    "subject attribute names.");
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR));
            return;
        }

        if (attributes.size() > 0) {
            for (String attr : attributes) {
                handler.handleResource(new Resource(attr,
                    Long.toString(System.currentTimeMillis()), JsonValue.json(attr)));
            }
        }

        handler.handleResult(new QueryResult(null, 0));
    }

    @Override
    public void readInstance(ServerContext context, String resourceId, ReadRequest request, ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request, ResultHandler<Resource> handler) {
        RestUtils.generateUnsupportedOperation(handler);
    }

    SubjectAttributesManager getSubjectAttributesManager(Subject mySubject, String realm) {
        return SubjectAttributesManager.getInstance(mySubject, realm);
    }

}
