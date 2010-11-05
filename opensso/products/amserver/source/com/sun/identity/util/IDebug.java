/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: IDebug.java,v 1.3 2008/06/25 05:44:13 qcheng Exp $
 *
 */

package com.sun.identity.util;

/**
 * Allows a pluggable implementation of the Debug service within the Access
 * Manager SDK. The implementation of this interface as well as the
 * <code>com.sun.identity.util.IDebugProvider</code> interface togehter
 * provide the necessary functionality to replace or enhance the Debug service.
 *
 * @deprecated As of OpenSSO version 8.0
 *             {@link com.sun.identity.shared.debug.IDebug}
 */
public interface IDebug {

    /**
     * Convenience method to query the name of the IDebug instance. The return
     * value of this method is a string exactly equal to the one that was first
     * used to create this instance.
     * 
     * @return name of this IDebug instance
     */
    public String getName();

    /**
     * Convenience method to query the current debug level used by this
     * instance. The return value of this method is an integer equal to one of
     * the various debug level integers as defined in the class
     * <code>com.iplanet.am.util.Debug</code>. This value could be one of the
     * following:<br>
     * <ul>
     * <li><code>com.iplanet.am.util.Debug.OFF</code>
     * <li><code>com.iplanet.am.util.Debug.ERROR</code>
     * <li><code>com.iplanet.am.util.Debug.WARNING</code>
     * <li><code>com.iplanet.am.util.Debug.MESSAGE</code>
     * <li><code>com.iplanet.am.util.Debug.ON</code>
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
     * <li><code>com.iplanet.am.util.Debug.OFF</code>
     * <li><code>com.iplanet.am.util.Debug.ERROR</code>
     * <li><code>com.iplanet.am.util.Debug.WARNING</code>
     * <li><code>com.iplanet.am.util.Debug.MESSAGE</code>
     * <li><code>com.iplanet.am.util.Debug.ON</code>
     * </ul>
     * 
     * @param level
     *            an integer indicating the debug level to be used by this
     *            instance.
     */
    public void setDebug(int level);

    /**
     * Allows runtime modification of the debug level used by this instance. The
     * argument <code>level</code> must be a string which should exactly match
     * the string definitions of debug level as defined in the class
     * <code>com.iplanet.am.util.Debug</code>. This value could be one of the
     * following:
     * <ul>
     * <li><code>com.iplanet.am.util.Debug.STR_OFF</code>
     * <li><code>com.iplanet.am.util.Debug.STR_ERROR</code>
     * <li><code>com.iplanet.am.util.Debug.STR_WARNING</code>
     * <li><code>com.iplanet.am.util.Debug.STR_MESSAGE</code>
     * <li><code>com.iplanet.am.util.Debug.STR_ON</code>
     * </ul>
     * 
     * @param level
     *            a string representing the debug level to be used by this
     *            instance.
     */
    public void setDebug(String level);

    /**
     * Convenience method to query if the current instance allows logging of of
     * <code>MESSAGE</code> level debug messages.
     * 
     * @return a boolean indicating if <code>MESSAGE</code> level debugging is
     *         enabled or not.
     */
    public boolean messageEnabled();

    /**
     * Convenience method to query if the current instance allows logging of of
     * <code>WARNING</code> level debug messages.
     * 
     * @return a boolean indicating if <code>WARNING</code> level debugging is
     *         enabled or not.
     */
    public boolean warningEnabled();

    /**
     * Convenience method to query if the current instances allows logging of of
     * <code>ERROR</code> level debug messages.
     * 
     * @return a boolean indicating if <code>ERROR</code> level debugging is
     *         enabled or not.
     */
    public boolean errorEnabled();

    /**
     * Allows the recording of messages if the debug level is set to
     * <code>MESSAGE</code> for this instance.
     * 
     * @param message
     *            the message to be recorded.
     * @param th
     *            the optional <code>java.lang.Throwable</code> which if
     *            present will be used to record the stack trace.
     */
    public void message(String message, Throwable th);

    /**
     * Allows the recording of messages if the debug level is set to
     * <code>WARNING</code> or higher for this instance.
     * 
     * @param message
     *            the message to be recorded.
     * @param th
     *            the optional <code>java.lang.Throwable</code> which if
     *            present will be used to record the stack trace.
     */
    public void warning(String message, Throwable th);

    /**
     * Allows the recording of messages if the debug level is set to
     * <code>ERROR</code> or higher for this instance.
     * 
     * @param message
     *            the message to be recorded.
     * @param th
     *            the optional <code>java.lang.Throwable</code> which if
     *            present will be used to record the stack trace.
     */
    public void error(String message, Throwable th);
}
