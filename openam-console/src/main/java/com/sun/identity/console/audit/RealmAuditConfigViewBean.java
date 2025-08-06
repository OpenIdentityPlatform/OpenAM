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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package com.sun.identity.console.audit;

import static com.sun.identity.console.audit.AuditConsoleConstants.*;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.view.ViewBean;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.audit.model.RealmAuditConfigModel;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.realm.ServicesViewBean;
import com.sun.web.ui.view.alert.CCAlert;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Realm Audit configuration UI view bean.
 *
 * @since 13.0.0
 */
public class RealmAuditConfigViewBean extends AbstractAuditViewBean {

    private static final String DEFAULT_DISPLAY_URL = "/console/audit/RealmAuditConfig.jsp";
    private static final String PAGE_NAME = "RealmAuditConfig";

    /**
     * Create a new {@code RealmAuditConfigViewBean}.
     */
    public RealmAuditConfigViewBean() {
        super(PAGE_NAME, DEFAULT_DISPLAY_URL);
    }

    @Override
    protected AMModel getModelInternal() {
        HttpServletRequest req = RequestManager.getRequestContext().getRequest();
        try {
            return new RealmAuditConfigModel(req, getPageSessionAttributes());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, ERROR_MESSAGE, e.getMessage());
        }
        return null;
    }

    @Override
    boolean isGlobalService() {
        return false;
    }

    @Override
    protected String getBackButtonLabel() {
        return getBackButtonLabel("page.title.services");
    }

    @Override
    protected ViewBean getSelectViewBean() {
        return getViewBean(RealmEventHandlerSelectViewBean.class);
    }

    @Override
    protected ViewBean getEditViewBean() {
        return getViewBean(RealmEventHandlerEditViewBean.class);
    }

    @Override
    public void handleButton3Request(RequestInvocationEvent event) {
        backTrail();
        ViewBean vb = getViewBean(ServicesViewBean.class);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }
}
