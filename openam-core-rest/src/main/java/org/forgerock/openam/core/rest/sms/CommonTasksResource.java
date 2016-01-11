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
 */

package org.forgerock.openam.core.rest.sms;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;

import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.util.promise.Promise;

/**
 * CREST resource which returns configuration details about each available common task wizard.
 *
 * <pre>
 * {
 *   "name" : "Create SAMLv2 Providers",
 *   "description" : "Use these work flows to create hosted or remote identity and service providers for SAMLv2
 *   Federation.",
 *   "tasks" : [ {
 *     "name" : "Create Hosted Identity Provider",
 *     "description" : "This allows you to configure this instance of OpenAM server as an Identity Provider (IDP).
 *     You need three things: Name, Circle of Trust (COT) and optionally Signing Certificate. Metadata represents
 *     the configuration necessary to execute federation protocols (eg SAMLv2) as well as the mechanism to
 *     communicate this configuration to other entities (eg SPs) in a COT. A COT is a group of IDPs and SPs that
 *     trust each other and in effect represents the confines within which all federation communications are
 *     performed.",
 *     "link" : "task/CreateHostedIDP"
 *   }, {
 *     "name" : "Create Hosted Service Provider",
 *     "description" : "This allows you to configure this instance of OpenAM server as an Service Provider (SP).
 *     You need three things: Name, Circle of Trust (COT). Metadata represents the configuration necessary to
 *     execute federation protocols (eg SAMLv2) as well as the mechanism to communicate this configuration to
 *     other entities (eg IDPs) in a COT. A COT is a group of IDPs and SPs that trust each other and in effect
 *     represents the confines within which all federation communications are performed.",
 *     "link" : "task/CreateHostedSP"
 *   }, {
 *     "name" : "Register Remote Identity Provider",
 *     "description" : "This allows you to register a remote Identity Provider (IDP). You need two things: Circle
 *     of Trust (COT). Metadata represents the configuration necessary to execute federation protocols (eg SAMLv2)
 *     as well as the mechanism to communicate this configuration to other entities (eg SPs) in a COT. A COT is a
 *     group of IDPs and SPs that trust each other and in effect represents the confines within which all
 *     federation communications are performed.",
 *     "link" : "task/CreateRemoteIDP"
 *   }, {
 *     "name" : "Register Remote Service Provider",
 *     "description" : "This allows you to register a remote Service Provider (SP). You need two things: Circle
 *     of Trust (COT). Metadata represents the configuration necessary to execute federation protocols (eg SAMLv2)
 *     as well as the mechanism to communicate this configuration to other entities (eg SPs) in a COT. A COT is
 *     a group of IDPs and SPs that trust each other and in effect represents the confines within which all
 *     federation communications are performed.",
 *     "link" : "task/CreateRemoteSP"
 *   }
 * }
 * </pre>
 *
 * @since 13.0.0
 */
class CommonTasksResource implements CollectionResourceProvider {

    private static final String RESOURCE_BUNDLE_NAME = "amConsole";
    private final CommonTasksConfigurationManager configurationManager;

    public CommonTasksResource() {
        this.configurationManager = new CommonTasksConfigurationManager();
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {
        return new NotSupportedException().asPromise();
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String resourceId,
            ActionRequest request) {
        return new NotSupportedException().asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(Context context, CreateRequest request) {
        return new NotSupportedException().asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context context, String resourceId,
            DeleteRequest request) {
        return new NotSupportedException().asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String resourceId,
            PatchRequest request) {
        return new NotSupportedException().asPromise();
    }

    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request, QueryResourceHandler handler) {
        if (!"true".equals(request.getQueryFilter().toString())) {
            return new NotSupportedException("Query not supported: " + request.getQueryFilter()).asPromise();
        }
        //TODO pass in locale
        Locale locale = Locale.ROOT;
        JsonValue configuration = configurationManager.getCommonTasksConfiguration(getResourceBundle(locale));
        for (String key : configuration.keys()) {
            JsonValue resource = configuration.get(key);
            resource.add(ResourceResponse.FIELD_CONTENT_ID, key);
            handler.handleResource(newResourceResponse(key, String.valueOf(resource.getObject().hashCode()), resource));
        }
        return newResultPromise(newQueryResponse());
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId,
            ReadRequest request) {
        //TODO pass in locale
        Locale locale = Locale.ROOT;
        JsonValue configuration = configurationManager.getCommonTasksConfiguration(getResourceBundle(locale));
        if (!configuration.isDefined(resourceId)) {
            return new BadRequestException("Invalid common task").asPromise();
        }
        JsonValue resource = configuration.get(resourceId);
        return newResultPromise(newResourceResponse(resourceId, String.valueOf(resource.getObject().hashCode()), resource));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, String resourceId,
            UpdateRequest request) {
        return new NotSupportedException().asPromise();
    }

    private ResourceBundle getResourceBundle(Locale locale) {
        return ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME, locale);
    }

    private static final class CommonTasksConfigurationManager {

        private static final String NAME_FIELD = "name";
        private static final String DESCRIPTION_FIELD = "description";
        private static final String TASKS_FIELD = "tasks";
        private static final String LINK_FIELD = "link";

