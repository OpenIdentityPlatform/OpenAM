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
package com.sun.identity.console.scripts;

import static com.sun.identity.console.XuiRedirectHelper.getRedirectRealm;
import static com.sun.identity.console.XuiRedirectHelper.redirectToXui;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.realm.HasEntitiesTabs;
import com.sun.identity.console.realm.RealmPropertiesBase;

import javax.servlet.http.HttpServletRequest;
import java.text.MessageFormat;

/**
 * Script management and editing.
 * @since 13.0.0
 */
public class ScriptsViewBean extends RealmPropertiesBase implements HasEntitiesTabs {

    public static final String DEFAULT_DISPLAY_URL = "/console/scripts/Scripts.jsp";

    /**
     * Creates a scripts view bean.
     */
    public ScriptsViewBean() {
        super("Scripts");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    @Override
    protected void initialize() {
        if (!initialized) {
            super.initialize();
            initialized = true;
        }
    }

    /**
     * This redirects to the script editor.  This isn't a natural thing for JATO, and so the strategy used here is
     * to set up a redirect and then throw a CompleteRequestException (thus pretending an error occurred) which causes
     * the redirect to happen.
     *
     * @param event The display event.
     */
    public void beginDisplay(DisplayEvent event) {
        redirectToXui(getRequestContext().getRequest(), getRedirectRealm(this),
                MessageFormat.format("realms/{0}/scripts/list", getCurrentRealmEncoded()));
    }

    @Override
    protected AMModel getModelInternal() {
        RequestContext rc = RequestManager.getRequestContext();
        HttpServletRequest req = rc.getRequest();
        return new AMModelBase(req, getPageSessionAttributes());
    }
}
