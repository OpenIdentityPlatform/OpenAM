package org.forgerock.openam.core.rest;

import static org.forgerock.openam.rest.RestUtils.isAdmin;

import org.forgerock.services.context.Context;

/**
 * Determines if the user can access the realm administration pages in the UI.
 */
public class RealmAdminUiRolePredicate implements UiRolePredicate {
    @Override
    public String getRole() {
        return "ui-realm-admin";
    }

    @Override
    public boolean apply(Context context) {
        return isAdmin(context);
    }
}
