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
 * $Id: IEntitlementListenerRegistry.java,v 1.2 2009/12/15 00:44:18 veiming Exp $
 */

package com.sun.identity.entitlement.interfaces;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.EntitlementListener;
import java.util.Set;
import javax.security.auth.Subject;

/**
 * This interface defines the methods for adding and removing
 * entitlement listeners.
 */
public interface IEntitlementListenerRegistry {
    /**
     * Adds entitlement listener.
     *
     * @param adminSubject administrator subject.
     * @param l entitlement listener.
     * @throws EntitlementException if listener cannot be added.
     */
    void addListener(Subject adminSubject, EntitlementListener l)
        throws EntitlementException;

    /**
     * Returns a set of registered entitlement listener.
     *
     * @param adminSubject administrator subject.
     * @return a set of registered entitlement listener.
     * @throws EntitlementException if listener cannot be retrieved.
     */
    Set<EntitlementListener> getListeners(Subject adminSubject)
        throws EntitlementException;

    /**
     * Returns <code>true</code> if listener(s) is/are successfully removed.
     *
     * @param adminSubject administrator subject.
     * @param url Notification URL.
     * @return <code>true</code> if listener(s) is/are successfully removed.
     * @throws EntitlementException if listener(s) cannot be removed.
     */
    boolean removeListener(Subject adminSubject, String url)
        throws EntitlementException;
}
