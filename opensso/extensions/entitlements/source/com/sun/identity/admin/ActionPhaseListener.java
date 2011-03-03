/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: ActionPhaseListener.java,v 1.3 2009/06/08 18:06:12 farble1670 Exp $
 */

package com.sun.identity.admin;

import com.sun.identity.admin.NavigationRules.NavigationCase;
import com.sun.identity.admin.NavigationRules.NavigationRule;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletRequest;

public class ActionPhaseListener implements PhaseListener {

    public ActionPhaseListener() {
    }

    public PhaseId getPhaseId() {
        return PhaseId.RESTORE_VIEW;
    }

    public void beforePhase(PhaseEvent phaseEvent) {
    }

    public void afterPhase(PhaseEvent phaseEvent) {
        if (navigationRules == null) {
            navigationRules = new NavigationRules();
        }

        FacesContext ctx = phaseEvent.getFacesContext();
        HttpServletRequest request =
                (HttpServletRequest) ctx.getExternalContext().getRequest();

        String action = request.getParameter("jsf.action");

        if (action != null) {
            String currentViewId = ctx.getViewRoot().getViewId();
            NavigationRule nr = navigationRules.getNavigationRules().get(currentViewId);
            if (nr == null) {
                nr = navigationRules.getNavigationRules().get(null);
            }
            NavigationCase nc = nr.getNavigationCases().get(action);
            if (nc != null) {
                String newViewId = nc.getToViewId();
                UIViewRoot page = ctx.getApplication().getViewHandler().createView(ctx, newViewId);
                ctx.setViewRoot(page);
                ctx.renderResponse();
            }
        }
    }

    private NavigationRules navigationRules = null;
}
