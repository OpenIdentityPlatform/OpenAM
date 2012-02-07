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
 * $Id: ReferralResource.java,v 1.2 2009/06/24 19:23:46 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.icesoft.faces.context.effects.Effect;
import com.icesoft.faces.context.effects.SlideDown;
import com.icesoft.faces.context.effects.SlideUp;
import com.sun.identity.admin.handler.MultiPanelHandler;
import javax.faces.event.ActionEvent;

public class ReferralResource
    extends Resource
    implements MultiPanelBean, MultiPanelHandler, PolicyResourcesBean {

    private Effect panelExpandEffect;
    private Effect panelEffect;
    private boolean panelExpanded = false;
    private boolean panelVisible = true;
    private ViewEntitlement viewEntitlement = new ViewEntitlement();

    public Resource deepClone() {
        ReferralResource rr = new ReferralResource();
        rr.setName(getName());

        return rr;
    }

    public void panelExpandListener(ActionEvent event) {
        Effect e;
        if (isPanelExpanded()) {
            e = new SlideUp();
        } else {
            e = new SlideDown();
        }
        e.setSubmit(true);
        e.setTransitory(false);
        setPanelExpandEffect(e);
    }

    public void panelRemoveListener(ActionEvent event) {
        // nothing
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        viewEntitlement.setViewApplication(getViewApplication());
    }

    private ViewApplication getViewApplication() {
        ViewApplicationsBean vasb = ViewApplicationsBean.getInstance();
        ViewApplication va = vasb.getViewApplications().get(getName());
        assert(va != null);

        return va;
    }

    @Override
    public String getTitle() {
        String title = null;
        ViewApplication va = getViewApplication();
        if (va != null) {
            title = va.getTitle();
        }
        if (title == null) {
            title = getName();
        }
        
        return title;
    }

    public Effect getPanelExpandEffect() {
        return panelExpandEffect;
    }

    public void setPanelExpandEffect(Effect panelExpandEffect) {
        this.panelExpandEffect = panelExpandEffect;
    }

    public Effect getPanelEffect() {
        return panelEffect;
    }

    public void setPanelEffect(Effect panelEffect) {
        this.panelEffect = panelEffect;
    }

    public boolean isPanelExpanded() {
        return panelExpanded;
    }

    public void setPanelExpanded(boolean panelExpanded) {
        this.panelExpanded = panelExpanded;
    }

    public boolean isPanelVisible() {
        return panelVisible;
    }

    public void setPanelVisible(boolean panelVisible) {
        this.panelVisible = panelVisible;
    }

    public ViewEntitlement getViewEntitlement() {
        return viewEntitlement;
    }
}
