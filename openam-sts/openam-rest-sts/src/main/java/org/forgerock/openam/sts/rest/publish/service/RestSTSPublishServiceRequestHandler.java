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
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
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
    private final Logger logger;

    /*
    No Injection, as ctor called from the RestSTSPublishServiceConnectionFactory (params obtained from RestSTSInjectorHolder).
     */
    RestSTSPublishServiceRequestHandler(RestSTSInstancePublisher publisher, Logger logger) {
        this.publisher = publisher;
        this.logger = logger;
    }

    public void handleAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleError(new NotSupportedException());
    }

    public void handleCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        RestSTSInstanceConfig instanceConfig;
        try {
            instanceConfig = RestSTSInstanceConfig.fromJson(request.getContent());
        } catch (Exception e) {
            logger.error("Exception caught marshalling json into RestSTSInstanceConfig instance: " + e);
            handler.handleError(new BadRequestException(e));
            return;
        }
        Injector instanceInjector;
        try {
            instanceInjector = Guice.createInjector(new RestSTSInstanceModule(instanceConfig));
        } catch (Exception e) {
            String message = "Exception caught creating the guice injector corresponding to rest sts instance: " + e;
            logger.error(message);
            handler.handleError(new InternalServerErrorException(message, e));
            return;
        }
        String urlElement = null;
        try {
            boolean republish = false;
            urlElement =
                    publisher.publishInstance(instanceConfig, instanceInjector.getInstance(RestSTS.class), republish);
            if (logger.isDebugEnabled()) {
                logger.debug("rest sts instance successfully published at " + urlElement);
            }
            handler.handleResult(new Resource(instanceConfig.getDeploymentSubPath(),
                    Integer.toString(instanceConfig.hashCode()), json(object(field(RESULT, SUCCESS),
                    field(AMSTSConstants.SUCCESSFUL_REST_STS_PUBLISH_URL_ELEMENT, urlElement)))));
        } catch (STSPublishException e) {
            String message = "Exception caught publishing instance: at url " + urlElement + ". Exception" + e;
            logger.error(message, e);
            handler.handleError(e);
        } catch (Exception e) {
            String message = "Exception caught publishing instance: at url " + urlElement + ". Exception" + e;
            logger.error(message, e);
            handler.handleError(new InternalServerErrorException(message, e));
        }

    }

    public void handleDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler) {
        String stsId = request.getResourceName();
        String realm = getRealmFromResourceName(request.getResourceName());
        try {
            publisher.removeInstance(stsId, realm);
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
                RestSTSInstanceConfig instanceConfig =
                        publisher.getPublishedInstance(request.getResourceName(), getRealmFromResourceName(request.getResourceName()));
                handler.handleResult(new Resource(instanceConfig.getDeploymentSubPath(),
                        Integer.toString(instanceConfig.hashCode()), instanceConfig.toJson()));
            }
        } catch (STSPublishException e) {
            String message = "Exception caught obtaining rest sts instance corresponding to id: " +
                    request.getResourceName() + "; Exception: " + e;
            logger.error(message, e);
            handler.handleError(e);
        }
    }

    public void handleUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException());
    }

    private String getRealmFromResourceName(String resourceName) {
        if (resourceName.lastIndexOf(AMSTSConstants.FORWARD_SLASH) == -1) {
            return AMSTSConstants.FORWARD_SLASH;
        }
        return resourceName.substring(0, resourceName.lastIndexOf(AMSTSConstants.FORWARD_SLASH));
    }
}
