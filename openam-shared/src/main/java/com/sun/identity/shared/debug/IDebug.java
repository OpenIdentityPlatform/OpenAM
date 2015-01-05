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
 * $Id: IDebug.java,v 1.4 2009/03/07 08:01:52 veiming Exp $
 *
 */

/**
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.shared.debug;

/**
 * <p>
 * Allows a pluggable implementation of the Debug service within the Access
 * Manager SDK. The implementation of this interface as well as the
 * <code>com.sun.identity.shared.debug.IDebugProvider</code> interface together
 * provide the necessary functionality to replace or enhance the Debug service.
 * </p>
 */
public interface IDebug {

    /**
     * @deprecated use {@link DebugLevel} instead.
     */
    @Deprecated
    public static final int OFF = DebugLevel.OFF.getLevel();

    /**
     * @deprecated use {@link DebugLevel} instead.
     */
    @Deprecated
    public static final int ERROR = DebugLevel.ERROR.getLevel();

    /**
     * @deprecated use {@link DebugLevel} instead.
     */
    @Deprecated
    public static final int WARNING = DebugLevel.WARNING.getLevel();

    /**
     * @deprecated use {@link DebugLevel} instead.
     */
    @Deprecated
    public static final int MESSAGE = DebugLevel.MESSAGE.getLevel();

    /**
     * @deprecated use {@link DebugLevel} instead.
     */
    @Deprecated
    public static final int ON = DebugLevel.ON.getLevel();

    /**
     * @deprecated use {@link DebugLevel} instead.
     */
    @Deprecated
    public static final String STR_OFF = DebugLevel.OFF.getName();

    /**
     * @deprecated use {@link DebugLevel} instead.
     */
    @Deprecated
    public static final String STR_ERROR = DebugLevel.ERROR.getName();

    /**
     * @deprecated use {@link DebugLevel} instead.
     */
    @Deprecated
    public static final String STR_WARNING = DebugLevel.WARNING.getName();

    /**
     * @deprecated use {@link DebugLevel} instead.
     */
    @Deprecated
    public static final String STR_MESSAGE = DebugLevel.MESSAGE.getName();

    /**
     * @deprecated use {@link DebugLevel} instead.
     */
    @Deprecated
    public static final String STR_ON = DebugLevel.ON.getName();

    /**
     * Returns the name of the IDebug instance. The value is exactly equal
     * to the one that was first used to create this instance.
     *
     * @return name of this <code>IDebug</code> instance
     */
    String getName();

    /**
     * Returns current debug level used by this instance. The value is an
     * integer equals to one of the various debug level integers as defined in
     * the class <code>com.iplanet.am.util.Debug</code>. This value could be
     * one of the followings:
     * <ul>
     * <li><code>OFF</code>
     * <li><code>ERROR</code>
     * <li><code>WARNING</code>
     * <li><code>MESSAGE</code>
     * <li><code>ON</code>
     * </ul>
     *
     * @return an integer indicating the debug level used by this instance.
     */
    public int getState();

    /**
     * Allows runtime modification of the debug level used by this instance. The
     * argument <code>level</code> must be an integer exactly equal to one of
     * the debug level integers as defined in the class
     * <code>com.iplanet.am.util.Debug</code>. This value could be one of the
     * following:<br>
     * <ul>
     * <li><code>OFF</code>
     * <li><code>ERROR</code>
     * <li><code>WARNING</code>
     * <li><code>MESSAGE</code>
     * <li><code>ON</code>
     * </ul>
     *
     * @param level An integer indicating the debug level to be used by this
     *              instance.
     */
    public void setDebug(int level);

    /**
     * Allows runtime modification of the backend used by this instance.
     * by resetting the debug instance to reinitialize itself.
     *
     * NB : The debug file associated with this debug won't be closed. Because of
     * merge, a debug file could be used by more than one debug instances.
     *
     * @param mf merge flag, on to create a single debug file
     */
    public void resetDebug(String mf);

    /**
     * Allows runtime modification of the debug level used by this instance. The
     * argument <code>level</code> must be a string which should exactly match
     * the string definitions of debug level as defined in the class
     * <code>com.iplanet.am.util.Debug</code>. This value could be one of the
     * following:
     * <ul>
     * <li><code>STR_OFF</code>
     * <li><code>STR_ERROR</code>
     * <li><code>STR_WARNING</code>
     * <li><code>STR_MESSAGE</code>
     * <li><code>STR_ON</code>
     * </ul>
     *
     * @param level String representing the debug level to be used by this
     *              instance.
     */
    public void setDebug(String level);

    /**
     * Returns <code>true</code> if the current instance allows logging of
     * <code>MESSAGE</code> level debug messages.
     *
     * @return <code>true</code> if <code>MESSAGE</code> level debugging is
     * enabled.
     */
    public boolean messageEnabled();

    /**
     * Returns <code>true</code> if the current instance allows logging of
     * <code>WARNING</code> level debug messages.
     *
     * @return <code>true</code> if <code>WARNING</code> level debugging is
     * enabled.
     */
    public boolean warningEnabled();

    /**
     * Returns <code>true</code> if the current instances allows logging of
     * <code>ERROR</code> level debug messages.
     *
     * @return <code>true</code> if <code>ERROR</code> level debugging is
     * enabled.
     */
    public boolean errorEnabled();

    /**
     * Allows the recording of messages if the debug level is set to
     * <code>MESSAGE</code> for this instance.
     *
     * @param message Message to be recorded.
     * @param th      The optional <code>java.lang.Throwable</code> which if
     *                present will be used to record the stack trace.
     */
    public void message(String message, Throwable th);

    /**
     * Allows the recording of messages if the debug level is set to
     * <code>WARNING</code> or higher for this instance.
     *
     * @param message Message to be recorded.
     * @param th      The optional <code>java.lang.Throwable</code> which if
     *                present will be used to record the stack trace.
     */
    public void warning(String message, Throwable th);

    /**
     * Allows the recording of messages if the debug level is set to
     * <code>ERROR</code> or higher for this instance.
     *
     * @param message Message to be recorded.
     * @param th      the optional <code>java.lang.Throwable</code> which if
     *                present will be used to record the stack trace.
     */
    void error(String message, Throwable th);
}
