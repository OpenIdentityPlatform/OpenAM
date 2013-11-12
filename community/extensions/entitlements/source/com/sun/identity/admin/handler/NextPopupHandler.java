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
 * $Id: NextPopupHandler.java,v 1.4 2009/07/24 23:55:37 farble1670 Exp $
 */

package com.sun.identity.admin.handler;

import com.sun.identity.admin.model.LinkBean;
import com.sun.identity.admin.model.NextPopupBean;
import java.io.IOException;
import java.io.Serializable;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

public class NextPopupHandler implements Serializable {

    private NextPopupBean nextPopupBean;

    public void setNextPopupBean(NextPopupBean nextPopupBean) {
        this.nextPopupBean = nextPopupBean;
    }

    public void closeListener(ActionEvent event) {
        nextPopupBean.reset();
        LinkBean.HOME.getRedirect();
    }

    public void nextListener(ActionEvent event) {
        nextPopupBean.reset();

        LinkBean lb = getLinkBean(event);
        lb.redirect();
    }

    public LinkBean getLinkBean(ActionEvent event) {
        LinkBean lb = (LinkBean) event.getComponent().getAttributes().get("linkBean");
        return lb;
    }
}
