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
 * Copyright 2014 ForgeRock, AS.
 */

package org.forgerock.openam.forgerockrest.entitlements;

import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.shared.debug.Debug;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.exc.UnrecognizedPropertyException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.forgerockrest.entitlements.model.json.JsonDecision;
import org.forgerock.openam.forgerockrest.entitlements.model.json.JsonEntitlement;
import org.forgerock.openam.forgerockrest.entitlements.model.json.JsonEntitlementConditionModule;
import org.forgerock.openam.forgerockrest.entitlements.model.json.JsonPolicy;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.util.promise.Function;
import org.forgerock.util.promise.NeverThrowsException;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Parses entitlements policies ("privileges") to/from JSON representations.
 *
 * @since 12.0.0
 */
public final class JsonPolicyParser implements PolicyParser {

    private static final Debug DEBUG = Debug.getInstance(JsonPolicyParser.class.getName());

    /**
     * Jackson JSON ObjectMapper instance for parsing/serialising policies to/from JSON. We using a static final
     * instance of this as per Jackson best practices, because internally it creates lots of expensive state such as
     * caches etc. We configure the mapper with our entitlement module, which adds mixins to customise JSON format
     * for various entitlements classes.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper().withModule(new JsonEntitlementConditionModule());

    private static final EntitlementToDecisionMapper ENTITLEMENT_TO_DECISION = new EntitlementToDecisionMapper();

    static {
        // Note: while SimpleDateFormat is not thread-safe, ObjectMapper does the right thing and clones it before use.
        // OPENAM-4757: Include milliseconds in the time output
        DateFormat iso8601FormatWithMilliseconds = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");
        iso8601FormatWithMilliseconds.setTimeZone(TimeZone.getTimeZone("UTC"));
        MAPPER.setDateFormat(iso8601FormatWithMilliseconds);
        // Exclude null fields from serialisation (e.g., condition names etc).
        MAPPER.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
    }

    @Override
    public Privilege parsePolicy(String name, JsonValue json)
            throws EntitlementException {

        if (json == null || json.isNull()) {
            throw new EntitlementException(EntitlementException.INVALID_JSON);
        }

        return parsePrivilege(name, json);
    }

    @Override
    public JsonValue printPolicy(Privilege policy) throws EntitlementException {
        JsonPolicy jsonPolicy = new JsonPolicy(policy);
        try {
            String json = MAPPER.writeValueAsString(jsonPolicy);
            return JsonValueBuilder.toJsonValue(json);
        } catch (IOException ex) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Unable to serialise policy to JSON", ex);
            }
            throw new EntitlementException(EntitlementException.UNABLE_TO_SERIALIZE_OBJECT);
        }
    }

    @Override
    public JsonValue printEntitlement(Entitlement entitlement) throws EntitlementException {
        JsonEntitlement jsonEntitlement = new JsonEntitlement(entitlement);
        try {
            String json = MAPPER.writeValueAsString(jsonEntitlement);
            return JsonValueBuilder.toJsonValue(json);
        } catch (IOException ex) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Unable to serialise entitlement to JSON", ex);
            }
            throw new EntitlementException(EntitlementException.UNABLE_TO_SERIALIZE_OBJECT);
        }
    }

    @Override
    public JsonValue printEntitlements(final List<Entitlement> entitlements) throws EntitlementException {
        final List<JsonDecision> jsonDecisions = CollectionUtils.transformList(entitlements, ENTITLEMENT_TO_DECISION);

        try {
            final String json = MAPPER.writeValueAsString(jsonDecisions);
            return JsonValueBuilder.toJsonArray(json);
        } catch (IOException ioE) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Unable to serialise entitlement to json", ioE);
            }
            throw new EntitlementException(EntitlementException.UNABLE_TO_SERIALIZE_OBJECT);
        }
    }

    private Privilege parsePrivilege(String providedName, JsonValue jsonValue) throws EntitlementException {
        try {
            // Note: this is a bit ugly as we re-serialise the JsonValue back into a JSON String to then parse it
            // again using Jackson. Unfortunately, that appears to be the easiest way as JsonValue does not support
            // data binding.
            JsonPolicy policy = MAPPER.readValue(jsonValue.toString(), JsonPolicy.class);
            Privilege privilege = policy.asPrivilege();

            if (isBlank(privilege.getName())) {
                privilege.setName(providedName);
            }

            if (isBlank(privilege.getName())) {
                throw new EntitlementException(EntitlementException.MISSING_PRIVILEGE_NAME);
            }

            // Validate the condition if present
            if (privilege.getCondition() != null) {
                privilege.getCondition().validate();
            }

            return privilege;
        } catch (UnrecognizedPropertyException ex) {
            throw new EntitlementException(EntitlementException.INVALID_VALUE,
                    new Object[] { ex.getUnrecognizedPropertyName() });
        } catch (JsonMappingException ex) {
            throw new EntitlementException(EntitlementException.INVALID_JSON, ex);
        } catch (IOException e) {
            throw new EntitlementException(EntitlementException.UNABLE_TO_CREATE_POLICY, e);
        }
    }

    /**
     * Mapping function that maps an entitlement to a json decision.
     */
    private static final class EntitlementToDecisionMapper
            implements Function<Entitlement, JsonDecision, NeverThrowsException> {

        @Override
        public JsonDecision apply(final Entitlement entitlement) {
            return new JsonDecision(entitlement);
        }

    }
}
