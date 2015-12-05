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

package org.forgerock.openam.rest.audit;

import static org.forgerock.json.JsonValue.*;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.forgerock.audit.AuditException;
import org.forgerock.json.JsonValue;
import org.forgerock.util.Function;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.BufferingRepresentation;
import org.restlet.representation.Representation;

/**
 * Base auditor for extracting relevant information from request and response bodies
 */
public abstract class RestletBodyAuditor<T> implements Function<Representation, JsonValue, AuditException> {

    private final String[] fields;

    /**
     * Creates a body auditor with the specified fields
     * @param fields Fields to audit
     */
    RestletBodyAuditor(String... fields) {
        this.fields = fields;
    }

    JsonValue extractValues(T representation) throws AuditException {
        JsonValue result = json(object());
        for (String field : fields) {
            Object value = getValue(field, representation);
            if (value != null) {
                result.put(field, value);
            }
        }
        return result;
    }

    abstract Object getValue(String field, T representation) throws AuditException;

    /**
     * Create a body auditor for JSON bodies.
     * @param fields The fields that should be captured if they exist.
     * @return The auditor object.
     */
    public static RestletBodyAuditor jsonAuditor(String... fields) {
        return new RestletBodyAuditor<JSONObject>(fields) {
            @Override
            public JsonValue apply(Representation representation) throws AuditException {
                try {
                    boolean isBufferingRepresentation = (representation instanceof BufferingRepresentation);
                    boolean isEmptyBufferingRepresentation = isBufferingRepresentation
                            && ((BufferingRepresentation) representation).getWrappedRepresentation().isEmpty();
                    if(isEmptyBufferingRepresentation || (!isBufferingRepresentation && representation.isEmpty())) {
                        return json(object());
                    }
                    return extractValues(new JsonRepresentation(representation).getJsonObject());
                } catch (IOException | JSONException e) {
                    throw new AuditException("Could not parse body as JSON - wrong body auditor?", e);
                }
            }

            @Override
            Object getValue(String field, JSONObject object) throws AuditException {
                return object.opt(field);
            }
        };
    }

    /**
     * Create a body auditor for JSON bodies.
     * @param fields The fields that should be captured if they exist.
     * @return The auditor object.
     */
    public static RestletBodyAuditor jacksonAuditor(String... fields) {
        return new RestletBodyAuditor<Map<String, Object>>(fields) {
            @Override
            public JsonValue apply(Representation representation) throws AuditException {
                try {
                    if(!representation.isEmpty()
                            && ((JacksonRepresentation) representation).getObject() instanceof Map) {
                        return extractValues((Map<String, Object>) ((JacksonRepresentation) representation).getObject());
                    }
                    return json(object());
                } catch (IOException e) {
                    throw new AuditException("Could not parse body as JSON - wrong body auditor?", e);
                }
            }

            @Override
            Object getValue(String field, Map<String, Object> object) throws AuditException {
                return object.get(field);
            }
        };
    }

    /**
     * Create a body auditor for post request form bodies.
     * @param fields The fields that should be captured if they exist.
     * @return The auditor object.
     */
    public static RestletBodyAuditor formAuditor(String... fields) {
        return new RestletBodyAuditor<Form>(fields) {
            @Override
            public JsonValue apply(Representation representation) throws AuditException {
                return extractValues(new Form(representation));
            }

            @Override
            Object getValue(String field, Form representation) throws AuditException {
                Parameter parameter = representation.getFirst(field);
                return parameter == null ? null : parameter.getValue();
            }
        };
    }

    /**
     * The body auditor for when there is no auditing required.
     * @return {@code null}.
     */
    public static RestletBodyAuditor<?> noBodyAuditor() {
        return null;
    }

}
