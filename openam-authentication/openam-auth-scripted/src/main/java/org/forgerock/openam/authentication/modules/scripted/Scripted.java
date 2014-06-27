/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: LDAP.java,v 1.17 2010/01/25 22:09:16 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2014 ForgeRock, Inc.
 */
package org.forgerock.openam.authentication.modules.scripted;

import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.client.HttpClient;
import org.forgerock.http.client.HttpClientFactory;
import org.forgerock.http.client.request.HttpClientRequest;
import org.forgerock.http.client.request.HttpClientRequestFactory;
import org.forgerock.openam.scripting.ScriptEvaluator;
import org.forgerock.openam.scripting.ScriptObject;
import org.forgerock.openam.scripting.StandardScriptEvaluator;
import org.forgerock.openam.scripting.SupportedScriptingLanguage;

import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;
import java.security.Principal;
import java.util.Map;

import com.sun.identity.shared.datastruct.CollectionHelper;

/**
 * An authentication module that allows users to authenticate via a scripting language
 */
public class Scripted extends AMLoginModule {
    public static final String ATTR_NAME_PREFIX = "iplanet-am-auth-scripted-";
    public static final String CLIENT_SCRIPT_ATTR_NAME = ATTR_NAME_PREFIX + "client-script";
    public static final String CLIENT_SCRIPT_ENABLED_ATTR_NAME = ATTR_NAME_PREFIX + "client-script-enabled";
    public static final String SCRIPT_TYPE_ATTR_NAME = ATTR_NAME_PREFIX + "script-type";
    public static final String SERVER_SCRIPT_ATTRIBUTE_NAME = ATTR_NAME_PREFIX + "server-script";
    public static final String SCRIPT_NAME = "server-side-script";

    public static final String JAVA_SCRIPT_LABEL = "Java Script";
    public static final String GROOVY_LABEL = "Groovy";
    private final static int STATE_BEGIN = 1;

    private final static int STATE_RUN_SCRIPT = 2;
    public static final String STATE_VARIABLE_NAME = "authState";
    private static final String SUCCESS_ATTR_NAME = "SUCCESS";
    public static final int SUCCESS_VALUE = -1;
    private static final String FAILED_ATTR_NAME = "FAILED";
    public static final int FAILURE_VALUE = -2;
    public static final String USERNAME_VARIABLE_NAME = "username";
    public static final String HTTP_CLIENT_REQUEST_VARIABLE_NAME = "httpClientRequest";
    public static final String HTTP_CLIENT_VARIABLE_NAME = "httpClient";
    public static final String LOGGER_VARIABLE_NAME = "logger";
    public static final String IDENTITY_REPOSITORY = "idRepository";

    private String userName;
    private String clientSideScript;
    private boolean clientSideScriptEnabled;
    private ScriptObject serverSideScript;
    private ScriptEvaluator scriptEvaluator;
    public Map moduleConfiguration;

    /** Debug logger instance used by scripts to log error/debug messages. */
    private static final Debug DEBUG = Debug.getInstance("amScript");

