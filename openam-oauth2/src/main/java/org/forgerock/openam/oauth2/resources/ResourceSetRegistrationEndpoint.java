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
 * Portions Copyrighted 2025 3A Systems, LLC.
 */

package org.forgerock.openam.oauth2.resources;

import static java.util.Collections.emptyList;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.oauth2.ResourceSetDescription;
import org.forgerock.oauth2.resources.ResourceSetStore;
import org.forgerock.oauth2.restlet.ExceptionHandler;
import org.forgerock.oauth2.restlet.resources.ResourceSetDescriptionValidator;
import org.forgerock.oauth2.restlet.resources.ResourceSetRegistrationHook;
import org.forgerock.openam.cts.api.fields.ResourceSetTokenField;
import org.forgerock.openam.oauth2.extensions.ExtensionFilterManager;
import org.forgerock.openam.oauth2.extensions.ResourceRegistrationFilter;
import org.forgerock.openam.oauth2.resources.labels.ResourceSetLabel;
import org.forgerock.openam.oauth2.resources.labels.UmaLabelsStore;
import org.forgerock.openam.rest.representations.JacksonRepresentationFactory;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.util.query.QueryFilter;
import org.json.JSONException;
import org.restlet.Request;
import org.restlet.data.Status;
import org.restlet.data.Tag;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

/**
 * Restlet endpoint for OAuth2 resource servers to register resource set that should be protected.
 *
 * @link https://tools.ietf.org/html/draft-hardjono-oauth-resource-reg-04
 * @since 13.0.0
 */
public class ResourceSetRegistrationEndpoint extends ServerResource {

    private static final String RESOURCE_SET_ID_KEY = "rsid";
    private static final String ID_FIELD = "_id";
    private static final String POLICY_URI_FIELD = "user_access_policy_uri";

    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final ResourceSetDescriptionValidator validator;
    private final OAuth2RequestFactory requestFactory;
    private final Set<ResourceSetRegistrationHook> hooks;
    private final ResourceSetLabelRegistration labelRegistration;
    private final ExtensionFilterManager extensionFilterManager;
    private final ExceptionHandler exceptionHandler;
    private final UmaLabelsStore umaLabelsStore;
    private final JacksonRepresentationFactory jacksonRepresentationFactory;

    /**
     * Construct a new ResourceSetRegistrationEndpoint instance.
     * @param providerSettingsFactory An instance of the {@link OAuth2ProviderSettingsFactory}.
     * @param validator An instance of the {@link ResourceSetDescriptionValidator}.
     * @param requestFactory An instance of the OAuth2RequestFactory.
     * @param hooks A {@code Set} of {@code ResourceSetRegistrationHook}s.
     * @param labelRegistration An instance of the {@code ResourceSetLabelRegistration}.
     * @param extensionFilterManager An instance of the {@code ExtensionFilterManager}.
     * @param exceptionHandler An instance of the {@code ExceptionHandler}.
     * @param umaLabelsStore An instance of the Uma Label Store
     * @param jacksonRepresentationFactory The factory for {@code JacksonRepresentation} instances.
     */
    @Inject
    public ResourceSetRegistrationEndpoint(OAuth2ProviderSettingsFactory providerSettingsFactory,
            ResourceSetDescriptionValidator validator, OAuth2RequestFactory requestFactory,
            Set<ResourceSetRegistrationHook> hooks, ResourceSetLabelRegistration labelRegistration,
            ExtensionFilterManager extensionFilterManager, ExceptionHandler exceptionHandler,
            UmaLabelsStore umaLabelsStore, JacksonRepresentationFactory jacksonRepresentationFactory) {
        this.providerSettingsFactory = providerSettingsFactory;
        this.validator = validator;
        this.requestFactory = requestFactory;
        this.hooks = hooks;
        this.labelRegistration = labelRegistration;
        this.extensionFilterManager = extensionFilterManager;
        this.exceptionHandler = exceptionHandler;
        this.umaLabelsStore = umaLabelsStore;
        this.jacksonRepresentationFactory = jacksonRepresentationFactory;
    }

    /**
     * <p>Creates or updates a resource set description.</p>
     *
     * <p>If the request contains a If-Match header an update is performed, otherwise a create is performed.</p>
     *
     * <p>An update will replace the current description of the resource set with the contents of the request body.</p>
     *
     * @param entity The new resource set description.
     * @return A JSON object containing the authorization server's unique id for the resource set and, optionally,
     * a policy uri.
     * @throws NotFoundException If the requested resource set description does not exist.
     * @throws ServerException When an error occurs during creating or updating.
     * @throws BadRequestException If the request JSON is invalid.
     */

