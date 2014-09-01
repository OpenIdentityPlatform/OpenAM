/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
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
 * "Portions Copyrighted [2012] [Forgerock Inc]"
 */

package org.forgerock.restlet.ext.openam.logging;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;


/**
 * An OpenAMLogger does ...
 * 
 * @author Laszlo Hordos
 */
public class OpenAMLogger extends Logger {

    /**
     * The wrapped OpenAM logger.
     */
    private com.sun.identity.shared.debug.Debug eventDebug;

    /**
     * Constructor.
     * 
     * @param debug
     *            The OpenAM logger to wrap.
     */
    public OpenAMLogger(com.sun.identity.shared.debug.Debug debug) {
        super(debug.getName(), null);
        this.eventDebug = debug;
    }

    /**
     * Constructor.
     * 
     * @param name
     *            The logger name.
     * @param resourceBundleName
     *            The optional resource bundle name.
     */
    protected OpenAMLogger(String name, String resourceBundleName) {
        super(name, resourceBundleName);
    }

    /**
     * Logs a configuration message. By default, it invokes
     * {@link com.sun.identity.shared.debug.Debug#message(String)}.
     * 
     * @param msg
     *            The message to log.
     */
    @Override
    public void config(String msg) {
        getDebugLogger().message(msg);
    }

    /**
     * Logs a fine trace. By default, it invokes
     * {@link com.sun.identity.shared.debug.Debug#message(String)}.
     * 
     * @param msg
     *            The message to log.
     */
    @Override
    public void fine(String msg) {
        getDebugLogger().message(msg);
    }

    /**
     * Logs a finer trace. By default, it invokes
     * {@link com.sun.identity.shared.debug.Debug#message(String)}.
     * 
     * @param msg
     *            The message to log.
     */
    @Override
    public void finer(String msg) {
        getDebugLogger().message(msg);
    }

    /**
     * Logs a finest trace. By default, it invokes
     * {@link com.sun.identity.shared.debug.Debug#message(String)}.
     * 
     * @param msg
     *            The message to log.
     */
    @Override
    public void finest(String msg) {
        getDebugLogger().message(msg);
    }

    /**
     * Returns the wrapped OpenAM logger.
     * 
     * @return The wrapped OpenAM logger.
     */
    public com.sun.identity.shared.debug.Debug getDebugLogger() {
        return eventDebug;
    }

    /**
     * Logs an info message. By default, it invokes
     * {@link com.sun.identity.shared.debug.Debug#message(String)}.
     * 
     * @param msg
     *            The message to log.
     */
    @Override
    public void info(String msg) {
        getDebugLogger().message(msg);
    }

    @Override
    public boolean isLoggable(Level level) {
        if (Level.ALL == level) {
            return true;
        } else if (Level.CONFIG == level) {
            return getDebugLogger().messageEnabled();
        } else if (Level.FINE == level) {
            return getDebugLogger().messageEnabled();
        } else if (Level.FINER == level) {
            return getDebugLogger().messageEnabled();
        } else if (Level.FINEST == level) {
            return getDebugLogger().messageEnabled();
        } else if (Level.INFO == level) {
            return getDebugLogger().messageEnabled();
        } else if (Level.OFF == level) {
            return false;
        } else if (Level.SEVERE == level) {
            return getDebugLogger().errorEnabled();
        } else if (Level.WARNING == level) {
            return getDebugLogger().warningEnabled();
        } else {
            return false;
        }
    }

    @Override
    public void log(Level level, String msg) {
        if (Level.CONFIG == level) {
            getDebugLogger().message(msg);
        } else if (Level.FINE == level) {
            getDebugLogger().message(msg);
        } else if (Level.FINER == level) {
            getDebugLogger().message(msg);
        } else if (Level.FINEST == level) {
            getDebugLogger().message(msg);
        } else if (Level.INFO == level) {
            getDebugLogger().message(msg);
        } else if (Level.SEVERE == level) {
            getDebugLogger().error(msg);
        } else if (Level.WARNING == level) {
            getDebugLogger().warning(msg);
        }
    }

    @Override
    public void log(Level level, String msg, Object param) {
        if (Level.CONFIG == level) {
            getDebugLogger().message(msg.replace("{0}", param.toString()));
        } else if (Level.FINE == level) {
            getDebugLogger().message(msg.replace("{0}", param.toString()));
        } else if (Level.FINER == level) {
            getDebugLogger().message(msg.replace("{0}", param.toString()));
        } else if (Level.FINEST == level) {
            getDebugLogger().message(msg.replace("{0}", param.toString()));
        } else if (Level.INFO == level) {
            getDebugLogger().message(msg.replace("{0}", param.toString()));
        } else if (Level.SEVERE == level) {
            getDebugLogger().error(msg.replace("{0}", param.toString()));
        } else if (Level.WARNING == level) {
            getDebugLogger().warning(msg.replace("{0}", param.toString()));
        }
    }

    @Override
    public void log(Level level, String msg, Object[] params) {
        for (int i = 0; i < params.length; i++) {
            msg = msg.replace("{" + i + "}", params[i].toString());
        }
        if (Level.CONFIG == level) {
            getDebugLogger().message(msg);
        } else if (Level.FINE == level) {
            getDebugLogger().message(msg);
        } else if (Level.FINER == level) {
            getDebugLogger().message(msg);
        } else if (Level.FINEST == level) {
            getDebugLogger().message(msg);
        } else if (Level.INFO == level) {
            getDebugLogger().message(msg);
        } else if (Level.SEVERE == level) {
            getDebugLogger().error(msg);
        } else if (Level.WARNING == level) {
            getDebugLogger().warning(msg);
        }
    }

    @Override
    public void log(Level level, String msg, Throwable thrown) {
        if (Level.CONFIG == level) {
            getDebugLogger().message(msg, thrown);
        } else if (Level.FINE == level) {
            getDebugLogger().message(msg, thrown);
        } else if (Level.FINER == level) {
            getDebugLogger().message(msg, thrown);
        } else if (Level.FINEST == level) {
            getDebugLogger().message(msg, thrown);
        } else if (Level.INFO == level) {
            getDebugLogger().message(msg, thrown);
        } else if (Level.SEVERE == level) {
            getDebugLogger().error(msg, thrown);
        } else if (Level.WARNING == level) {
            getDebugLogger().warning(msg, thrown);
        }
    }

    @Override
    public void log(LogRecord record) {
        Level level = record.getLevel();
        String msg = record.getMessage();
        Object[] params = record.getParameters();
        Throwable thrown = record.getThrown();

        if (thrown != null) {
            log(level, msg, thrown);
        } else if (params != null) {
            log(level, msg, params);
        } else {
            log(level, msg);
        }
    }

    /**
     * Sets the wrapped OpenAM logger.
     * 
     * @param debug
     *            The wrapped OpenAM logger.
     */
    public void setDebugLogger(com.sun.identity.shared.debug.Debug debug) {
        this.eventDebug = debug;
    }

    /**
     * Logs a severe message. By default, it invokes
     * {@link com.sun.identity.shared.debug.Debug#error(String)}.
     * 
     * @param msg
     *            The message to log.
     */
    @Override
    public void severe(String msg) {
        getDebugLogger().error(msg);
    }

    /**
     * Logs a warning message. By default, it invokes
     * {@link com.sun.identity.shared.debug.Debug#warning(String)}.
     * 
     * @param msg
     *            The message to log.
     */
    @Override
    public void warning(String msg) {
        getDebugLogger().warning(msg);
    }

}
