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
package org.forgerock.openam.rest.query;

import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.shared.locale.L10NMessage;
import org.forgerock.util.Reject;

import java.text.MessageFormat;
import java.util.Locale;

/**
 * General query exception that allows for localised messages.
 *
 * @since 13.0.0
 */
public class QueryException extends RuntimeException implements L10NMessage {

    /**
     * Error messages are stored in the queryError.properties file to facilitate translation.
     * Each entry in this enum corresponds to a specific error message in the file.
     */
    public static enum QueryErrorCode {
        FILTER_BOOLEAN_LITERAL_FALSE,
        FILTER_EXTENDED_MATCH,
        FILTER_GREATER_THAN,
        FILTER_GREATER_THAN_OR_EQUAL,
        FILTER_LESS_THAN,
        FILTER_LESS_THAN_OR_EQUAL,
        FILTER_NOT,
        FILTER_PRESENT,
        FILTER_DEPTH_SUPPORTED;
    }

    private static final String RESOURCE_BUNDLE = "queryError";

    private final QueryErrorCode errorCode;
    private final String[] arguments;
    private final String message;

    /**
     * Construct a {@code QueryException}.
     * @param errorCode The error code to use for the exception message.
     * @param cause The {@code Throwable} that caused the error.
     * @param arguments Arguments used in the message associated with the error code.
     * @throws NullPointerException if errorCode is null
     */
    public QueryException(QueryErrorCode errorCode, Throwable cause, String... arguments) {
        Reject.ifNull(errorCode, "QueryErrorCode may not be null");
        this.errorCode = errorCode;
        this.arguments = arguments;
        this.message = translateMessage(Locale.ENGLISH, errorCode.name(), arguments);
        initCause(cause);
    }

    /**
     * Construct a {@code QueryException}.
     * @param errorCode The error code to use for the exception message.
     * @param arguments Arguments used in the message associated with the error code.
     */
    public QueryException(QueryErrorCode errorCode, String... arguments) {
        this(errorCode, null, arguments);
    }

    @Override
    public String getL10NMessage(Locale locale) {
        return locale == null ? message : translateMessage(locale, errorCode.name(), arguments);
    }

    private String translateMessage(Locale locale, String errorCode, String... arguments) {
        String result = AMResourceBundleCache.getInstance().getResBundle(RESOURCE_BUNDLE, locale).getString(errorCode);
        if (arguments == null || arguments.length == 0) {
            return result;
        } else {
            return MessageFormat.format(result, arguments);
        }
    }

    @Override
    public String getResourceBundleName() {
        return RESOURCE_BUNDLE;
    }

    @Override
    public String getErrorCode() {
        return errorCode.name();
    }

    @Override
    public Object[] getMessageArgs() {
        return arguments;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getLocalizedMessage() {
        return getL10NMessage(Locale.getDefault());
    }

}