    @Post
    public Representation createResourceSet(JsonRepresentation entity) throws NotFoundException, ServerException,
            BadRequestException {
        ResourceSetDescription resourceSetDescription = new ResourceSetDescription(null, getClientId(),
                getResourceOwnerId(), validator.validate(toMap(entity)));
        OAuth2Request oAuth2Request = requestFactory.create(getRequest());
        ResourceSetStore store = providerSettingsFactory.get(oAuth2Request).getResourceSetStore();

        QueryFilter<String> query = QueryFilter.and(
                QueryFilter.equalTo(ResourceSetTokenField.NAME, resourceSetDescription.getName()),
                QueryFilter.equalTo(ResourceSetTokenField.CLIENT_ID, getClientId()),
                QueryFilter.equalTo(ResourceSetTokenField.RESOURCE_OWNER_ID, getResourceOwnerId()));

        if (!store.query(query).isEmpty()) {
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            Map<String, Object> response = new HashMap<String, Object>();
            response.put(OAuth2Constants.Params.ERROR, Status.CLIENT_ERROR_BAD_REQUEST.getReasonPhrase());
            response.put(OAuth2Constants.Params.ERROR_DESCRIPTION, "A shared item with the name '" +
                    resourceSetDescription.getName() + "' already exists");
            return new JsonRepresentation(response);
        }

        JsonValue labels = resourceSetDescription.getDescription().get(OAuth2Constants.ResourceSets.LABELS);
        resourceSetDescription.getDescription().remove(OAuth2Constants.ResourceSets.LABELS);
        for (ResourceRegistrationFilter filter : extensionFilterManager.getFilters(ResourceRegistrationFilter.class)) {
            filter.beforeResourceRegistration(resourceSetDescription);
        }
        store.create(oAuth2Request, resourceSetDescription);
        if (labels.isNotNull()) {
            resourceSetDescription.getDescription().add(OAuth2Constants.ResourceSets.LABELS, labels.getObject());
        } else {
            resourceSetDescription.getDescription().add(OAuth2Constants.ResourceSets.LABELS, emptyList());
        }
        labelRegistration.updateLabelsForNewResourceSet(resourceSetDescription);
        for (ResourceRegistrationFilter filter : extensionFilterManager.getFilters(ResourceRegistrationFilter.class)) {
            filter.afterResourceRegistration(resourceSetDescription);
        }



        for (ResourceSetRegistrationHook hook : hooks) {
            hook.resourceSetCreated(oAuth2Request.<String>getParameter("realm"), resourceSetDescription);
        }
        getResponse().setStatus(Status.SUCCESS_CREATED);

        return createJsonResponse(resourceSetDescription, false, true);
    }

    @Put
    public Representation updateResourceSet(JsonRepresentation entity) throws NotFoundException,
            ServerException, BadRequestException {

        if (!isConditionalRequest()) {
            throw new ResourceException(512, "precondition_failed", "Require If-Match header to update Resource Set",
                    null);
        }

        final Map<String, Object> resourceSetDescriptionAttributes = validator.validate(toMap(entity));
        final String resourceSetId = getResourceSetId();

        ResourceSetStore store = providerSettingsFactory.get(requestFactory.create(getRequest())).getResourceSetStore();
        ResourceSetDescription resourceSetDescription = store.read(resourceSetId, getResourceOwnerId())
                .update(resourceSetDescriptionAttributes);

        JsonValue labels = resourceSetDescription.getDescription().get(OAuth2Constants.ResourceSets.LABELS);
        resourceSetDescription.getDescription().remove(OAuth2Constants.ResourceSets.LABELS);
        store.update(resourceSetDescription);
        if (labels.isNotNull()) {
            resourceSetDescription.getDescription().add(OAuth2Constants.ResourceSets.LABELS, labels.getObject());
        } else {
            resourceSetDescription.getDescription().add(OAuth2Constants.ResourceSets.LABELS, emptyList());
        }
        labelRegistration.updateLabelsForExistingResourceSet(resourceSetDescription);

        return createJsonResponse(resourceSetDescription, false, true);
    }

    /**
     * Reads the requested resource set description or queries all the client's (resource server's) resource sets.
     *
     * @return When reading returns a JSON object of the authorization server's unique id and the description of the
     * resource set. When querying returns a JSON array of the resource set ids.
     * @throws NotFoundException If the requested resource set description does not exist.
     * @throws ServerException When the resource set description cannot be loaded.
     */
    @Get
    public Representation readOrListResourceSet() throws NotFoundException, ServerException {
        String resourceSetId = getResourceSetId();
        if (resourceSetId == null || resourceSetId.isEmpty()) {
            return listResourceSets();
        } else {
            return readResourceSet(resourceSetId);
        }
    }

    private Representation readResourceSet(String resourceSetId) throws NotFoundException, ServerException {
        ResourceSetStore store = providerSettingsFactory.get(requestFactory.create(getRequest())).getResourceSetStore();
        ResourceSetDescription resourceSetDescription = store.read(resourceSetId, getResourceOwnerId());

        List<String> labels = new ArrayList<>();
        try {
            Set<ResourceSetLabel> labelSet = umaLabelsStore.forResourceSet(resourceSetDescription.getRealm(),
                    resourceSetDescription.getResourceOwnerId(), resourceSetDescription.getId(), false);
            for (ResourceSetLabel label : labelSet) {
                labels.add(label.getName());
            }
        } catch (org.forgerock.json.resource.ResourceException e) {
            throw new ServerException(e);
        }
        resourceSetDescription.getDescription().put("labels", labels);
        return createJsonResponse(resourceSetDescription, true, true);
    }

