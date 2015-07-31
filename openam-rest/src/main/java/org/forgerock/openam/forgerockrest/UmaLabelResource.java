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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.forgerockrest;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import com.sun.identity.common.ISLocaleContext;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
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
import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.openam.oauth2.resources.labels.LabelType;
import org.forgerock.openam.oauth2.resources.labels.ResourceSetLabel;
import org.forgerock.openam.oauth2.resources.labels.UmaLabelsStore;
import org.forgerock.openam.rest.resource.ContextHelper;

/**
 * A collection provider for UMA Labels.
 * @Since 13.0.0
 */
public class UmaLabelResource implements CollectionResourceProvider {

    private static final Debug debug = Debug.getInstance("umaLabel");
    private static final String TYPE_LABEL = "type";
    private static final String NAME_LABEL = "name";
    private final UmaLabelsStore labelStore;
    private final ContextHelper contextHelper;
    private final ClientRegistrationStore clientRegistrationStore;

    @Inject
    public UmaLabelResource(UmaLabelsStore labelStore, ContextHelper contextHelper,
            ClientRegistrationStore clientRegistrationStore) {
        this.labelStore = labelStore;
        this.contextHelper = contextHelper;
        this.clientRegistrationStore = clientRegistrationStore;
    }

    @Override
    public void actionCollection(ServerContext serverContext, ActionRequest actionRequest, ResultHandler<JsonValue> resultHandler) {
        resultHandler.handleError(new NotSupportedException("Not supported."));
    }

    @Override
    public void actionInstance(ServerContext serverContext, String s, ActionRequest actionRequest, ResultHandler<JsonValue> resultHandler) {
        resultHandler.handleError(new NotSupportedException("Not supported."));
    }

    @Override
    public void createInstance(ServerContext serverContext, CreateRequest createRequest, ResultHandler<Resource> resultHandler) {
        final JsonValue umaLabel = createRequest.getContent();

        try {
            validate(umaLabel);
        } catch (BadRequestException e) {
            resultHandler.handleError(e);
            return;
        }

        final String realm = getRealm(serverContext);
        final String userName = getUserName(serverContext);
        final String labelName = umaLabel.get(NAME_LABEL).asString();
        final String labelType = umaLabel.get(TYPE_LABEL).asString();
        final ResourceSetLabel label;

        try {
            label = labelStore.create(realm, userName, new ResourceSetLabel("", labelName, LabelType.valueOf(labelType), Collections.EMPTY_SET));
            resultHandler.handleResult(new Resource(label.getId(), String.valueOf(label.hashCode()), label.asJson()));
        } catch (ResourceException e) {
            resultHandler.handleError(new BadRequestException("Error creating label"));
        }
    }

    private void validate(JsonValue umaLabel) throws BadRequestException {
        try {
            umaLabel.get(TYPE_LABEL).required();
            umaLabel.get(TYPE_LABEL).asEnum(LabelType.class);
            umaLabel.get(NAME_LABEL).required();
        } catch (JsonValueException e) {
            debug.error("Invalid Json - " + e.getMessage());
            throw new BadRequestException("Invalid Json - " + e.getMessage());
        }
    }

    @Override
    public void deleteInstance(ServerContext serverContext, String labelId, DeleteRequest deleteRequest, ResultHandler<Resource> resultHandler) {
        try {
            ResourceSetLabel resourceSetLabel = labelStore.read(getRealm(serverContext), getUserName(serverContext), labelId);

            if (!isSameRevision(deleteRequest, resourceSetLabel)) {
                throw new BadRequestException("Revision number doesn't match latest revision.");
            }

            labelStore.delete(getRealm(serverContext), getUserName(serverContext), labelId);
            resultHandler.handleResult(new Resource(labelId, null, resourceSetLabel.asJson()));
        } catch (ResourceException e) {
            resultHandler.handleError(new BadRequestException("Error deleting label."));
        }
    }

