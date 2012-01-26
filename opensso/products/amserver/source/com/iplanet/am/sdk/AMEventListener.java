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
 * $Id: AMEventListener.java,v 1.4 2008/06/25 05:41:20 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

/**
 * <p>
 * Represents the event listener interface that consumers of this API should
 * implement and register with the SDK to receive Sun Java System Access Manager
 * SDK notifications.
 * <p>
 * <b>NOTE: </b> It is recommended that the classes that implement this
 * interface also implement {@link Object#equals Object.equals()} method if the
 * default behavior of the method is not desired.
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 * @supported.all.api
 */
public interface AMEventListener extends java.util.EventListener {
    /**
     * Sun Java System Access Manager SDK invokes this method when any object or
     * its contents change.
     * 
     * @param event
     *            <code>AMEvent</code> object representing the
     *            <code>AMEvent.OBJECT_CHANGED</code> event.
     */
    public void objectChanged(AMEvent event);

    /**
     * Sun Java System Access Manager SDK invokes this method when any object is
     * removed.
     * 
     * @param event
     *            <code>AMEvent</code> object representing the
     *            <code>AMEvent.OBJECT_REMOVED</code> event.
     */
    public void objectRemoved(AMEvent event);

    /**
     * Sun Java System Access Manager SDK invokes this method when any object is
     * renamed.
     * 
     * @param event
     *            <code>AMEvent</code> object representing the
     *            <code>AMEvent.OBJECT_RENAMED</code> event.
     */
    public void objectRenamed(AMEvent event);
}
