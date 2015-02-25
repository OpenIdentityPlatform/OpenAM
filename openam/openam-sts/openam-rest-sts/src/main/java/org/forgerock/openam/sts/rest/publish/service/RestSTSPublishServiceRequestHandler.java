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
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.rest.publish.service;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.router.RestRealmValidator;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.STSPublishException;
import org.forgerock.openam.sts.rest.RestSTS;
import org.forgerock.openam.sts.rest.config.RestSTSInstanceModule;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;
import org.forgerock.openam.sts.rest.publish.RestSTSInstancePublisher;
import org.forgerock.openam.utils.JsonObject;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.slf4j.Logger;

import java.util.List;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

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
    private final Logger logger;

    /*
    No Injection, as ctor called from the RestSTSPublishServiceConnectionFactory (params obtained from RestSTSInjectorHolder).
     */
    RestSTSPublishServiceRequestHandler(RestSTSInstancePublisher publisher, RestRealmValidator realmValidator, Logger logger) {
        this.publisher = publisher;
        this.realmValidator = realmValidator;
        this.logger = logger;
    }

    public void handleAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleError(new NotSupportedException());
    }

    /*
     This method will be invoked by either a programmatic client, in which case a RestSTSInstanceConfig has emitted
     properly-formatted json, or from the RestSecurityTokenServiceViewBean, in which case the configuration state is
     in the sms-centric Map<String, Set<String>> format. This method needs to be able to handle both invocation types,
     and marshal the invocation state in to a RestSTSInstanceConfig instance either way. It also needs to return an accurate
     error message, so that in the case of RestSecurityTokenServiceViewBean invocation, the user can make appropriate
      corrections to the configuration state.
      */
    public void handleCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        final RestSTSInstanceConfig instanceConfig;
        try {
            instanceConfig = marshalInstanceConfigFromInvocation(request.getContent());
        } catch (BadRequestException e) {
            handler.handleError(e);
            return;
        }
        if (!realmValidator.isRealm(instanceConfig.getDeploymentConfig().getRealm())) {
            logger.warn("Publish of Rest STS instance " + instanceConfig.getDeploymentSubPath() + " to realm "
                    + instanceConfig.getDeploymentConfig().getRealm() + " rejected because realm does not exist.");
            handler.handleError(new NotFoundException("The specified realm does not exist."));
            return;
        }
        Injector instanceInjector;
        try {
            instanceInjector = createInjector(instanceConfig);
        } catch (ResourceException e) {
            handler.handleError(e);
            return;
        }
        try {
            publishInstance(instanceConfig, instanceInjector, handler);
        } catch (ResourceException e) {
            handler.handleError(e);
        }
    }

    public void handleDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler) {
        String stsId = request.getResourceName();
        String realm = getRealmFromResourceName(request.getResourceName());
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
            handler.handleResult(new Resource(stsId, stsId, json(object(field
                    (RESULT, "rest sts instance " + stsId + " successfully removed from realm " + realm)))));
        } catch (STSPublishException e) {
            String message = "Exception caught removing instance: " + stsId + " from realm " + realm + ". Exception:" + e;
            logger.error(message, e);
            handler.handleError(e);
        } catch (Exception e) {
            String message = "Exception caught removing instance: " + stsId + " from realm " + realm + ". Exception:" + e;
            logger.error(message, e);
            handler.handleError(new InternalServerErrorException(message, e));
        }
    }

    public void handlePatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException());
    }

    public void handleQuery(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        handler.handleError(new NotSupportedException());
    }

    public void handleRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        try {
            if (EMPTY_STRING.equals(request.getResourceName())) {
                List<RestSTSInstanceConfig> publishedInstances = publisher.getPublishedInstances();
                JsonObject jsonObject = JsonValueBuilder.jsonValue();
                for (RestSTSInstanceConfig instanceConfig : publishedInstances) {
                    jsonObject.put(instanceConfig.getDeploymentSubPath(), instanceConfig.toJson().toString());
                }
                /*
                Note that the revision etag is not set, as this is not a resource which should really be cached.
                If caching becomes necessary, a string composed of the hash codes of each of the RestSTSInstanceConfig
                instances could be used (or a hash of that string).
                 */
                handler.handleResult(new Resource(PUBLISHED_INSTANCES, EMPTY_STRING, jsonObject.build()));
            } else {
                final String realm = getRealmFromResourceName(request.getResourceName());
                if (!realmValidator.isRealm(realm)) {
                    logger.warn("Read of rest STS instance state for instance " + request.getResourceName() +
                            " in realm " + realm + " rejected because realm does not exist");
                    handler.handleError(new NotFoundException("The specified realm does not exist."));
                    return;
                }
                RestSTSInstanceConfig instanceConfig =
                        publisher.getPublishedInstance(request.getResourceName(), realm);
                handler.handleResult(new Resource(instanceConfig.getDeploymentSubPath(),
                        Integer.toString(instanceConfig.hashCode()),
                        JsonValueBuilder.jsonValue().put(instanceConfig.getDeploymentSubPath(), instanceConfig.toJson().toString()).build()));
            }
        } catch (STSPublishException e) {
            String message = "Exception caught obtaining rest sts instance corresponding to id: " +
                    request.getResourceName() + "; Exception: " + e;
            logger.error(message, e);
            handler.handleError(e);
        }
    }

     /*
      * A PUT to the url composed of the publish endpont + the sts instance id with a payload corresponding to a
      * RestSTSInstanceId (wrapped in invocation context information) will result in republishing the existing instance
      * (which is a delete followed by a create).
      */
    public void handleUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        String stsId = request.getResourceName();
        String realm = getRealmFromResourceName(request.getResourceName());
        if (!realmValidator.isRealm(realm)) {
            logger.warn("Update of rest STS instance state for instance " + stsId +
                    " in realm " + realm + " rejected because realm does not exist");
            handler.handleError(new NotFoundException("The specified realm does not exist."));
            return;
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
            handler.handleError(e);
            return;
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
                handler.handleError(e);
                return;
            }
            Injector instanceInjector;
            try {
                instanceInjector = createInjector(instanceConfig);
            } catch (ResourceException e) {
                logger.error("In RestSTSPublishServiceRequestHandler#handleUpdate, exception caught creating an " +
                        "Injector using the RestSTSInstanceConfig. The instance: "+ instanceConfig.toJson() +
                        "; Exception: " + e, e);
                handler.handleError(e);
                return;
            }
            try {
                boolean removeOnlyFromRouter = false;
                publisher.removeInstance(stsId, realm, removeOnlyFromRouter);
            } catch (STSPublishException e) {
                logger.error("In RestSTSPublishServiceRequestHandler#handleUpdate, exception caught removing " +
                        "rest sts instance " + instanceConfig.getDeploymentSubPath() + ". This means instance is" +
                        "in indeterminate state, and has not been updated. The instance config: " +  instanceConfig
                        + "; Exception: " + e, e);
                handler.handleError(e);
            }
            try {
                publishInstance(instanceConfig, instanceInjector, handler);
                logger.info("Rest STS instance " + instanceConfig.getDeploymentSubPath() + " updated to state " +
                    instanceConfig.toJson());
            } catch (ResourceException e) {
                logger.error("In RestSTSPublishServiceRequestHandler#handleUpdate, exception caught publishing " +
                        "rest sts instance " + instanceConfig.getDeploymentSubPath() + ". This means instance is" +
                        "in indeterminate state, having been removed, but not successfully published with updated " +
                        "state. The instance config: " +  instanceConfig + "; Exception: " + e, e);
                handler.handleError(e);
            }
        } else {
            //404 - realm and id not found in SMS
            handler.handleError(new NotFoundException("No rest sts instance with id " + stsId + " in realm " + realm));
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
        the RestSecurityTokenServiceViewBean (i.e. a Map<String, List<String>>) so that the correct un-marshaling logic
        can be invoked, and the correct error messages displayed. The two cases will be distinguished by distinct values
        corresponding to the AMSTSContants.REST_STS_PUBLISH_INVOCATION_CONTEXT string in the top-level json object.
         */
        String invocationContext = requestContent.get(AMSTSConstants.REST_STS_PUBLISH_INVOCATION_CONTEXT).asString();
        if (AMSTSConstants.REST_STS_PUBLISH_INVOCATION_CONTEXT_CLIENT_SDK.equals(invocationContext)) {
            try {
                return RestSTSInstanceConfig.fromJson(requestContent.get(AMSTSConstants.REST_STS_PUBLISH_INSTANCE_STATE));
            } catch (Exception e) {
                logger.error("Exception caught marshalling json into RestSTSInstanceConfig instance for SDK invocation " +
                        "context: " + e, e);
                throw new BadRequestException(e);
            }
        } else if (AMSTSConstants.REST_STS_PUBLISH_INVOCATION_CONTEXT_VIEW_BEAN.equals(invocationContext)) {
            try {
                return RestSTSInstanceConfig.marshalFromJsonAttributeMap(requestContent.get(
                        AMSTSConstants.REST_STS_PUBLISH_INSTANCE_STATE));
            } catch (Exception e) {
                logger.error("Exception caught marshalling attribute map into RestSTSInstanceConfig instance for " +
                        "ViewBean invocation context: " + e, e);
                throw new BadRequestException(e);
            }
        } else {
            String message = "The top-level json object must contain a key named "
                    + AMSTSConstants.REST_STS_PUBLISH_INVOCATION_CONTEXT + " and with a value corresponding to either "
                    + AMSTSConstants.REST_STS_PUBLISH_INVOCATION_CONTEXT_CLIENT_SDK + " or "
                    + AMSTSConstants.REST_STS_PUBLISH_INVOCATION_CONTEXT_VIEW_BEAN + ". Actual invocation content: "
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

    private void publishInstance(RestSTSInstanceConfig instanceConfig, Injector instanceInjector,
                                 ResultHandler<Resource> handler) throws ResourceException {
        try {
            boolean republish = false;
            final String urlElement =
                    publisher.publishInstance(instanceConfig, instanceInjector.getInstance(RestSTS.class), republish);
            if (logger.isDebugEnabled()) {
                logger.debug("rest sts instance successfully published at " + urlElement);
            }
            handler.handleResult(new Resource(instanceConfig.getDeploymentSubPath(),
                    Integer.toString(instanceConfig.hashCode()), json(object(field(RESULT, SUCCESS),
                    field(AMSTSConstants.SUCCESSFUL_REST_STS_PUBLISH_URL_ELEMENT, urlElement)))));
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
}