    private boolean isSameRevision(DeleteRequest deleteRequest, ResourceSetLabel resourceSetLabel) {
        return deleteRequest.getRevision().equals(String.valueOf(resourceSetLabel.hashCode()));
    }

    @Override
    public void patchInstance(ServerContext serverContext, String s, PatchRequest patchRequest, ResultHandler<Resource> resultHandler) {
        resultHandler.handleError(new NotSupportedException("Not supported."));
    }

    @Override
    public void queryCollection(ServerContext serverContext, QueryRequest queryRequest, QueryResultHandler queryResultHandler) {
        if (!queryRequest.getQueryFilter().toString().equals("true")) {
            queryResultHandler.handleError(new BadRequestException("Invalid query"));
            return;
        }

        Set<ResourceSetLabel> labels;
        try {
            labels = labelStore.list(getRealm(serverContext), getUserName(serverContext));
        } catch (ResourceException e) {
            queryResultHandler.handleError(new BadRequestException("Error retrieving labels."));
            return;
        }

        ISLocaleContext localeContext = new ISLocaleContext();
        localeContext.setLocale(serverContext.asContext(HttpContext.class));
        for (ResourceSetLabel label : labels) {
            try {
                label = resolveLabelName(contextHelper.getRealm(serverContext), label, localeContext);
            } catch (InternalServerErrorException e) {
                debug.error("Could not resolve Resource Server label name. id: {}, name: {}", label.getId(),
                        label.getName(), e);
            }
            queryResultHandler.handleResource(new Resource(label.getId(),
                    String.valueOf(label.asJson().getObject().hashCode()), label.asJson()));
        }

        queryResultHandler.handleResult(new QueryResult());
    }

    private ResourceSetLabel resolveLabelName(String realm, ResourceSetLabel label, ISLocaleContext localeContext)
            throws InternalServerErrorException {
        if (label.getId().endsWith("/" + label.getName())) {
            String resourceServerId = label.getId().substring(0, label.getId().lastIndexOf("/"));
            String resourceServerName = resolveResourceServerName(resourceServerId, realm, localeContext);
            if (resourceServerName != null) {
                label.setName(resourceServerName + "/" + label.getName());
            }
        }
        return label;
    }

    private String resolveResourceServerName(String resourceServerId, final String realm, ISLocaleContext localeContext)
            throws InternalServerErrorException {
        try {
            ClientRegistration clientRegistration = clientRegistrationStore.get(resourceServerId, new OAuth2Request() {
                @Override
                public <T> T getRequest() {
                    throw new UnsupportedOperationException("Realm parameter only OAuth2Request");
                }

                @Override
                public <T> T getParameter(String name) {
                    if (OAuth2Constants.Custom.REALM.equals(name)) {
                        return (T) realm;
                    }
                    throw new UnsupportedOperationException("Realm parameter only OAuth2Request");
                }

                @Override
                public JsonValue getBody() {
                    return null;
                }

                @Override
                public Locale getLocale() {
                    return null;
                }
            });
            return clientRegistration.getDisplayName(localeContext.getLocale());
        } catch (InvalidClientException | NotFoundException e) {
            throw new InternalServerErrorException("Could not resolve Resource Server label name", e);
        }
    }

    @Override
    public void readInstance(ServerContext serverContext, String s, ReadRequest readRequest, ResultHandler<Resource> resultHandler) {
        resultHandler.handleError(new NotSupportedException("Not supported."));
    }

    @Override
    public void updateInstance(ServerContext serverContext, String s, UpdateRequest updateRequest, ResultHandler<Resource> resultHandler) {
        resultHandler.handleError(new NotSupportedException("Not supported."));
    }

    private String getRealm(ServerContext context) {
        return contextHelper.getRealm(context);
    }

    private String getUserName(ServerContext context) {
        return contextHelper.getUserId(context);
    }
}
