/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock Inc. All rights reserved.
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
 * "Portions copyright [year] [name of copyright owner]"
 */

package org.forgerock.openam.ext.cts.repo;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ldap.CTSPersistentStore;
import com.sun.identity.sm.ldap.adapters.OAuthAdapter;
import com.sun.identity.sm.ldap.adapters.TokenAdapter;
import com.sun.identity.sm.ldap.api.fields.CoreTokenField;
import com.sun.identity.sm.ldap.api.fields.OAuthTokenField;
import com.sun.identity.sm.ldap.api.tokens.Token;
import com.sun.identity.sm.ldap.api.tokens.TokenIdFactory;
import com.sun.identity.sm.ldap.exceptions.CoreTokenException;
import com.sun.identity.sm.ldap.impl.QueryFilter;
import com.sun.identity.sm.ldap.utils.LDAPDataConversion;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.json.resource.SimpleJsonResource;
import org.forgerock.openam.guice.InjectorHolder;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.forgerock.opendj.ldap.Filter;
import org.restlet.Request;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OpenDJTokenRepo implements JsonResource {


    final static Debug debug = Debug.getInstance("CTS");

    private static final TokenIdFactory tokenIdFactory = InjectorHolder.getInstance(TokenIdFactory.class);
    private static final TokenAdapter<JsonValue> tokenAdapter = InjectorHolder.getInstance(OAuthAdapter.class);
    private static final CTSPersistentStore cts = InjectorHolder.getInstance(CTSPersistentStore.class);
    private static final OpenDJTokenRepo instance = new OpenDJTokenRepo();

    private static final LDAPDataConversion conversion = new LDAPDataConversion();
    private static final QueryFilter queryFilter = new QueryFilter(conversion);

    /**
     * Default Singleton Constructor.
     *
     */
    private OpenDJTokenRepo() {
    }

    /**
     * Obtain the Associated Implementation Instance currently available at runtime.
     *
     * @return OpenAMService - Obtain Instance of OpenAM Service Implementation.
     */
    public static OpenDJTokenRepo getInstance() {
        return instance;
    }

    /**
     * Handles a JSON resource request by dispatching to the method corresponding with the
     * method member of the request. If the request method is not one of the standard JSON
     * resource request methods, a {@code JsonResourceException} is thrown.
     * <p/>
     * This method catches any thrown {@code JsonValueException}, and rethrows it as a
     * {@link JsonResourceException#BAD_REQUEST}. This allows the use of JsonValue methods
     * to validate the content of the request.
     *
     * @param request the JSON resource request.
     * @return the JSON resource response.
     * @throws if there is an exception handling the request.
     */
    public JsonValue handle(JsonValue request) throws JsonResourceException {
        try {
            try {
                switch (request.get("method").required().asEnum(SimpleJsonResource.Method.class)) {
                    case create:
                        try{
                            request = validateTokenId(request);
                            cts.create(tokenAdapter.toToken(request));
                            if (OAuth2Utils.logStatus) {
                                String[] obs = {"CREATED_TOKEN", request.toString()};
                                OAuth2Utils.logAccessMessage("CREATED_TOKEN", obs, null);
                            }
                            return request;
                        } catch(CoreTokenException e){
                            OAuth2Utils.DEBUG.error("Create Token failed", e);
                            if (OAuth2Utils.logStatus) {
                                String[] obs = {"FAILED_CREATE_TOKEN", request.toString()};
                                OAuth2Utils.logErrorMessage("FAILED_CREATE_TOKEN", obs, null);
                            }
                            throw e;
                        }
                    case read:
                        String tokenId = tokenIdFactory.toOAuthTokenId(request);
                        Token token = cts.read(tokenId);
                        if (token != null) {
                            return tokenAdapter.fromToken(token);
                        } else {
                            return null;
                        }
                    case update:
                        cts.update(tokenAdapter.toToken(request));
                        return request;
                    case delete:
                        try{
                            cts.delete(tokenAdapter.toToken(request));
                            if (OAuth2Utils.logStatus) {
                                String[] obs = {"DELETED_TOKEN", request.toString()};
                                OAuth2Utils.logAccessMessage("DELETED_TOKEN", obs, null);
                            }
                            return request;
                        } catch(CoreTokenException e){
                            OAuth2Utils.DEBUG.error("Delete Token failed", e);
                            if (OAuth2Utils.logStatus) {
                                String[] obs = {"DELETE_FAILED", request.toString()};
                                OAuth2Utils.logErrorMessage("DELETE_FAILED", obs, null);
                            }
                            throw e;
                        }
                    case query:
                        Collection<Token> tokens = cts.list(convertRequest(request));
                        return convertResults(tokens);
                    default:
                        throw new JsonResourceException(JsonResourceException.BAD_REQUEST);
                }
            } catch (JsonValueException jve) {
                throw new JsonResourceException(JsonResourceException.BAD_REQUEST, jve);
            }
        } catch (Exception e1) {
            try {
                //onException(e1); // give handler opportunity to throw its own exception
                throw e1;
            } catch (Exception e2) {
                OAuth2Utils.DEBUG.error("OpenDJTokenRepo.handle(): ", e2);
                if (e2 instanceof OAuthProblemException) { // no rethrowing necessary
                    throw (OAuthProblemException) e2;
                } else { // need to rethrow as resource exception
                    throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(Request.getCurrent());
                }
            }
        }
    }

    /**
     * Parse the JsonValue and extract the query parameters from this.
     *
     * Note: We are assuming that values will correspond with the known attributes
     * of the OAuthTokenField enum. If they do not, the OAuthTokenField will throw
     * a runtime exception.
     *
     * @param request Non null JsonValue query request.
     * @return A Mapping of CoreTokenField to Objects to query by.
     */
    private Filter convertRequest(JsonValue request) {
        @SuppressWarnings("unchecked") // If this cast fails, it should be a runtime error.
        Map<String, Object> filters = (Map<String, Object>) request.get("params").required().asMap().get("filter");

        QueryFilter.QueryFilterBuilder builder = queryFilter.or();
        for (OAuthTokenField field : OAuthTokenField.values()) {
            builder.attribute(field.getField(), filters.get(field.getOAuthField()));
        }

        return builder.build();
    }

    /**
     * Internal conversion function to handle the CTSPersistentStore query result.
     * @param tokens A non null, but possibly empty collection of tokens.
     * @return The JsonValue expected by the caller.
     */
    private JsonValue convertResults(Collection<Token> tokens) {
        Set<Map<String, Set<String>>> results = new HashSet<Map<String, Set<String>>>();

        for (Token token : tokens) {
            results.add(convertToken(token));
        }

        return new JsonValue(results);
    }

    /**
     * Internal conversion function.
     *
     * @param token The token to convert.
     * @return A Token in String to Set of Strings representation.
     */
    private Map<String, Set<String>> convertToken(Token token) {
        Map<String, Set<String>> results = new HashMap<String, Set<String>>();

        for (CoreTokenField field : token.getAttributeNames()) {
            Set<String> values = new HashSet<String>();
            Object value = token.getValue(field);
            values.add(value.toString());
            results.put(field.toString(), values);
        }

        return results;
    }

    /**
     * Validates that the request contains an id value.
     *
     * @param request The request to validate.
     * @return The returned JsonValue will have an id value assigned.
     */
    private JsonValue validateTokenId(JsonValue request) {
        String id = tokenIdFactory.toOAuthTokenId(request);
        request.get(TokenIdFactory.ID).setObject(id);
        return request;
    }
}
