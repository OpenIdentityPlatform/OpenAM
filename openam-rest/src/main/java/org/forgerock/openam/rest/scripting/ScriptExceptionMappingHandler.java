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
package org.forgerock.openam.rest.scripting;

import static org.forgerock.json.resource.ResourceException.BAD_REQUEST;
import static org.forgerock.json.resource.ResourceException.CONFLICT;
import static org.forgerock.json.resource.ResourceException.INTERNAL_ERROR;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptErrorCode;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptErrorCode.*;

import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.errors.ExceptionMappingHandler;
import org.forgerock.openam.forgerockrest.utils.ServerContextUtils;
import org.forgerock.openam.scripting.ScriptException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Maps {@code ScriptException} to {@code ResourceException}.
 *
 * @since 13.0.0
 */
public class ScriptExceptionMappingHandler implements ExceptionMappingHandler<ScriptException, ResourceException> {

    private static final Map<ScriptErrorCode, Integer> ERROR_CODE_MAP = new HashMap<ScriptErrorCode, Integer>();

    @Override
    public ResourceException handleError(Context context, String debug, Request request, ScriptException error) {
        return ResourceException.getException(getResourceErrorCode(error.getScriptErrorCode()),
                getLocalizedMessage(context, error), error);
    }

    @Override
    public ResourceException handleError(String debug, Request request, ScriptException error) {
        return handleError(error);
    }

    @Override
    public ResourceException handleError(Context context, Request request, ScriptException error) {
        return ResourceException.getException(getResourceErrorCode(error.getScriptErrorCode()),
                getLocalizedMessage(context, error), error);
    }

    @Override
    public ResourceException handleError(Request request, ScriptException error) {
        return handleError(error);
    }

    @Override
    public ResourceException handleError(ScriptException error) {
        return ResourceException.getException(getResourceErrorCode(error.getScriptErrorCode()), error.getMessage(),
                error);
    }

    /**
     * Get the localized message for the specified script exception.
     * @param context The server context from which the language header can be read.
     * @param exception The exception that contains the message.
     * @return The localized message.
     */
    private String getLocalizedMessage(Context context, ScriptException exception) {
        final Locale local = ServerContextUtils.getLocaleFromContext(context);

        if (local == null) {
            return exception.getMessage();
        } else {
            return exception.getL10NMessage(local);
        }
    }

    /**
     * Get the HTTP status code that is associated with the given script error code.
     * @param scriptErrorCode The associated script error code.
     * @return The HTTP status code.
     */
    private int getResourceErrorCode(ScriptErrorCode scriptErrorCode) {
        if (ERROR_CODE_MAP.isEmpty()) {
            ERROR_CODE_MAP.put(CONTEXT_NOT_RECOGNISED, BAD_REQUEST);
            ERROR_CODE_MAP.put(LANGUAGE_NOT_SUPPORTED, BAD_REQUEST);
            ERROR_CODE_MAP.put(FIND_BY_NAME_FAILED, INTERNAL_ERROR);
            ERROR_CODE_MAP.put(FIND_BY_UUID_FAILED, INTERNAL_ERROR);
            ERROR_CODE_MAP.put(DELETE_FAILED, INTERNAL_ERROR);
            ERROR_CODE_MAP.put(RETRIEVE_FAILED, INTERNAL_ERROR);
            ERROR_CODE_MAP.put(RETRIEVE_ALL_FAILED, INTERNAL_ERROR);
            ERROR_CODE_MAP.put(SAVE_FAILED, INTERNAL_ERROR);
            ERROR_CODE_MAP.put(MISSING_SCRIPT_UUID, BAD_REQUEST);
            ERROR_CODE_MAP.put(MISSING_SCRIPT_NAME, BAD_REQUEST);
            ERROR_CODE_MAP.put(MISSING_SCRIPT, BAD_REQUEST);
            ERROR_CODE_MAP.put(MISSING_SCRIPTING_LANGUAGE, BAD_REQUEST);
            ERROR_CODE_MAP.put(MISSING_SCRIPT_CONTEXT, BAD_REQUEST);
            ERROR_CODE_MAP.put(SCRIPT_NAME_EXISTS, CONFLICT);
            ERROR_CODE_MAP.put(SCRIPT_UUID_EXISTS, INTERNAL_ERROR);
            ERROR_CODE_MAP.put(SCRIPT_UUID_NOT_FOUND, BAD_REQUEST);
            ERROR_CODE_MAP.put(FILTER_BOOLEAN_LITERAL_FALSE, BAD_REQUEST);
            ERROR_CODE_MAP.put(FILTER_EXTENDED_MATCH, BAD_REQUEST);
            ERROR_CODE_MAP.put(FILTER_GREATER_THAN, BAD_REQUEST);
            ERROR_CODE_MAP.put(FILTER_GREATER_THAN_OR_EQUAL, BAD_REQUEST);
            ERROR_CODE_MAP.put(FILTER_LESS_THAN, BAD_REQUEST);
            ERROR_CODE_MAP.put(FILTER_LESS_THAN_OR_EQUAL, BAD_REQUEST);
            ERROR_CODE_MAP.put(FILTER_NOT, BAD_REQUEST);
            ERROR_CODE_MAP.put(FILTER_PRESENT, BAD_REQUEST);
            ERROR_CODE_MAP.put(SCRIPT_ENCODING_FAILED, INTERNAL_ERROR);
            ERROR_CODE_MAP.put(RESOURCE_FILTER_NOT_SUPPORTED, BAD_REQUEST);
        }
        return ERROR_CODE_MAP.containsKey(scriptErrorCode) ? ERROR_CODE_MAP.get(scriptErrorCode) : INTERNAL_ERROR;
    }
}
