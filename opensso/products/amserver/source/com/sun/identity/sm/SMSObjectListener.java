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
 * $Id: SMSObjectListener.java,v 1.2 2008/06/25 05:44:05 qcheng Exp $
 *
 */

package com.sun.identity.sm;

import java.util.EventListener;

import javax.naming.event.NamingEvent;

/**
 * The purpose of this interface is to allow SMSObject implementors to return
 * changes about SMSObjects
 */
public interface SMSObjectListener extends EventListener {

    /**
     * This callback method is called by the EventService when the Directory
     * Server triggers a PersistentSearch notification
     */
    public void objectChanged(String name, int type);

    /**
     * This callback notifies the listener that all object should be marked as
     * "changed" or "dirty". This callback is only used in the case when Event
     * Service looses the directory connection and does not know what could have
     * changed in the directory.
     */
    public void allObjectsChanged();

    public static final int ADD = NamingEvent.OBJECT_ADDED;

    public static final int DELETE = NamingEvent.OBJECT_REMOVED;

    public static final int MODIFY = NamingEvent.OBJECT_CHANGED;

}
