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
 * Copyright 2026 3A Systems LLC.
 */
package org.openidentityplatform.openam.config.servlet;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sun.identity.config.wizard.Step1;
import com.sun.identity.shared.debug.Debug;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Replacement for the Apache Click {@code ClickServlet} on the {@code *.htm} mapping.
 * Per-request it looks the request path up in a small, explicit migrated-page registry: a
 * registered path is instantiated as a {@link SetupPage} and rendered with FreeMarker; anything
 * else is forwarded, by name, to the still-installed (but no longer URL-mapped) {@code
 * click-servlet}, so unmigrated pages keep working exactly as before.
 *
 * <p>A named {@link RequestDispatcher#forward} does not rewrite the request path, so Click still
 * sees {@code getServletPath() == /config/wizard/stepN.htm} and resolves the page exactly as it
 * did when it owned the {@code *.htm} mapping directly.
 */
public class ConfiguratorServlet extends HttpServlet {

    private static final Debug debug = Debug.getInstance("amConfigurator");

    private static final String CLICK_SERVLET_NAME = "click-servlet";
    private static final String ACTION_LINK_PARAM = "actionLink";
    private static final String TEMPLATE_ROOT = "/WEB-INF/templates/config";
    private static final String CONFIG_PREFIX = "/config/";
    private static final String HTM_SUFFIX = ".htm";

    /** The migrated-page registry: URL (servlet path) -> page class. Grows one entry per increment. */
    private static final Map<String, Class<? extends SetupPage>> PAGES = new HashMap<>();
    static {
        PAGES.put("/config/wizard/step1.htm", Step1.class);
    }

    private volatile Configuration freemarkerConfig;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        Class<? extends SetupPage> pageClass = PAGES.get(path);

        if (pageClass == null) {
            forwardToClick(req, resp);
            return;
        }

        SetupPage page = newPage(pageClass);
        page.setContext(new ConfiguratorContext(req, resp));

        if (!page.onSecurityCheck()) {
            return;
        }

        // onInit() always runs before an actionLink dispatch: it is what Click did too, since
        // ActionLink controls fire during the control-processing phase that follows onInit().
        page.onInit();

        String actionLink = req.getParameter(ACTION_LINK_PARAM);
        if (actionLink != null) {
            invokeAction(page, actionLink, resp);
            return;
        }

        page.onGet();
        if (!page.isSkipRender()) {
            render(page, path, req, resp);
        }
    }

    private void forwardToClick(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher(CLICK_SERVLET_NAME);
        if (dispatcher == null) {
            throw new ServletException("Named dispatcher '" + CLICK_SERVLET_NAME + "' is not available; "
                    + "check that it is still declared (but unmapped) in web.xml");
        }
        dispatcher.forward(req, resp);
    }

    private SetupPage newPage(Class<? extends SetupPage> pageClass) throws ServletException {
        try {
            return pageClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new ServletException("Unable to instantiate configurator page " + pageClass.getName(), e);
        }
    }

    private void invokeAction(SetupPage page, String actionName, HttpServletResponse resp)
            throws ServletException, IOException {
        Method action = findAction(page.getClass(), actionName);
        if (action == null) {
            if (debug.warningEnabled()) {
                debug.warning("ConfiguratorServlet: no @ConfiguratorAction '" + actionName
                        + "' on " + page.getClass().getName());
            }
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            action.invoke(page);
        } catch (IllegalAccessException e) {
            throw new ServletException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            throw new ServletException(cause != null ? cause : e);
        }
    }

    private Method findAction(Class<?> pageClass, String actionName) {
        for (Method method : pageClass.getMethods()) {
            ConfiguratorAction annotation = method.getAnnotation(ConfiguratorAction.class);
            if (annotation == null) {
                continue;
            }
            String name = annotation.value().isEmpty() ? method.getName() : annotation.value();
            if (name.equals(actionName)) {
                return method;
            }
        }
        return null;
    }

    private void render(SetupPage page, String servletPath, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Map<String, Object> model = new HashMap<>(page.getModel());
        model.put("context", req.getContextPath());
        model.put("path", servletPath);
        copyPublicFields(page, model);

        try {
            Template template = getFreemarkerConfig().getTemplate(templateName(servletPath));
            Writer writer = resp.getWriter();
            template.process(model, writer);
        } catch (TemplateException e) {
            throw new ServletException("Error rendering template for " + servletPath, e);
        }
    }

    /** Reproduces Click's auto-exposure of a page's public fields into the template model. */
    private void copyPublicFields(SetupPage page, Map<String, Object> model) {
        for (Field field : page.getClass().getFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            try {
                Object value = field.get(page);
                if (value != null) {
                    model.put(field.getName(), value);
                }
            } catch (IllegalAccessException e) {
                // field is public; this should not happen
            }
        }
    }

    private static String templateName(String servletPath) {
        String relative = servletPath.startsWith(CONFIG_PREFIX)
                ? servletPath.substring(CONFIG_PREFIX.length())
                : servletPath;
        return relative.substring(0, relative.length() - HTM_SUFFIX.length()) + ".ftl";
    }

    private Configuration getFreemarkerConfig() {
        Configuration config = freemarkerConfig;
        if (config == null) {
            synchronized (this) {
                config = freemarkerConfig;
                if (config == null) {
                    config = new Configuration(Configuration.VERSION_2_3_31);
                    config.setTemplateLoader(new ServletContextTemplateLoader(getServletContext(), TEMPLATE_ROOT));
                    config.setDefaultEncoding("UTF-8");
                    freemarkerConfig = config;
                }
            }
        }
        return config;
    }
}
