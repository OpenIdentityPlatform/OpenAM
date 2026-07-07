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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sun.identity.config.SessionAttributeNames;
import com.sun.identity.setup.AMSetupServlet;
import com.sun.identity.setup.AMSetupUtils;
import com.sun.identity.setup.SetupConstants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.ResultCode;

/**
 * Non-Click base class for the configurator/upgrade wizard pages, rendered by
 * {@link ConfiguratorServlet} instead of Apache Click's {@code ClickServlet}. This is the
 * framework-agnostic half of the old {@code com.sun.identity.config.util.AjaxPage}, holding a
 * {@link ConfiguratorContext} instead of a Click thread-local context.
 *
 * <p>Pure validation/formatting logic shared with the still-Click {@code AjaxPage} lives in
 * {@link SetupUtils} so both engines stay behaviorally identical while pages migrate one at a
 * time.
 */
public abstract class SetupPage {

    private static final String RB_NAME = "amConfigurator";

    public static Debug debug = Debug.getInstance("amConfigurator");

    private final Map<String, Object> model = new HashMap<>();
    private ConfiguratorContext context;
    private boolean skipRender = false;
    private java.util.Locale configLocale;
    private ResourceBundle rb;
    private String hostName;

    public String responseString = "true";

    public void setContext(ConfiguratorContext context) {
        this.context = context;
    }

    public ConfiguratorContext getContext() {
        return context;
    }

    /**
     * Invoked before any other lifecycle method. Returning {@code false} tells
     * {@link ConfiguratorServlet} to render nothing (the port of Click's
     * {@code Page.onSecurityCheck()} / the old {@code ProtectedPage}).
     */
    public boolean onSecurityCheck() {
        return true;
    }

    public void onInit() {
        initializeResourceBundle();
        addModel("page", this);
    }

    public void onGet() {
    }

    public void onRender() {
    }

    /** Replaces Click's {@code setPath(null)} idiom for handlers that write the response directly. */
    protected void skipRender() {
        skipRender = true;
    }

    public boolean isSkipRender() {
        return skipRender;
    }

    public void addModel(String name, Object value) {
        model.put(name, value);
    }

    public Map<String, Object> getModel() {
        return model;
    }

    protected String toString(String paramName) {
        String value = getContext().getRequest().getParameter(paramName);
        value = (value != null ? value.trim() : null);
        value = ("".equals(value) ? null : value);
        return value;
    }

    protected boolean toBoolean(String paramName) {
        return SetupUtils.parseBoolean(toString(paramName));
    }

    protected int toInt(String paramName) {
        return SetupUtils.parseInt(toString(paramName));
    }

    protected void writeValid() {
        writeValid(null);
    }

    protected void writeValid(String message) {
        writeJsonResponse(true, message != null ? message : "");
    }

    protected void writeInvalid(String message) {
        writeJsonResponse(false, message != null ? message : "");
    }

    protected void writeJsonResponse(boolean valid, String responseBody) {
        writeToResponse(SetupUtils.jsonResponse(valid, responseBody));
    }

    protected void writeJsonResponse(String valid, String responseBody) {
        writeToResponse(SetupUtils.jsonResponse(valid, responseBody));
    }

    protected Connection getConnection(String host, int port, String bindDN, char[] bindPwd, int timeout,
            boolean isSSl) throws GeneralSecurityException, LdapException {
        return SetupUtils.getConnection(host, port, bindDN, bindPwd, timeout, isSSl);
    }

    protected boolean writeErrorToResponse(ResultCode resultCode) {
        String msg = SetupUtils.getMessage(resultCode);
        if (msg != null) {
            writeToResponse(getLocalizedString(msg));
            return true;
        }
        return false;
    }

