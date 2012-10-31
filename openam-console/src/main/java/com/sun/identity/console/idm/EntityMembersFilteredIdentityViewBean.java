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
 * $Id: EntityMembersFilteredIdentityViewBean.java,v 1.2 2008/06/25 05:42:59 qcheng Exp $
 *
 */

package com.sun.identity.console.idm;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.idm.model.EntitiesModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.html.CCSelectableList;
import com.sun.web.ui.view.alert.CCAlert;
import java.util.Set;

public class EntityMembersFilteredIdentityViewBean
    extends EntityEditViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/idm/EntityMembersFilteredIdentity.jsp";

    private static final String TF_FILTER = "tfFilter";
    private static final String BTN_SEARCH = "btnSearch";
    private static final String MEMBERS = "members";

    public EntityMembersFilteredIdentityViewBean() {
        super("EntityMembersFilteredIdentity", DEFAULT_DISPLAY_URL);
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);
        getMemberNames();
    }

    private void getMemberNames() {
        EntitiesModel model = (EntitiesModel)getModel();
        try {
            String curRealm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            String type = (String)getPageSessionAttribute(
                PG_SESSION_MEMBER_TYPE);
            String universalId = (String)getPageSessionAttribute(UNIVERSAL_ID);
            Set entities = model.getMembers(curRealm, universalId, type);

            CCSelectableList list = (CCSelectableList)getChild(MEMBERS);
            list.setOptions(getOptionListForEntities(entities));
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

    protected void setSelectedTab() {
    }

    protected AMPropertySheetModel createPropertySheetModel(String type) {
        String xmlFile =
        "com/sun/identity/console/propertyEntityMembersFilteredIdentity.xml";
        AMPropertySheetModel psModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(xmlFile));
        return psModel;
    }

    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/simplePageTitle.xml"));
    }
}