        private JsonValue getCommonTasksConfiguration(ResourceBundle resourceBundle) {
            return json(object(
                    field("saml2", getSaml2CommonTasksConfiguration(resourceBundle).getObject()),
                    field("oauth2", getOAuth2OpenIDConnectCommonTaskConfiguration(resourceBundle).getObject()),
                    field("fedlet", getFedletCommonTaskConfiguration(resourceBundle).getObject()),
                    field("googleapps", getGoogleAppsCommonTaskConfiguration(resourceBundle).getObject()),
                    field("salesforce", getSalesforceCommonTaskConfiguration(resourceBundle).getObject()),
                    field("socialauthentication",
                            getSocialAuthenticationCommonTaskConfiguration(resourceBundle).getObject()),
                    field("documentation", getProductDocumentationCommonTaskConfiguration(resourceBundle).getObject()),
                    field("soapstsdeployment",
                            getCreateSoapSTSDeploymentCommonTaskConfiguration(resourceBundle).getObject())
            ));
        }

        private JsonValue getSaml2CommonTasksConfiguration(ResourceBundle resourceBundle) {
            return createTaskGroup(resourceBundle, "SAML2",
                    createTask(resourceBundle, "create.hosted.idp", "CreateHostedIDP"),
                    createTask(resourceBundle, "create.hosted.sp", "CreateHostedSP"),
                    createTask(resourceBundle, "create.remote.idp", "CreateRemoteIDP"),
                    createTask(resourceBundle, "create.remote.sp", "CreateRemoteSP"));
        }

        private JsonValue getOAuth2OpenIDConnectCommonTaskConfiguration(ResourceBundle resourceBundle) {
            return createTaskGroup(resourceBundle, "OAuth2",
                    createTask(resourceBundle, "configure.oauth2", "ConfigureOAuth2?type=oauth2"),
                    createTask(resourceBundle, "configure.oidc", "ConfigureOAuth2?type=oidc"),
                    createTask(resourceBundle, "configure.mobileconnect", "ConfigureOAuth2?type=mobileconnect"),
                    createTask(resourceBundle, "configure.uma", "ConfigureOAuth2?type=uma"));
        }

        private JsonValue getFedletCommonTaskConfiguration(ResourceBundle resourceBundle) {
            return createTaskGroup(resourceBundle, "createFedlet",
                    createTask(resourceBundle, "create.fedlet", "CreateFedlet"));
        }

        private JsonValue getGoogleAppsCommonTaskConfiguration(ResourceBundle resourceBundle) {
            return createTaskGroup(resourceBundle, "configure.google.apps",
                    createTask(resourceBundle, "configure.google.apps", "ConfigureGoogleApps"));
        }

        private JsonValue getSalesforceCommonTaskConfiguration(ResourceBundle resourceBundle) {
            return createTaskGroup(resourceBundle, "configure.salesforce.apps",
                    createTask(resourceBundle, "configure.salesforce.apps", "ConfigureSalesForceApps"));
        }

        private JsonValue getSocialAuthenticationCommonTaskConfiguration(ResourceBundle resourceBundle) {
            return createTaskGroup(resourceBundle, "configure.social.authn",
                    createTask(resourceBundle, "configure.facebook.authn", "ConfigureSocialAuthN?type=facebook"),
                    createTask(resourceBundle, "configure.google.authn", "ConfigureSocialAuthN?type=google"),
                    createTask(resourceBundle, "configure.microsoft.authn", "ConfigureSocialAuthN?type=microsoft"),
                    createTask(resourceBundle, "configure.other.social.authn", "ConfigureSocialAuthN?type=other"));
        }

        private JsonValue getTestFederationConnectivityCommonTaskConfiguration(ResourceBundle resourceBundle) {
            return createTaskGroup(resourceBundle, "validateSAMLv2",
                    createTask(resourceBundle, "saml2.validate", "ValidateSAML2Setup"));
        }

        private JsonValue getProductDocumentationCommonTaskConfiguration(ResourceBundle resourceBundle) {
            return createTaskGroup(resourceBundle, "documentation",
                    createTaskWithAbsoluteLink(resourceBundle, "doc",
                            "http://docs.forgerock.org/en/index.html?product=openam"));
        }

        private JsonValue getCreateSoapSTSDeploymentCommonTaskConfiguration(ResourceBundle resourceBundle) {
            return createTaskGroup(resourceBundle, "soapSTSDeployment",
                    createTask(resourceBundle, "create.soap.sts.deployment", "CreateSoapSTSDeployment"));
        }

        private JsonValue createTaskGroup(ResourceBundle resourceBundle, String nameSuffix, Object... tasks) {
            JsonValue taskGroup = json(object(
                    field(NAME_FIELD, resourceBundle.getString("page.title.common.tasks.section." + nameSuffix)),
                    field(DESCRIPTION_FIELD,
                            resourceBundle.getString("page.title.common.tasks.section.desc." + nameSuffix))));
            taskGroup.add(TASKS_FIELD, Arrays.asList(tasks));
            return taskGroup;
        }

        private Object createTask(ResourceBundle resourceBundle, String taskSuffix, String linkSuffix) {
            return object(
                    field(NAME_FIELD, resourceBundle.getString("commontask.label." + taskSuffix)),
                    field(DESCRIPTION_FIELD, resourceBundle.getString("commontask." + taskSuffix)),
                    field(LINK_FIELD, "task/" + linkSuffix));
        }

        private Object createTaskWithAbsoluteLink(ResourceBundle resourceBundle, String taskSuffix, String link) {
            return object(
                    field(NAME_FIELD, resourceBundle.getString("commontask.label." + taskSuffix)),
                    field(DESCRIPTION_FIELD, resourceBundle.getString("commontask." + taskSuffix)),
                    field(LINK_FIELD, link));
        }
    }
}
