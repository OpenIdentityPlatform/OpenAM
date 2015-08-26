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
package org.forgerock.openam.entitlement.conditions.environment;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.utils.CollectionUtils.transformMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementConditionAdaptor;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.idm.IdRepoException;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.client.RestletHttpClient;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.entitlement.PolicyConstants;
import org.forgerock.openam.scripting.ScriptConstants.ScriptContext;
import org.forgerock.openam.scripting.ScriptEvaluator;
import org.forgerock.openam.scripting.ScriptException;
import org.forgerock.openam.scripting.ScriptObject;
import org.forgerock.openam.scripting.SupportedScriptingLanguage;
import org.forgerock.openam.scripting.api.ScriptedIdentity;
import org.forgerock.openam.scripting.api.ScriptedSession;
import org.forgerock.openam.scripting.service.ScriptConfiguration;
import org.forgerock.openam.scripting.service.ScriptingServiceFactory;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import javax.security.auth.Subject;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Scripted condition to enable scripts to be evaluated during policy evaluation.
 *
 * @since 13.0.0
 */
public class ScriptCondition extends EntitlementConditionAdaptor {

    private static final ListToSetTransformation<String> LIST_TO_SET = new ListToSetTransformation<>();
    private static final String SCRIPT_ID = "scriptId";

    private final ScriptingServiceFactory scriptingServiceFactory;
    private final ScriptEvaluator evaluator;

    private final CoreWrapper coreWrapper;

    private String scriptId;

    public ScriptCondition() {
        scriptingServiceFactory = InjectorHolder.getInstance(
                Key.get(new TypeLiteral<ScriptingServiceFactory>() {
                }));
        evaluator = InjectorHolder.getInstance(
                Key.get(ScriptEvaluator.class, Names.named(ScriptContext.POLICY_CONDITION.name())));
        coreWrapper = InjectorHolder.getInstance(CoreWrapper.class);
    }

    @Override
    public void setState(String state) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(state);
            scriptId = node.get(SCRIPT_ID).asText();
        } catch (IOException ioE) {
            throw new IllegalStateException("Script condition is in an invalid state", ioE);
        }
    }

    @Override
    public String getState() {
        return JsonValue.json(
                object(
                        field(SCRIPT_ID, scriptId)
                )
        ).toString();
    }

    @Override
    public void validate() throws EntitlementException {
        if (StringUtils.isEmpty(scriptId)) {
            throw new EntitlementException(EntitlementException.PROPERTY_VALUE_NOT_DEFINED, SCRIPT_ID);
        }
    }

    @Override
    public ConditionDecision evaluate(String realm, Subject subject, String resourceName,
                                      Map<String, Set<String>> environment) throws EntitlementException {
        try {
            ScriptConfiguration configuration = getScriptConfiguration(realm);

            if (configuration == null) {
                throw new EntitlementException(EntitlementException.INVALID_SCRIPT_ID, scriptId);
            }

            ScriptObject script = new ScriptObject(
                    configuration.getName(), configuration.getScript(), configuration.getLanguage());

            Map<String, List<String>> advice = new HashMap<>();
            Map<String, List<String>> responseAttributes = new HashMap<>();

            Bindings scriptVariables = new SimpleBindings();
            scriptVariables.put("logger", PolicyConstants.DEBUG);
            scriptVariables.put("username", SubjectUtils.getPrincipalId(subject));
            scriptVariables.put("resourceURI", resourceName);
            scriptVariables.put("environment", environment);
            scriptVariables.put("advice", advice);
            scriptVariables.put("responseAttributes", responseAttributes);
            scriptVariables.put("httpClient", getHttpClient(configuration.getLanguage()));
            scriptVariables.put("authorized", Boolean.FALSE);
            scriptVariables.put("ttl", Long.MAX_VALUE);

            SSOToken ssoToken = SubjectUtils.getSSOToken(subject);

            if (ssoToken != null) {
                // If a token is present include the corresponding identity and session objects.
                scriptVariables.put("identity", new ScriptedIdentity(coreWrapper.getIdentity(ssoToken)));
                scriptVariables.put("session", new ScriptedSession(ssoToken));
            }

            evaluator.evaluateScript(script, scriptVariables);
            boolean authorized = (Boolean)scriptVariables.get("authorized");

            if (!authorized) {
                return ConditionDecision
                        .newFailureBuilder()
                        .setAdvice(transformMap(advice, LIST_TO_SET))
                        .setResponseAttributes(transformMap(responseAttributes, LIST_TO_SET))
                        .build();
            }

            long ttl = ((Number)scriptVariables.get("ttl")).longValue();
            return ConditionDecision
                    .newSuccessBuilder()
                    .setResponseAttributes(transformMap(responseAttributes, LIST_TO_SET))
                    .setTimeToLive(ttl)
                    .build();

        } catch (ScriptException | javax.script.ScriptException | IdRepoException | SSOException ex) {
            throw new EntitlementException(EntitlementException.CONDITION_EVALUATION_FAILED, ex);
        }
    }

    /**
     * Retrieve the script configuration from the scripting service.
     *
     * @param realm
     *         The realm in which to locate the configuration
     *
     * @return The script configuration or null if it was not found
     * @throws ScriptException if the operation was not successful
     */
    protected ScriptConfiguration getScriptConfiguration(String realm) throws ScriptException {
        return scriptingServiceFactory.create(SubjectUtils.createSuperAdminSubject(), realm).get(scriptId);
    }

    /**
     * Retrieves the http client for the supported language type.
     *
     * @param language
     *         the language type
     *
     * @return a http client
     */
    private RestletHttpClient getHttpClient(SupportedScriptingLanguage language) {
        Reject.ifNull(language);

        return InjectorHolder
                .getInstance(Key.get(RestletHttpClient.class, Names.named(language.name())));

    }

    /**
     * Gets the script Id.
     *
     * @return the script Id
     */
    public String getScriptId() {
        return scriptId;
    }

    /**
     * Sets the script Id.
     *
     * @param scriptId
     *         the script Id
     */
    public void setScriptId(String scriptId) {
        this.scriptId = scriptId;
    }

    // Simple function to transform a list into a set.
    private static final class ListToSetTransformation<T> implements Function<List<T>, Set<T>, NeverThrowsException> {

        @Override
        public Set<T> apply(List<T> list) {
            return new HashSet<>(list);
        }

    }

}
