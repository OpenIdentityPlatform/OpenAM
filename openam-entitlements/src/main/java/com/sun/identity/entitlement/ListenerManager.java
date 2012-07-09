/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ListenerManager.java,v 1.3 2010/01/07 00:19:11 veiming Exp $
 */

package com.sun.identity.entitlement;

import com.sun.identity.entitlement.interfaces.IEntitlementListenerRegistry;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import javax.security.auth.Subject;

/**
 * Listener manager manages registering and de-registering of listener.
 */
public final class ListenerManager {
    private static final ListenerManager instance = new ListenerManager();
    private static IEntitlementListenerRegistry registry;

    static {
        //RFE: make it pluggable
        try {
            Class clazz = Class.forName(
                "com.sun.identity.entitlement.opensso.OpenSSOEntitlementListener");
            registry = (IEntitlementListenerRegistry)clazz.newInstance();
        } catch (InstantiationException ex) {
            PrivilegeManager.debug.error("ListenerManager.<init>", ex);
        } catch (IllegalAccessException ex) {
            PrivilegeManager.debug.error("ListenerManager.<init>", ex);
        } catch (ClassNotFoundException ex) {
            PrivilegeManager.debug.error("ListenerManager.<init>", ex);
        }
        
    }

    private ListenerManager() {
    }

    public static ListenerManager getInstance() {
        return instance;
    }

    /**
     * Adds entitlement listener.
     *
     * @param adminSubject administrator subject.
     * @param listener entitlement listener.
     * @throws EntitlementException if listener cannot be added.
     */
    public void addListener(Subject adminSubject, EntitlementListener listener)
        throws EntitlementException {
        if (registry != null) {
            registry.addListener(adminSubject, listener);
        }
    }

    /**
     * Returns registered listener of the notification URL.
     * 
     * @param adminSubject administrator subject.
     * @param url Notification URL.
     * @return registered listener.
     * @throws EntitlementException if listener cannot be retrieved.
     */
    public EntitlementListener getListener(
        Subject adminSubject,
        String url)
        throws EntitlementException {

        if (url == null) {
            throw new EntitlementException(436);
        }

        try {
            URL urlObj = new URL(url);
            Set<EntitlementListener> listeners = getListeners(adminSubject);
            for (EntitlementListener l : listeners) {
                if (l.getUrl().equals(urlObj)) {
                    return l;
                }
            }
            return null;
        } catch (MalformedURLException e) {
            throw new EntitlementException(435);
        }
    }

    /**
     * Returns a set of registered listeners.
     *
     * @param adminSubject administrator subject.
     * @return a set of registered listeners.
     * @throws EntitlementException if listeners cannot be retrieved.
     */
    public Set<EntitlementListener> getListeners(Subject adminSubject)
        throws EntitlementException {
        return (registry != null) ?
            registry.getListeners(adminSubject) : Collections.EMPTY_SET;
    }

    /**
     * Returns <code>true</code> if listener(s) is/are successfully removed.
     *
     * @param adminSubject administrator subject.
     * @param url Notification URL.
     * @return <code>true</code> if listener(s) is/are successfully removed.
     * @throws EntitlementException if listener(s) cannot be removed.
     */
    public boolean removeListener(Subject adminSubject, String url)
        throws EntitlementException {
        if (registry != null) {
            return registry.removeListener(adminSubject, url);
        }
        return false;
    }
}
