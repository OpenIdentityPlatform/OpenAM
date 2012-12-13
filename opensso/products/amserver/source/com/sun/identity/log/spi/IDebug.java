/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: IDebug.java,v 1.3 2008/06/25 05:43:40 qcheng Exp $
 *
 */

package com.sun.identity.log.spi;

/**
 * A DebugInterface class is necessary because different clients
 * have a requirement to direct their debug outputs to different
 * destinations.  DSAME write to debug files.
 */

public interface IDebug {
    /**
     * Integer constant represent MESSAGE log level.
     */
    public static final int MESSAGE = 0;
    /**
     * Integer constant represent WARNING log level.
     */
    public static final int WARNING = 1;
    /**
     * Integer constant represent ERROR log level.
     */
    public static final int ERROR = 2;

    /**
     * The method which performs the actual debug operation.
     *
     * @param level The level of the debug message.
     * @param msg The message string, which should be i18n-ed here.
     * @param e The exception whose stacktrace is required.
     */
    public void debug(int level, String msg, Throwable e);

    /**
     * The method which performs the actual debug operation.
     *
     * @param level The level of the debug message.
     * @param msg The message string, which should be i18n-ed here.
     */
    public void debug (int level, String msg);

    /**
     * Check whether debug message level is set
     *
     * @return true if debug level is set to "message".
     */
    public boolean messageEnabled();

    /**
     * Check whether debug warning level is set
     *
     * @return true if debug level is set to "warning".
     */
    public boolean warningEnabled();
}
