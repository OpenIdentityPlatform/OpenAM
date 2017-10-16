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

import static org.forgerock.api.enums.CreateMode.ID_FROM_CLIENT;
import static org.forgerock.api.models.Create.create;
import static org.forgerock.api.models.Delete.delete;
import static org.forgerock.api.models.Read.read;
import static org.forgerock.api.models.Update.update;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.annotations.SingletonProvider;
import org.forgerock.api.annotations.Update;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.Paths;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.VersionedPath;
import com.google.common.base.Optional;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.identity.idm.AMIdentityRepositoryFactory;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.i18n.LocalizableString;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceSchema;

/**
 * A CREST singleton provider for SMS global schema config.
 *
 * @since 13.0.0
 */
@SingletonProvider(@Handler(mvccSupported = false, resourceSchema = @Schema(fromType = Object.class)))
public class SmsGlobalSingletonProvider extends SmsSingletonProvider {

    private final SmsJsonConverter organizationConverter;
    private final ServiceSchema organizationSchema;

    @Inject
    SmsGlobalSingletonProvider(@Assisted SmsJsonConverter globalConverter,
            @Assisted("global") ServiceSchema globalSchema,
            @Assisted("organization") @Nullable ServiceSchema organizationSchema,
            @Assisted("dynamic") @Nullable ServiceSchema dynamicSchema, @Assisted SchemaType type,
            @Assisted List<ServiceSchema> subSchemaPath, @Assisted String uriPath,
            @Assisted boolean serviceHasInstanceName, @Named("frRest") Debug debug,
            @Named("AMResourceBundleCache") AMResourceBundleCache resourceBundleCache,
            @Named("DefaultLocale") Locale defaultLocale, AMIdentityRepositoryFactory idRepoFactory) {
        super(globalConverter, globalSchema, dynamicSchema, type, subSchemaPath, uriPath, serviceHasInstanceName, debug,
                resourceBundleCache, defaultLocale, idRepoFactory);
        this.organizationSchema = organizationSchema;
        if (organizationSchema != null) {
            this.organizationConverter = new SmsJsonConverter(organizationSchema);
        } else {
            this.organizationConverter = null;
        }
        initDescription(globalSchema);
    }

    protected ApiDescription initDescription(ServiceSchema schema) {
        return ApiDescription.apiDescription().id("fake").version("v")
                .paths(Paths.paths().put("", VersionedPath.versionedPath()
                        .put(VersionedPath.UNVERSIONED, Resource.resource()
                                .title(getI18NName())
                                .description(getSchemaDescription(schema.getI18NKey()))
                                .mvccSupported(false)
                                .resourceSchema(org.forgerock.api.models.Schema.schema().schema(
                                        createSchema(Optional.<Context>absent())).build())
                                .read(read().build())
                                .update(update().build())
                                .build()).build()
                ).build()).build();
    }

    @Update(operationDescription = @Operation)
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context serverContext,
            UpdateRequest updateRequest) {
        if (organizationSchema != null) {
            try {
                Map<String, Set<String>> defaults = organizationConverter.fromJson(updateRequest.getContent().get("defaults"));
                organizationSchema.setAttributeDefaults(defaults);
            } catch (SMSException e) {
                debug.warning("::SmsCollectionProvider:: SMSException on create", e);
                return new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()).asPromise();
            } catch (SSOException e) {
                debug.warning("::SmsCollectionProvider:: SSOException on create", e);
                return new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()).asPromise();
            } catch (ResourceException e) {
                return e.asPromise();
            }
        }
        return super.handleUpdate(serverContext, updateRequest);
    }

    @Override
    protected void saveConfigAttributes(ServiceConfig config, Map<String, Set<String>> attributes) throws SSOException, SMSException {
        if (attributes != null) {
            schema.setAttributeDefaults(attributes);
        }
    }

    @Override
    protected void addGlobalAttributes(ServiceConfig config, JsonValue result) {
        if (schema != organizationSchema) {
            converter.toJson(schema.getAttributeDefaults(), false, result);
        }
    }

    @Override
    protected void addOrganisationAttributes(String realm, ServiceConfig config, JsonValue result) {
        if (organizationSchema != null) {
            JsonValue defaults = organizationConverter.toJson(organizationSchema.getAttributeDefaults(), true);
            if (defaults.size() > 0) {
                result.add("defaults", defaults.getObject());
            }
        }
    }

    @Override
    protected void addOrganisationSchema(Optional<Context> context, JsonValue result) {
        if (organizationSchema != null) {
            addAttributeSchema(result, "/properties/defaults/properties/", organizationSchema, context);
            if (result.isDefined("properties") && result.get("properties").isDefined("defaults")) {
                result.put(new JsonPointer("/properties/defaults/type"), "object");
                result.put(new JsonPointer("/properties/defaults/title"),
                        new LocalizableString("i18n:amConsole#section.label.common.realmDefaults",
                                this.getClass().getClassLoader()));
            }
        }
    }

    /**
     * Global config can be null which is fine when a schema has no global attributes, but it may
     * have realm attribute defaults and/or dynamic attributes.
     *
     * @param scm {@inheritDoc}
     * @param resourceId {@inheritDoc}
     * @return {@inheritDoc}
     * @throws SSOException {@inheritDoc}
     * @throws SMSException {@inheritDoc}
     */
    @Override
    protected ServiceConfig getGlobalConfigNode(ServiceConfigManager scm, String resourceId)
            throws SSOException, SMSException {
        return scm.getGlobalConfig(resourceId);
    }
}
