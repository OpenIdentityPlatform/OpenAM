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

package org.forgerock.openam.rest.sms;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceSchema;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.utils.StringUtils;

/**
 * A CREST singleton provider for SMS global schema config.
 *
 * @since 13.0.0
 */
public class SmsGlobalSingletonProvider extends SmsSingletonProvider {

    private final SmsJsonConverter organizationConverter;
    private final ServiceSchema organizationSchema;

    @Inject
    SmsGlobalSingletonProvider(@Assisted SmsJsonConverter globalConverter,
            @Assisted("global") ServiceSchema globalSchema,
            @Assisted("organization") @Nullable ServiceSchema organizationSchema,
            @Assisted("dynamic") @Nullable ServiceSchema dynamicSchema, @Assisted SchemaType type,
            @Assisted List<ServiceSchema> subSchemaPath, @Assisted String uriPath,
            @Assisted boolean serviceHasInstanceName, @Named("frRest") Debug debug) {
        super(globalConverter, globalSchema, dynamicSchema, type, subSchemaPath, uriPath, serviceHasInstanceName, debug);
        this.organizationSchema = organizationSchema;
        if (organizationSchema != null) {
            this.organizationConverter = new SmsJsonConverter(organizationSchema);
        } else {
            this.organizationConverter = null;
        }
    }

    @Override
    public void handleUpdate(ServerContext serverContext, UpdateRequest updateRequest, ResultHandler<Resource> handler) {
        if (organizationSchema != null) {
            try {
                Map<String, Set<String>> defaults = organizationConverter.fromJson(updateRequest.getContent().get("defaults"));
                organizationSchema.setAttributeDefaults(defaults);
            } catch (SMSException e) {
                debug.warning("::SmsCollectionProvider:: SMSException on create", e);
                handler.handleError(new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()));
            } catch (SSOException e) {
                debug.warning("::SmsCollectionProvider:: SSOException on create", e);
                handler.handleError(new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()));
            }
        }
        super.handleUpdate(serverContext, updateRequest, handler);
    }

    /**
     * Additionally adds "default" entry for realm attribute defaults, if present.
     *
     * @param value {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    protected JsonValue withExtraAttributes(ServerContext context, JsonValue value) {
        if (organizationSchema != null) {
            value.add("defaults", organizationConverter.toJson(organizationSchema.getAttributeDefaults()).getObject());
        }
        return super.withExtraAttributes(context, value);
    }

    @Override
    protected Map<String, Set<String>> getDynamicAttributes(ServerContext context) {
        return dynamicSchema.getAttributeDefaults();
    }

    @Override
    protected JsonValue createTemplate(ServerContext context) {
        JsonValue result = super.createTemplate(context);
        if (organizationSchema != null) {
            Map<String, String> attributeSectionMap = getAttributeNameToSection(organizationSchema);
            ResourceBundle console = ResourceBundle.getBundle("amConsole");
            String serviceType = organizationSchema.getServiceType().getType();
            String sectionOrder = getConsoleString(console, "sections." + serviceName + "." + serviceType);
            List<String> sections = new ArrayList<String>();
            if (StringUtils.isNotEmpty(sectionOrder)) {
                sections.addAll(Arrays.asList(sectionOrder.split("\\s+")));
            }
            addAttributeSchema(result, "/_schema/properties/defaults/", organizationSchema, sections,
                    attributeSectionMap, console, serviceType, context);
        }
        return result;
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
