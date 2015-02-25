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
 * Copyright 2014 ForgeRock Inc.
 */
package org.forgerock.openam.rest.authz;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * For a given privilege verb, defines which actions best represent it and therefore are allowed.
 *
 * @since 12.0.0
 */
public final class PrivilegeDefinition {

    public static enum Action {
        READ, MODIFY, DELEGATE
    }

    private final String commonVerb;
    private final Set<Action> action;

    private PrivilegeDefinition(final String commonVerb, final Set<Action> action) {
        this.commonVerb = commonVerb;
        this.action = action;
    }

    String getCommonVerb() {
        return commonVerb;
    }

    Set<Action> getActions() {
        return action;
    }

    /**
     * Creates a new definition from the privilege verb and the allowed actions.
     *
     * @param commonVerb
     *         the privilege verb
     * @param actions
     *         the allowed actions
     *
     * @return a privilege definition
     */
    public static PrivilegeDefinition getInstance(final String commonVerb, final Action... actions) {
        return new PrivilegeDefinition(commonVerb, EnumSet.copyOf(Arrays.asList(actions)));
    }

}
