/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SelectRealmModel.java,v 1.2 2008/06/25 05:43:08 qcheng Exp $
 *
 */

package com.sun.identity.console.policy.model;

import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMConsoleException;
import java.util.Set;

/**
 * This defines the method for searching realms.
 */
public interface SelectRealmModel
    extends AMModel
{
    /**
     * Returns realms that have names matching with a filter.
     *
     * @param base Base realm name for this search. null indicates root
     *        suffix.
     * @param filter Filter string.
     * @return realms that have names matching with a filter.
     * @throws AMConsoleException if search fails.
     */
    Set getRealmNames(String base, String filter)
        throws AMConsoleException;

    /**
     * Returns set of authentication instances.
     *
     * @param realmName Name of Realm.
     * @return set of authentication instances.
     * @throws AMConsoleException if authentication instances cannot be
     *         obtained.
     */
    Set getAuthenticationInstances(String realmName)
        throws AMConsoleException;
}
