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
 * $Id: IdentitySubjectAddViewBean.java,v 1.2 2008/06/25 05:43:02 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.policy;

import com.iplanet.sso.SSOToken;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.ChildDisplayEvent;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils; 
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.policy.model.IdentitySubjectModel;
import com.sun.identity.console.policy.model.IdentitySubjectModelImpl;
import com.sun.identity.policy.PolicyUtils;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdUtils;
import com.sun.web.ui.view.addremove.CCAddRemove;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCDropDownMenu;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public class IdentitySubjectAddViewBean
    extends SubjectAddViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/policy/IdentitySubjectAdd.jsp";
    private static final String FILTER_TYPE = "tfType";
    private static final String ENTITY_TYPE = "searchEntityType";

    /**
     * Creates a policy creation view bean.
     */
    public IdentitySubjectAddViewBean() {
        super("IdentitySubjectAdd", DEFAULT_DISPLAY_URL);
    }

    protected String getPropertyXMLFileName(boolean readonly) {
        return "com/sun/identity/console/propertyPMIdentitySubject.xml";
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        IdentitySubjectModel model = (IdentitySubjectModel)getModel();
        Set values = null;
        if (bFilter) {
            Set defaultValue = getValues();
            if (defaultValue != null) {
                values = getAMIdentity(model, defaultValue);
            }
        }
        super.beginDisplay(event);

        String realmName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);

        // initialize the 'Filter' drop down menu
        // supportedEntityTypes is a map of entity name to its
        // localized value
        CCDropDownMenu menu = (CCDropDownMenu)getChild(FILTER_TYPE);
        Map supportedEntityTypes = model.getSupportedEntityTypes(realmName);
        OptionList entityTypes = createOptionList(supportedEntityTypes);
        entityTypes.add(0, "policy.subject.select.identity.type", "");
        menu.setOptions(entityTypes);
        menu.setValue("");

        // initialize the available/selected component
        CCAddRemove child = (CCAddRemove)getChild(
            VALUES_MULTIPLE_CHOICE_VALUE);
        child.restoreStateData();
        OptionList selected = addRemoveModel.getSelectedOptionList();
        OptionList possible = createOptionList(
            getPossibleValues(model, realmName));
        child.resetStateData();
        addRemoveModel.setAvailableOptionList(possible);
        List selectedIds = AMAdminUtils.toList(selected);
        addRemoveModel.setSelectedOptionList(
            createOptionList(getAMIdentity(model, selectedIds)));
    }

    /**
     * Returns a set of supported AMIdentity objects for a realm.
     */
    private Set getPossibleValues(IdentitySubjectModel model, String realmName){
        Set possibleValues = null;
        String entityType = (String)getPageSessionAttribute(ENTITY_TYPE);
        if ((entityType != null) && (entityType.length() > 0)) {
            String pattern = (String)propertySheetModel.getValue(FILTER);

            try {
                IdSearchResults results = model.getEntityNames(
                    realmName, entityType, pattern);
                int errorCode = results.getErrorCode();
                                                                                
                switch (errorCode) {
                case IdSearchResults.SIZE_LIMIT_EXCEEDED:
                    setInlineAlertMessage(CCAlert.TYPE_WARNING,
                        "message.warning", "message.sizelimit.exceeded");
                    break;
                case IdSearchResults.TIME_LIMIT_EXCEEDED:
                    setInlineAlertMessage(CCAlert.TYPE_WARNING,
                        "message.warning", "message.timelimit.exceeded");
                    break;
                }
                
                possibleValues = results.getSearchResults();
                if ((possibleValues != null) && !possibleValues.isEmpty()) {                    
                    // remove the system users which should not be displayed.
                    Set hiddenUsers = model.getSpecialUsers(realmName);
                    possibleValues.removeAll(hiddenUsers);                    
                
                    // remove the identities that are already selected
                    Set selected = getValues(
                        addRemoveModel.getSelectedOptionList());
                    if ((selected != null) && !selected.isEmpty()) {
                        Set amids = getAMIdentity(model, selected);
                        possibleValues.removeAll(amids);
                    }
                }
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
            }
        }

        return (possibleValues != null) ?
            possibleValues : Collections.EMPTY_SET;
    }

    private Set getAMIdentity(IdentitySubjectModel model, Collection ids) {
        Set values = new HashSet(ids.size()*2);
        SSOToken token = model.getUserSSOToken();

        for (Iterator i = ids.iterator(); i.hasNext(); ) {
            String id = (String)i.next();
            try {
                AMIdentity amid = IdUtils.getIdentity(token, id);
                values.add(amid);
            } catch(IdRepoException e) {
                debug.warning("IdentitySubjectAddViewBean.getAMIdentity", e);
            }
        }

        return values;
    }

    protected Set getValues(String subjectType)
        throws ModelControlException {
        CCAddRemove child = (CCAddRemove)getChild(
            VALUES_MULTIPLE_CHOICE_VALUE);
        child.restoreStateData();
        Set values = getValues(addRemoveModel.getSelectedOptionList());

        if ((values == null) || values.isEmpty()) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                "policy.missing.subject.value");
            values = null;
        }

        return values;
    }

    /**
     * Creates an OptionList based on a set of AMIdentity objects.
     */
    protected OptionList createOptionList(Set values) {
        OptionList optList = new OptionList();

        if ((values != null) && !values.isEmpty()) {
            /*
             * Need to convert to AMIdentity object if the set contains
             * universal Ids
             */
            Set amIdentity = (values.iterator().next() instanceof String)
                ? getAMIdentity((IdentitySubjectModel)getModel(), values)
                : values;
            Map entries = new HashMap(values.size()*2);

            for (Iterator iter = amIdentity.iterator(); iter.hasNext(); ) {
                AMIdentity identity = (AMIdentity)iter.next();
                entries.put(IdUtils.getUniversalId(identity),
                    PolicyUtils.getDNDisplayString(identity.getName()));
            }
            optList = createOptionList(entries);
        }

        return optList;
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new IdentitySubjectModelImpl(req, getPageSessionAttributes());
    }

    /**
     * Handles filter results request.
     *
     * @param event Request invocation event.
     */
    public void handleBtnFilterRequest(RequestInvocationEvent event) {
        CCDropDownMenu menu = (CCDropDownMenu)getChild(FILTER_TYPE);
        setPageSessionAttribute(ENTITY_TYPE, (String)menu.getValue());
        super.handleBtnFilterRequest(event);
    }

    public boolean beginChildDisplay(ChildDisplayEvent event) {
        // do nothing, shortcircuit the implementation from parent class.
        return true;
    }

    protected Set getValidValues() {
        // Do not go to plugins to get valid values
        return Collections.EMPTY_SET;
    }
}
