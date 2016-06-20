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
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2014-2016 ForgeRock AS.
 */
package org.forgerock.openam.slf4j;

import static org.opends.messages.ExternalMessages.INFO_EXTERNAL_LIB_MESSAGE;

import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.i18n.slf4j.LocalizedMarker;
import org.opends.messages.Severity;
import org.opends.server.loggers.DebugLogger;
import org.opends.server.loggers.DebugTracer;
import org.opends.server.loggers.ErrorLogger;
import org.opends.server.loggers.LoggingCategoryNames;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;

/**
 * OpenDJ implementation of a SLF4J Logger.
 * <p>
 * Log calls at trace level are redirected to {@code DebugLogger}, while calls
 * at other levels are redirected to {@code ErrorLogger}.
 * <p>
 * Trace-level calls do no expect a Marker argument. The marker argument is ignored if provided.
 * <pre>
 * Example of expected trace-level call:
 *   logger.trace("This is an error message");
 * </pre>
 * <p>
 * Non trace-level calls expect a Marker argument that is an instance of
 * {@code LocalizedMarker}. This is the standard way for OpenDJ code.
 * <pre>
 * Example of expected non trace-level call:
 *   LocalizableMessage message = ...
 *   logger.error(new LocalizedMarker(message), message.toString(locale), t);
 * </pre>
 * <br>
 * However, to support logger calls from external libraries, calls without a Marker are supported,
 * by creating a raw LocalizableMessage on the fly.
 * <p>
 * Note that these methods are never called directly from OpenDJ.
 * Instead, OpenDJ code instantiates a LocalizedLogger
 * which then delegates to the underlying SLF4J logger.
 * <p>
 * For third party libraries (e.g commons-audit, grizzly),
 * messages with trace and debug level will end in the debug logger.
 * Messages with other levels will end in the error logger.
 */
final class OpenDJLoggerAdapter implements Logger {
    /** Name of logger, used as the category. */
    private final String name;

    /** The tracer associated to this logger. */
    private final DebugTracer tracer;

    /**
     * Creates a new logger with the provided name.
     *
     * @param name
     *            The name of logger.
     */
    public OpenDJLoggerAdapter(final String name) {
        // Tracer always use the provided name
        // which should be a classname
        this.tracer = DebugLogger.getTracer(name);
        // Name is simplified if possible
        this.name = LoggingCategoryNames.getCategoryName(name);
    }

    @Override
    public String getName() {
        return name;
    }

    /** Format a message containing '{}' as arguments placeholder. */
    private String formatMessage(String message, Object...args)
    {
      if (args == null || args.length == 0) {
          return message;
      }
      return MessageFormatter.arrayFormat(message, args).getMessage();
    }

    /** Trace with message only. */
    private void publishInDebugLogger(String msg) {
        tracer.trace(msg);
    }

    /** Trace with message and exception. */
    private void publishInDebugLogger(String message, Throwable t) {
        tracer.traceException(message, t);
    }

    /**
     * Log a message to {@code ErrorLogger} with the provided severity.
     * <p>
     * If this method is called from OpenDJ libraries,
     * extracting {code LocalizableMessage} from the provided {code Marker marker} argument,
     * otherwise log the provided {@param message}.
     *
     * @param marker
     *            The marker, expected to be an instance of
     *            {@code LocalizedMarker} class, from which message to log is
     *            extracted.
     * @param message
     *            The message to log if this logger is called by external libraries.
     * @param severity
     *            The severity to use when logging message.
     * @param throwable
     *            Exception to log. May be {@code null}.
     */
    private void publish(Marker marker, String message, Severity severity, Throwable throwable) {
        if (marker instanceof LocalizedMarker) {
            // OpenDJ logs with all severity levels but trace.
            publishInErrorLogger(((LocalizedMarker) marker).getMessage(), severity, throwable);
        } else if (severity == Severity.DEBUG) {
            // Third party messages with debug level go to the debug logger.
            publishInDebugLogger(message, throwable);
        } else {
            // Other Third party messages.
            publishInErrorLogger(message, severity, throwable);
        }
    }

    /**
     * Log a message to {@code ErrorLogger} with the provided message and severity.
     * <p>
     * This should be avoided, but when using an external library there can be calls
     * with a String.
     *
     * @param message
     *            The message as string.
     * @param severity
     *            The severity to use when logging message.
     * @param throwable
     *            Exception to log. May be {@code null}.
     */
    private void publishInErrorLogger(String message, Severity severity, Throwable throwable) {
      // Use a LocalizedMessage template instead of raw() to avoid null message resource name and ID in logs.
      publishInErrorLogger(INFO_EXTERNAL_LIB_MESSAGE.get(message), severity, throwable);
    }

    private void publishInErrorLogger(LocalizableMessage localizableMessage, Severity severity, Throwable throwable) {
        ErrorLogger.log(name, severity, localizableMessage, throwable);
    }

    @Override
    public boolean isTraceEnabled() {
        return DebugLogger.debugEnabled() && tracer.enabled();
    }

