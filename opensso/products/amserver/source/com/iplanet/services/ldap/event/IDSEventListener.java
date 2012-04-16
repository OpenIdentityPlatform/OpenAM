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
 * $Id: IDSEventListener.java,v 1.4 2009/01/28 05:34:50 ww203982 Exp $
 *
 */

package com.iplanet.services.ldap.event;

import java.util.EventListener;
import java.util.Map;

import com.sun.identity.shared.ldap.controls.LDAPPersistSearchControl;

/**
 * The purpose of this interface is to allow classes that implement this
 * interface to listen to Directory Server Events.
 * @supported.api
 */
public interface IDSEventListener extends EventListener {

    /**
     * This callback method is called by the EventService when the Directory
     * Server triggers a PersistentSearch notification
     * @supported.api
     */
    public void entryChanged(DSEvent e);

    /**
     * This callback method is called by the EventService when an error is
     * encountered after setting a Persistent Search request in the Directory
     * Server
     * @supported.api
     */
    public void eventError(String err);

    /**
     * This callback notifies listeners that EventService is restarting the
     * Persistent Search connections due to connection errors, and there is no
     * guarantee of what could have changed in the directory so mark all entries
     * as modified.
     */
    public void allEntriesChanged();

    public static int CHANGE_ADD = LDAPPersistSearchControl.ADD;

    public static int CHANGE_DELETE = LDAPPersistSearchControl.DELETE;

    public static int CHANGE_MOD_LOCATION = LDAPPersistSearchControl.MODDN;

    public static int CHANGE_MODIFY = LDAPPersistSearchControl.MODIFY;

    public String getBase();

    public String getFilter();

    public int getScope();

    public int getOperations();

    public void setListeners(Map listener);

}
