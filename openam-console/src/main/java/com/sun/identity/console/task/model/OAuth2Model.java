/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package com.sun.identity.console.task.model;

import java.util.SortedSet;

import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;

/**
 * Model class for common tasks involved in configuring OAuth2 profiles.
 *
 * @since 13.0.0
 */
public interface OAuth2Model extends AMModel {

    /**
     * Get a set of all configured realms, sorted lexicographically.
     *
     * @return the set of all realms.
     */
    SortedSet<String> getRealms() throws AMConsoleException;

    /**
     * Get the name of this OAuth2 profile, suitable for display to a user.
     *
     * @return the (possibly localized) name of the OAuth2 profile.
     */
    String getDisplayName();

    /**
     * A localized message describing the OAuth2 profile.
     *
     * @return a localized help message for the profile.
     */
    String getLocalizedHelpMessage();
}