    @Override
    public void trace(String msg) {
        if (isTraceEnabled()) {
            publishInDebugLogger(msg);
        }
    }

    @Override
    public void trace(Marker marker, String msg) {
        if (isTraceEnabled()) {
            publishInDebugLogger(msg);
        }
    }

    @Override
    public void trace(String msg, Throwable t) {
        if (isTraceEnabled()) {
            publishInDebugLogger(msg, t);
        }
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        if (isTraceEnabled()) {
            publishInDebugLogger(msg, t);
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return ErrorLogger.isEnabledFor(name, Severity.INFORMATION);
    }

    @Override
    public void debug(Marker marker, String msg) {
        if (isDebugEnabled()) {
            publish(marker, msg, Severity.INFORMATION, null);
        }
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        if (isDebugEnabled()) {
            publish(marker, msg, Severity.INFORMATION, t);
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return ErrorLogger.isEnabledFor(name, Severity.NOTICE);
    }

    @Override
    public void info(Marker marker, String msg) {
        if (isInfoEnabled()) {
            publish(marker, msg, Severity.NOTICE, null);
        }
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        if (isInfoEnabled()) {
            publish(marker, msg, Severity.NOTICE, t);
        }
    }

    @Override
    public boolean isWarnEnabled() {
        return ErrorLogger.isEnabledFor(name, Severity.WARNING);
    }

    @Override
    public void warn(Marker marker, String msg) {
        if (isWarnEnabled()) {
            publish(marker, msg, Severity.WARNING, null);
        }
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        if (isWarnEnabled()) {
            publish(marker, msg, Severity.WARNING, t);
        }
    }

    @Override
    public boolean isErrorEnabled() {
        return ErrorLogger.isEnabledFor(name, Severity.ERROR);
    }

    @Override
    public void error(Marker marker, String msg) {
        if (isErrorEnabled()) {
            publish(marker, msg, Severity.ERROR, null);
        }
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        if (isErrorEnabled()) {
            publish(marker, msg, Severity.ERROR, t);
        }
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return isTraceEnabled();
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return isDebugEnabled();
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return isWarnEnabled();
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return isErrorEnabled();
    }

    @Override
    public void trace(String message, Object arg) {
        if (isTraceEnabled()) {
            publishInDebugLogger(formatMessage(message, arg));
        }
    }

    @Override
    public void trace(String message, Object arg1, Object arg2) {
        if (isTraceEnabled()) {
            publishInDebugLogger(formatMessage(message, arg1, arg2));
        }
    }

    @Override
    public void trace(String message, Object... argArray) {
        if (isTraceEnabled()) {
            publishInDebugLogger(formatMessage(message, argArray));
        }
    }

    @Override
    public void trace(Marker marker, String message, Object arg) {
        if (isTraceEnabled()) {
            publishInDebugLogger(formatMessage(message, arg));
        }
    }

    @Override
    public void trace(Marker marker, String message, Object arg1, Object arg2) {
        if (isTraceEnabled()) {
            publishInDebugLogger(formatMessage(message, arg1, arg2));
        }
    }

    @Override
    public void trace(Marker marker, String message, Object... argArray) {
        if (isTraceEnabled()) {
            publishInDebugLogger(formatMessage(message, argArray));
        }
    }

    // Methods below should only by used by third-party libraries.
    // Such third-party libraries include, but are not limited to: commons-audit, grizzly, etc.

    @Override
    public void debug(String msg, Throwable t) {
        if (isDebugEnabled()) {
            publishInDebugLogger(formatMessage(msg, (Object[]) null), t);
        }
    }

    @Override
    public void debug(String msg) {
        if (isDebugEnabled()) {
            publishInDebugLogger(formatMessage(msg, (Object[]) null), null);
        }
    }

    @Override
    public void debug(String message, Object arg) {
        if (isDebugEnabled()) {
            publishInDebugLogger(formatMessage(message, arg), null);
        }
    }

    @Override
    public void debug(String message, Object arg1, Object arg2) {
        if (isDebugEnabled()) {
            publishInDebugLogger(formatMessage(message, arg1, arg2), null);
        }
    }

    @Override
    public void debug(String message, Object... argArray) {
        if (isDebugEnabled()) {
            publishInDebugLogger(formatMessage(message, argArray), null);
        }
    }

    @Override
    public void debug(Marker marker, String message, Object arg) {
        if (isDebugEnabled()) {
            publishInDebugLogger(formatMessage(message, arg), null);
        }
    }

    @Override
    public void debug(Marker marker, String message, Object arg1, Object arg2) {
        if (isDebugEnabled()) {
            publishInDebugLogger(formatMessage(message, arg1, arg2), null);
        }
    }

    @Override
    public void debug(Marker marker, String message, Object... arguments) {
        if (isDebugEnabled()) {
            publishInDebugLogger(formatMessage(message, arguments), null);
        }
    }

    @Override
    public void info(String msg, Throwable t) {
        if (isInfoEnabled()) {
            publishInErrorLogger(formatMessage(msg, (Object[]) null), Severity.INFORMATION, t);
        }
    }

    @Override
    public void info(String msg) {
        if (isInfoEnabled()) {
            publishInErrorLogger(formatMessage(msg, (Object[]) null), Severity.INFORMATION, null);
        }
    }

    @Override
    public void info(String message, Object arg) {
        if (isInfoEnabled()) {
            publishInErrorLogger(formatMessage(message, arg), Severity.INFORMATION, null);
        }
    }

    @Override
    public void info(String message, Object arg1, Object arg2) {
        if (isInfoEnabled()) {
            publishInErrorLogger(formatMessage(message, arg1, arg2), Severity.INFORMATION, null);
        }
    }

    @Override
    public void info(String message, Object... argArray) {
        if (isInfoEnabled()) {
            publishInErrorLogger(formatMessage(message, argArray), Severity.INFORMATION, null);
        }
    }

    @Override
    public void info(Marker marker, String message, Object arg) {
        if (isInfoEnabled()) {
            publishInErrorLogger(formatMessage(message, arg), Severity.INFORMATION, null);
        }
    }

    @Override
    public void info(Marker marker, String message, Object arg1, Object arg2) {
        if (isInfoEnabled()) {
            publishInErrorLogger(formatMessage(message, arg1, arg2), Severity.INFORMATION, null);
        }
    }

    @Override
    public void info(Marker marker, String message, Object... arguments) {
        if (isInfoEnabled()) {
            publishInErrorLogger(formatMessage(message, arguments), Severity.INFORMATION, null);
        }
    }

    @Override
    public void warn(String msg, Throwable t) {
        if (isWarnEnabled()) {
            publishInErrorLogger(formatMessage(msg, (Object[]) null), Severity.WARNING, t);
        }
    }

    @Override
    public void warn(String msg) {
        if (isWarnEnabled()) {
            publishInErrorLogger(formatMessage(msg, (Object[]) null), Severity.WARNING, null);
        }
    }

    @Override
    public void warn(String message, Object arg) {
        if (isWarnEnabled()) {
            publishInErrorLogger(formatMessage(message, arg), Severity.WARNING, null);
        }
    }

    @Override
    public void warn(String message, Object arg1, Object arg2) {
        if (isWarnEnabled()) {
            publishInErrorLogger(formatMessage(message, arg1, arg2), Severity.WARNING, null);
        }
    }

    @Override
    public void warn(String message, Object... argArray) {
        if (isWarnEnabled()) {
            publishInErrorLogger(formatMessage(message, argArray), Severity.WARNING, null);
        }
    }

    @Override
    public void warn(Marker marker, String message, Object arg) {
        if (isWarnEnabled()) {
            publishInErrorLogger(formatMessage(message, arg), Severity.WARNING, null);
        }
    }

    @Override
    public void warn(Marker marker, String message, Object arg1, Object arg2) {
        if (isWarnEnabled()) {
            publishInErrorLogger(formatMessage(message, arg1, arg2), Severity.WARNING, null);
        }
    }

    @Override
    public void warn(Marker marker, String message, Object... arguments) {
        if (isWarnEnabled()) {
            publishInErrorLogger(formatMessage(message, arguments), Severity.WARNING, null);
        }
    }

    @Override
    public void error(String msg, Throwable t) {
        if (isErrorEnabled()) {
            publishInErrorLogger(formatMessage(msg, (Object[]) null), Severity.ERROR, t);
        }
    }

    @Override
    public void error(String msg) {
        if (isErrorEnabled()) {
            publishInErrorLogger(formatMessage(msg, Severity.ERROR, null), Severity.ERROR, null);
        }
    }

    @Override
    public void error(String message, Object arg) {
        if (isErrorEnabled()) {
            publishInErrorLogger(formatMessage(message, arg), Severity.ERROR, null);
        }
    }

    @Override
    public void error(String message, Object arg1, Object arg2) {
        if (isErrorEnabled()) {
            publishInErrorLogger(formatMessage(message, arg1, arg2), Severity.ERROR, null);
        }
    }

    @Override
    public void error(String message, Object... arguments) {
        if (isErrorEnabled()) {
            publishInErrorLogger(formatMessage(message, arguments), Severity.ERROR, null);
        }
    }

    @Override
    public void error(Marker marker, String message, Object arg) {
        if (isErrorEnabled()) {
            publishInErrorLogger(formatMessage(message, arg), Severity.ERROR, null);
        }
    }

    @Override
    public void error(Marker marker, String message, Object arg1, Object arg2) {
        if (isErrorEnabled()) {
            publishInErrorLogger(formatMessage(message, arg1, arg2), Severity.ERROR, null);
        }
    }

    @Override
    public void error(Marker marker, String message, Object... arguments) {
        if (isErrorEnabled()) {
            publishInErrorLogger(formatMessage(message, arguments), Severity.ERROR, null);
        }
    }
}
