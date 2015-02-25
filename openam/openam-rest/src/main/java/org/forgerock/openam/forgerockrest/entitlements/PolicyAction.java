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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.forgerockrest.entitlements;

import java.util.EnumSet;

/**
 * Enum class that represents the different policy actions available.
 *
 * @since 12.0.0
 */
public enum PolicyAction {

    EVALUATE("evaluate"), TREE_EVALUATE("evaluateTree"), UNKNOWN("unknown");

    private static final EnumSet<PolicyAction> EVALUATE_ACTIONS = EnumSet.of(EVALUATE, TREE_EVALUATE);

    private final String actionName;

    private PolicyAction(final String actionName) {
        this.actionName = actionName;
    }

    /**
     * Verifies whether the passed action is an evaluation action.
     *
     * @param action
     *         action of interest
     *
     * @return whether the action is an evaluation action
     */
    public static boolean isEvaluateAction(final PolicyAction action) {
        return EVALUATE_ACTIONS.contains(action);
    }

    /**
     * Maps the action string to an actual action instance.
     *
     * @param actionName
     *         the action string
     *
     * @return the corresponding action if match, else null
     */
    public static PolicyAction getAction(final String actionName) {
        for (PolicyAction action : values()) {
            if (action.actionName.equals(actionName)) {
                return action;
            }
        }

        return null;
    }

}
