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
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.forgerock.services.context.Context;
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
import org.forgerock.openam.sts.publish.rest.RestSTSInstancePublisher;
import org.forgerock.openam.sts.rest.RestSTS;
import org.forgerock.openam.sts.rest.config.RestSTSInstanceModule;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;
import org.forgerock.openam.utils.JsonObject;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;

/**
  * A custom RequestHandler to allow the Rest STS publish service to act like a CollectionResourceProvider, while still
  * handling the routing of urls identifying rest sts instances, which are identified by their deployment url (relative
  * to the servlet root of the rest-sts endpoint), deployment urls which can include realms, and thus '/' characters.
  * These characters are not handled as resource names by the CollectionResourceProvider, which is necessary for the
  * rest sts publish service.
 */
class RestSTSPublishServiceRequestHandler implements RequestHandler {
    private static final String PUBLISHED_INSTANCES = "published_instances";
    private static final String RESULT = "result";
    private static final String SUCCESS = "success";
    private static final String EMPTY_STRING = "";

    private final RestSTSInstancePublisher publisher;
    private final RestRealmValidator realmValidator;
     private final InstanceConfigMarshaller<RestSTSInstanceConfig> instanceConfigMarshaller;
     private final Logger logger;

    /*
    No Injection, as ctor called from the STSPublishServiceConnectionFactory (params obtained from RestSTSPublishInjectorHolder).
     */
    RestSTSPublishServiceRequestHandler(RestSTSInstancePublisher publisher, RestRealmValidator realmValidator,
                                        InstanceConfigMarshaller<RestSTSInstanceConfig> instanceConfigMarshaller, Logger logger) {
        this.publisher = publisher;
        this.realmValidator = realmValidator;
        this.instanceConfigMarshaller = instanceConfigMarshaller;
        this.logger = logger;
    }

