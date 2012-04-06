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
 * $Id: EntityAddViewBean.java,v 1.4 2008/09/04 23:59:37 veiming Exp $
 *
 */

package com.sun.identity.console.idm;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.idm.model.EntitiesModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class EntityAddViewBean
    extends EntityOpViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/idm/EntityAdd.jsp";

    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";

    EntityAddViewBean(String name, String defaultDisplayURL) {
        super(name, defaultDisplayURL);
    }

    public EntityAddViewBean() {
        super("EntityAdd", DEFAULT_DISPLAY_URL);
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(PGTITLE_TWO_BTNS, CCPageTitle.class);
    }

    protected View createChild(String name) {
        View view = null;
        if (name.equals(PGTITLE_TWO_BTNS)) {
            view = new CCPageTitle(this, ptModel, name);
        } else {
            view = super.createChild(name);
        }
        return view;
    }

    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/twoBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.ok");
        ptModel.setValue("button2", "button.cancel");
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);

        EntitiesModel model = (EntitiesModel)getModel();
        String type = (String)getPageSessionAttribute(ENTITY_TYPE);
        String realmName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        Map types = model.getSupportedEntityTypes(realmName);
        String i18nName = (String)types.get(type);
        String title = model.getLocalizedString(
            "page.title.entities.create");
        Object[] param = {i18nName};
        ptModel.setPageTitleText(MessageFormat.format(title, param));

        if (hasNoAttributeToDisplay) {
            disableButton("button1", true);
        }
    }

    protected void setDefaultValues(String type)
        throws AMConsoleException
    {
        if (!submitCycle && (propertySheetModel != null)) {
            EntitiesModel model = (EntitiesModel)getModel();
            // null for agent type
            Map defaultValues = model.getDefaultAttributeValues(
                type, null, true);

            for (Iterator i = defaultValues.keySet().iterator(); i.hasNext();) {
                String name = (String)i.next();
                Set values = (Set)defaultValues.get(name);

                if ((values != null) && !values.isEmpty()) {
                    propertySheetModel.setValues(
                        name, values.toArray(), model);
                }
            }
        }
    }

    /**
     * Handles create realm request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException {
        submitCycle = true;
        EntitiesModel model = (EntitiesModel)getModel();
        AMPropertySheet prop = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
        String entityType = (String)getPageSessionAttribute(ENTITY_TYPE);
        String entityName = (String)propertySheetModel.getValue(ENTITY_NAME);
        entityName = entityName.trim();

        try {
            // null for agent type
            Map defaultValues = model.getDefaultAttributeValues(
                entityType, null, true);
            Map values = prop.getAttributeValues(defaultValues.keySet());
            String realmName = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);

            model.createEntity(realmName, entityName, entityType, values);
            forwardToEntitiesViewBean();
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
            forwardTo();
        }
    }

    protected boolean isCreateViewBean() {
        return true;
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.addentities";
    }

    protected boolean startPageTrail() {
        return false;
    }
}
