package org.forgerock.openam.core.rest;

import org.forgerock.guava.common.base.Predicate;
import org.forgerock.services.context.Context;

/**
 * A predicate that determines if a user has a particular UI role.
 */
public interface UiRolePredicate extends Predicate<Context> {
    /**
     * @return The UI role that this predicate is associated with.
     */
    String getRole();
}
