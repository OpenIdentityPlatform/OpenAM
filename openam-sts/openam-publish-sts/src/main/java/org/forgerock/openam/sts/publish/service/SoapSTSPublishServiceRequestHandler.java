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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.publish.service;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.forgerock.services.context.Context;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.router.RestRealmValidator;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.InstanceConfigMarshaller;
import org.forgerock.openam.sts.STSPublishException;
import org.forgerock.openam.sts.publish.soap.SoapSTSInstancePublisher;
import org.forgerock.openam.sts.soap.config.user.SoapSTSInstanceConfig;
import org.forgerock.openam.utils.JsonObject;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;
import org.slf4j.Logger;

/**
 * A custom RequestHandler to allow the Soap STS publish service to act like a CollectionResourceProvider, while still
 * handling the routing of urls identifying soap sts instances, which are identified by their deployment url (relative
 * to the servlet root of the soap-sts endpoint), deployment urls which can include realms, and thus '/' characters.
 * These characters are not handled as resource names by the CollectionResourceProvider, which is necessary for the
 * soap sts publish service.
 */
class SoapSTSPublishServiceRequestHandler implements RequestHandler {
    private static final String PUBLISHED_INSTANCES = "published_instances";
    private static final String RESULT = "result";
    private static final String SUCCESS = "success";
    private static final String EMPTY_STRING = "";
    private static final String REALM = "realm";

    private final SoapSTSInstancePublisher publisher;
    private final RestRealmValidator realmValidator;
    private final InstanceConfigMarshaller<SoapSTSInstanceConfig> instanceConfigMarshaller;

    private final Logger logger;

