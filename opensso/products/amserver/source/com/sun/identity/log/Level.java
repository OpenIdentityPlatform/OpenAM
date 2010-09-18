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
 * $Id: Level.java,v 1.3 2008/06/25 05:43:35 qcheng Exp $
 *
 */

package com.sun.identity.log;

/**
 * The extension to JDK1.4 Level class. This class adds the levels
 * needed by CMS.
 */
public class Level extends java.util.logging.Level {
    /**
     * Create a named Level with a given integer value.
     * <p>
     * Note that this constructor is "protected" to allow subclassing.
     * In general clients of logging should use one of the constant Level
     * objects such as SEVERE or FINEST.  However, if clients need to
     * add new logging levels, they may subclass Level and define new
     * constants.
     * @param name  the name of the Level, for example "SEVERE".
     * @param value an integer value for the level.
     */
    Level(String name, int value) {
        super(name, value);
    }
    /**
     * Marks a security related log entry.
     */
    public static final Level LL_SECURITY = new Level("LL_SECURITY",950);
    /**
     * Indicates that this log entry denotes a catastrophe.
     */
    public static final Level LL_CATASTRPHE = new Level("LL_CATASTRPHE",850);
    /**
     * Indicates that this log entry denotes a misconfiguration.
     */
    public static final Level LL_MISCONF = new Level("LL_MISCONF",750);
    /**
     * Indicates that this log entry denotes a failure.
     */
    public static final Level LL_FAILURE = new Level("LL_FAILURE",650);
    /**
     * Indicates that this log entry denotes a warning.
     */
    public static final Level LL_WARN = new Level("LL_WARN",550);
    /**
     * Indicates that this log entry denotes a information.
     */
    public static final Level LL_INFO = new Level("LL_INFO",450);
    /**
     * Indicates that this log entry denotes a debug.
     */
    public static final Level LL_DEBUG = new Level("LL_DEBUG",350);
    /**
     * Indicates that this log entry denotes a common event.
     */
    public static final Level LL_ALL = new Level("LL_ALL",250);
}

