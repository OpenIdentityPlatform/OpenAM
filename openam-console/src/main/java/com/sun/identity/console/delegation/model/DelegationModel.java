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
 * $Id: DelegationModel.java,v 1.2 2008/06/25 05:42:53 qcheng Exp $
 *
 */

package com.sun.identity.console.delegation.model;

import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMConsoleException;
import java.util.Set;
import java.util.Map;

/* - NEED NOT LOG - */

public interface DelegationModel
    extends AMModel
{
    /**
     * Returns delegation subjects under a realm. Returning a set of 
     * universal ID of subject.
     *
     * @param realmName Name of realm.
     * @param pattern Wildcard for matching subject name.
     * @return delegation subjects under a realm.
     * @throws AMConsoleException if subject universal ID cannot be obtained.
     */
    Set getSubjects(String realmName, String pattern)
        throws AMConsoleException;

    /**
     * Returns a set of privileges of an identity.
     *
     * @param realmName Name of realm.
     * @param uid Universal ID of the identity.
     * @return a set of privileges of an identity.
     * @throws AMConsoleException if privilege cannot be determined.
     */
    Set getPrivileges(String realmName, String uid)
        throws AMConsoleException;

    /**
     * Set privileges of an identity.
     *
     * @param realmName Name of realm.
     * @param uid Universal ID of the identity.
     * @param privileges Map of privilege name to privilege value.
     * @throws AMConsoleException if privilege cannot be set.
     */
    void setPrivileges(String realmName, String uid, Map privileges)
        throws AMConsoleException;
}