    private Representation listResourceSets() throws ServerException, NotFoundException {
        ResourceSetStore store = providerSettingsFactory.get(requestFactory.create(getRequest())).getResourceSetStore();
        QueryFilter<String> query = QueryFilter.and(
                QueryFilter.equalTo(ResourceSetTokenField.CLIENT_ID, getClientId()),
                QueryFilter.equalTo(ResourceSetTokenField.RESOURCE_OWNER_ID, getResourceOwnerId()));
        Set<ResourceSetDescription> resourceSetDescriptions = store.query(query);

        Set<String> resourceSetIds = new HashSet<String>();

        for (ResourceSetDescription resourceSetDescription : resourceSetDescriptions) {
            resourceSetIds.add(resourceSetDescription.getId());
        }

        return jacksonRepresentationFactory.create(resourceSetIds);
    }

    /**
     * <p>Deletes the resource set description for the request resource set id as long as the If-Match header matches
     * the current version of the resource set.</p>
     *
     * <p>If no If-Match header is present on the request a 512 Precondition Failed response will be returned.</p>
     *
     * @return An empty representation.
     * @throws NotFoundException If the requested resource set description does not exist.
     * @throws ServerException When an error occurs during removal.
     */
    @Delete
    public Representation deleteResourceSet() throws NotFoundException, ServerException {

        if (!isConditionalRequest()) {
            throw new ResourceException(512, "precondition_failed", "Require If-Match header to delete Resource Set",
                    null);
        }

        ResourceSetStore store = providerSettingsFactory.get(requestFactory.create(getRequest())).getResourceSetStore();
        ResourceSetDescription resourceSetDescription = store.read(getResourceSetId(), getResourceOwnerId());
        OAuth2Request oAuth2Request = requestFactory.create(getRequest());

        for (ResourceSetRegistrationHook hook : hooks) {
            hook.resourceSetDeleted(oAuth2Request.<String>getParameter("realm"), resourceSetDescription);
        }
        labelRegistration.updateLabelsForDeletedResourceSet(resourceSetDescription);
        store.delete(getResourceSetId(), getResourceOwnerId());

        return createEmptyResponse();
    }

    @Override
    protected void doCatch(Throwable throwable) {
        exceptionHandler.handle(throwable, getResponse());
    }

    private boolean isConditionalRequest() {
        return !getConditions().getMatch().isEmpty();
    }

    private String getResourceSetId() {
        return (String) getRequestAttributes().get(RESOURCE_SET_ID_KEY);
    }

    private String getClientId() {
        return requestFactory.create(getRequest()).getToken(AccessToken.class).getClientId();
    }

    private String getResourceOwnerId() {
        return requestFactory.create(getRequest()).getToken(AccessToken.class).getResourceOwnerId();
    }

    private Representation createJsonResponse(ResourceSetDescription resourceSetDescription, boolean includeResourceSet,
            boolean withPolicyUri) {
        Map<String, Object> response = new HashMap<String, Object>();
        if (includeResourceSet) {
            response = new HashMap<String, Object>(resourceSetDescription.asMap());
        }
        response.put(ID_FIELD, resourceSetDescription.getId());
        if (withPolicyUri && resourceSetDescription.getPolicyUri() != null) {
            response.put(POLICY_URI_FIELD, resourceSetDescription.getPolicyUri());
        }
        Representation representation = jacksonRepresentationFactory.create(response);
        representation.setTag(generateETag(resourceSetDescription));
        return representation;
    }

    private Representation createEmptyResponse() {
        Representation representation = new EmptyRepresentation();
        getResponse().setStatus(new Status(204));
        return representation;
    }

    private Tag generateETag(ResourceSetDescription resourceSetDescription) {
        int hashCode = resourceSetDescription.hashCode();
        JsonValue description = resourceSetDescription.getDescription();
        if(!description.isDefined(OAuth2Constants.ResourceSets.LABELS)) {
            description.put(OAuth2Constants.ResourceSets.LABELS, null);
            hashCode = resourceSetDescription.hashCode();
            description.remove(OAuth2Constants.ResourceSets.LABELS);
        }
        return new Tag(Integer.toString(hashCode), true);
    }

    private Map<String, Object> toMap(JsonRepresentation entity) throws BadRequestException {
        if (entity == null) {
            return Collections.emptyMap();
        }

        try {
            final String jsonString = entity.getJsonObject().toString();
            if (StringUtils.isNotEmpty(jsonString)) {
                JsonValue jsonContent = JsonValueBuilder.toJsonValue(jsonString);
                return jsonContent.asMap(Object.class);
            }

            return Collections.emptyMap();
        } catch (JSONException e) {
            throw new BadRequestException(e.getMessage());
        }
    }
}
