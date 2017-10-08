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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.openam.slf4j;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import org.slf4j.helpers.MarkerIgnoringBase;

/**
 * Delegate logging invocations to the AM Debug instance.
 */
public class AMDebugLogger extends MarkerIgnoringBase {
    private final Debug debug;

    public AMDebugLogger(Debug debug) {
        this.debug = debug;
    }

    public boolean isTraceEnabled() {
        return debug.messageEnabled() && SystemPropertiesManager.getAsBoolean(Constants.ENABLE_TRACE_IN_MESSAGE_MODE);
    }

    public void trace(String s) {
        debug.message(s);
    }

    public void trace(String s, Object o) {
        debug.message(s, o);
    }

    public void trace(String s, Object o, Object o2) {
        debug.message(s, o, o2);
    }

    public void trace(String s, Object... objects) {
        debug.message(s, objects);
    }

    public void trace(String s, Throwable throwable) {
        debug.message(s, throwable);
    }

    public boolean isDebugEnabled() {
        return debug.messageEnabled();
    }

    public void debug(String s) {
        debug.message(s);
    }

    public void debug(String s, Object o) {
        debug.message(s, o);
    }

    public void debug(String s, Object o, Object o2) {
        debug.message(s, o, o2);
    }

    public void debug(String s, Object... objects) {
        debug.message(s, objects);
    }

    public void debug(String s, Throwable throwable) {
        debug.message(s, throwable);
    }

    public boolean isInfoEnabled() {
        return debug.messageEnabled();
    }

    public void info(String s) {
        debug.message(s);
    }

    public void info(String s, Object o) {
        debug.message(s, o);
    }

    public void info(String s, Object o, Object o2) {
        debug.message(s, o, o2);
    }

    public void info(String s, Object... objects) {
        debug.message(s, objects);
    }

    public void info(String s, Throwable throwable) {
        debug.message(s, throwable);
    }

    public boolean isWarnEnabled() {
        return debug.warningEnabled();
    }

    public void warn(String s) {
        debug.warning(s);
    }

    public void warn(String s, Object o) {
        debug.warning(s, o);
    }

    public void warn(String s, Object... objects) {
        debug.warning(s, objects);
    }

    public void warn(String s, Object o, Object o2) {
        debug.warning(s, o, o2);
    }

    public void warn(String s, Throwable throwable) {
        debug.warning(s, throwable);
    }

    public boolean isErrorEnabled() {
        return debug.errorEnabled();
    }

    public void error(String s) {
        debug.error(s);
    }

    public void error(String s, Object o) {
        debug.error(s, o);
    }

    public void error(String s, Object o, Object o2) {
        debug.error(s, o, o2);
    }

    public void error(String s, Object... objects) {
        debug.error(s, objects);
    }

    public void error(String s, Throwable throwable) {
        debug.error(s, throwable);
    }
}