     public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        return new NotSupportedException().asPromise();
    }

    /*
     This method will be invoked by either a programmatic client, in which case a RestSTSInstanceConfig has emitted
     properly-formatted json, or from the RestSecurityTokenServiceViewBean, in which case the configuration state is
     in the sms-centric Map<String, Set<String>> format. This method needs to be able to handle both invocation types,
     and marshal the invocation state in to a RestSTSInstanceConfig instance either way. It also needs to return an accurate
     error message, so that in the case of RestSecurityTokenServiceViewBean invocation, the user can make appropriate
      corrections to the configuration state.
      */
    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
        final RestSTSInstanceConfig instanceConfig;
        try {
            instanceConfig = marshalInstanceConfigFromInvocation(request.getContent());
        } catch (BadRequestException e) {
            return e.asPromise();
        }
        if (!realmValidator.isRealm(instanceConfig.getDeploymentConfig().getRealm())) {
            logger.warn("Publish of Rest STS instance " + instanceConfig.getDeploymentSubPath() + " to realm "
                    + instanceConfig.getDeploymentConfig().getRealm() + " rejected because realm does not exist.");
            return new NotFoundException("The specified realm does not exist.").asPromise();
        }
        Injector instanceInjector;
        try {
            instanceInjector = createInjector(instanceConfig);
        } catch (ResourceException e) {
            return e.asPromise();
        }
        return publishInstance(instanceConfig, instanceInjector);
    }

    public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
        String stsId = request.getResourcePath();
        String realm = getRealmFromResourceName(request.getResourcePath());
        /*
        Don't reject invocation for specious realm here. It is possible that a user deletes a realm, and then
        re-creates it, and wants to re-publish a rest-sts instance with the same id. Not rejecting this invocation
        allows the RestSTSInstancePublisherImpl to purge its Route cache referencing previously published instances.
        And a specious delete that is not occurring in the context of this scenario will result in a 404 exception.
         */
        try {
            boolean removeOnlyFromRouter = false;
            publisher.removeInstance(stsId, realm, removeOnlyFromRouter);
            if (logger.isDebugEnabled()) {
                logger.debug("rest sts instance " + stsId + " successfully removed from realm " + realm);
            }
            return newResultPromise(newResourceResponse(stsId, stsId, json(object(field
                    (RESULT, "rest sts instance " + stsId + " successfully removed from realm " + realm)))));
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
        return new NotSupportedException().asPromise();
    }

    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        try {
            if (EMPTY_STRING.equals(request.getResourcePath())) {
                List<RestSTSInstanceConfig> publishedInstances = publisher.getPublishedInstances();
                JsonObject jsonObject = JsonValueBuilder.jsonValue();
                for (RestSTSInstanceConfig instanceConfig : publishedInstances) {
                    jsonObject.put(instanceConfig.getDeploymentSubPath(), mapStringToJson(instanceConfig.toJson().toString()));
                }
                /*
                Note that the revision etag is not set, as this is not a resource which should really be cached.
                If caching becomes necessary, a string composed of the hash codes of each of the RestSTSInstanceConfig
                instances could be used (or a hash of that string).
                 */
                return newResultPromise(newResourceResponse(PUBLISHED_INSTANCES, EMPTY_STRING, jsonObject.build()));
            } else {
                final String realm = getRealmFromResourceName(request.getResourcePath());
                if (!realmValidator.isRealm(realm)) {
                    logger.warn("Read of rest STS instance state for instance " + request.getResourcePath() +
                            " in realm " + realm + " rejected because realm does not exist");
                    return new NotFoundException("The specified realm does not exist.").asPromise();
                }
                RestSTSInstanceConfig instanceConfig =
                        publisher.getPublishedInstance(request.getResourcePath(), realm);
                return newResultPromise(newResourceResponse(
                                instanceConfig.getDeploymentSubPath(),
                                Integer.toString(instanceConfig.hashCode()),
                                JsonValueBuilder.jsonValue().put(instanceConfig.getDeploymentSubPath(), mapStringToJson(instanceConfig.toJson().toString())).build()));
            }
        } catch (STSPublishException e) {
            String message = "Exception caught obtaining rest sts instance corresponding to id: " +
                    request.getResourcePath() + "; Exception: " + e;
            logger.error(message, e);
            return e.asPromise();
        }
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
      * RestSTSInstanceId (wrapped in invocation context information) will result in republishing the existing instance
      * (which is a delete followed by a create).
      */
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
        String stsId = request.getResourcePath();
        String realm = getRealmFromResourceName(request.getResourcePath());
        if (!realmValidator.isRealm(realm)) {
            logger.warn("Update of rest STS instance state for instance " + stsId +
                    " in realm " + realm + " rejected because realm does not exist");
            return new NotFoundException("The specified realm does not exist.").asPromise();
        }
        /*
        Insure that the instance is published before performing an update.
         */
        final boolean publishedToSMS;
        try {
            publishedToSMS = publisher.isInstancePersistedInSMS(stsId, realm);
        } catch (STSPublishException e) {
            logger.error("In RestSTSPublishServiceRequestHandler#handleUpdate, exception caught determining whether " +
                    "instance persisted in SMS. Instance not updated. Exception: " + e, e);
            return e.asPromise();
        }
        final boolean publishedToCrest = publisher.isInstanceExposedInCrest(stsId);

        if (publishedToSMS) {
            if (!publishedToCrest) {
                /*
                Entering this branch would seem to be an error condition. It could possibly happen in a site deployment,
                where a rest sts instance is published to a different server than the current server, and the registered
                ServiceListener was not called when the ldap replication created the service entry on the current server.
                I will log a warning, and still publish the instance, just for robustness.
                 */
                logger.warn("The rest sts instance " + stsId + " in realm " + realm + " is present in the SMS, but " +
                        "has not been hung off of the CREST router. This is an illegal state. The instance will be" +
                        " republished.");
            }
            RestSTSInstanceConfig instanceConfig;
            try {
                instanceConfig = marshalInstanceConfigFromInvocation(request.getContent());
            } catch (BadRequestException e) {
                logger.error("In RestSTSPublishServiceRequestHandler#handleUpdate, exception caught marshalling " +
                        "invocation state to RestSTSInstanceConfig. Instance not updated. The state: "
                        + request.getContent() + "Exception: " + e, e);
                return e.asPromise();
            }
            Injector instanceInjector;
            try {
                instanceInjector = createInjector(instanceConfig);
            } catch (ResourceException e) {
                logger.error("In RestSTSPublishServiceRequestHandler#handleUpdate, exception caught creating an " +
                        "Injector using the RestSTSInstanceConfig. The instance: " + instanceConfig.toJson() +
                        "; Exception: " + e, e);
                return e.asPromise();
            }
            try {
                publisher.updateInstanceInSMS(stsId, realm, instanceConfig, instanceInjector.getInstance(RestSTS.class));
                return newResultPromise(newResourceResponse(instanceConfig.getDeploymentSubPath(),
                        Integer.toString(instanceConfig.hashCode()), json(object(field(RESULT, SUCCESS)))));
            } catch (STSPublishException e) {
                logger.error("In RestSTSPublishServiceRequestHandler#handleUpdate, exception caught removing " +
                        "rest sts instance " + instanceConfig.getDeploymentSubPath() + ". This means instance is" +
                        "in indeterminate state, and has not been updated. The instance config: " + instanceConfig
                        + "; Exception: " + e, e);
                return e.asPromise();
            }
        } else {
            //404 - realm and id not found in SMS
            return new NotFoundException("No rest sts instance with id " + stsId + " in realm " + realm).asPromise();
        }
    }

    private String getRealmFromResourceName(String resourceName) {
        if (resourceName.lastIndexOf(AMSTSConstants.FORWARD_SLASH) == -1) {
            return AMSTSConstants.FORWARD_SLASH;
        }
        return resourceName.substring(0, resourceName.lastIndexOf(AMSTSConstants.FORWARD_SLASH));
    }

    private RestSTSInstanceConfig marshalInstanceConfigFromInvocation(JsonValue requestContent) throws BadRequestException {
        /*
        I want to distinguish the case where this method is invoked with a payload generated via a toJson()
        invocation on a RestSTSInstanceConfig instance, and where this method is invoked with a payload generated by
        the RestSecurityTokenServiceViewBean (i.e. a Map<String, Set<String>>) so that the correct un-marshaling logic
        can be invoked, and the correct error messages displayed. The two cases will be distinguished by distinct values
        corresponding to the AMSTSContants.STS_PUBLISH_INVOCATION_CONTEXT string in the top-level json object.
         */
        String invocationContext = requestContent.get(AMSTSConstants.STS_PUBLISH_INVOCATION_CONTEXT).asString();
        if (AMSTSConstants.STS_PUBLISH_INVOCATION_CONTEXT_CLIENT_SDK.equals(invocationContext)) {
            try {
                return  instanceConfigMarshaller.fromJson(requestContent.get(AMSTSConstants.STS_PUBLISH_INSTANCE_STATE));
            } catch (Exception e) {
                logger.error("Exception caught marshalling json into RestSTSInstanceConfig instance for SDK invocation " +
                        "context: " + e, e);
                throw new BadRequestException(e);
            }
        } else if (AMSTSConstants.STS_PUBLISH_INVOCATION_CONTEXT_VIEW_BEAN.equals(invocationContext)) {
            try {
                return instanceConfigMarshaller.fromJsonAttributeMap(requestContent.get(
                        AMSTSConstants.STS_PUBLISH_INSTANCE_STATE));
            } catch (Exception e) {
                logger.error("Exception caught marshalling attribute map into RestSTSInstanceConfig instance for " +
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

    private Injector createInjector(RestSTSInstanceConfig instanceConfig) throws ResourceException {
        try {
            return Guice.createInjector(new RestSTSInstanceModule(instanceConfig));
        } catch (Exception e) {
            String message = "Exception caught creating the guice injector corresponding to rest sts instance: " + e;
            logger.error(message);
            throw new InternalServerErrorException(message, e);
        }
    }

    private Promise<ResourceResponse, ResourceException> publishInstance(RestSTSInstanceConfig instanceConfig,
            Injector instanceInjector) {
        try {
            boolean republish = false;
            final String urlElement =
                    publisher.publishInstance(instanceConfig, instanceInjector.getInstance(RestSTS.class), republish);
            if (logger.isDebugEnabled()) {
                logger.debug("rest sts instance successfully published at " + urlElement);
            }
            return newResultPromise(newResourceResponse(instanceConfig.getDeploymentSubPath(),
                    Integer.toString(instanceConfig.hashCode()), json(object(field(RESULT, SUCCESS),
                            field(AMSTSConstants.SUCCESSFUL_REST_STS_PUBLISH_URL_ELEMENT, urlElement)))));
        } catch (STSPublishException e) {
            String message = "Exception caught publishing instance: " + instanceConfig.getDeploymentSubPath() + ". Exception" + e;
            logger.error(message, e);
            return e.asPromise();
        } catch (Exception e) {
            String message = "Exception caught publishing instance: " + instanceConfig.getDeploymentSubPath() + ". Exception" + e;
            logger.error(message, e);
            return new InternalServerErrorException(message, e).asPromise();
        }
    }
}
