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
 * $Id: EntityMembershipViewBean.java,v 1.2 2008/06/25 05:42:59 qcheng Exp $
 *
 */

package com.sun.identity.console.idm;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.identity.console.idm.model.EntitiesModel;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdUtils;
import com.sun.web.ui.model.CCAddRemoveModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.addremove.CCAddRemove;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCButton;
import com.sun.web.ui.view.html.CCTextField;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class EntityMembershipViewBean
    extends EntityEditViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/idm/EntityMembership.jsp";

    private static final String TF_FILTER = "tfFilter";
    private static final String BTN_SEARCH = "btnSearch";
    private static final String ADD_REMOVE_MEMBERS = "addRemoveMembers";

    private boolean canModify;
    private boolean submitCycle;
    private String filter;
    private List assignedMembers;
    private OptionList cacheAssigned;

    public EntityMembershipViewBean() {
        super("EntityMembership", DEFAULT_DISPLAY_URL);
        registerChild(TF_FILTER, CCTextField.class);
        registerChild(BTN_SEARCH, CCButton.class);
    }

    protected void registerChildren() {
        super.registerChildren();
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);
        getMemberships();
    }

    private void getMemberships() {
        if (!submitCycle) {
            try {
                String curRealm = (String)getPageSessionAttribute(
                    AMAdminConstants.CURRENT_REALM);
                String type = (String)getPageSessionAttribute(
                    PG_SESSION_MEMBER_TYPE);
                String universalId = (String)getPageSessionAttribute(
                    UNIVERSAL_ID);

                if (cacheAssigned != null) {
                    assignedMembers = AMAdminUtils.toList(cacheAssigned);
                } else {
                    EntitiesModel model = (EntitiesModel)getModel();
                    assignedMembers = new ArrayList();
                    assignedMembers.addAll(
                        model.getMembership(curRealm, universalId, type));
                }

                if (canModify) {
                    CCAddRemoveModel addRemoveModel = (CCAddRemoveModel)
                        propertySheetModel.getModel(ADD_REMOVE_MEMBERS);
                    addRemoveModel.clear();
                    if (cacheAssigned != null) {
                        addRemoveModel.setSelectedOptionList(cacheAssigned);
                    } else {
                        addRemoveModel.setSelectedOptionList(
                            getOptionListForEntities(assignedMembers));
                    }
                    addRemoveModel.setAvailableOptionList(
                        getAssignableMembership());
                } else {
                    propertySheetModel.setValue(ADD_REMOVE_MEMBERS,
                        AMAdminUtils.getString(
                            getEntityDisplayNames(assignedMembers),
                            ",", false));
                }
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
            }

            if (canModify) {
                CCAddRemove addRemove = 
                    (CCAddRemove)getChild(ADD_REMOVE_MEMBERS);
                addRemove.resetStateData();
            }
        }
    }

    private OptionList getAssignableMembership() {
        OptionList avail = null;
        EntitiesModel model = (EntitiesModel)getModel();
        if ((filter == null) || (filter.trim().length() == 0)) {
            filter = "*";
            setDisplayFieldValue(TF_FILTER, "*");
        }
        setDisplayFieldValue(TF_FILTER, filter);

        try {
            String curRealm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            String searchType = (String)getPageSessionAttribute(
                EntityMembersViewBean.PG_SESSION_MEMBER_TYPE);

            IdSearchResults results = model.getEntityNames(
                curRealm, searchType, filter);
            int errorCode = results.getErrorCode();

            switch (errorCode) {
            case IdSearchResults.SIZE_LIMIT_EXCEEDED:
                setInlineAlertMessage(CCAlert.TYPE_WARNING, "message.warning",
                    "message.sizelimit.exceeded");
                break;
            case IdSearchResults.TIME_LIMIT_EXCEEDED:
                setInlineAlertMessage(CCAlert.TYPE_WARNING, "message.warning",
                    "message.timelimit.exceeded");
                break;
            }
                                                                                
            Set members = results.getSearchResults();
            String universalId = (String)getPageSessionAttribute(
                EntityEditViewBean.UNIVERSAL_ID);
            removeAlreadyAssignedMembers(members);
            avail = getOptionListForEntities(members);
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }

        return avail;
    }

    private void removeAlreadyAssignedMembers(Set assignable) {
        for (Iterator i = assignedMembers.iterator(); i.hasNext(); ) {
            Object obj = i.next();
            if (obj instanceof AMIdentity) {
                assignable.remove(obj);
            } else {
                boolean removed = false;
                for (Iterator iter = assignable.iterator();
                    iter.hasNext() && !removed;
                ) {
                    AMIdentity amid = (AMIdentity)iter.next();
                    if (IdUtils.getUniversalId(amid).equalsIgnoreCase(
                        (String)obj)
                    ) {
                        iter.remove();
                        removed = true;
                    }
                }
            }
        }
    }


    protected void setSelectedTab() {
    }

    protected AMPropertySheetModel createPropertySheetModel(String type) {
        String realmName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        DelegationConfig dConfig = DelegationConfig.getInstance();
        canModify = dConfig.hasPermission(realmName, null,
            AMAdminConstants.PERMISSION_MODIFY, getModel(),
            getClass().getName());

        String xmlFile = (!canModify) ?
            "com/sun/identity/console/propertyEntityMembership_Readonly.xml" :
            "com/sun/identity/console/propertyEntityMembership.xml";
        AMPropertySheetModel psModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(xmlFile));

        if (canModify) {
            psModel.setModel(ADD_REMOVE_MEMBERS, createAddRemoveModel());
        }

        return psModel;
    }

    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", 
            getBackButtonLabel("tab.sub.subjects.label"));
    }

    private CCAddRemoveModel createAddRemoveModel() {
        CCAddRemoveModel addRemoveModel = new CCAddRemoveModel();
        addRemoveModel.setOrientation(CCAddRemoveModel.HORIZONTAL);
        addRemoveModel.setListboxHeight(
            CCAddRemoveModel.DEFAULT_LISTBOX_HEIGHT);
        return addRemoveModel;
    }

    public void handleBtnSearchRequest(RequestInvocationEvent event) {
        submitCycle = false;
        CCAddRemove addRemove = (CCAddRemove)getChild(ADD_REMOVE_MEMBERS);
        addRemove.restoreStateData();
        CCAddRemoveModel addRemoveModel = (CCAddRemoveModel)
            propertySheetModel.getModel(ADD_REMOVE_MEMBERS);
        cacheAssigned = addRemoveModel.getSelectedOptionList();
        filter = ((String)getDisplayFieldValue(TF_FILTER));
        filter = filter.trim();
        forwardTo();
    }

    public void handleButton2Request(RequestInvocationEvent event) {
        forwardTo();
    }

    public void handleButton1Request(RequestInvocationEvent event) {
        submitCycle = true;
        CCAddRemove child = (CCAddRemove)getChild(ADD_REMOVE_MEMBERS);
        child.restoreStateData();
        CCAddRemoveModel addRemoveModel = (CCAddRemoveModel)
            propertySheetModel.getModel(ADD_REMOVE_MEMBERS);
        OptionList os = addRemoveModel.getSelectedOptionList();
        EntitiesModel model = (EntitiesModel)getModel();
        String universalId = (String)getPageSessionAttribute(UNIVERSAL_ID);
        String curRealm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        String type = (String)getPageSessionAttribute(
            PG_SESSION_MEMBER_TYPE);

        try {
            Set entities = getEntitiesID(
                model.getMembership(curRealm, universalId, type));
            Set selected = getValues(os);
            Set toAdd = new HashSet(selected);
            toAdd.removeAll(entities);
            entities.removeAll(selected);

            if (!toAdd.isEmpty()) {
                model.addMemberships(universalId, toAdd);
            }
            if (!entities.isEmpty()) {
                model.removeMemberships(universalId, entities);
            }
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                "message.updated");
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
        forwardTo();
    }
}
