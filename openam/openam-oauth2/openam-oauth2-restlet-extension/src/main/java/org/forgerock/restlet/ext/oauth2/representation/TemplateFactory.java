/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [2012] [ForgeRock Inc]"
 */
package org.forgerock.restlet.ext.oauth2.representation;

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.ext.freemarker.ContextTemplateLoader;
import org.restlet.ext.freemarker.TemplateRepresentation;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 *
 * @see <a
 *      href="http://freemarker.sourceforge.net/docs/pgui_config_templateloading.html">Template
 *      loading</a>
 */
public class TemplateFactory {
    private final Configuration config;

    /**
     * Never allow this to be instantiated.
     */
    private TemplateFactory(Context context) {
        config = new Configuration();
        try {
            TemplateLoader ctx = new ContextTemplateLoader(context, "clap:///");
            TemplateLoader ctl = new ClassTemplateLoader(TemplateFactory.class, "/");
            TemplateLoader[] loaders = new TemplateLoader[] { ctx, ctl };
            MultiTemplateLoader mtl = new MultiTemplateLoader(loaders);
            config.setTemplateUpdateDelay(3600);
            config.setTemplateLoader(mtl);
            config.setSetting(Configuration.CACHE_STORAGE_KEY, "strong:20, soft:250");
        } catch (TemplateException e) {
        }
    }

    public static TemplateFactory newInstance(Context context) {
        return new TemplateFactory(context);
    }

    public TemplateRepresentation getTemplateRepresentation(String templateName) {
        Template template = TemplateRepresentation.getTemplate(config, templateName);
        if (null != template) {
            return new TemplateRepresentation(template, MediaType.TEXT_HTML);
        }
        return null;
    }

}
