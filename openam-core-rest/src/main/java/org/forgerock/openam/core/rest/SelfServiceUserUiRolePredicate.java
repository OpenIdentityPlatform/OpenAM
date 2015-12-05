package org.forgerock.openam.core.rest;

import javax.inject.Inject;
import javax.inject.Named;

import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.sso.SSOException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.utils.Config;
import org.forgerock.services.context.Context;

/**
 * Determines if the user can access the self-service pages within the UI.
 */
public class SelfServiceUserUiRolePredicate implements UiRolePredicate {
    private final Config<SessionService> sessionService;
    private final Debug logger;

    @Inject
    public SelfServiceUserUiRolePredicate(Config<SessionService> sessionService, @Named("frRest") Debug logger) {
        this.sessionService = sessionService;
        this.logger = logger;
    }

    @Override
    public String getRole() {
        return "ui-self-service-user";
    }

    @Override
    public boolean apply(Context context) {
        try {
            String id = context.asContext(SSOTokenContext.class).getCallerSSOToken().getPrincipal().getName();
            return !sessionService.get().isSuperUser(id);
        } catch (SSOException e) {
            logger.message("Failed to get SSO token for requested user", e);
            return false;
        }
    }
}
