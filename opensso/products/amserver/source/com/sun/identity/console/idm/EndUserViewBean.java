/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: EndUserViewBean.java,v 1.4 2008/09/04 23:59:36 veiming Exp $
 *
 */

package com.sun.identity.console.idm;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.sso.SSOException;
import com.sun.identity.console.base.AuthenticatedViewBean;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.idm.model.EntitiesModel;
import com.sun.identity.console.idm.model.EntitiesModelImpl;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.web.ui.model.CCPageTitleModel;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public class EndUserViewBean
    extends EntityEditViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/idm/EndUser.jsp";

    public EndUserViewBean() {
        super("EndUser", DEFAULT_DISPLAY_URL);
    }

    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/twoBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
    }

    public void beginDisplay(DisplayEvent event) throws ModelControlException {
        super.beginDisplay(event);
        ptModel.setPageTitleText(identityDisplayName);
    }

    public void forwardTo(RequestContext rc) {
        String userId = (String)getPageSessionAttribute(UNIVERSAL_ID);
        if (userId == null) {
            EntitiesModel model = (EntitiesModel)getModel();
            try {
                userId = model.getUserName();
                AMIdentity amid = IdUtils.getIdentity(
                    model.getUserSSOToken(), userId);
                setPageSessionAttribute(UNIVERSAL_ID, userId);
                setPageSessionAttribute(EntityOpViewBeanBase.ENTITY_NAME,
                    amid.getName());
                setPageSessionAttribute(EntityOpViewBeanBase.ENTITY_TYPE,
                        amid.getType().getName());
                 Set agentTypes = amid.getAttribute("AgentType");
                if ((agentTypes != null) && !agentTypes.isEmpty()) {
                    setPageSessionAttribute(
                        EntityOpViewBeanBase.ENTITY_AGENT_TYPE,
                        (String)agentTypes.iterator().next());
                }               
                super.forwardTo(rc);
            } catch (IdRepoException e) {
                AuthenticatedViewBean vb = (AuthenticatedViewBean)
                    getViewBean(AuthenticatedViewBean.class);
                passPgSessionMap(vb);
                vb.forwardTo(rc);
            } catch (SSOException e) {
                AuthenticatedViewBean vb = (AuthenticatedViewBean)
                    getViewBean(AuthenticatedViewBean.class);
                passPgSessionMap(vb);
                vb.forwardTo(rc);
            }
        } else {
            super.forwardTo(rc);
        }
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        EntitiesModel model = new EntitiesModelImpl(
            req, getPageSessionAttributes());
        model.setEndUser(true);
        return model;
    }

    protected boolean startPageTrail() {
        return true;
    }

    protected String getBreadCrumbDisplayName() {
        return "";
    }

}
