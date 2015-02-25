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
 * Copyright 2012-2014 ForgeRock AS.
 */

package org.forgerock.oauth2.restlet;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.ext.freemarker.ContextTemplateLoader;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating TemplateRepresentations.
 *
 * @since 11.0.0
 */
public class TemplateFactory {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final Configuration config;

    /**
     * Constructs a new TemplateFactory.
     *
     * @param context The Restlet context.
     */
    private TemplateFactory(Context context) {
        config = new Configuration();
        try {
            final TemplateLoader ctx = new ContextTemplateLoader(context, "clap:///");
            final TemplateLoader ctl = new ClassTemplateLoader(TemplateFactory.class, "/");
            final TemplateLoader[] loaders = new TemplateLoader[] { ctx, ctl };
            final MultiTemplateLoader mtl = new MultiTemplateLoader(loaders);
            config.setTemplateUpdateDelay(3600);
            config.setTemplateLoader(mtl);
            config.setSetting(Configuration.CACHE_STORAGE_KEY, "strong:20, soft:250");
        } catch (TemplateException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Creates a new instance of the TemplateFactory.
     *
     * @param context The Restlet context.
     * @return A new instance of the TemplateFactory.
     */
    public static TemplateFactory newInstance(Context context) {
        return new TemplateFactory(context);
    }

    /**
     * Gets the template representation for the template with specified name.
     *
     * @param templateName The template name.
     * @return A TemplateRepresentation.
     */
    public TemplateRepresentation getTemplateRepresentation(final String templateName) {
        final Template template = TemplateRepresentation.getTemplate(config, templateName);
        if (template != null) {
            return new TemplateRepresentation(template, MediaType.TEXT_HTML);
        }
        return null;
    }
}
