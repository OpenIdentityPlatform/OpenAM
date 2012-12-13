package com.sun.identity.admin;

import com.sun.identity.admin.model.FromAction;
import com.sun.identity.admin.model.PermissionsBean;
import javax.faces.application.NavigationHandler;
import javax.faces.context.FacesContext;

public class PermissionNavigationHandler extends NavigationHandler {

    private NavigationHandler _base;

    public PermissionNavigationHandler(NavigationHandler base) {
        super();
        _base = base;

    }

    public void handleNavigation(FacesContext fc, String fromAction, String outcome) {
        boolean allowed = PermissionsBean.getInstance().isActionAllowed(outcome);
        if (!allowed) {
            outcome = FromAction.PERMISSION_DENIED.getAction();
            fromAction = null;
        }

        _base.handleNavigation(fc, fromAction, outcome);
    }

}
