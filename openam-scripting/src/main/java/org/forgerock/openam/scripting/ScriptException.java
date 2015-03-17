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
package org.forgerock.openam.scripting;

import static org.forgerock.openam.scripting.ScriptConstants.*;

import com.sun.identity.shared.locale.L10NMessageImpl;
import org.slf4j.Logger;

/**
 * General script exception that allows for localised messages.
 *
 * @since 13.0.0
 */
public class ScriptException extends L10NMessageImpl {

    private final ScriptErrorCode scriptErrorCode;

    /**
     * Construct a {@code ScriptException}.
     * @param errorCode The error code to use for the exception message.
     * @param cause The {@code Throwable} that caused the error.
     * @param arguments Arguments used in the message associated with the error code.
     */
    public ScriptException(ScriptErrorCode errorCode, Throwable cause, String... arguments) {
        super(RESOURCE_BUNDLE, errorCode.getCode(), arguments);
        initCause(cause);
        this.scriptErrorCode = errorCode;
    }

    /**
     * Construct a {@code ScriptException}.
     * @param errorCode The error code to use for the exception message.
     * @param arguments Arguments used in the message associated with the error code.
     */
    public ScriptException(ScriptErrorCode errorCode, String... arguments) {
        this(errorCode, null, arguments);
    }

    /**
     * Get the {@code ScriptErrorCode} that describes this error.
     * @return the script error code.
     */
    public ScriptErrorCode getScriptErrorCode() {
        return scriptErrorCode;
    }

    /**
     * Convenience method that will log the message represented by the error code and
     * construct a new {@code ScriptException}.
     * @param logger The logger to which the error should be written.
     * @param errorCode The error code to use for the exception message.
     * @param cause The {@code Throwable} that caused the error.
     * @param arguments Arguments used in the message associated with the error code.
     * @return A new {@code ScriptException}.
     */
    public static ScriptException createAndLogError(Logger logger, ScriptErrorCode errorCode, Throwable cause,
                                                    String... arguments) {
        final ScriptException exception = new ScriptException(errorCode, cause, arguments);
        logger.error(exception.getMessage(), cause);
        return exception;
    }

    /**
     * Convenience method that will log the message represented by the error code and
     * construct a new {@code ScriptException}.
     * @param logger The logger to which the error should be written.
     * @param errorCode The error code to use for the exception message.
     * @param cause The {@code Throwable} that caused the error.
     * @param arguments Arguments used in the message associated with the error code.
     * @return A new {@code ScriptException}.
     */
    public static ScriptException createAndLogDebug(Logger logger, ScriptErrorCode errorCode, Throwable cause,
                                                    String... arguments) {
        final ScriptException exception = new ScriptException(errorCode, cause, arguments);
        logger.debug(exception.getMessage(), cause);
        return exception;
    }

    /**
     * Convenience method that will log the message represented by the error code and
     * construct a new {@code ScriptException}.
     * @param logger The logger to which the error should be written.
     * @param errorCode The error code to use for the exception message.
     * @param arguments Arguments used in the message associated with the error code.
     * @return A new {@code ScriptException}.
     */
    public static ScriptException createAndLogError(Logger logger, ScriptErrorCode errorCode, String... arguments) {
        final ScriptException exception = new ScriptException(errorCode, arguments);
        logger.error(exception.getMessage());
        return exception;
    }

    /**
     * Convenience method that will log the message represented by the error code and
     * construct a new {@code ScriptException}.
     * @param logger The logger to which the error should be written.
     * @param errorCode The error code to use for the exception message.
     * @param arguments Arguments used in the message associated with the error code.
     * @return A new {@code ScriptException}.
     */
    public static ScriptException createAndLogDebug(Logger logger, ScriptErrorCode errorCode, String... arguments) {
        final ScriptException exception = new ScriptException(errorCode, arguments);
        logger.debug(exception.getMessage());
        return exception;
    }
}
