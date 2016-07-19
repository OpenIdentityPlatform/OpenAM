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

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceSchema;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.annotations.RequestHandler;
import org.forgerock.json.resource.annotations.Update;
import org.forgerock.openam.identity.idm.AMIdentityRepositoryFactory;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * A CREST singleton provider for SMS global schema config.
 *
 * @since 13.0.0
 */
@RequestHandler
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
    }

    @Update
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
    protected void addOrganisationAttributes(String realm, ServiceConfig config, JsonValue result) {
        if (organizationSchema != null) {
            result.add("defaults", organizationConverter.toJson(organizationSchema.getAttributeDefaults(), true)
                    .getObject());
        }
    }

    @Override
    protected void addOrganisationSchema(Context context, JsonValue result) {
        if (organizationSchema != null) {
            addAttributeSchema(result, "/properties/defaults/", organizationSchema, context);
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
