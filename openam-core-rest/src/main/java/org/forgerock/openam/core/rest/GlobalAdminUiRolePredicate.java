package org.forgerock.openam.core.rest;

import static org.forgerock.openam.rest.RestUtils.isAdmin;

import org.forgerock.openam.rest.RealmContext;
import org.forgerock.services.context.Context;

/**
 * Determines if the user can access global administration pages within the UI.
 */
public class GlobalAdminUiRolePredicate implements UiRolePredicate {
    @Override
    public String getRole() {
        return "ui-global-admin";
    }

    @Override
    public boolean apply(Context context) {
        return isAdmin(context) && context.asContext(RealmContext.class).isRootRealm();
    }
}