    protected void writeToResponse(String text) {
        try {
            getContext().getResponse().getWriter().write(text);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        skipRender();
    }

    public void initializeResourceBundle() {
        HttpServletRequest req = getContext().getRequest();
        HttpServletResponse res = getContext().getResponse();

        setLocale(req);
        res.setContentType("text/html; charset=UTF-8");
    }

    private void setLocale(HttpServletRequest request) {
        if (request != null) {
            String superLocale = request.getParameter("locale");
            if (superLocale != null && superLocale.length() > 0) {
                configLocale = Locale.getLocaleObjFromAcceptLangHeader(superLocale);
            } else {
                String acceptLangHeader = request.getHeader("Accept-Language");
                if (acceptLangHeader != null && acceptLangHeader.length() > 0) {
                    configLocale = Locale.getLocaleObjFromAcceptLangHeader(acceptLangHeader);
                } else {
                    configLocale = java.util.Locale.getDefault();
                }
            }
            try {
                rb = ResourceBundle.getBundle(RB_NAME, configLocale);
            } catch (MissingResourceException mre) {
                // do nothing
            }
        }
    }

    public String getQuoteEscapedLocalizedString(String i18nKey) {
        String value = getLocalizedString(i18nKey);
        return value.replace("'", "\\'");
    }

    public String getLocalizedString(String i18nKey) {
        if (rb == null) {
            initializeResourceBundle();
        }

        String localizedValue = null;
        try {
            localizedValue = Locale.getString(rb, i18nKey, debug);
        } catch (MissingResourceException mre) {
            // do nothing
        }
        return (localizedValue == null) ? i18nKey : localizedValue;
    }

    protected String getAttribute(String attr, String defaultValue) {
        String value = (String) getContext().getSessionAttribute(attr);
        return (value != null) ? value : defaultValue;
    }

    protected String getHostName() {
        if (hostName == null) {
            hostName = getContext().getRequest().getServerName();
        }
        return hostName;
    }

    protected String getCookieDomain() {
        return getHostName();
    }

    protected String getAvailablePort(int portNumber) {
        return Integer.toString(AMSetupUtils.getFirstUnusedPort(getHostName(), portNumber, 1000));
    }

    protected String getBaseDir(HttpServletRequest req) {
        String basedir = AMSetupServlet.getPresetConfigDir();
        if ((basedir == null) || (basedir.length() == 0)) {
            String tmp = System.getProperty("user.home");
            if (File.separatorChar == '\\') {
                tmp = tmp.replace('\\', '/');
            }
            String uri = req.getRequestURI();
            int idx = uri.indexOf("/", 1);
            if (idx != -1) {
                uri = uri.substring(0, idx);
            }
            basedir = (tmp.endsWith("/")) ? tmp.substring(0, tmp.length() - 1) : tmp;
            basedir += uri;
        }
        return basedir;
    }

    @ConfiguratorAction
    public boolean validateInput() {
        String key = toString("key");
        String value = toString("value");

        if (value == null) {
            responseString = "missing.required.field";
        } else {
            getContext().setSessionAttribute(key, value);
        }

        writeToResponse(getLocalizedString(responseString));
        return false;
    }

    @ConfiguratorAction
    public boolean resetSessionAttributes() {
        try {
            Field[] fields = SessionAttributeNames.class.getDeclaredFields();
            for (Field field : fields) {
                try {
                    getContext().removeSessionAttribute((String) field.get(null));
                } catch (IllegalAccessException e) {
                    // ignore
                }
            }
        } catch (SecurityException e) {
            writeToResponse(e.getMessage());
        }
        skipRender();
        return false;
    }

    @ConfiguratorAction
    public boolean checkPasswords() {
        String confirm = toString("confirm");
        String password = toString("password");
        String otherPassword = toString("otherPassword");
        String type = toString("type");

        if (password == null) {
            responseString = getLocalizedString("missing.password");
        } else if (password.length() < SetupUtils.MIN_PASSWORD_SIZE) {
            responseString = getLocalizedString("password.size.invalid");
        } else if (confirm == null) {
            responseString = getLocalizedString("missing.confirm.password");
        } else if (confirm.length() < SetupUtils.MIN_PASSWORD_SIZE) {
            responseString = getLocalizedString("password.size.invalid");
        } else if (!password.equals(confirm)) {
            responseString = getLocalizedString("password.dont.match");
        } else if (otherPassword != null && otherPassword.equals(password)) {
            responseString = getLocalizedString("agent.admin.password.same");
        } else {
            if (type.equals("agent")) {
                type = SessionAttributeNames.CONFIG_VAR_AMLDAPUSERPASSWD;
            } else {
                type = SetupConstants.CONFIG_VAR_ADMIN_PWD;
            }
            getContext().setSessionAttribute(type, password);
        }

        writeToResponse(responseString);
        return false;
    }
}
