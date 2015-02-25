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
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.slf4j;

import com.sun.identity.shared.debug.Debug;
import org.slf4j.helpers.MarkerIgnoringBase;

/*
Delegate logging invocations to the AM Debug instance. Where more expensive MessageFormat formatting of to-be-logged
Strings would take place, insure that the corresponding level of logging is enabled before incurring this overhead.
 */
public class AMDebugLogger extends MarkerIgnoringBase {
    private final Debug debug;

    public AMDebugLogger(Debug debug) {
        this.debug = debug;
    }

    public boolean isTraceEnabled() {
        return debug.messageEnabled();
    }

    public void trace(String s) {
        debug.message(s);
    }

    public void trace(String s, Object o) {
        if (isTraceEnabled()) {
            debug.message(format(s, o));
        }
    }

    public void trace(String s, Object o, Object o2) {
        if (isTraceEnabled()) {
            debug.message(format(s, o, o2));
        }
    }

    public void trace(String s, Object... objects) {
        if (isTraceEnabled()) {
            debug.message(format(s, objects));
        }
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
        if (isDebugEnabled()) {
            debug.message(format(s, o));
        }
    }

    public void debug(String s, Object o, Object o2) {
        if (isDebugEnabled() ) {
            debug.message(format(s, o, o2));
        }
    }

    public void debug(String s, Object... objects) {
        if (isDebugEnabled()) {
            debug.message(format(s, objects));
        }
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
        if (isInfoEnabled()) {
            debug.message(format(s, o));
        }
    }

    public void info(String s, Object o, Object o2) {
        if (isInfoEnabled()) {
            debug.message(format(s, o, o2));
        }
    }

    public void info(String s, Object... objects) {
        if (isInfoEnabled()) {
            debug.message(format(s, objects));
        }
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
        if (isWarnEnabled()) {
            debug.warning(format(s, o));
        }
    }

    public void warn(String s, Object... objects) {
        if (isWarnEnabled()) {
            debug.warning(format(s, objects));
        }
    }

    public void warn(String s, Object o, Object o2) {
        if (isWarnEnabled()) {
            debug.warning(format(s, o, o2));
        }
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
        if (isErrorEnabled()) {
            debug.error(format(s, o));
        }
    }

    public void error(String s, Object o, Object o2) {
        if (isErrorEnabled()) {
            debug.error(format(s, o, o2));
        }
    }

    public void error(String s, Object... objects) {
        if (isErrorEnabled()) {
            debug.error(format(s, objects));
        }
    }

    public void error(String s, Throwable throwable) {
        debug.error(s, throwable);
    }

    private String format(String pattern, Object... objects) {
        try {
            return String.format(pattern, objects);
        } catch (IllegalArgumentException e) {
            debug.warning("The MessageFormat pattern passed to log method incorrect, or the parameters are of an unexpected type. " +
                    "Pattern of message which can't be logged: " + pattern + ";\t and the object params: " + objects);
        }
        return "";
    }
}