    /*
    No Injection, as ctor called from the STSPublishServiceConnectionFactory (params obtained from SoapSTSPublishInjectorHolder).
     */
    SoapSTSPublishServiceRequestHandler(SoapSTSInstancePublisher publisher, RestRealmValidator realmValidator,
                                        InstanceConfigMarshaller<SoapSTSInstanceConfig> instanceConfigMarshaller,
                                        Logger logger) {
        this.publisher = publisher;
        this.realmValidator = realmValidator;
        this.instanceConfigMarshaller = instanceConfigMarshaller;
        this.logger = logger;
    }

    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        return new NotSupportedException().asPromise();
    }

    /*
     This method will be invoked by either a programmatic client, in which case a SoapSTSInstanceConfig has emitted
     properly-formatted json, or from the SoapSTSAddViewBean, in which case the configuration state is
     in the sms-centric Map<String, Set<String>> format. This method needs to be able to handle both invocation types,
     and marshal the invocation state in to a SoapSTSInstanceConfig instance either way. It also needs to return an accurate
     error message, so that in the case of SoapSecurityTokenServiceViewBean invocation, the user can make appropriate
      corrections to the configuration state.
      */
    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
        final SoapSTSInstanceConfig instanceConfig;
        try {
            instanceConfig = marshalInstanceConfigFromInvocation(request.getContent());
        } catch (BadRequestException e) {
            return e.asPromise();
        }
        if (!realmValidator.isRealm(instanceConfig.getDeploymentConfig().getRealm())) {
            logger.warn("Publish of Soap STS instance " + instanceConfig.getDeploymentSubPath() + " to realm "
                    + instanceConfig.getDeploymentConfig().getRealm() + " rejected because realm does not exist.");
            return new NotFoundException("The specified realm does not exist.").asPromise();
        }
        try {
            return newResultPromise(publishInstance(instanceConfig));
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
        String stsId = request.getResourcePath();
        String realm = getRealmFromResourceName(request.getResourcePath());
        /*
        Don't reject invocation for specious realm here. It is possible that a user deletes a realm, and then
        re-creates it, and wants to re-publish a soap-sts instance with the same id. Not rejecting this invocation
        allows the SoapSTSInstancePublisherImpl to purge its Route cache referencing previously published instances.
        And a specious delete that is not occurring in the context of this scenario will result in a 404 exception.
         */
        try {
            publisher.removeInstance(stsId, realm);
            if (logger.isDebugEnabled()) {
                logger.debug("soap sts instance " + stsId + " successfully removed from realm " + realm);
            }
            return newResultPromise(newResourceResponse(stsId, stsId, json(object(field
                    (RESULT, "soap sts instance " + stsId + " successfully removed from realm " + realm)))));
        } catch (STSPublishException e) {
            String message = "Exception caught removing instance: " + stsId + " from realm " + realm + ". Exception:" + e;
            logger.error(message, e);
            return e.asPromise();
        } catch (Exception e) {
            String message = "Exception caught removing instance: " + stsId + " from realm " + realm + ". Exception:" + e;
            logger.error(message, e);
            return new InternalServerErrorException(message, e).asPromise();
        }
    }

    public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
        return new NotSupportedException().asPromise();
    }

    public Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        QueryFilter<JsonPointer> queryFilter = request.getQueryFilter();
        if (queryFilter == null) {
            return new BadRequestException(getQueryUsageString()).asPromise();
        }
        String realm;
        try {
            realm = getRealmFromQueryFilter(queryFilter);
        } catch (STSPublishException e) {
            return e.asPromise();
        }
        try {
            if (!realmValidator.isRealm(realm)) {
                return new BadRequestException("The specified realm does not exist.").asPromise();
            }
            final List<SoapSTSInstanceConfig> publishedInstances = publisher.getPublishedInstances(realm);
            for (SoapSTSInstanceConfig instanceConfig : publishedInstances) {
                /*
                Although instanceConfig.toJson() will yield the JsonValue which the handleResource invocation requires,
                the SoapSTSInstanceConfig is a complicated nesting of JsonValue objects, which should be 'homogenized'
                into a json format prior to inclusion in the response.
                 */
                handler.handleResource(newResourceResponse(instanceConfig.getDeploymentSubPath(), getInstanceConfigEtag(instanceConfig),
                        new JsonValue(mapStringToJson(instanceConfig.toJson().toString()))));
            }
            return newResultPromise(newQueryResponse());
        } catch (STSPublishException e) {
            logger.error("Exception caught obtaining soap sts instances for realm " + (realm != null ? realm : "null realm" ) + "; Exception: " + e);
            return e.asPromise();
        }
    }

    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        try {
            if (EMPTY_STRING.equals(request.getResourcePath())) {
                List<SoapSTSInstanceConfig> publishedInstances = publisher.getPublishedInstances();
                JsonObject jsonObject = JsonValueBuilder.jsonValue();
                for (SoapSTSInstanceConfig instanceConfig : publishedInstances) {
                    jsonObject.put(instanceConfig.getDeploymentSubPath(), mapStringToJson(instanceConfig.toJson().toString()));
                }
                /*
                Note that the revision etag is not set, as this is not a resource which should really be cached.
                If caching becomes necessary, a string composed of the hash codes of each of the SoapSTSInstanceConfig
                instances could be used (or a hash of that string).
                 */
                return newResultPromise(newResourceResponse(PUBLISHED_INSTANCES, EMPTY_STRING, jsonObject.build()));
            } else {
                final String realm = getRealmFromResourceName(request.getResourcePath());
                if (!realmValidator.isRealm(realm)) {
                    logger.warn("Read of soap STS instance state for instance " + request.getResourcePath() +
                            " in realm " + realm + " rejected because realm does not exist");
                    return new NotFoundException("The specified realm does not exist.").asPromise();
                }
                SoapSTSInstanceConfig instanceConfig =
                        publisher.getPublishedInstance(request.getResourcePath(), realm);
                return newResultPromise(
                        newResourceResponse(
                                instanceConfig.getDeploymentSubPath(),
                                getInstanceConfigEtag(instanceConfig),
                                JsonValueBuilder.jsonValue().put(instanceConfig.getDeploymentSubPath(), mapStringToJson(instanceConfig.toJson().toString())).build()));
            }
        } catch (STSPublishException e) {
            String message = "Exception caught obtaining soap sts instance corresponding to id: " +
                    request.getResourcePath() + "; Exception: " + e;
            logger.error(message, e);
            return e.asPromise();
        }
    }

    private String getInstanceConfigEtag(SoapSTSInstanceConfig instanceConfig) {
        return Integer.toString(instanceConfig.hashCode());
    }

    /*
    A sub-optimal bit of business: I want to return json corresponding to published rest-sts instances from the GET. Published
    rest-sts state is represented as instances of the RestSTSInstanceConfig class, which can emit a JsonValue corresponding to
    encapsulated config by calling toJson. Unfortunately, it was not clear that to obtain the json representation of a JsonValue
    requires calling getObject, and that this invocation will only return the top-level object encapsulated in JsonValue. However, if
    the values in this top-level object (e.g. a Map) are also JsonValue objects, then calling toString on the Map will end up
    calling toString on the encapsulated JsonValue instances, which will include information about the json pointer, transformers, etc.
    Because RestSTSInstanceConfig encapsulates other config objects (SAML2Config, OpenIdConnectTokenConfig, ...), each of which
    will return a JsonValue implementation from their toJson methods, and because the RestSTSInstanceConfig delegates to these
    instances to obtain the complete configuration state, calling getObject on the top-level JsonValue returned by RestSTSInstanceConfig
    will not result in proper json, again because some of the values encapsulated in this map are JsonValue objects.

    The proper solution would be to change the signature of all of the toJson methods in the config hierarchy to return and Object
    instance obtained from calling getObject on the generated JsonValue instance, but this is deemed too risky to undertake now.

    So the interim solution is to marshal the json string back to a Map using Jackson.
     */
    private Map mapStringToJson(String jsonValueString) throws STSPublishException {
        try {
            return JsonValueBuilder.getObjectMapper().readValue(jsonValueString, Map.class);
        } catch (IOException e) {
            throw new STSPublishException(ResourceException.INTERNAL_ERROR, "Exception caught mapping String to json: " + e.getMessage(), e);
        }
    }


    /*
     * A PUT to the url composed of the publish endpont + the sts instance id with a payload corresponding to a
     * SoapSTSInstanceId (wrapped in invocation context information) will result in republishing the existing instance
     * (which is a delete followed by a create).
     */
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
        String stsId = request.getResourcePath();
        String realm = getRealmFromResourceName(request.getResourcePath());
        if (!realmValidator.isRealm(realm)) {
            logger.warn("Update of soap STS instance state for instance " + stsId +
                    " in realm " + realm + " rejected because realm does not exist");
            return new NotFoundException("The specified realm does not exist.").asPromise();
        }
        /*
        Insure that the instance is published before performing an update.
         */
        final boolean publishedToSMS;
        try {
            publishedToSMS = (publisher.getPublishedInstance(stsId, realm) != null);
        } catch (STSPublishException e) {
            logger.error("In SoapSTSPublishServiceRequestHandler#handleUpdate, exception caught determining whether " +
                    "instance persisted in SMS. Instance not updated. Exception: " + e, e);
            return e.asPromise();
        }

        if (publishedToSMS) {
            SoapSTSInstanceConfig instanceConfig;
            try {
                instanceConfig = marshalInstanceConfigFromInvocation(request.getContent());
            } catch (BadRequestException e) {
                logger.error("In SoapSTSPublishServiceRequestHandler#handleUpdate, exception caught marshalling " +
                        "invocation state to SoapSTSInstanceConfig. Instance not updated. The state: "
                        + request.getContent() + "Exception: " + e, e);
                return e.asPromise();
            }
            try {
                publisher.removeInstance(stsId, realm);
            } catch (STSPublishException e) {
                logger.error("In SoapSTSPublishServiceRequestHandler#handleUpdate, exception caught removing " +
                        "soap sts instance " + instanceConfig.getDeploymentSubPath() + ". This means instance is" +
                        "in indeterminate state, and has not been updated. The instance config: " + instanceConfig
                        + "; Exception: " + e, e);
                return e.asPromise();
            }
            try {
                ResourceResponse response = publishInstance(instanceConfig);
                logger.info("Soap STS instance " + instanceConfig.getDeploymentSubPath() + " updated to state " +
                        instanceConfig.toJson());
                return newResultPromise(response);
            } catch (ResourceException e) {
                logger.error("In SoapSTSPublishServiceRequestHandler#handleUpdate, exception caught publishing " +
                        "soap sts instance " + instanceConfig.getDeploymentSubPath() + ". This means instance is" +
                        "in indeterminate state, having been removed, but not successfully published with updated " +
                        "state. The instance config: " + instanceConfig + "; Exception: " + e, e);
                return e.asPromise();
            }
        } else {
            //404 - realm and id not found in SMS
            return new NotFoundException("No soap sts instance with id " + stsId + " in realm " + realm).asPromise();
        }
    }

    private String getRealmFromResourceName(String resourceName) {
        if (resourceName.lastIndexOf(AMSTSConstants.FORWARD_SLASH) == -1) {
            return AMSTSConstants.FORWARD_SLASH;
        }
        return resourceName.substring(0, resourceName.lastIndexOf(AMSTSConstants.FORWARD_SLASH));
    }

    private SoapSTSInstanceConfig marshalInstanceConfigFromInvocation(JsonValue requestContent) throws BadRequestException {
        /*
        I want to distinguish the case where this method is invoked with a payload generated via a toJson()
        invocation on a SoapSTSInstanceConfig instance, and where this method is invoked with a payload generated by
        the SoapSecurityTokenServiceViewBean (i.e. a Map<String, List<String>>) so that the correct un-marshaling logic
        can be invoked, and the correct error messages displayed. The two cases will be distinguished by distinct values
        corresponding to the AMSTSContants.STS_PUBLISH_INVOCATION_CONTEXT string in the top-level json object.
         */
        String invocationContext = requestContent.get(AMSTSConstants.STS_PUBLISH_INVOCATION_CONTEXT).asString();
        if (AMSTSConstants.STS_PUBLISH_INVOCATION_CONTEXT_CLIENT_SDK.equals(invocationContext)) {
            try {
                return instanceConfigMarshaller.fromJson(requestContent.get(AMSTSConstants.STS_PUBLISH_INSTANCE_STATE));
            } catch (Exception e) {
                logger.error("Exception caught marshalling json into SoapSTSInstanceConfig instance for SDK invocation " +
                        "context: " + e, e);
                throw new BadRequestException(e);
            }
        } else if (AMSTSConstants.STS_PUBLISH_INVOCATION_CONTEXT_VIEW_BEAN.equals(invocationContext)) {
            try {
                return instanceConfigMarshaller.fromJsonAttributeMap(requestContent.get(
                        AMSTSConstants.STS_PUBLISH_INSTANCE_STATE));
            } catch (Exception e) {
                logger.error("Exception caught marshalling attribute map into SoapSTSInstanceConfig instance for " +
                        "ViewBean invocation context: " + e, e);
                throw new BadRequestException(e);
            }
        } else {
            String message = "The top-level json object must contain a key named "
                    + AMSTSConstants.STS_PUBLISH_INVOCATION_CONTEXT + " and with a value corresponding to either "
                    + AMSTSConstants.STS_PUBLISH_INVOCATION_CONTEXT_CLIENT_SDK + " or "
                    + AMSTSConstants.STS_PUBLISH_INVOCATION_CONTEXT_VIEW_BEAN + ". Actual invocation content: "
                    + requestContent.toString();
            logger.error(message);
            throw new BadRequestException(message);
        }
    }

    private ResourceResponse publishInstance(SoapSTSInstanceConfig instanceConfig) throws ResourceException {
        try {
            final String urlElement =
                    publisher.publishInstance(instanceConfig);
            if (logger.isDebugEnabled()) {
                logger.debug("soap sts instance successfully published at " + urlElement);
            }
            return newResourceResponse(instanceConfig.getDeploymentSubPath(),
                    Integer.toString(instanceConfig.hashCode()), json(object(field(RESULT, SUCCESS),
                            field(AMSTSConstants.SUCCESSFUL_REST_STS_PUBLISH_URL_ELEMENT, urlElement))));
        } catch (STSPublishException e) {
            String message = "Exception caught publishing instance: " + instanceConfig.getDeploymentSubPath() + ". Exception" + e;
            logger.error(message, e);
            throw e;
        } catch (Exception e) {
            String message = "Exception caught publishing instance: " + instanceConfig.getDeploymentSubPath() + ". Exception" + e;
            logger.error(message, e);
            throw new InternalServerErrorException(message, e);
        }
    }

    private String getRealmFromQueryFilter(QueryFilter<JsonPointer> queryFilter) throws STSPublishException {
        try {
            return queryFilter.accept(REALM_QUERY_FILTER_VISITOR, null);
        } catch (IllegalArgumentException e) {
            throw new STSPublishException(ResourceException.BAD_REQUEST, e.getMessage(), e);
        }
    }

    private static final QueryFilterVisitor<String, Void, JsonPointer> REALM_QUERY_FILTER_VISITOR =
        new QueryFilterVisitor<String, Void, JsonPointer>() {
            @Override
            public String visitAndFilter(Void aVoid, List<QueryFilter<JsonPointer>> subFilters) {
                throw new IllegalArgumentException("Querying published soap STS instances via multiple clauses " +
                        "joined by an add relationship unsupported. Usage:" + getQueryUsageString());
            }

            @Override
            public String visitEqualsFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
                final String fieldString = field.toString().substring(1);
                if (REALM.equals(fieldString)) {
                    return (String)valueAssertion;
                } else {
                    throw new IllegalArgumentException("Querying for published soap STS instances on field " + fieldString +
                            " not supported. Usage: " + getQueryUsageString());
                }
            }

            @Override
            public String visitBooleanLiteralFilter(Void aVoid, boolean value) {
                throw new IllegalArgumentException("Querying published soap STS instances via boolean literal " +
                        "unsupported. Usage: " + getQueryUsageString());
            }

            @Override
            public String visitContainsFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
                throw new IllegalArgumentException("Querying published soap STS instances via contains relationship " +
                        "unsupported. Usage: " + getQueryUsageString());
            }

            @Override
            public String visitExtendedMatchFilter(Void aVoid, JsonPointer field, String operator, Object valueAssertion) {
                throw new IllegalArgumentException("Querying published soap STS instances via extended match filter " +
                        "unsupported. Usage: " + getQueryUsageString());
            }

            @Override
            public String visitGreaterThanFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
                throw new IllegalArgumentException("Querying published soap STS instances via greater-than filter " +
                        "unsupported. Usage: " + getQueryUsageString());

            }

            @Override
            public String visitGreaterThanOrEqualToFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
                throw new IllegalArgumentException("Querying published soap STS instances via greater-than-or-equal-to " +
                        "filter unsupported. Usage: " + getQueryUsageString());
            }

            @Override
            public String visitLessThanFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
                throw new IllegalArgumentException("Querying published soap STS instances via less-than filter " +
                        "unsupported. Usage: " + getQueryUsageString());
            }

            @Override
            public String visitLessThanOrEqualToFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
                throw new IllegalArgumentException("Querying published soap STS instances via less-than-or-equal-to filter " +
                        "unsupported. Usage: " + getQueryUsageString());

            }

            @Override
            public String visitNotFilter(Void aVoid, QueryFilter<JsonPointer> subFilter) {
                throw new IllegalArgumentException("Querying published soap STS instances via not filter unsupported. " +
                        "Usage: " + getQueryUsageString());

            }

            @Override
            public String visitOrFilter(Void aVoid, List<QueryFilter<JsonPointer>> subFilters) {
                throw new IllegalArgumentException("Querying published soap STS instances via or filter unsupported. " +
                        "Usage: " + getQueryUsageString());

            }

            @Override
            public String visitPresentFilter(Void aVoid, JsonPointer field) {
                throw new IllegalArgumentException("Querying published soap STS instances via present filter unsupported. " +
                        "Usage: " + getQueryUsageString());

            }

            @Override
            public String visitStartsWithFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
                throw new IllegalArgumentException("Querying published soap STS instances via starts-with filter unsupported. " +
                        "Usage: " + getQueryUsageString());

            }
        };

    private static String getQueryUsageString() {
        return "Url must have a url-encoded query param of format: _queryFilter=/" + REALM +
                " eq \"OpenAM-realm\"";
    }
}