    final HttpClientFactory httpClientFactory = InjectorHolder.getInstance(HttpClientFactory.class);
    final HttpClientRequestFactory httpClientRequestFactory = InjectorHolder.getInstance(HttpClientRequestFactory.class);
    private HttpClient httpClient;
    private HttpClientRequest httpClientRequest;
    private ScriptIdentityRepository identityRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Subject subject, Map sharedState, Map options) {
        userName = (String) sharedState.get(getUserKey());
        moduleConfiguration = options;
        clientSideScript = getClientSideScript();
        scriptEvaluator = getScriptEvaluator();
        serverSideScript = getServerSideScript();
        clientSideScriptEnabled = getClientSideScriptEnabled();
        httpClient = getHttpClient();
        httpClientRequest = getHttpRequest();
        identityRepository  = getScriptIdentityRepository();
    }

    private ScriptIdentityRepository getScriptIdentityRepository() {
        return new ScriptIdentityRepository(getAmIdentityRepository());
    }

    private AMIdentityRepository getAmIdentityRepository() {
        return getAMIdentityRepository(getRequestOrg());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int process(Callback[] callbacks, int state) throws LoginException {

        switch (state) {

            case STATE_BEGIN:
                if (!clientSideScriptEnabled || clientSideScript.isEmpty()) {
                    clientSideScript = " ";
                }

                substituteUIStrings();

                return STATE_RUN_SCRIPT;

            case STATE_RUN_SCRIPT:
                Bindings scriptVariables = new SimpleBindings();
                scriptVariables.put("requestData", getScriptHttpRequestWrapper());
                scriptVariables.put(LOGGER_VARIABLE_NAME, DEBUG);
                scriptVariables.put(STATE_VARIABLE_NAME, state);
                scriptVariables.put(USERNAME_VARIABLE_NAME, userName);
                scriptVariables.put(SUCCESS_ATTR_NAME, SUCCESS_VALUE);
                scriptVariables.put(FAILED_ATTR_NAME, FAILURE_VALUE);
                scriptVariables.put(HTTP_CLIENT_VARIABLE_NAME, httpClient);
                scriptVariables.put(HTTP_CLIENT_REQUEST_VARIABLE_NAME, httpClientRequest);
                scriptVariables.put(IDENTITY_REPOSITORY, identityRepository);

                try {
                    scriptEvaluator.evaluateScript(serverSideScript, scriptVariables);
                } catch (ScriptException e) {
                    DEBUG.message("Error running server side scripts", e);
                    throw new AuthLoginException("Error running script");
                }
                
                state = ((Number) scriptVariables.get(STATE_VARIABLE_NAME)).intValue();
                userName = (String) scriptVariables.get(USERNAME_VARIABLE_NAME);

                if (state != SUCCESS_VALUE) {
                    throw new AuthLoginException("Authentication failed");
                }
                
                return state;
            default:
                throw new AuthLoginException("Invalid state");
        }

    }

    private ScriptObject getServerSideScript() {
        SupportedScriptingLanguage scriptType = getScriptType();
        String rawScript = getRawServerSideScript();

        return new ScriptObject(SCRIPT_NAME, rawScript, scriptType, null);
    }

    private ScriptEvaluator getScriptEvaluator() {
        return InjectorHolder.getInstance(StandardScriptEvaluator.class);
    }

    private HttpClient getHttpClient() {
       return httpClientFactory.createHttpClient();

    }

    private HttpClientRequest getHttpRequest() {
       return httpClientRequestFactory.createRequest();
    }

    private String getClientSideScript() {
        String clientSideScript = getConfigValue(CLIENT_SCRIPT_ATTR_NAME);

        if(clientSideScript == null) {
            clientSideScript = "";
        }

        return clientSideScript;
    }

    private String getRawServerSideScript() {
        String serverSideScript = getConfigValue(SERVER_SCRIPT_ATTRIBUTE_NAME);

        if (serverSideScript == null) {
            serverSideScript = "";
        }

        return serverSideScript;
    }

    private String getConfigValue(String attributeName) {
        return CollectionHelper.getMapAttr(moduleConfiguration, attributeName);
    }

    private ScriptHttpRequestWrapper getScriptHttpRequestWrapper() {
        return new ScriptHttpRequestWrapper(getHttpServletRequest());
    }

    private void substituteUIStrings() throws AuthLoginException {
        ScriptTextOutputCallback callback = new ScriptTextOutputCallback(clientSideScript);
        replaceCallback(STATE_RUN_SCRIPT, 0, callback);
    }

    private SupportedScriptingLanguage getScriptType() {
        String scriptTypeVariable = getConfigValue(SCRIPT_TYPE_ATTR_NAME);

        if (JAVA_SCRIPT_LABEL.equals(scriptTypeVariable)) {
            return SupportedScriptingLanguage.JAVASCRIPT;
        } else if (GROOVY_LABEL.equals(scriptTypeVariable)) {
            return SupportedScriptingLanguage.GROOVY;
        }

        return null;
    }

    private boolean getClientSideScriptEnabled() {
        String clientSideScriptEnabled = getConfigValue(CLIENT_SCRIPT_ENABLED_ATTR_NAME);
        return Boolean.parseBoolean(clientSideScriptEnabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Principal getPrincipal() {
        if(userName == null) {
            DEBUG.message("Warning: username is null");
        }

        return new ScriptedPrinciple(userName);
    }
}
